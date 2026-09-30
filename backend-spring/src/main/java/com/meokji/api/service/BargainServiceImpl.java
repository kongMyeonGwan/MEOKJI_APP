package com.meokji.api.service;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

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
 * 요즘 값이 내린 재료.
 *
 * 품목 101개의 시세를 하나씩 받아서 일주일 전보다 얼마나 내렸는지 본다.
 * 101번을 부르니까 1분 가까이 걸린다. 그래서
 *   - 요청이 오면 기다리게 하지 않고, 가지고 있는 결과를 바로 돌려준다 (처음엔 빈 목록)
 *   - 훑기는 "뒤에서" (별도 스레드) 돌린다
 *   - 훑은 결과는 3시간 동안 기억하고, 파일(data/bargains.json)에도 저장해둔다
 *
 * ※ 이 파일은 조금 어렵다. 팀원이 고칠 일은 거의 없는 영역이다.
 */
@Service
public class BargainServiceImpl implements BargainService {

    private static final double MIN_DROP_PERCENT = 5.0; // 5% 이상 내려야 "싸다" 고 본다
    private static final int SHOW_COUNT = 8; // 화면에 보여줄 최대 개수
    private static final long KEEP_MILLIS = 3 * 60 * 60 * 1000L; // 결과를 기억하는 시간 (3시간)

    @Autowired
    private IngredientMatcher ingredientMatcher;

    @Autowired
    private KamisClient kamisClient;

    @Value("${app.price.service-key}")
    private String serviceKey; // 시세 API 인증키

    @Value("${app.price.warm-bargains}")
    private boolean warmBargains; // 서버가 켜질 때 미리 훑을지

    @Value("${app.price.bargain-file}")
    private String bargainFile; // 결과를 저장할 파일

    // 마지막으로 훑은 결과 (내린 것 · 오른 것 모두, 많이 내린 순)
    private List<BargainItem> lastRows = null;
    private long lastSavedAt = 0; // 훑은 시각

    private boolean scanning = false; // 지금 훑는 중인지

    @Override
    public List<BargainItem> current() {
        // 1. 서버를 켠 직후라 기억한 게 없으면, 오늘 저장해둔 파일을 읽어본다
        if (lastRows == null) {
            List<BargainItem> saved = readFile();
            if (saved != null) {
                lastRows = saved;
                lastSavedAt = System.currentTimeMillis();
            }
        }

        // 2. 기억한 게 없거나 3시간이 지났으면 뒤에서 다시 훑는다
        if (lastRows == null || System.currentTimeMillis() - lastSavedAt >= KEEP_MILLIS) {
            startScan();
        }

        // 3. 지금 가진 것 중 많이 내린 것만 골라서 돌려준다
        if (lastRows == null) {
            return new ArrayList<>();
        }
        return pickCheap(lastRows);
    }

