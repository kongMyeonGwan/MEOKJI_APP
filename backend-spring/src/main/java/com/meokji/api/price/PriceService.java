package com.meokji.api.price;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.stereotype.Service;

import com.meokji.api.common.ApiException;
import com.meokji.api.config.AppProperties;
import com.meokji.api.recipe.dto.PriceItem;
import com.meokji.api.recipe.dto.PriceOffer;

import jakarta.annotation.PreDestroy;
import tools.jackson.databind.JsonNode;

/**
 * 레시피 재료의 시세를 모아 화면용으로 만든다 (STEP 17~19 · 39 · 42).
 *
 * <p>파이썬 쪽 {@code app/services/price_service.py} 자리다.
 *
 * <h2>재료마다 따로 부른다</h2>
 *
 * <p>KAMIS 는 품목 하나씩만 조회된다. 재료가 9개면 호출도 9번이다. 하나씩 순서대로
 * 부르면 레시피 화면이 6초 넘게 기다리므로 <b>한꺼번에 던진다</b>(파이썬은
 * {@code asyncio.gather} 를 썼다).
 *
 * <p>스레드 풀을 작게 잡는다. 같은 기관에 동시에 많이 던지면 호출 제한에 걸린다.
 */
@Service
public class PriceService {

    /** 한 번에 던질 최대 개수. 재료가 더 많으면 나머지는 기다린다. */
    private static final int PARALLELISM = 6;

    private final IngredientMatcher matcher;
    private final KamisClient kamis;
    private final UnitConvert convert;
    private final AppProperties app;

    /**
     * 시세 조회 전용 스레드.
     *
     * <p>공용 풀({@code ForkJoinPool.commonPool})을 쓰지 않는다. 그쪽은 CPU 개수만큼만
     * 있어서, 네트워크를 기다리는 작업으로 채우면 다른 요청까지 밀린다.
     */
    private final ExecutorService pool = Executors.newFixedThreadPool(PARALLELISM, runnable -> {
        Thread thread = new Thread(runnable, "kamis-price");
        thread.setDaemon(true);
        return thread;
    });

    public PriceService(
            IngredientMatcher matcher,
            KamisClient kamis,
            UnitConvert convert,
            AppProperties app) {
        this.matcher = matcher;
        this.kamis = kamis;
        this.convert = convert;
        this.app = app;
    }

    @PreDestroy
    void shutdown() {
        pool.shutdownNow();
    }

    /**
     * 레시피의 재료 목록으로 가격을 가져온다.
     *
     * @param ingredientsJson {@code [{name, quantity, unit, grams}, ...]}
     * @throws ApiException 설정이 잘못됐을 때 503. 바깥 API 가 죽은 것은 빈 값으로 넘긴다
     */
    public List<PriceItem> forIngredients(JsonNode ingredientsJson) {
        checkProviders();

        List<JsonNode> rows = new ArrayList<>();

        for (JsonNode item : ingredientsJson) {
            if (item.isObject() && !item.path("name").asString("").isBlank()) {
                rows.add(item);
            }
        }

        List<CompletableFuture<PriceItem>> jobs = new ArrayList<>();

        for (JsonNode item : rows) {
            jobs.add(CompletableFuture.supplyAsync(() -> priceOf(item), pool));
        }

        List<PriceItem> items = new ArrayList<>();

        for (CompletableFuture<PriceItem> job : jobs) {
            try {
                items.add(job.join());
            } catch (Exception exc) {
                // 한 재료가 실패해도 나머지는 보여준다. 값 하나 때문에 표 전체가
                // 비면 사용자는 아무것도 못 본다.
                Throwable cause = exc.getCause() == null ? exc : exc.getCause();

                if (cause instanceof ApiException apiException) {
                    throw apiException;
                }
            }
        }

        return items;
    }

    /**
     * 설정을 먼저 본다.
     *
     * <p>모르는 값을 조용히 건너뛰지 않는다. 그러면 "가격이 아무 데도 안 나오는데
     * 이유를 모르겠다" 가 된다. 파이썬 쪽에서 이 예외가 잡히지 않아 500 으로 나가던
     * 것을 503 으로 고친 적이 있다.
     */
    private void checkProviders() {
        List<String> providers = app.getPrice().getProviderList();

        if (providers.isEmpty()) {
            throw ApiException.serviceUnavailable(
                    "app.price.providers 가 비어 있습니다. 사용 가능한 값: kamis");
        }

        for (String provider : providers) {
            if (!"kamis".equalsIgnoreCase(provider)) {
                throw ApiException.serviceUnavailable(
                        "지원하지 않는 app.price.providers 항목: " + provider
                        + ". 사용 가능한 값: kamis");
            }
        }
    }

