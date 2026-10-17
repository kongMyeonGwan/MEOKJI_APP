package com.meokji.api.recommend.service;

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

import com.meokji.api.recipe.vo.PriceOfferVO;
import com.meokji.api.recommend.vo.BargainVO;
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
 *
 * <p>왜 요청할 때 훑지 않나
 *
 * <p>1분을 기다리게 할 수 없다. 화면이 멈춰 있고, 그 사이에 다른 사람이
 * 또 부르면 101번이 두 벌 나간다. KAMIS 는 너무 자주 부르면 막는다.
 *
 * <p>왜 결과를 기억하나
 *
 * <p>시세는 하루에 한 번 조사된 값이다. 3시간마다 다시 봐도 같은 값이 온다.
 * 매번 훑는 것은 KAMIS 쪽에도, 우리 쪽에도 낭비다.
 *
 * <p>왜 파일에도 저장하나
 *
 * <p>메모리에만 두면 서버를 다시 켤 때마다 101개를 처음부터 훑는다.
 * 배포할 때마다 1분 동안 이 카드가 비어 보인다. 오늘 날짜로 저장해 두면
 * 다시 켜도 읽어서 바로 쓴다.
 *
 * <p>값이 "없는 상태" 를 세 가지로 다룬다
 *   lastRows == null : 아직 한 번도 못 받았다 (빈 목록을 돌려주고 뒤에서 훑는다)
 *   lastRows 가 오래됐다 : 지금 가진 것을 돌려주고 뒤에서 다시 훑는다
 *   훑었는데 하나도 못 받았다 : 예전 결과를 그대로 둔다 (통신 문제일 수 있다)
 */
@Service
public class BargainServiceImpl implements BargainService {

    private static final double MIN_DROP_PERCENT = 5.0; // 5% 이상 내려야 "싸다" 고 본다
    private static final int SHOW_COUNT = 8; // 화면에 보여줄 최대 개수
    private static final long KEEP_MILLIS = 3 * 60 * 60 * 1000L; // 결과를 기억하는 시간 (3시간)
    // 3 * 60 * 60 * 1000 으로 적은 이유 — 10800000 이라고 쓰면 그게 3시간인지
    // 눈으로 세어봐야 한다. 끝의 L 은 int 한계(약 21억)를 넘지 않게 하는 표시다

    @Autowired
    private IngredientMatcher ingredientMatcher;

    @Autowired
    private KamisClient kamisClient;

    @Value("${app.price.service-key}")
    private String serviceKey; // 시세 API 인증키

    @Value("${app.price.warm-bargains}")
    private boolean warmBargains; // 서버가 켜질 때 미리 훑을지
    // 개발할 때는 꺼둔다. 서버를 자주 다시 켜는데 매번 1분씩 API 를 두드리면
    // 호출 제한에 걸린다

    @Value("${app.price.bargain-file}")
    private String bargainFile; // 결과를 저장할 파일

    // 마지막으로 훑은 결과 (내린 것 · 오른 것 모두, 많이 내린 순)
    //
    // 오른 것까지 담아두는 이유 — 기준(5%)을 바꾸고 싶을 때 다시 훑지 않아도
    // 된다. 걸러내는 것은 pickCheap 이 돌려줄 때 한다
    private List<BargainVO> lastRows = null;
    private long lastSavedAt = 0; // 훑은 시각

    private boolean scanning = false; // 지금 훑는 중인지
    // 이 표시가 없으면 요청이 올 때마다 스레드가 하나씩 생겨서,
    // 101번짜리 훑기가 여러 벌 동시에 돈다

    /**
     * 지금 가진 결과를 바로 돌려준다. 오래됐으면 뒤에서 다시 훑기 시작한다.
     *
     * 이 메서드는 빠르다. API 를 부르지 않고 메모리(또는 파일)에 있는 것만 본다.
     *
     * {@code @Transactional} 이 없다. DB 를 쓰지 않는다.
     */
    @Override
    public List<BargainVO> current() {
        // 1. 서버를 켠 직후라 기억한 게 없으면, 오늘 저장해둔 파일을 읽어본다
        if (lastRows == null) {
            List<BargainVO> saved = readFile();
            if (saved != null) {
                lastRows = saved;
                // 파일을 읽은 시각을 "훑은 시각" 으로 잡는다. 실제로 훑은
                // 시각은 모르지만, 파일은 오늘 것만 읽으므로 3시간 안에
                // 다시 훑게 되어도 손해가 크지 않다
                lastSavedAt = System.currentTimeMillis();
            }
        }

        // 2. 기억한 게 없거나 3시간이 지났으면 뒤에서 다시 훑는다
        //
        // 기다리지 않는다. 훑기가 끝나면 다음 요청이 새 결과를 받는다
        if (lastRows == null || System.currentTimeMillis() - lastSavedAt >= KEEP_MILLIS) {
            startScan();
        }

        // 3. 지금 가진 것 중 많이 내린 것만 골라서 돌려준다
        if (lastRows == null) {
            // 아직 아무것도 없다. null 이 아니라 빈 목록을 준다 -
            // 화면이 bargains.map 을 바로 쓸 수 있고, 카드도
            // 비어 있으면 아예 안 그린다
            return new ArrayList<>();
        }
        return pickCheap(lastRows);
    }

