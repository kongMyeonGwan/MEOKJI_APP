package com.meokji.api.service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.meokji.api.common.ApiException;
import com.meokji.api.dto.recipe.PriceItem;
import com.meokji.api.dto.recipe.PriceOffer;
import com.meokji.api.util.IngredientMatcher;
import com.meokji.api.util.KamisClient;
import com.meokji.api.util.Numbers;
import com.meokji.api.util.UnitConvert;

import jakarta.annotation.PreDestroy;
import tools.jackson.databind.JsonNode;

/**
 * 재료 가격.
 *
 * 흐름
 *   1. 재료 이름 → KAMIS 품목 코드 찾기        (IngredientMatcher)
 *   2. 품목 코드 → 시장별 가격 받아오기         (KamisClient)
 *   3. "1kg 에 12,000원" → "300g 에 3,600원"  (UnitConvert, 레시피 분량만큼으로 바꾸기)
 *
 * ※ 이 파일은 조금 어렵다. 팀원이 고칠 일은 거의 없는 영역이다.
 */
@Service
public class PriceServiceImpl implements PriceService {

    @Autowired
    private IngredientMatcher ingredientMatcher;

    @Autowired
    private KamisClient kamisClient;

    @Autowired
    private UnitConvert unitConvert;

    // 가격을 가져올 곳. 지금은 "kamis" 하나뿐이다 (application.yml)
    @Value("${app.price.providers}")
    private String providers;

    // 재료 가격을 "동시에" 가져오기 위한 일꾼 6명 (스레드 6개).
    // 재료가 9개면 하나씩 부를 때 6초 넘게 걸려서, 여러 개를 한꺼번에 부른다.
    private ExecutorService workers = Executors.newFixedThreadPool(6);

    // 서버가 꺼질 때 일꾼들도 정리한다
    @PreDestroy
    public void shutdown() {
        workers.shutdownNow();
    }

    @Override
    public List<PriceItem> forIngredients(JsonNode ingredientsJson) {
        // 설정이 kamis 가 아니면 503 에러 (가격이 왜 안 나오는지 바로 알 수 있게)
        for (String provider : providers.split(",")) {
            if (!provider.trim().equalsIgnoreCase("kamis")) {
                throw ApiException.serviceUnavailable(
                        "지원하지 않는 app.price.providers 항목: " + provider.trim() + ". 사용 가능한 값: kamis");
            }
        }

        // 1. 재료마다 "가격 가져오기" 일을 일꾼에게 맡긴다 (기다리지 않고 바로 다음 재료로)
        List<CompletableFuture<PriceItem>> jobs = new ArrayList<>();
        for (JsonNode item : ingredientsJson) {
            String name = item.path("name").asString("");
            if (item.isObject() && !name.isBlank()) {
                jobs.add(CompletableFuture.supplyAsync(() -> priceOf(item), workers));
            }
        }

        // 2. 맡긴 일이 끝나기를 순서대로 기다렸다가 결과를 모은다 (join = 끝날 때까지 기다리기)
        List<PriceItem> items = new ArrayList<>();
        for (CompletableFuture<PriceItem> job : jobs) {
            try {
                items.add(job.join());
            } catch (Exception e) {
                // 설정 문제(503)는 그대로 알린다. 재료 하나가 실패한 건 빼고 나머지만 보여준다.
                if (e.getCause() instanceof ApiException) {
                    throw (ApiException) e.getCause();
                }
            }
        }
        return items;
    }

