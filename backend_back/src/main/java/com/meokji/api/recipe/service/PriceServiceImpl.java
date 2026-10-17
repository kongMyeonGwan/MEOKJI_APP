package com.meokji.api.recipe.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.meokji.api.common.ApiException;
import com.meokji.api.recipe.vo.PriceItemVO;
import com.meokji.api.recipe.vo.PriceOfferVO;
import com.meokji.api.util.IngredientMatcher;
import com.meokji.api.util.KamisClient;
import com.meokji.api.util.Numbers;
import com.meokji.api.util.UnitConvert;

import tools.jackson.databind.JsonNode;

/**
 * 재료 가격.
 *
 * 흐름
 *   1. 재료 이름 → KAMIS 품목 코드 찾기        (IngredientMatcher)
 *   2. 품목 코드 → 시장별 가격 받아오기         (KamisClient)
 *   3. "1kg 에 12,000원" → "300g 에 3,600원"  (UnitConvert, 레시피 분량만큼으로 바꾸기)
 *
 * 재료를 하나씩 순서대로 조회한다. 재료가 많으면 그만큼 느리다.
 *
 * <p>왜 한꺼번에 묻지 않나 — KAMIS API 는 품목 하나씩만 받는다. 한 번에
 * 여러 품목을 묻는 길이 없다.
 *
 * <p>왜 여러 개를 동시에 부르지 않나 — 전에는 그렇게 했다. 빠르긴 한데
 * 호출 제한에 걸려 절반이 빈 값으로 오는 날이 생겼고, 어느 재료가
 * 실패했는지도 섞여서 알기 어려웠다. 지금은 느린 쪽을 골랐다.
 *
 * <p>실패를 세 가지로 나눠 다룬다
 *   설정이 틀렸다        → 503. 바로 알려야 고칠 수 있다
 *   KAMIS 가 안 된다     → 503. 재료 하나 문제가 아니다
 *   재료 하나가 실패했다 → 건너뛰고 나머지를 보여준다
 *
 * <p>클래스가 길지만 메서드마다 하는 일이 하나씩이다. 아래로 갈수록
 * 작은 일을 하는 메서드다 (priceOf → scaled → buyText · needText · label).
 */
@Service
public class PriceServiceImpl implements PriceService {

    // 재료 이름 -> KAMIS 품목 코드. "돼지고기" 를 코드로 바꿔준다
    @Autowired
    private IngredientMatcher ingredientMatcher;

    // 품목 코드로 바깥 API 를 부른다
    @Autowired
    private KamisClient kamisClient;

    // 단위 맞추기. "1kg 12,000원" 을 "300g 3,600원" 으로 바꾼다
    @Autowired
    private UnitConvert unitConvert;

    // 가격을 가져올 곳. 지금은 "kamis" 하나뿐이다 (application.yml)
    //
    // 설정으로 둔 이유 — 나중에 다른 곳을 붙일 자리를 남겨둔 것이다.
    // 쉼표로 여러 개를 적을 수 있는 모양이지만, 지금은 kamis 외의 값을
    // 적으면 아래에서 503 을 던진다
    @Value("${app.price.providers}")
    private String providers;

