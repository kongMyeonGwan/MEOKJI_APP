package com.meokji.api.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.meokji.api.recipe.vo.PriceOfferVO;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * KAMIS 소매 조사가격을 받아온다.
 *
 * 쇼핑몰 최저가가 아니다. 소매시장 조사가격이라 구매 링크가 없고 품목마다
 * 단위가 다르다(쌀 20kg / 깻잎 100g). 그래서 화면에 단위와 조사일자를 같이 적는다.
 *
 * 값을 지어내지 않는다. KAMIS 에 없는 재료면 빈 목록을 돌려주고, 화면이
 * '가격 정보 없음' 으로 표시한다 - 전에 값을 지어내던 mock 이 진짜 조사가격과
 * 구분되지 않은 채 합계까지 계산돼서 없앴다.
 *
 * 응답을 세 번 걸러서 쓴다
 *
 *   1. sameUnitRows : 단위가 같은 행만 남긴다 (섞이면 최저/최고가가 무의미해진다)
 *   2. priceTrend   : 그 행들로 일주일 전과 견준다
 *   3. latestRows   : 가장 최근 조사일자의 행만 남겨 시장별 가격으로 쓴다
 *
 * 1번이 먼저인 것이 중요하다. 단위를 섞은 채로 추이를 계산하면 가격이
 * 오른 것인지 단위가 바뀐 것인지 알 수 없다.
 *
 * 실패를 두 가지로 다룬다
 *
 *   인증키가 없다        -&gt; 예외를 던진다. 사람이 설정을 고쳐야 한다
 *   그 밖의 모든 실패    -&gt; 빈 목록 + 로그. 재료 하나 때문에 레시피 화면이
 *                           멈추면 안 된다
 *
 * static 메서드가 많은 이유 — 응답을 거르는 일들은 받은 값만 가지고 한다.
 * 그래서 테스트가 빈 없이 JSON 을 넣어 확인할 수 있다(package-private).
 */
@Component
public class KamisClient {

    private static final String URL = "https://apis.data.go.kr/B552845/perDay/price";

    /** 구분코드 01=소매. 도매 단가(10kg 상자 등)와 섞으면 최저/최고가 뒤죽박죽이 된다. */
    private static final String RETAIL_CODE = "01";

    /** 한 번에 받아올 최대 행 수(명세서 max 1000). 품목 하나가 시장×품종×등급×날짜로 쪼개진다. */
    // 최대로 받는 이유 — 일주일치를 한 번에 받아야 추이를 계산할 수 있다.
    // 페이지를 나눠 받으면 같은 품목에 요청이 여러 번 나간다
    private static final int ROWS = 1000;

    /** 조사일자는 필수 파라미터인데 주말·공휴일은 조사가 없다. 며칠 거슬러 본다. */
    // 7일로 둔 이유가 둘이다. 연휴가 길어도 한 번은 조사일이 끼고,
    // "일주일 전과 견준" 추이를 그 범위에서 바로 구할 수 있다
    private static final int LOOKBACK_DAYS = 7;

    /** 응답에서 실제로 쓰는 컬럼만 받는다. 행 수는 그대로지만 응답이 가벼워진다. */
    private static final String COLUMNS =
            "exmn_ymd,sgg_nm,mrkt_nm,unit,unit_sz,exmn_dd_prc,grd_nm";

    /** 사용자가 값을 직접 확인할 수 있는 곳. 상품 페이지가 아니라 조사가격 페이지다. */
    private static final String REFERENCE_URL =
            "https://www.kamis.or.kr/customer/price/retail/item.do";

    /** 조사값은 하루 한 번 갱신된다. 3시간이면 넉넉하다. */
    private static final long CACHE_MILLIS = 3 * 60 * 60 * 1000L;

    private static final int CACHE_MAX_ENTRIES = 300;

