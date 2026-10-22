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
 */
@Component
public class KamisClient {

    private static final String URL = "https://apis.data.go.kr/B552845/perDay/price";

    /** 구분코드 01=소매. 도매 단가(10kg 상자 등)와 섞으면 최저/최고가 뒤죽박죽이 된다. */
    private static final String RETAIL_CODE = "01";

    /** 한 번에 받아올 최대 행 수(명세서 max 1000). 품목 하나가 시장×품종×등급×날짜로 쪼개진다. */
    private static final int ROWS = 1000;

    /** 조사일자는 필수 파라미터인데 주말·공휴일은 조사가 없다. 며칠 거슬러 본다. */
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
    private static final double TREND_MIN_PERCENT = 5.0;

    private static final DateTimeFormatter DAY = DateTimeFormatter.ofPattern("yyyyMMdd");

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    /** 한 품목의 조사가격과 추이. 링크는 재료 이름에 따라 달라서 여기 담지 않는다. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Quote {

        private List<PriceOfferVO> offers;

        private JsonNode trend;

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
                            .timeout(Duration.ofSeconds(6))
                            .GET()
                            .build(),
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

        String error = errorOf(payload);
        if (error != null) {
            // 인증키 오타 · 트래픽 초과. 조용히 빈 값을 돌려주면 원인을 못 찾는다.
            System.out.println("[KAMIS 오류] " + codes + ": " + error);
            return Quote.empty();
        }

        // 단위를 먼저 맞춘 뒤 두 갈래로 쓴다.
        List<JsonNode> history = sameUnitRows(rowsOf(payload));
        JsonNode trend = priceTrend(history);
        List<JsonNode> rows = latestRows(history);

        List<PriceOfferVO> offers = new ArrayList<>();

        for (JsonNode row : rows) {
            Integer price = toInt(row.get("exmn_dd_prc"));

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
        offers.sort(Comparator.comparingInt(PriceOfferVO::getPrice));

        return new Quote(List.copyOf(offers), trend);
    }

    private String url(IngredientMatcher.Codes codes) {
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

        if (codes.getKind() != null && !codes.getKind().isBlank()) {
            url.append("&").append(encode("cond[vrty_cd::EQ]")).append("=").append(codes.getKind());
        }

        return url.toString();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /** 오류 사유를 꺼낸다. 없으면 null. */
    static String errorOf(JsonNode payload) {
        // 1) 게이트웨이 봉투
        JsonNode gateway = payload.path("OpenAPI_ServiceResponse").path("cmmMsgHeader");

        if (gateway.isObject()) {
            List<String> parts = new ArrayList<>();

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

        if (code == null || code.isBlank() || "00".equals(code) || "0".equals(code)) {
            return null;
        }

        return (code + " " + String.valueOf(text(header.get("resultMsg")))).trim();
    }

    /** 응답에서 데이터 행 목록만 꺼낸다. */
    static List<JsonNode> rowsOf(JsonNode payload) {
        JsonNode node = payload;

        for (String key : new String[] {"response", "body", "items", "item"}) {
            if (node.isObject() && node.has(key)) {
                node = node.get(key);
            }
        }

        List<JsonNode> rows = new ArrayList<>();

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

    /** 가장 최근 조사일자의 행만 남긴다. */
    static List<JsonNode> latestRows(List<JsonNode> rows) {
        String newest = null;

        for (JsonNode row : rows) {
            String day = text(row.get("exmn_ymd"));

            if (day != null && !day.isBlank() && (newest == null || day.compareTo(newest) > 0)) {
                newest = day;
            }
        }

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

        Map<String, Integer> counts = new LinkedHashMap<>();

        for (JsonNode row : rows) {
            counts.merge(unitOf(row), 1, Integer::sum);
        }

        // 가장 흔한 단위. 같으면 이름순으로 정해서 매번 같은 답이 나오게 한다.
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

    /** 일주일 전과 견줘 값이 얼마나 움직였는지. 흔들림 수준이면 null. */
    static JsonNode priceTrend(List<JsonNode> rows) {
        Map<String, List<Integer>> buckets = new LinkedHashMap<>();

        for (JsonNode row : rows) {
            String day = text(row.get("exmn_ymd"));
            Integer price = toInt(row.get("exmn_dd_prc"));

            if (day != null && !day.isBlank() && price != null && price > 0) {
                buckets.computeIfAbsent(day, key -> new ArrayList<>()).add(price);
            }
        }

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

        List<String> ordered = new ArrayList<>(average.keySet());
        ordered.sort(Comparator.naturalOrder());

        String firstDay = ordered.get(0);
        String lastDay = ordered.get(ordered.size() - 1);

        int was = average.get(firstDay);
        int now = average.get(lastDay);

        if (was <= 0) {
            return null;
        }

        double percent = (now - was) / (double) was * 100;

        if (Math.abs(percent) < TREND_MIN_PERCENT) {
            return null;
        }

        ObjectNode trend = MAPPER.createObjectNode();
        trend.put("percent", Numbers.round(percent, 1));
        trend.put("fromPrice", was);
        trend.put("toPrice", now);
        trend.put("fromDay", firstDay);
        trend.put("toDay", lastDay);

        return trend;
    }

    /** 화면에 보여줄 조사처 이름. "서울" + "가락시장" -> "서울 가락시장" */
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

    /** 가격의 기준 단위. "20" + "kg" -> "20kg" */
    static String unitOf(JsonNode row) {
        return (blankToEmpty(text(row.get("unit_sz"))) + blankToEmpty(text(row.get("unit"))))
                .trim();
    }

    /** 사용자가 값을 확인할 수 있는 KAMIS 가격 페이지 주소. */
    public static String link(String name) {
        return REFERENCE_URL + "?keyword=" + encode(name);
    }

    /** 가격이 문자열("4,350")로 와도 정수로. 실패하면 null. */
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
            return value.isEmpty() ? null : (int) Double.parseDouble(value);
        } catch (NumberFormatException exc) {
            return null;
        }
    }

    private static String text(JsonNode node) {
        return node == null || node.isNull() ? null : node.asString("").trim();
    }

    private static String blankToEmpty(String value) {
        return value == null ? "" : value;
    }
}