    // 재료 한 줄의 가격
    private PriceItem priceOf(JsonNode item) {
        String name = item.path("name").asString().trim();

        // 물 · 얼음처럼 사는 재료가 아니면 가격이 없다
        if (ingredientMatcher.isNotPriced(name)) {
            return emptyItem(name, item, false);
        }

        // KAMIS 품목 코드 찾기. 간장 · 두부 같은 가공식품은 조사 대상이 아니라 코드가 없다
        IngredientMatcher.Codes codes = ingredientMatcher.findCodes(name);
        if (codes == null) {
            return emptyItem(name, item, false);
        }

        // 시장별 가격 받아오기
        KamisClient.Quote quote;
        try {
            quote = kamisClient.fetch(codes);
        } catch (IllegalStateException e) {
            throw ApiException.serviceUnavailable(e.getMessage());
        }

        // 가격마다 "더 알아보기" 링크를 붙인다
        String link = KamisClient.link(name);
        List<PriceOffer> offers = new ArrayList<>();
        for (PriceOffer offer : quote.getOffers()) {
            offers.add(new PriceOffer(offer.getShop(), offer.getPrice(), link, offer.getUnit(), offer.getSurveyedAt()));
        }

        return scaled(label(name, item), offers, quote.getTrend(), item);
    }

    // 가격이 없는 재료 한 줄. surveyed 가 false 면 "조사 대상 아님"
    private PriceItem emptyItem(String name, JsonNode item, boolean surveyed) {
        PriceItem price = new PriceItem();
        price.setName(label(name, item));
        price.setOffers(new ArrayList<>());
        price.setSurveyed(surveyed);
        return price;
    }

    // 판매 단위 가격(1kg 12,000원)을 레시피 분량 가격(300g 3,600원)으로 바꾼다
    private PriceItem scaled(String label, List<PriceOffer> offers, JsonNode trend, JsonNode item) {
        PriceItem price = new PriceItem();
        price.setName(label);
        price.setOffers(offers);
        price.setTrend(trend);
        price.setSurveyed(true);

        if (offers.isEmpty()) {
            return price; // 이번에 가격을 못 받아옴
        }

        price.setNeedText(needText(item));

        String saleUnit = offers.get(0).getUnit();
        if (saleUnit == null) {
            saleUnit = "";
        }

        // 판매 단위 중 얼마만큼 필요한지. 예) 1kg 짜리에서 300g 이면 0.3
        Double ratio = unitConvert.neededRatio(
                number(item.get("grams")),
                number(item.get("quantity")),
                item.path("unit").asString(null),
                saleUnit,
                item.path("name").asString(""));
        if (ratio == null) {
            return price; // 단위를 맞출 수 없음 → 합계에서 빠진다
        }

        // 실제로 사야 하는 양. 무처럼 쪼개 팔지 않으면 0.2개가 필요해도 1개를 사야 한다
        Double buy = UnitConvert.buyRatio(ratio, saleUnit);
        if (buy == null) {
            return price;
        }

        boolean minUnit = buy > ratio; // 필요한 것보다 더 사야 하는지

        // offers 는 싼 순서로 와서 첫 번째가 최저가, 마지막이 최고가
        price.setLowCost(UnitConvert.scalePrice(offers.get(0).getPrice(), buy));
        price.setHighCost(UnitConvert.scalePrice(offers.get(offers.size() - 1).getPrice(), buy));
        price.setMinUnit(minUnit);
        if (minUnit) {
            price.setBuyText(buyText(buy, saleUnit));
        }
        return price;
    }

    // 실제로 사게 되는 양. 예) "1개", "30구"
    private String buyText(double buy, String saleUnit) {
        UnitConvert.Unit sale = UnitConvert.parseUnit(saleUnit);
        if (sale == null) {
            return null;
        }
        return UnitConvert.formatQuantity(Numbers.round(sale.getSize() * buy, 3)) + sale.getToken();
    }

    // 레시피에 필요한 양. 예) "1큰술", "0.5개"
    private String needText(JsonNode item) {
        Double quantity = number(item.get("quantity"));
        String unit = item.path("unit").asString(null);
        if (quantity == null || unit == null || unit.isBlank()) {
            return null;
        }
        return UnitConvert.formatQuantity(quantity) + unit;
    }

    // 화면에 보여줄 재료 이름. 예) "두부" + 1모 → "두부 1모"
    private String label(String name, JsonNode item) {
        String need = needText(item);
        if (need == null) {
            return name;
        }
        return name + " " + need;
    }

    // JSON 숫자 → Double (숫자가 아니면 null)
    private Double number(JsonNode node) {
        if (node == null || !node.isNumber()) {
            return null;
        }
        return node.asDouble();
    }
}