    /** 재료 한 줄의 가격. */
    private PriceItem priceOf(JsonNode item) {
        String name = item.path("name").asString().trim();

        // 물·얼음처럼 사는 재료가 아닌 것. 물어볼 일이 아니다.
        if (matcher.isNotPriced(name)) {
            return emptyItem(name, item, false);
        }

        IngredientMatcher.Codes codes = matcher.findCodes(name);

        // KAMIS 는 농수산물만 조사한다. 간장·두부·라면 같은 가공식품은 여기 없다.
        // 값을 지어내지 않고 빈 채로 돌려준다.
        if (codes == null) {
            return emptyItem(name, item, false);
        }

        KamisClient.Quote quote;
        try {
            quote = kamis.fetch(codes);
        } catch (IllegalStateException exc) {
            throw ApiException.serviceUnavailable(exc.getMessage());
        }

        // 링크는 재료 이름으로 만든다. 같은 품목코드라도 부른 이름이 다를 수 있다
        // ("마늘" / "다진 마늘").
        List<PriceOffer> offers = new ArrayList<>();
        String link = KamisClient.link(name);

        for (PriceOffer offer : quote.offers()) {
            offers.add(new PriceOffer(
                    offer.shop(), offer.price(), link, offer.unit(), offer.surveyedAt()));
        }

        // surveyed 는 품목표만 보고 정한다. 그래서 API 가 죽어도 이 값은 흔들리지
        // 않는다 - "조사 대상이 아니다" 와 "이번에 못 받았다" 를 가려주는 값이다.
        return scaled(label(name, item), offers, quote.trend(), item, true);
    }

    /**
     * 조사가격이 없는 재료 한 줄.
     *
     * <p>{@code needText} 를 넣지 않는다. 값은 알 수 있지만 파이썬이 그렇게 내보내고
     * 있어서 맞춘다 - {@code scale_offers} 가 offers 가 비면 빈 dict 를 돌려주고,
     * needText 는 기본값 None 으로 남는다.
     *
     * <p>화면 동작은 같다. {@code PriceTable.jsx} 는 {@code min_unit} 이 참일 때만
     * {@code need_text} 를 읽고, 조사가격이 없으면 {@code min_unit} 도 거짓이다.
     */
    private PriceItem emptyItem(String name, JsonNode item, boolean surveyed) {
        return new PriceItem(
                label(name, item),
                List.of(),
                null,
                null,
                null,
                null,
                false,
                null,
                surveyed);
    }

    /** 판매 단위 가격을 '이 레시피 때문에 내야 하는 돈' 으로 바꾼다. */
    private PriceItem scaled(
            String label,
            List<PriceOffer> offers,
            JsonNode trend,
            JsonNode item,
            boolean surveyed) {

        if (offers.isEmpty()) {
            // 위 emptyItem 과 같은 이유로 needText 를 넣지 않는다.
            return new PriceItem(
                    label, offers, trend, null, null, null, false, null, surveyed);
        }

        String needText = needText(item);

        String saleUnit = offers.get(0).unit() == null ? "" : offers.get(0).unit();

        Double ratio = convert.neededRatio(
                number(item.get("grams")),
                number(item.get("quantity")),
                item.path("unit").asString(null),
                saleUnit,
                item.path("name").asString(""));

        if (ratio == null) {
            // 견줄 수 없으면 조사가격은 보여주고 합계 계산에서만 뺀다.
            return new PriceItem(
                    label, offers, trend, needText, null, null, false, null, surveyed);
        }

        Double buy = UnitConvert.buyRatio(ratio, saleUnit);

        if (buy == null) {
            return new PriceItem(
                    label, offers, trend, needText, null, null, false, null, surveyed);
        }

        // 필요한 양보다 더 사야 했는가. 화면이 이 값으로 '최소단위' 배지를 붙인다.
        boolean minUnit = buy > ratio;

        return new PriceItem(
                label,
                offers,
                trend,
                needText,
                UnitConvert.scalePrice(offers.get(0).price(), buy),
                UnitConvert.scalePrice(offers.get(offers.size() - 1).price(), buy),
                minUnit,
                minUnit ? buyText(buy, saleUnit) : null,
                surveyed);
    }

    /** 실제로 사게 되는 양. "1개" · "30구" */
    private static String buyText(double buy, String saleUnit) {
        UnitConvert.Unit sale = UnitConvert.parseUnit(saleUnit);

        if (sale == null) {
            return null;
        }

        return UnitConvert.formatQuantity(
                com.meokji.api.common.Numbers.round(sale.size() * buy, 3)) + sale.token();
    }

    /** 화면에 적을 필요량. "1큰술" · "0.5개" */
    private static String needText(JsonNode item) {
        Double quantity = number(item.get("quantity"));
        String unit = item.path("unit").asString(null);

        if (quantity == null || unit == null || unit.isBlank()) {
            return null;
        }

        return UnitConvert.formatQuantity(quantity) + unit;
    }

    /** 화면에 보여줄 재료 이름. "두부" + 1모 -> "두부 1모" */
    private static String label(String name, JsonNode item) {
        Double quantity = number(item.get("quantity"));
        String unit = item.path("unit").asString(null);

        if (quantity == null || unit == null || unit.isBlank()) {
            return name;
        }

        return name + " " + UnitConvert.formatQuantity(quantity) + unit;
    }

    private static Double number(JsonNode node) {
        return node == null || node.isNull() || !node.isNumber() ? null : node.asDouble();
    }
}