    /**
     * 서버가 다 켜지면 한 번 미리 훑어둔다 (설정이 켜져 있을 때만)
     *
     * {@code @PostConstruct} 가 아니라 ApplicationReadyEvent 를 쓰는 이유 —
     * PostConstruct 는 이 빈이 만들어진 직후에 불리는데, 그때는 다른 빈이
     * 아직 다 준비되지 않았을 수 있다. 이 이벤트는 서버가 요청을 받을
     * 준비를 마친 뒤에 온다.
     */
    @EventListener(ApplicationReadyEvent.class)
    public void warmUp() {
        if (warmBargains) {
            startScan();
        }
    }

    // 뒤에서 훑기 시작. 이미 훑는 중이면 아무것도 안 한다.
    // synchronized : 여러 요청이 동시에 와도 이 메서드는 한 번에 하나씩만 실행된다
    //
    // synchronized 가 필요한 이유 — 두 요청이 거의 같은 순간에 들어오면
    // 둘 다 scanning 이 false 인 것을 보고 스레드를 하나씩 만든다.
    // 한 번에 하나만 들어오게 막으면 둘째는 scanning == true 를 본다
    private synchronized void startScan() {
        if (serviceKey.isBlank() || scanning) {
            return; // 인증키가 없거나 이미 훑는 중
            // 인증키가 없으면 불러도 소용없다. 에러를 던지지 않는 이유는
            // 이 기능이 없어도 추천은 돌아가야 하기 때문이다
        }
        scanning = true;

        // 새 스레드를 만들어서 scan() 을 돌린다 (요청한 사람은 기다리지 않는다)
        //
        // try-finally 로 감싸는 이유 — scan 이 예외로 끝나도 scanning 을
        // false 로 되돌려야 한다. 안 되돌리면 "영원히 훑는 중" 이 되어
        // 다시 훑을 길이 막힌다
        Thread thread = new Thread(() -> {
            try {
                scan();
            } finally {
                scanning = false;
            }
        });
        thread.setDaemon(true); // 서버를 끌 때 이 스레드를 기다리지 않는다
        // 데몬이 아니면 1분짜리 훑기가 끝날 때까지 서버가 안 꺼진다
        thread.start();
    }

    // 품목을 하나씩 훑어서 결과를 저장한다
    //
    // 이 메서드는 별도 스레드에서 돈다. 1분 가까이 걸린다
    private void scan() {
        List<BargainVO> rows = new ArrayList<>();

        // 한꺼번에 묻지 않고 하나씩 도는 이유 — KAMIS API 는 품목 하나씩만
        // 받는다. 동시에 여러 개를 부르면 호출 제한에 걸린다
        for (String name : ingredientMatcher.uniqueItemNames()) {
            BargainVO item = fetchOne(name);
            // 값을 못 받은 품목은 건너뛴다. 101개 중 몇 개가 비는 것은
            // 흔한 일이고, 하나 때문에 전체를 버릴 이유가 없다
            if (item != null) {
                rows.add(item);
            }
        }

        // 하나도 못 받았으면 (통신이 막힌 경우) 예전 결과를 그대로 둔다
        //
        // 빈 목록으로 덮어쓰면 카드가 통째로 사라진다. 어제 값이라도
        // 보여주는 쪽이 낫다 - 조사일자(surveyedAt)를 같이 보내므로
        // 사용자는 언제 값인지 알 수 있다
        if (rows.isEmpty()) {
            System.out.println("[시세] 훑었지만 값이 하나도 없습니다. 이전 결과를 그대로 둡니다.");
            return;
        }

        // 많이 내린 순으로 정렬 (percent 가 작을수록 많이 내린 것. 같으면 이름순)
        //
        // 같을 때 이름순으로 또 가리는 이유 — 기준이 하나뿐이면 같은 값끼리의
        // 순서가 돌릴 때마다 달라진다. 에러는 아니지만 카드가 들썩거린다
        rows.sort((a, b) -> {
            if (a.getPercent() != b.getPercent()) {
                return Double.compare(a.getPercent(), b.getPercent());
            }
            return a.getName().compareTo(b.getName());
        });

        // 정렬까지 끝난 뒤에 한 번에 갈아끼운다. 중간에 lastRows 를 건드리면
        // 그 사이에 들어온 요청이 반쯤 채워진 목록을 본다
        lastRows = rows;
        lastSavedAt = System.currentTimeMillis();
        writeFile(rows);

        // 서버 로그에 남긴다. 1분짜리 작업이 끝났는지, 몇 개를 찾았는지를
        // 볼 곳이 없으면 "카드가 왜 비었나" 를 짐작만 하게 된다
        System.out.println("[시세] 값이 내린 재료 " + pickCheap(rows).size() + "개를 찾았습니다 (훑은 품목 " + rows.size() + "개).");
    }