    /** 추이를 화면에 알릴 최소 변동폭(%). 이보다 작으면 흔들림이다. */
    // 시세는 날마다 1~2% 씩 움직인다. 그것까지 "올랐다/내렸다" 로 알리면
    // 모든 재료에 화살표가 붙어 뜻을 잃는다.
    //
    // BargainServiceImpl 의 MIN_DROP_PERCENT 와 같은 5.0 이다. 두 값이
    // 어긋나면 "싼 재료" 목록에는 있는데 추이는 안 뜨는 품목이 생긴다
    private static final double TREND_MIN_PERCENT = 5.0;

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    /** 한 품목의 조사가격과 추이. 링크는 재료 이름에 따라 달라서 여기 담지 않는다. */
    // 같은 품목코드라도 부른 이름이 다를 수 있다("마늘" / "다진 마늘").
    // 링크는 이름으로 만들므로, 캐시에 담기는 이 객체에 넣으면
    // 먼저 물어본 사람의 이름으로 굳는다
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Quote {

        private List<PriceOfferVO> offers;

        private JsonNode trend;

        // 빈 결과를 만드는 자리가 여러 곳이라 메서드로 뺐다.
        // List.of() 는 빈 목록이다 - null 이 아니라 빈 목록을 주면
        // 받는 쪽이 offers.isEmpty() 로 바로 가릴 수 있다
        public static Quote empty() {
            return new Quote(List.of(), null);
        }
    
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Cached {

        private long savedAt;

        private Quote quote;
    }

    // 공공데이터포털 인증키 (application.yml 의 app.price.service-key)
    @Value("${app.price.service-key}")
    private String serviceKey;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(4))
            .build();

    /** 품목코드 -> 조사가격. 같은 품목을 재료 여러 개가 쓸 수 있다. */
    // 재료 이름이 아니라 품목코드를 열쇠로 쓴다. "마늘" 과 "다진 마늘" 이
    // 같은 코드라서, 이름으로 두면 같은 응답을 두 번 받아온다
    //
    // ConcurrentHashMap 인 이유 — BargainService 가 별도 스레드에서
    // 101개를 훑는 동안, 요청 스레드도 이 캐시를 쓴다
    private final Map<String, Cached> cache = new ConcurrentHashMap<>();

    /**
     * 품목 하나의 조사가격.
     *
     * @throws IllegalStateException 인증키가 없을 때. 부르는 쪽이 503 으로 바꾼다
     */
    public Quote fetch(IngredientMatcher.Codes codes) {
        if (serviceKey.isBlank()) {
            throw new IllegalStateException(
                    "app.price.service-key 가 비어 있습니다. 공공데이터포털에서 활용신청 후 "
                    + "일반 인증키의 Decoding 값을 넣으세요.");
        }

        // 코드 세 개를 이어 열쇠로 쓴다. Codes 객체를 그대로 쓸 수도 있지만
        // (@Data 가 equals 를 만들어 준다) 글자가 로그로 보기 쉽다
        String key = codes.getGroup() + "/" + codes.getItem() + "/" + codes.getKind();
        Cached cached = cache.get(key);

        if (cached != null && System.currentTimeMillis() - cached.getSavedAt() < CACHE_MILLIS) {
            return cached.getQuote();
        }

        Quote quote = call(codes);

        if (cache.size() > CACHE_MAX_ENTRIES) {
            // 다시 물어보면 채워지는 값이라 오래된 것만 골라 지우는 정교함이 필요 없다.
            cache.clear();
        }
        cache.put(key, new Cached(System.currentTimeMillis(), quote));

        return quote;
    }