    // 서버가 다 켜지면 한 번 미리 훑어둔다 (설정이 켜져 있을 때만)
    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        if (warmBargains) {
            startScan();
        }
    }

    // 뒤에서 훑기 시작. 이미 훑는 중이면 아무것도 안 한다.
    // synchronized : 여러 요청이 동시에 와도 이 메서드는 한 번에 하나씩만 실행된다
    private synchronized void startScan() {
        if (serviceKey.isBlank() || scanning) {
            return; // 인증키가 없거나 이미 훑는 중
        }
        scanning = true;

        // 새 스레드를 만들어서 scan() 을 돌린다 (요청한 사람은 기다리지 않는다)
        Thread thread = new Thread(() -> {
            try {
                scan();
            } finally {
                scanning = false;
            }
        });
        thread.setDaemon(true); // 서버를 끌 때 이 스레드를 기다리지 않는다
        thread.start();
    }

    // 품목을 하나씩 훑어서 결과를 저장한다
    private void scan() {
        List<BargainItem> rows = new ArrayList<>();

        for (String name : ingredientMatcher.uniqueItemNames()) {
            BargainItem item = fetchOne(name);
            if (item != null) {
                rows.add(item);
            }
        }

        // 하나도 못 받았으면 (통신이 막힌 경우) 예전 결과를 그대로 둔다
        if (rows.isEmpty()) {
            System.out.println("[시세] 훑었지만 값이 하나도 없습니다. 이전 결과를 그대로 둡니다.");
            return;
        }

        // 많이 내린 순으로 정렬 (percent 가 작을수록 많이 내린 것. 같으면 이름순)
        rows.sort((a, b) -> {
            if (a.getPercent() != b.getPercent()) {
                return Double.compare(a.getPercent(), b.getPercent());
            }
            return a.getName().compareTo(b.getName());
        });

        lastRows = rows;
        lastSavedAt = System.currentTimeMillis();
        writeFile(rows);

        System.out.println("[시세] 값이 내린 재료 " + pickCheap(rows).size() + "개를 찾았습니다 (훑은 품목 " + rows.size() + "개).");
    }

    // 품목 하나의 시세와 등락률. 값이 없으면 null
    private BargainItem fetchOne(String name) {
        IngredientMatcher.Codes codes = ingredientMatcher.findCodes(name);
        if (codes == null) {
            return null;
        }

        // 최대 3번까지 시도한다 (API 가 잠깐 막혔을 수 있어서)
        KamisClient.Quote quote = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                quote = kamisClient.fetch(codes);
            } catch (RuntimeException e) {
                return null;
            }

            sleep(150); // 너무 빨리 연달아 부르면 API 가 막는다

            if (!quote.getOffers().isEmpty()) {
                break;
            }
            if (attempt < 3) {
                sleep(2000);
            }
        }

        if (quote == null || quote.getOffers().isEmpty() || quote.getTrend() == null) {
            return null;
        }

        PriceOffer cheapest = quote.getOffers().get(0); // 가장 싼 가격
        BargainItem item = new BargainItem();
        item.setName(name);
        item.setPercent(Numbers.round(quote.getTrend().path("percent").asDouble(), 1));
        item.setPrice(cheapest.getPrice());
        item.setUnit(cheapest.getUnit());
        item.setSurveyedAt(cheapest.getSurveyedAt());
        return item;
    }

    // 5% 이상 내린 것만, 최대 8개
    private List<BargainItem> pickCheap(List<BargainItem> rows) {
        List<BargainItem> cheap = new ArrayList<>();
        for (BargainItem row : rows) {
            if (cheap.size() >= SHOW_COUNT) {
                break;
            }
            if (row.getPercent() <= -MIN_DROP_PERCENT) {
                cheap.add(row);
            }
        }
        return cheap;
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ===================== 파일에 저장 · 읽기 =====================
    // 서버를 다시 켤 때마다 101개를 훑지 않으려고 오늘 결과를 파일로 남겨둔다.

    // 오늘 저장한 파일이 있으면 읽는다. 없거나 날짜가 다르면 null
    private List<BargainItem> readFile() {
        try {
            Path path = Path.of(bargainFile);
            if (!Files.exists(path)) {
                return null;
            }

            JsonNode saved = JsonMapper.builder().build().readTree(Files.readString(path, StandardCharsets.UTF_8));

            // 오늘 날짜가 아니면 버린다 (어제 시세로 "지금 싸요" 라고 하면 안 되니까)
            if (!LocalDate.now().toString().equals(saved.path("scanned_on").asString(""))) {
                return null;
            }

            List<BargainItem> rows = new ArrayList<>();
            for (JsonNode row : saved.path("rows")) {
                BargainItem item = new BargainItem();
                item.setName(row.path("name").asString(""));
                item.setPercent(row.path("percent").asDouble());
                item.setPrice(row.path("price").asInt());
                item.setUnit(row.path("unit").asString(""));
                if (!row.path("surveyed_at").isNull()) {
                    item.setSurveyedAt(row.path("surveyed_at").asString(""));
                }
                rows.add(item);
            }

            if (rows.isEmpty()) {
                return null;
            }
            return rows;
        } catch (Exception e) {
            System.out.println("[시세] 저장해둔 파일을 읽지 못했습니다: " + e.getClass().getSimpleName());
            return null;
        }
    }

    // 결과를 파일로 저장한다. 실패해도 괜찮다 (메모리에 기억한 것으로 돌아간다)
    private void writeFile(List<BargainItem> rows) {
        ObjectNode root = JsonMapper.builder().build().createObjectNode();
        root.put("scanned_on", LocalDate.now().toString());

        ArrayNode array = root.putArray("rows");
        for (BargainItem row : rows) {
            ObjectNode one = array.addObject();
            one.put("name", row.getName());
            one.put("percent", row.getPercent());
            one.put("price", row.getPrice());
            one.put("unit", row.getUnit());
            one.put("surveyed_at", row.getSurveyedAt());
        }

        try {
            Path path = Path.of(bargainFile);
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            Files.writeString(path, root.toPrettyString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            System.out.println("[시세] 파일 저장 실패: " + e.getMessage());
        }
    }
}