    /**
     * 재료 목록 전체의 가격.
     *
     * {@code @Transactional} 이 없다. DB 를 쓰지 않는다 — 전부 바깥 API 와
     * 파일에서 온다. 붙여두면 API 응답을 기다리는 동안 DB 연결을 붙잡는다.
     */
    @Override
    public List<PriceItemVO> forIngredients(JsonNode ingredientsJson) {
        // 설정이 kamis 가 아니면 503 에러 (가격이 왜 안 나오는지 바로 알 수 있게)
        //
        // 조용히 무시하면 "가격이 전부 안 나오는데 에러도 없는" 상태가 된다.
        // 설정 오타는 사람이 고쳐야 하는 문제라 드러나야 한다
        for (String provider : providers.split(",")) {
            // equalsIgnoreCase 라 "KAMIS" 도 통과한다. 설정값의 대소문자까지
            // 맞추라고 하면 실수할 자리만 늘어난다
            if (!provider.trim().equalsIgnoreCase("kamis")) {
                // 쓸 수 있는 값을 문장에 적어준다. 안 적으면 로그를 보고도
                // 무엇으로 고쳐야 할지 모른다
                throw ApiException.serviceUnavailable(
                        "지원하지 않는 app.price.providers 항목: " + provider.trim() + ". 사용 가능한 값: kamis");
            }
        }

        // 재료를 하나씩 순서대로 가격 조회한다.
        // 재료가 9개면 6초쯤 걸린다. 그동안 화면에는 "레시피를 만들고 있어요" 가 떠 있다.
        List<PriceItemVO> items = new ArrayList<>();
        for (JsonNode item : ingredientsJson) {
            String name = item.path("name").asString("");
            // 모양이 이상한 줄은 건너뛴다. recipes.json 을 손으로 고치다
            // 빈 칸이 생겼을 때 전체가 터지지 않게 하려는 것이다
            if (!item.isObject() || name.isBlank()) {
                continue;
            }

            try {
                items.add(priceOf(item));
            } catch (ApiException e) {
                throw e; // 설정 문제 · KAMIS 접속 문제는 그대로 알린다
                // 재료 하나가 아니라 전체가 안 되는 문제라서, 건너뛰면
                // 가격이 통째로 빈 응답이 조용히 나간다
            } catch (Exception e) {
                // 재료 하나가 실패한 것은 건너뛰고 나머지만 보여준다
                //
                // 그 재료가 목록에서 빠지는 셈이라 화면의 재료 목록과
                // 줄 수가 어긋날 수 있다. 그래도 아홉 개 중 하나 때문에
                // 레시피를 통째로 못 보여주는 쪽이 더 나쁘다
            }
        }
        return items;
    }

    /**
     * 재료 한 줄의 가격.
     *
     * 가격이 없는 경우가 둘이다. 둘 다 surveyed = false 로 돌려주고,
     * 화면이 "조사 대상이 아니다" 로 안내한다
     *   물 · 얼음처럼 애초에 사는 재료가 아닌 것
     *   간장 · 두부처럼 KAMIS 조사 품목이 아닌 것
     */
    private PriceItemVO priceOf(JsonNode item) {
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
            // KamisClient 는 접속 · 인증 문제를 IllegalStateException 으로
            // 알린다. 그것을 503 으로 바꿔 올려보내면 위 forIngredients 의
            // catch (ApiException) 이 그대로 통과시킨다
            throw ApiException.serviceUnavailable(e.getMessage());
        }

        // 가격마다 "더 알아보기" 링크를 붙인다
        //
        // 링크는 재료 이름으로 만들고 시장마다 같다. KamisClient 가 준
        // offer 에는 링크가 없어서 여기서 새 객체로 다시 만든다 - 받은
        // 객체를 고치면 그 객체가 어디서 왔는지 따라가기 어려워진다
        String link = KamisClient.link(name);
        List<PriceOfferVO> offers = new ArrayList<>();
        for (PriceOfferVO offer : quote.getOffers()) {
            offers.add(new PriceOfferVO(offer.getShop(), offer.getPrice(), link, offer.getUnit(), offer.getSurveyedAt()));
        }