    private Quote call(IngredientMatcher.Codes codes) {
        JsonNode payload;

        try {
            HttpResponse<String> response = http.send(
                    HttpRequest.newBuilder(URI.create(url(codes)))
                            // 6초. 101개를 훑을 때 하나가 오래 걸리면
                            // 전체가 그만큼 늦어진다
                            .timeout(Duration.ofSeconds(6))
                            .GET()
                            .build(),
                    // 글자 집합을 UTF_8 로 못박는다. 안 주면 응답 헤더를
                    // 보고 짐작하는데, 이 API 는 헤더가 없어서 한글 시장
                    // 이름이 깨진다
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

            // raise_for_status 를 쓰지 않는다. 인증키가 틀리면 403 과 함께 사유가
            // 본문에 담겨 오는데, 그걸 읽어야 무엇이 틀렸는지 알 수 있다.
            payload = MAPPER.readTree(response.body());
        } catch (InterruptedException exc) {
            Thread.currentThread().interrupt();
            return Quote.empty();
        } catch (Exception exc) {
            // 통신 오류 · 시간 초과 · JSON 이 아닌 본문(점검 안내 페이지 등).
            // 값 하나 때문에 레시피 화면 전체가 멈추면 안 된다.
            System.out.println("[KAMIS 조회 실패] " + codes + ": "
                    + exc.getClass().getSimpleName());
            return Quote.empty();
        }

        // 200 으로 와도 본문에 오류가 담겨 있을 수 있다. 공공데이터포털은
        // 인증 문제를 상태 코드가 아니라 본문으로 알려준다
        String error = errorOf(payload);
        if (error != null) {
            // 인증키 오타 · 트래픽 초과. 조용히 빈 값을 돌려주면 원인을 못 찾는다.
            System.out.println("[KAMIS 오류] " + codes + ": " + error);
            return Quote.empty();
        }

        // 단위를 먼저 맞춘 뒤 두 갈래로 쓴다.
        //
        // history : 일주일치 (추이 계산용)
        // rows    : 그 중 가장 최근 날짜만 (시장별 가격용)
        //
        // 순서가 중요하다. 단위를 섞은 채로 추이를 계산하면 가격이 오른
        // 것인지 단위가 바뀐 것인지 알 수 없다
        List<JsonNode> history = sameUnitRows(rowsOf(payload));
        JsonNode trend = priceTrend(history);
        List<JsonNode> rows = latestRows(history);

        List<PriceOfferVO> offers = new ArrayList<>();

        for (JsonNode row : rows) {
            Integer price = toInt(row.get("exmn_dd_prc"));

            // 값이 없거나 0 인 행은 건너뛴다. 조사를 못 한 시장이
            // 0 으로 와서, 그대로 두면 최저가가 0원이 된다
            if (price == null || price <= 0) {
                continue;
            }

            offers.add(new PriceOfferVO(
                    marketLabel(row),
                    price,
                    // 링크는 재료 이름으로 만든다. 같은 품목코드라도 부른 이름이
                    // 다를 수 있어서("마늘" / "다진 마늘") 여기서는 비워둔다.
                    "",
                    unitOf(row),
                    text(row.get("exmn_ymd"))));
        }

        // 싼 곳부터 보여준다. 화면도 최저가를 먼저 읽는다.
        //
        // 이 순서에 기대는 곳이 둘이다 — PriceServiceImpl 이 첫 번째를
        // 최저가, 마지막을 최고가로 쓰고, BargainServiceImpl 도 첫 번째를
        // 쓴다. 정렬을 빼면 두 곳이 조용히 틀어진다
        offers.sort(Comparator.comparingInt(PriceOfferVO::getPrice));

        // List.copyOf 로 고칠 수 없는 목록을 만든다. 캐시에 담기는 값이라,
        // 받은 쪽에서 목록을 고치면 다음 사람이 그 결과를 받는다
        return new Quote(List.copyOf(offers), trend);
    }

    // 요청 주소를 만든다. 조건이 많아서 StringBuilder 로 이어 붙인다.
    //
    // cond[...] 모양이 이 API 의 조건 문법이다. 대괄호와 쌍콜론은 주소에
    // 그대로 쓸 수 없는 글자라 encode 를 거쳐야 한다 - 안 거치면 400 이 온다
    private String url(IngredientMatcher.Codes codes) {
        // Kst.today() 가 아니라 LocalDate.now() 다. 조사일자는 KAMIS 가
        // 한국 날짜로 매기고, 이 서버도 한국에서 돌리는 것을 전제한다
        LocalDate today = LocalDate.now();
        LocalDate start = today.minusDays(LOOKBACK_DAYS);

        StringBuilder url = new StringBuilder(URL)
                .append("?serviceKey=").append(encode(serviceKey))
                .append("&returnType=JSON")
                .append("&pageNo=1")
                .append("&numOfRows=").append(ROWS)
                .append("&").append(encode("cond[exmn_ymd::GTE]"))
                .append("=").append(start.format(DAY))
                .append("&").append(encode("cond[exmn_ymd::LTE]"))
                .append("=").append(today.format(DAY))
                .append("&").append(encode("cond[se_cd::EQ]")).append("=").append(RETAIL_CODE)
                .append("&").append(encode("cond[ctgry_cd::EQ]")).append("=").append(codes.getGroup())
                .append("&").append(encode("cond[item_cd::EQ]")).append("=").append(codes.getItem())
                .append("&selectable=").append(encode(COLUMNS));

        // 품종 코드는 없을 수 있다. 없으면 조건을 아예 빼야 한다 -
        // 빈 값으로 보내면 "품종이 빈 것" 을 찾아 0건이 온다
        if (codes.getKind() != null && !codes.getKind().isBlank()) {
            url.append("&").append(encode("cond[vrty_cd::EQ]")).append("=").append(codes.getKind());
        }

        return url.toString();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * 오류 사유를 꺼낸다. 없으면 null.
     *
     * 봉투가 두 종류인 이유 — 공공데이터포털은 앞단(게이트웨이)에서 막힌
     * 경우와 서비스까지 도달한 경우의 응답 모양이 다르다. 인증키가 틀리면
     * 앞단에서 막혀 OpenAPI_ServiceResponse 로 오고, 키는 맞는데 조건이
     * 틀리면 response.header 로 온다.
     */
    static String errorOf(JsonNode payload) {
        // 1) 게이트웨이 봉투
        JsonNode gateway = payload.path("OpenAPI_ServiceResponse").path("cmmMsgHeader");

        if (gateway.isObject()) {
            List<String> parts = new ArrayList<>();

            // 세 칸을 다 모아 붙인다. 어느 칸에 사유가 담겨 올지
            // 일정하지 않아서, 있는 것을 전부 로그에 남긴다
            for (String field : new String[] {"returnReasonCode", "errMsg", "returnAuthMsg"}) {
                String value = text(gateway.get(field));
                if (value != null && !value.isBlank()) {
                    parts.add(value);
                }
            }

            return String.join(" ", parts);
        }

        // 2) 서비스 봉투
        JsonNode header = payload.path("response").path("header");

        if (!header.isObject()) {
            return null;
        }

        String code = text(header.get("resultCode"));

        // 성공 코드를 "00" 과 "0" 둘 다 본다. 같은 포털인데 서비스에
        // 따라 모양이 다르게 온다
        if (code == null || code.isBlank() || "00".equals(code) || "0".equals(code)) {
            return null;
        }

        return (code + " " + String.valueOf(text(header.get("resultMsg")))).trim();
    }

    /**
     * 응답에서 데이터 행 목록만 꺼낸다.
     *
     * response -&gt; body -&gt; items -&gt; item 으로 네 겹 들어간다. 겹마다
     * has 로 확인하며 내려가는 이유 — 겹이 빠진 응답이 온다. 0건일 때
     * items 가 아예 없거나, item 이 하나면 배열이 아니라 객체로 온다.
     */
    static List<JsonNode> rowsOf(JsonNode payload) {
        JsonNode node = payload;

        // 있는 겹만 내려간다. 없으면 그 자리에 멈추고, 아래에서
        // 객체도 배열도 아니면 빈 목록이 된다
        for (String key : new String[] {"response", "body", "items", "item"}) {
            if (node.isObject() && node.has(key)) {
                node = node.get(key);
            }
        }

        List<JsonNode> rows = new ArrayList<>();

        // 한 건이면 배열이 아니라 객체로 온다. 이것을 안 다루면
        // 조사 시장이 하나뿐인 품목의 가격이 통째로 빈다
        if (node.isObject()) {
            rows.add(node);
        } else if (node.isArray()) {
            for (JsonNode row : node) {
                if (row.isObject()) {
                    rows.add(row);
                }
            }
        }

        return rows;
    }

    /**
     * 가장 최근 조사일자의 행만 남긴다.
     *
     * 일주일치를 받아왔으므로 그대로 쓰면 같은 시장이 날짜별로 여러 번
     * 나온다. 화면에는 "지금 가격" 만 보여야 한다.
     *
     * 날짜가 "20261011" 모양이라 글자로 견줘도 순서가 맞는다
     * (연 · 월 · 일 자리가 고정이다)
     */
    static List<JsonNode> latestRows(List<JsonNode> rows) {
        String newest = null;

        for (JsonNode row : rows) {
            String day = text(row.get("exmn_ymd"));

            if (day != null && !day.isBlank() && (newest == null || day.compareTo(newest) > 0)) {
                newest = day;
            }
        }

        // 날짜가 담긴 행이 하나도 없는 경우. 빈 목록을 주면
        // 부르는 쪽이 "값을 못 받았다" 로 처리한다
        if (newest == null) {
            return List.of();
        }

        List<JsonNode> latest = new ArrayList<>();

        for (JsonNode row : rows) {
            if (newest.equals(text(row.get("exmn_ymd")))) {
                latest.add(row);
            }
        }

        return latest;
    }

    /**
     * 단위가 같은 행만 남긴다.
     *
     * 같은 품목인데 시장마다 단위가 다를 수 있다("1kg" 과 "100g"). 섞으면
     * 최저/최고가가 시장 차이가 아니라 단위 차이가 된다.
     */
    static List<JsonNode> sameUnitRows(List<JsonNode> rows) {
        if (rows.isEmpty()) {
            return List.of();
        }

        // 단위별로 몇 행인지 센다.
        // merge 는 "없으면 1, 있으면 더하기" 를 한 줄로 한다
        Map<String, Integer> counts = new LinkedHashMap<>();

        for (JsonNode row : rows) {
            counts.merge(unitOf(row), 1, Integer::sum);
        }

        // 가장 흔한 단위. 같으면 이름순으로 정해서 매번 같은 답이 나오게 한다.
        //
        // 가장 흔한 쪽을 고르는 이유 — 시장 대부분이 쓰는 단위가
        // 그 품목의 기준이다. 드문 단위를 골라 남기면 시장이 한둘로 줄어든다
        //
        // 이름순을 더하는 이유 — 개수가 같을 때 순서가 흔들리면,
        // 같은 품목의 가격이 요청마다 달라 보인다
        String best = counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .findFirst()
                .map(Map.Entry::getKey)
                .orElse("");

        List<JsonNode> kept = new ArrayList<>();

        for (JsonNode row : rows) {
            if (best.equals(unitOf(row))) {
                kept.add(row);
            }
        }

        return kept;
    }

    /**
     * 일주일 전과 견줘 값이 얼마나 움직였는지. 흔들림 수준이면 null.
     *
     * 날짜별로 묶어 평균을 낸 뒤, 가장 이른 날과 가장 늦은 날을 견준다.
     *
     * 평균을 내는 이유 — 한 날짜에 시장이 여러 곳이다. 아무 시장 하나를
     * 골라 견주면, 그 시장이 조사를 안 한 날에 추이가 사라진다.
     */
    static JsonNode priceTrend(List<JsonNode> rows) {
        Map<String, List<Integer>> buckets = new LinkedHashMap<>();

        for (JsonNode row : rows) {
            String day = text(row.get("exmn_ymd"));
            Integer price = toInt(row.get("exmn_dd_prc"));

            if (day != null && !day.isBlank() && price != null && price > 0) {
                buckets.computeIfAbsent(day, key -> new ArrayList<>()).add(price);
            }
        }

        // 날짜가 하나뿐이면 견줄 대상이 없다. 연휴에 조사일이 하루만
        // 끼는 경우다
        if (buckets.size() < 2) {
            return null;
        }

        Map<String, Integer> average = new LinkedHashMap<>();
        buckets.forEach((day, prices) -> {
            int total = 0;
            for (int price : prices) {
                total += price;
            }
            average.put(day, total / prices.size());
        });

        // 날짜를 글자순으로 정렬한다. "20261004" < "20261011" 이라
        // 글자순이 곧 날짜순이다
        List<String> ordered = new ArrayList<>(average.keySet());
        ordered.sort(Comparator.naturalOrder());

        String firstDay = ordered.get(0);
        String lastDay = ordered.get(ordered.size() - 1);

        int was = average.get(firstDay);
        int now = average.get(lastDay);

        // 0 으로 나눌 수 없다. 옛 평균이 0 이면 변동률을 낼 수 없다
        if (was <= 0) {
            return null;
        }

        // (double) 로 바꿔 나눈다. int 끼리 나누면 소수점이 버려져
        // 변동률이 0 이나 100 만 나온다
        double percent = (now - was) / (double) was * 100;

        // 오른 쪽도 내린 쪽도 같은 기준으로 가린다(절댓값).
        // 5% 미만은 흔들림으로 보고 알리지 않는다
        if (Math.abs(percent) < TREND_MIN_PERCENT) {
            return null;
        }

        // 어느 날짜의 어떤 값과 견준 것인지 같이 담는다. percent 만
        // 보내면 "12% 내렸다는데 언제와 비교한 거지" 를 알 수 없다
        ObjectNode trend = MAPPER.createObjectNode();
        trend.put("percent", Numbers.round(percent, 1));
        trend.put("from_price", was);
        trend.put("to_price", now);
        trend.put("from_day", firstDay);
        trend.put("to_day", lastDay);

        return trend;
    }

    /**
     * 화면에 보여줄 조사처 이름. "서울" + "가락시장" -&gt; "서울 가락시장"
     *
     * 둘 중 하나만 오는 경우가 있어서 네 갈래로 가린다. 둘 다 없으면
     * "조사가격" 이라고 적는다 - 빈 글자를 내보내면 화면에 가격만
     * 덩그러니 남는다.
     */
    static String marketLabel(JsonNode row) {
        String region = blankToEmpty(text(row.get("sgg_nm")));
        String market = blankToEmpty(text(row.get("mrkt_nm")));

        if (!region.isEmpty() && !market.isEmpty()) {
            return region + " " + market;
        }

        if (!market.isEmpty()) {
            return market;
        }

        return region.isEmpty() ? "조사가격" : region;
    }

    /**
     * 가격의 기준 단위. "20" + "kg" -&gt; "20kg"
     *
     * 크기와 단위가 따로 온다. 이어 붙여야 UnitConvert 가 읽을 수 있는
     * 모양이 된다.
     *
     * 둘 다 없으면 빈 글자다. 그래도 위 sameUnitRows 가 빈 글자끼리
     * 묶어주므로 계산이 멈추지는 않는다
     */
    static String unitOf(JsonNode row) {
        return (blankToEmpty(text(row.get("unit_sz"))) + blankToEmpty(text(row.get("unit"))))
                .trim();
    }

    /** 사용자가 값을 확인할 수 있는 KAMIS 가격 페이지 주소. */
    public static String link(String name) {
        return REFERENCE_URL + "?keyword=" + encode(name);
    }

    /**
     * 가격이 문자열("4,350")로 와도 정수로. 실패하면 null.
     *
     * asInt() 만 쓰면 글자로 온 값이 0 이 된다. 0 은 "공짜" 로 읽혀서
     * 최저가가 0원이 되는데, 에러가 안 나서 알아채기 어렵다.
     */
    static Integer toInt(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }

        if (node.isIntegralNumber()) {
            return node.asInt();
        }

        // 천 단위 쉼표가 붙어서 오는 기관이 있다.
        String value = node.asString("").replace(",", "").trim();

        try {
            // Double 로 읽었다가 int 로 바꾼다. "4350.0" 처럼 소수점이
            // 붙어 오는 경우가 있어서, Integer.parseInt 로는 터진다
            return value.isEmpty() ? null : (int) Double.parseDouble(value);
        } catch (NumberFormatException exc) {
            return null;
        }
    }

    // JSON 칸을 글자로. 없거나 null 이면 null, 있으면 앞뒤 공백을 떼어낸다.
    //
    // 공백을 떼는 이유 — 시장 이름에 공백이 붙어 오는 경우가 있다.
    // 그대로 두면 이름 사이가 두 칸으로 벌어진다
    private static String text(JsonNode node) {
        return node == null || node.isNull() ? null : node.asString("").trim();
    }

    // null 을 빈 글자로. 이어 붙일 때 "null" 네 글자가 섞이지 않게 한다
    private static String blankToEmpty(String value) {
        return value == null ? "" : value;
    }
}
