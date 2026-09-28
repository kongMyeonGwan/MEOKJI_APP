package com.meokji.api.service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.meokji.api.config.AppProperties;
import com.meokji.api.dto.recipe.PriceOffer;
import com.meokji.api.dto.recommend.BargainItem;
import com.meokji.api.util.IngredientMatcher;
import com.meokji.api.util.KamisClient;
import com.meokji.api.util.Numbers;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * 요즘 값이 내린 재료를 찾는다 (STEP 17~19 연장).
 *
 * <p>파이썬 쪽 {@code app/services/bargain_service.py} 자리다. KAMIS 품목을 전부
 * 훑어 일주일 전 가격과 견주고, 많이 내린 것만 골라 화면에 넘긴다. 대시보드의
 * 「지금 싼 재료로」 카드가 이 값으로 "대파 12%↓" 칩을 그린다
 * ({@code frontend/src/components/recommendation/BargainPick.jsx}).
 *
 * <h2>파이썬과 달라진 곳 — 부르는 쪽을 기다리게 하지 않는다</h2>
 *
 * <p>파이썬은 기억해둔 것이 없으면 <b>훑기가 끝날 때까지 기다렸다가</b> 돌려준다.
 * 비동기 서버라 그동안 다른 요청을 받을 수 있어서 가능한 방식이다.
 *
 * <p>여기서는 그렇게 하지 않는다. 훑기는 101번을 부르는 일이라 1분 가까이 걸리고,
 * 그동안 톰캣 작업 스레드 하나가 통째로 묶인다. 먼저 온 사람은 어차피 브라우저
 * 쪽에서 먼저 끊긴다. 그래서 <b>가진 것을 바로 돌려주고 훑기는 뒤에서 돌린다</b> —
 * 첫 요청은 빈 목록을 받고, 훑기가 끝난 뒤의 요청부터 값이 나온다.
 *
 * <p>화면은 빈 목록을 이미 견딘다. 칩이 없으면 안 그리고 메뉴만 보여준다.
 * 서버가 뜰 때 미리 한 번 훑어두면({@link #warmUp}) 빈 목록을 보는 일은 드물다.
 */
@Service
public class BargainService {

    /** 동시에 몇 개까지 물어볼지. 초당 제한을 넘으면 한동안 전부 막힌다. */
    private static final int SCAN_CONCURRENCY = 2;

    /** 요청 사이에 쉬는 시간(밀리초). */
    private static final long SCAN_PAUSE_MILLIS = 150;

    /** 값이 안 나온 품목을 다시 물어볼 횟수와 쉬는 시간. 제한은 잠깐이라 대개 들어온다. */
    private static final int SCAN_RETRIES = 2;
    private static final long SCAN_RETRY_WAIT_MILLIS = 2_000;

    /** 훑은 결과를 기억해두는 시간. 조사값은 하루 한 번 갱신된다. */
    private static final long CACHE_MILLIS = 3 * 60 * 60 * 1000L;

    /**
     * "쌈" 이라고 부를 최소 하락폭(%).
     *
     * <p>3~4% 는 장 볼 때 체감되지 않는다. 배지를 붙일 만한 폭만 고른다.
     * {@link KamisClient} 도 같은 값으로 추이를 걸러낸다 — 그보다 작게 움직인
     * 품목은 애초에 {@code trend} 가 비어서 여기까지 오지 않는다.
     */
    private static final double MIN_DROP_PERCENT = 5.0;

    /**
     * 화면에 넘길 기본 개수.
     *
     * <p>너무 많으면 칩이 줄바꿈을 두 번 하고, 무엇이 싼지가 오히려 안 읽힌다.
     */
    private static final int DEFAULT_LIMIT = 8;

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Autowired
    private IngredientMatcher matcher;
    @Autowired
    private KamisClient kamis;
    @Autowired
    private AppProperties app;

    /**
     * 마지막으로 훑은 결과. 내린 것과 오른 것이 <b>모두</b> 등락률 순으로 들어 있다.
     *
     * <p>자르는 것은 꺼내 쓰는 쪽({@link #pickCheap})이 한다. 오른 것도 남겨두면
     * 나중에 화면이 "오늘은 배추가 비싸요" 까지 말할 수 있다.
     */
    private volatile Snapshot snapshot;

    /** 훑기는 한 번에 하나만. 여럿이 동시에 돌면 제한에 걸려 양쪽 다 절반만 받아온다. */
    private final AtomicBoolean scanning = new AtomicBoolean(false);

    private record Snapshot(long savedAt, List<BargainItem> rows) {
    }


    /**
     * 값이 내린 재료 목록.
     *
     * <p>바로 돌려준다. 가진 것이 없거나 오래됐으면 훑기를 뒤에서 시작해두고,
     * 지금은 가진 것(없으면 빈 목록)을 준다.
     */
    public List<BargainItem> current() {
        Snapshot kept = snapshot;

        if (kept == null) {
            // 서버를 다시 켠 직후. 오늘 훑어둔 파일이 있으면 그걸 쓴다.
            kept = readFile();
            if (kept != null) {
                snapshot = kept;
            }
        }

        if (kept == null || isStale(kept)) {
            refreshInBackground();
        }

        return kept == null ? List.of() : pickCheap(kept.rows(), DEFAULT_LIMIT);
    }

    /**
     * 서버가 뜰 때 미리 한 번 훑어둔다.
     *
     * <p>기본값은 <b>꺼짐</b>이다(파이썬은 늘 켠다). 켜두면 {@code gradlew test} 를
     * 돌릴 때마다 바깥 API 를 101번 두드리게 되고, 테스트가 바깥 사정에 따라
     * 느려지거나 제한에 걸린다. 실제로 띄울 때만 {@code WARM_BARGAINS=true} 로 켠다.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        if (!app.getPrice().isWarmBargains()) {
            return;
        }

        refreshInBackground();
    }

    /** 훑기를 뒤에서 시작한다. 이미 돌고 있으면 아무것도 하지 않는다. */
    private void refreshInBackground() {
        if (app.getPrice().getServiceKey().isBlank()) {
            // 인증키가 없으면 훑어봐야 전부 빈 값이다. 레시피 쪽은 503 을 내지만
            // 여기서는 화면이 칩만 안 그리면 되므로 조용히 넘어간다.
            return;
        }

        if (!scanning.compareAndSet(false, true)) {
            return;
        }

        Thread worker = new Thread(this::scanAndStore, "bargain-scan");
        // 데몬으로 둔다. 훑는 중에 서버를 내릴 때 1분을 더 기다리게 하지 않는다.
        worker.setDaemon(true);
        worker.start();
    }

    private void scanAndStore() {
        try {
            List<BargainItem> rows = scan();

            // 한 건도 못 건졌으면 가진 것을 버리지 않는다. 통신이 잠깐 막혔을 때
            // 어제 값이라도 보여주는 쪽이 낫다.
            if (rows.isEmpty()) {
                System.out.println("[시세] 훑었지만 값이 하나도 없습니다. 이전 결과를 그대로 둡니다.");
                return;
            }

            snapshot = new Snapshot(System.currentTimeMillis(), rows);
            writeFile(rows);

            System.out.printf("[시세] 값이 내린 재료 %d개를 찾았습니다 (훑은 품목 %d개).%n",
                    pickCheap(rows, DEFAULT_LIMIT).size(), rows.size());
        } catch (Exception exc) {
            System.out.println("[시세] 훑기 실패: " + exc.getClass().getSimpleName()
                    + ": " + exc.getMessage());
        } finally {
            scanning.set(false);
        }
    }

    /** 모든 품목의 가격 흐름을 훑어 값이 내린 순으로 돌려준다. */
    private List<BargainItem> scan() {
        List<String> names = matcher.uniqueItemNames();

        ExecutorService pool = Executors.newFixedThreadPool(SCAN_CONCURRENCY, runnable -> {
            Thread thread = new Thread(runnable, "bargain-fetch");
            thread.setDaemon(true);
            return thread;
        });

        List<BargainItem> found = new ArrayList<>();

        try {
            List<Callable<BargainItem>> tasks = new ArrayList<>();
            for (String name : names) {
                tasks.add(() -> fetchOne(name));
            }

            for (Future<BargainItem> future : pool.invokeAll(tasks)) {
                try {
                    BargainItem item = future.get();
                    if (item != null) {
                        found.add(item);
                    }
                } catch (Exception exc) {
                    // 품목 하나가 실패해도 나머지는 살린다.
                    System.out.println("[시세 훑기 실패] " + exc.getClass().getSimpleName());
                }
            }
        } catch (InterruptedException exc) {
            Thread.currentThread().interrupt();
            return List.of();
        } finally {
            pool.shutdownNow();
        }

        // 많이 내린 것부터. 같으면 이름순으로 정해서 매번 같은 답이 나오게 한다.
        found.sort(Comparator.comparingDouble(BargainItem::percent)
                .thenComparing(BargainItem::name));

        return List.copyOf(found);
    }

    /** 품목 하나. 조사가격이나 추이가 없으면 null (대개 가공식품이거나 제한에 걸린 것). */
    private BargainItem fetchOne(String name) {
        IngredientMatcher.Codes codes = matcher.findCodes(name);

        if (codes == null) {
            return null;
        }

        KamisClient.Quote quote = KamisClient.Quote.empty();

        for (int attempt = 0; attempt <= SCAN_RETRIES; attempt++) {
            try {
                quote = kamis.fetch(codes);
            } catch (RuntimeException exc) {
                // 인증키가 비면 IllegalStateException 이 온다. 훑기를 통째로
                // 멈출 일은 아니라 이 품목만 건너뛴다.
                return null;
            }

            // 다음 요청을 바로 던지지 않는다. 초당 제한을 넘으면 한동안 전부
            // 막혀서 오히려 더 오래 걸린다.
            sleep(SCAN_PAUSE_MILLIS);

            if (!quote.offers().isEmpty()) {
                break;
            }

            // 값이 없는 품목(가공식품)인지 제한에 걸린 것인지 여기서는 구분되지
            // 않는다. 그래서 몇 번만 더 물어보고 그만둔다.
            if (attempt < SCAN_RETRIES) {
                sleep(SCAN_RETRY_WAIT_MILLIS);
            }
        }

        if (quote.offers().isEmpty() || quote.trend() == null) {
            return null;
        }

        // offers 는 KamisClient 가 이미 싼 순으로 정렬해 둔다.
        PriceOffer cheapest = quote.offers().get(0);
        JsonNode trend = quote.trend();

        return new BargainItem(
                name,
                Numbers.round(trend.path("percent").asDouble(), 1),
                cheapest.price(),
                cheapest.unit(),
                cheapest.surveyedAt());
    }

    /** 많이 내린 재료만 골라낸다. 오른 것과 조금 움직인 것은 뺀다. */
    private static List<BargainItem> pickCheap(List<BargainItem> rows, int limit) {
        List<BargainItem> cheap = new ArrayList<>();

        for (BargainItem row : rows) {
            if (cheap.size() >= limit) {
                break;
            }
            if (row.percent() <= -MIN_DROP_PERCENT) {
                cheap.add(row);
            }
        }

        return List.copyOf(cheap);
    }

    private boolean isStale(Snapshot kept) {
        return System.currentTimeMillis() - kept.savedAt() >= CACHE_MILLIS;
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException exc) {
            Thread.currentThread().interrupt();
        }
    }

    // --------------------------------------------------------------- 파일 캐시

    /**
     * 오늘 훑어둔 파일이 있으면 읽는다.
     *
     * <p>서버를 다시 켤 때마다 101개를 훑지 않기 위함이다. 개발 중에는 서버를
     * 자주 내렸다 올리는데, 그때마다 1분씩 바깥 API 를 두드릴 이유가 없다.
     */
    private Snapshot readFile() {
        Path path = bargainFile();

        try {
            if (!Files.exists(path)) {
                return null;
            }

            JsonNode saved = MAPPER.readTree(Files.readString(path, StandardCharsets.UTF_8));

            // 날짜가 오늘이 아니면 버린다. 어제 시세로 "지금 쌉니다" 라고 하면 안 된다.
            if (!LocalDate.now().toString().equals(saved.path("scanned_on").asString(""))) {
                return null;
            }

            List<BargainItem> rows = new ArrayList<>();

            for (JsonNode row : saved.path("rows")) {
                JsonNode surveyed = row.path("surveyed_at");

                rows.add(new BargainItem(
                        row.path("name").asString(""),
                        row.path("percent").asDouble(),
                        row.path("price").asInt(),
                        row.path("unit").asString(""),
                        surveyed.isNull() ? null : surveyed.asString("")));
            }

            if (rows.isEmpty()) {
                return null;
            }

            // 파일에 적힌 시각이 아니라 읽은 시각을 쓴다. 오늘 아침에 훑어둔
            // 파일이면 3시간 뒤에 한 번 더 훑게 된다 — 조사값이 갱신됐을 수 있다.
            return new Snapshot(System.currentTimeMillis(), List.copyOf(rows));
        } catch (Exception exc) {
            // 파일이 깨졌으면 없는 것으로 본다. 다시 훑으면 채워진다.
            System.out.println("[시세] 저장해둔 파일을 읽지 못했습니다: "
                    + exc.getClass().getSimpleName());
            return null;
        }
    }

    /** 훑은 결과를 남긴다. 실패해도 넘어간다 — 메모리에 기억해둔 것만으로도 돌아간다. */
    private void writeFile(List<BargainItem> rows) {
        Path path = bargainFile();

        ObjectNode root = MAPPER.createObjectNode();
        root.put("scanned_on", LocalDate.now().toString());

        ArrayNode array = root.putArray("rows");

        for (BargainItem row : rows) {
            ObjectNode one = array.addObject();
            one.put("name", row.name());
            one.put("percent", row.percent());
            one.put("price", row.price());
            one.put("unit", row.unit());
            one.put("surveyed_at", row.surveyedAt());
        }

        try {
            Path parent = path.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.writeString(path, root.toPrettyString(), StandardCharsets.UTF_8);
        } catch (IOException exc) {
            System.out.println("[시세] 파일 저장 실패: " + exc.getMessage());
        }
    }

    private Path bargainFile() {
        return Path.of(app.getPrice().getBargainFile());
    }
}