        return scaled(label(name, item), offers, quote.getTrend(), item);
    }

    // 가격이 없는 재료 한 줄. surveyed 가 false 면 "조사 대상 아님"
    //
    // 목록에서 빼지 않고 빈 줄로 넣는 이유 — 빼면 화면의 재료 목록과
    // 가격표의 줄 수가 어긋난다. "이 재료는 값을 모른다" 도 정보다
    private PriceItemVO emptyItem(String name, JsonNode item, boolean surveyed) {
        PriceItemVO price = new PriceItemVO();
        price.setName(label(name, item));
        // null 이 아니라 빈 목록을 넣는다. 화면이 offers.map 을 바로 쓸 수 있다
        price.setOffers(new ArrayList<>());
        price.setSurveyed(surveyed);
        return price;
    }

    /**
     * 판매 단위 가격(1kg 12,000원)을 레시피 분량 가격(300g 3,600원)으로 바꾼다
     *
     * 중간에 못 하게 되는 지점이 셋 있고, 그때마다 거기까지 채운 것을
     * 그대로 돌려준다. 금액만 비고 이름 · 시장별 가격은 남는다 —
     * 화면은 금액이 null 이면 "가격 정보 없음" 으로 그린다.
     */
    private PriceItemVO scaled(String label, List<PriceOfferVO> offers, JsonNode trend, JsonNode item) {
        PriceItemVO price = new PriceItemVO();
        price.setName(label);
        price.setOffers(offers);
        price.setTrend(trend);
        // 여기까지 왔으면 조사 품목은 맞다. 값을 받았는지는 offers 로 가린다
        price.setSurveyed(true);

        if (offers.isEmpty()) {
            return price; // 이번에 가격을 못 받아옴
            // surveyed 는 true 로 남는다. 이 둘의 조합이 "조사 품목인데
            // 이번에만 못 받았다" 를 뜻한다 (PriceItemVO 주석의 표 참고)
        }

        price.setNeedText(needText(item));

        // 첫 번째 시장의 단위를 기준으로 삼는다. 같은 품목이면 시장마다
        // 단위가 같게 와서 어느 것을 봐도 된다
        String saleUnit = offers.get(0).getUnit();
        if (saleUnit == null) {
            saleUnit = "";
        }

        // 판매 단위 중 얼마만큼 필요한지. 예) 1kg 짜리에서 300g 이면 0.3
        //
        // grams 를 먼저 넘기는 이유 — 무게가 적혀 있으면 그것이 가장 정확하다.
        // 없으면 quantity + unit("1큰술")로 어림잡아야 하고, 그건 재료마다
        // 다르다(간장 1큰술과 설탕 1큰술의 무게가 다르다). 그래서 이름도 넘긴다
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
        //
        // 범위로 주는 이유 — 시장마다 값이 꽤 다르다. 평균 하나만 보여주면
        // "이 가격에 살 수 있다" 로 읽히는데 그렇지 않다
        price.setLowCost(UnitConvert.scalePrice(offers.get(0).getPrice(), buy));
        price.setHighCost(UnitConvert.scalePrice(offers.get(offers.size() - 1).getPrice(), buy));
        price.setMinUnit(minUnit);
        // 더 사야 할 때만 "얼마를 사게 되는지" 를 알려준다. 딱 맞을 때는
        // 쓸 말이 없고, 화면에 "1개 구입" 이 늘 붙으면 잔소리가 된다
        if (minUnit) {
            price.setBuyText(buyText(buy, saleUnit));
        }
        return price;
    }

    // 실제로 사게 되는 양. 예) "1개", "30구"
    //
    // saleUnit("1개", "30구")에서 숫자와 단위를 떼어내고, 숫자에 buy 를 곱한다.
    // 소수점 세 자리로 자르는 이유 — 계산 과정에서 0.30000000000000004 같은
    // 값이 나오는데, 화면에 그대로 보이면 안 된다
    private String buyText(double buy, String saleUnit) {
        UnitConvert.Unit sale = UnitConvert.parseUnit(saleUnit);
        // 단위 모양을 못 읽으면 문장을 못 만든다. null 이면 화면이 이 줄을 안 그린다
        if (sale == null) {
            return null;
        }
        return UnitConvert.formatQuantity(Numbers.round(sale.getSize() * buy, 3)) + sale.getToken();
    }

    // 레시피에 필요한 양. 예) "1큰술", "0.5개"
    //
    // 분량이 안 적힌 재료("소금 약간")도 있어서 null 을 정상으로 본다.
    // 그때는 이름만 보여준다 (아래 label)
    private String needText(JsonNode item) {
        Double quantity = number(item.get("quantity"));
        String unit = item.path("unit").asString(null);
        if (quantity == null || unit == null || unit.isBlank()) {
            return null;
        }
        return UnitConvert.formatQuantity(quantity) + unit;
    }

    // 화면에 보여줄 재료 이름. 예) "두부" + 1모 → "두부 1모"
    //
    // 서버가 붙여주는 이유 — 화면이 재료 목록과 가격표를 따로 읽어
    // 짝지으려면 순서에 기대야 한다. 이름에 분량이 붙어 있으면
    // 가격표만 봐도 무엇의 가격인지 읽힌다
    private String label(String name, JsonNode item) {
        String need = needText(item);
        if (need == null) {
            return name;
        }
        return name + " " + need;
    }

    // JSON 숫자 → Double (숫자가 아니면 null)
    //
    // asDouble() 만 쓰면 값이 없을 때 0.0 이 온다. 0 은 "분량이 0" 으로
    // 읽혀서 계산이 0원이 된다. 숫자인지 먼저 보고 아니면 null 을 준다
    private Double number(JsonNode node) {
        if (node == null || !node.isNumber()) {
            return null;
        }
        return node.asDouble();
    }
}