    // 품목 하나의 시세와 등락률. 값이 없으면 null
    private BargainVO fetchOne(String name) {
        IngredientMatcher.Codes codes = ingredientMatcher.findCodes(name);
        if (codes == null) {
            return null;
        }

        // 최대 3번까지 시도한다 (API 가 잠깐 막혔을 수 있어서)
        //
        // 다시 시도하는 이유 — 101개를 연달아 부르면 중간에 몇 개가 빈
        // 응답으로 온다. 한 번에 포기하면 카드에 들어갈 품목이 뚝뚝 빠진다
        KamisClient.Quote quote = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                quote = kamisClient.fetch(codes);
            } catch (RuntimeException e) {
                return null;
                // 예외가 난 경우는 다시 시도하지 않는다. 인증키가 틀렸거나
                // 접속이 안 되는 것이라 다시 해도 같은 결과다
            }

            sleep(150); // 너무 빨리 연달아 부르면 API 가 막는다

            if (!quote.getOffers().isEmpty()) {
                break; // 값을 받았으면 끝
            }
            // 빈 응답이면 조금 더 기다렸다 다시 해본다.
            // 마지막 시도 뒤에는 기다릴 이유가 없으므로 if 로 가린다
            if (attempt < 3) {
                sleep(2000);
            }
        }

        // 세 번 해도 못 받았거나, 등락률이 없으면 쓸 수 없다.
        // trend 가 없으면 "얼마나 내렸는지" 를 알 수 없어서 이 기능에 못 쓴다
        if (quote == null || quote.getOffers().isEmpty() || quote.getTrend() == null) {
            return null;
        }

        PriceOfferVO cheapest = quote.getOffers().get(0); // 가장 싼 가격
        // offers 는 싼 순서로 와서 첫 번째가 최저가다
        BargainVO item = new BargainVO();
        item.setName(name);
        // 소수점 한 자리로 자른다. 12.456% 를 그대로 보내면 화면에 길게 나오고,
        // 정수로 반올림하면 5% 기준선 근처에서 목록과 표시가 어긋난다
        item.setPercent(Numbers.round(quote.getTrend().path("percent").asDouble(), 1));
        item.setPrice(cheapest.getPrice());
        item.setUnit(cheapest.getUnit());
        item.setSurveyedAt(cheapest.getSurveyedAt());
        return item;
    }

    // 5% 이상 내린 것만, 최대 8개
    //
    // 훑을 때 걸러내지 않고 돌려줄 때 거르는 이유 — lastRows 에는 오른 것까지
    // 다 담아 두었다. 기준(5%)이나 개수(8개)를 바꿀 때 다시 훑지 않아도 된다
    private List<BargainVO> pickCheap(List<BargainVO> rows) {
        List<BargainVO> cheap = new ArrayList<>();
        for (BargainVO row : rows) {
            // 8개를 채우면 그만 본다. 목록이 많이 내린 순으로 정렬돼 있어서
            // 앞에서 8개를 집으면 그게 가장 많이 내린 8개다
            if (cheap.size() >= SHOW_COUNT) {
                break;
            }
            // percent 는 내렸을 때 음수다. "-5% 이하" 가 "5% 이상 내림" 이다
            if (row.getPercent() <= -MIN_DROP_PERCENT) {
                cheap.add(row);
            }
        }
        return cheap;
    }

    // 잠깐 멈춘다. API 를 너무 빨리 연달아 부르지 않기 위한 것이다.
    //
    // InterruptedException 을 그냥 삼키지 않고 interrupt() 를 다시 켜는 이유 —
    // "멈추라는 신호가 왔다" 는 사실을 지우면, 이 스레드를 멈추려는 쪽이
    // 영영 멈출 수 없다. 서버를 끌 때 그 신호가 온다
    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ===================== 파일에 저장 · 읽기 =====================
    // 서버를 다시 켤 때마다 101개를 훑지 않으려고 오늘 결과를 파일로 남겨둔다.
    //
    // 표에 넣지 않은 이유 — 사용자 자료가 아니고, 오늘 것만 쓰고 버리는 값이다.
    // 표로 두면 날마다 쓰레기가 쌓이고 지우는 일도 만들어야 한다.

    // 오늘 저장한 파일이 있으면 읽는다. 없거나 날짜가 다르면 null
    private List<BargainVO> readFile() {
        // 전체를 try 로 감싼다. 파일이 깨졌든 모양이 다르든, 못 읽으면
        // 그냥 다시 훑으면 된다. 이 기능 때문에 서버가 멈출 이유가 없다
        try {
            Path path = Path.of(bargainFile);
            if (!Files.exists(path)) {
                return null;
            }

            JsonNode saved = JsonMapper.builder().build().readTree(Files.readString(path, StandardCharsets.UTF_8));

            // 오늘 날짜가 아니면 버린다 (어제 시세로 "지금 싸요" 라고 하면 안 되니까)
            //
            // 저장할 때 날짜를 같이 적어두기 때문에 가릴 수 있다.
            // 파일의 수정 시각을 보지 않는 이유 — 파일을 옮기거나 복사하면
            // 그 시각이 바뀐다
            if (!LocalDate.now().toString().equals(saved.path("scanned_on").asString(""))) {
                return null;
            }

            List<BargainVO> rows = new ArrayList<>();
            for (JsonNode row : saved.path("rows")) {
                BargainVO item = new BargainVO();
                // path(...).asXxx(기본값) 으로 읽는다. 칸이 없어도 터지지 않는다
                item.setName(row.path("name").asString(""));
                item.setPercent(row.path("percent").asDouble());
                item.setPrice(row.path("price").asInt());
                item.setUnit(row.path("unit").asString(""));
                // 조사일자는 null 로 저장될 수 있다. asString("") 으로 읽으면
                // 빈 글자가 되는데, 그러면 화면이 "조사일자 없음" 과
                // 구별할 수 없어서 아예 안 채운다
                if (!row.path("surveyed_at").isNull()) {
                    item.setSurveyedAt(row.path("surveyed_at").asString(""));
                }
                rows.add(item);
            }

            // 빈 파일은 없는 것과 같다. 빈 목록을 돌려주면 "읽었다" 가 되어
            // 다시 훑지 않게 된다
            if (rows.isEmpty()) {
                return null;
            }
            return rows;
        } catch (Exception e) {
            // 클래스 이름만 남긴다. 파일 경로나 내용이 로그에 다 찍힐 이유가 없다
            System.out.println("[시세] 저장해둔 파일을 읽지 못했습니다: " + e.getClass().getSimpleName());
            return null;
        }
    }

    // 결과를 파일로 저장한다. 실패해도 괜찮다 (메모리에 기억한 것으로 돌아간다)
    private void writeFile(List<BargainVO> rows) {
        ObjectNode root = JsonMapper.builder().build().createObjectNode();
        // 날짜를 같이 적는다. 읽을 때 "오늘 것인지" 를 가리는 근거다
        root.put("scanned_on", LocalDate.now().toString());

        // putArray 는 칸을 만들면서 그 배열을 돌려준다. 따로 만들어
        // set 으로 넣는 것보다 한 줄 짧다
        ArrayNode array = root.putArray("rows");
        for (BargainVO row : rows) {
            ObjectNode one = array.addObject();
            one.put("name", row.getName());
            one.put("percent", row.getPercent());
            one.put("price", row.getPrice());
            one.put("unit", row.getUnit());
            one.put("surveyed_at", row.getSurveyedAt());
        }

        try {
            Path path = Path.of(bargainFile);
            // 폴더가 없으면 만든다. 처음 배포한 서버에는 data 폴더가 없다
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            // toPrettyString 으로 보기 좋게 적는다. 사람이 열어볼 수 있는
            // 파일이라 한 줄로 적어두면 확인할 수가 없다
            Files.writeString(path, root.toPrettyString(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            // 저장이 안 되면 다음에 다시 훑으면 된다. 예외를 올려보내면
            // 훑기 자체가 실패한 것처럼 되어 lastRows 까지 못 쓰게 된다
            System.out.println("[시세] 파일 저장 실패: " + e.getMessage());
        }
    }
}
