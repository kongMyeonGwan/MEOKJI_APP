package com.meokji.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import com.meokji.api.recipe.service.PriceService;
import com.meokji.api.util.IngredientMatcher;
import com.meokji.api.util.UnitConvert;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 재료 시세 — 이름 매칭 · 단위 환산 · 실제 KAMIS 호출.
 *
 * <p>가격 숫자는 조사일자에 따라 매일 바뀌므로 정답지와 값을 비교하지 않는다.
 * 대신 <b>계산 규칙</b>과 <b>값이 없는 이유의 구분</b>을 본다 - 그게 틀리면
 * 합계가 조용히 어긋난다.
 */
@SpringBootTest
@DisplayName("재료 시세")
class PriceTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private IngredientMatcher matcher;

    @Autowired
    private UnitConvert convert;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    // ---------------------------------------------------------------- 이름 매칭

    @Test
    @DisplayName("품목표 116개를 읽었다")
    void itemTableLoaded() {
        assertThat(matcher.itemCount()).isEqualTo(116);
    }

    @Test
    @DisplayName("손질 표현과 괄호를 떼고 품목을 찾는다")
    void normalizesBeforeMatching() {
        // "다진 마늘 (국산)" -> "마늘"
        assertThat(matcher.findCodes("다진 마늘 (국산)")).isNotNull();
        assertThat(matcher.findCodes("다진마늘")).isNotNull();
        assertThat(matcher.findCodes("채썬 양파")).isNotNull();

        // '생강' 의 '생' 을 떼면 '강' 이 되어 아무것도 못 찾는다.
        // 떼기 전 이름이 이미 품목이면 건드리지 않아야 한다.
        assertThat(matcher.findCodes("생강")).isNotNull();
    }

    @Test
    @DisplayName("가공식품은 원물 시세를 빌려 쓰지 않는다")
    void processedFoodsDoNotBorrowRawPrices() {
        // 부분일치가 이름 안에서 품목명을 찾기 때문에 그냥 두면 이렇게 된다.
        //   고추장 -> '고추' · 배추김치 -> '배추' · 깨소금 -> '소금'
        // 값이 틀리는 것도 문제지만, 화면이 "조사 대상이다" 라고 말하게 되는 게 더 나쁘다.
        for (String name : new String[] {"고추장", "배추김치", "깨소금", "두부", "국간장"}) {
            assertThat(matcher.findCodes(name))
                    .as("%s 는 조사 대상이 아니다", name)
                    .isNull();
        }
    }

    @Test
    @DisplayName("액젓은 멸치 시세에 걸리지 않는다")
    void anchovySauceIsNotAnchovy() {
        // 둘 다 두 글자라 길이로 못 가린다. 별칭으로 못박아야 한다.
        IngredientMatcher.Codes sauce = matcher.findCodes("멸치액젓");
        IngredientMatcher.Codes anchovy = matcher.findCodes("멸치");

        assertThat(sauce).as("액젓은 실제 조사 품목이다").isNotNull();
        assertThat(anchovy).isNotNull();
        assertThat(sauce).as("액젓과 멸치는 다른 품목이어야 한다").isNotEqualTo(anchovy);
    }

    @Test
    @DisplayName("물은 물어볼 대상이 아니다")
    void waterIsNotPriced() {
        for (String name : new String[] {"물", "찬물", "끓는물", "멸치 다시마 육수용 물"}) {
            assertThat(matcher.isNotPriced(name)).as(name).isTrue();
        }

        assertThat(matcher.isNotPriced("돼지고기")).isFalse();
    }

    // ---------------------------------------------------------------- 단위 환산

    @Test
    @DisplayName("무게 판매 단위는 쪼개서 산다")
    void weightUnitsAreDivisible() {
        // 고춧가루 1큰술(15g)을 1kg 시세로 환산하면 0.015배다. 올리지 않는다 -
        // 올리면 1큰술이 1kg(25,200원)이 되어 환산이 없애려던 문제로 돌아간다.
        Double ratio = convert.neededRatio(15.0, 1.0, "큰술", "1kg", "고춧가루");

        assertThat(ratio).isNotNull();
        assertThat(ratio).isCloseTo(0.015, org.assertj.core.data.Offset.offset(0.0001));

        Double buy = UnitConvert.buyRatio(ratio, "1kg");
        assertThat(buy).as("쪼갤 수 있으니 그대로").isEqualTo(ratio);
    }

    @Test
    @DisplayName("세는 판매 단위는 최소단위로 올려서 산다 (STEP 42)")
    void countUnitsRoundUpToMinimum() {
        // 무 0.2개가 필요한데 무를 0.2개 파는 가게는 없다. 1개를 산다.
        Double ratio = convert.neededRatio(200.0, 0.2, "개", "1개", "무");
        assertThat(ratio).isNotNull();

        Double buy = UnitConvert.buyRatio(ratio, "1개");
        assertThat(buy).as("0.2개 필요 -> 1개 구입").isEqualTo(1.0);
        assertThat(buy).isGreaterThan(ratio);

        // 계란 2개가 필요해도 KAMIS 는 30구 한 판으로 판다.
        Double eggRatio = convert.neededRatio(120.0, 2.0, "개", "30구", "계란");
        assertThat(eggRatio).isNotNull();
        assertThat(UnitConvert.buyRatio(eggRatio, "30구"))
                .as("2개 필요 -> 30구 한 판").isEqualTo(1.0);
    }

    @Test
    @DisplayName("2개/1개 가 3개가 되지 않는다")
    void exactMultiplesDoNotOverRound() {
        // 부동소수점으로 2.0000000000000004 가 되는 경우가 있다.
        // 그대로 올리면 두 개 살 것을 세 개 사게 된다.
        Double ratio = convert.neededRatio(200.0, 2.0, "개", "1개", "양파");

        assertThat(UnitConvert.buyRatio(ratio, "1개")).isEqualTo(2.0);
    }

    @Test
    @DisplayName("단위가 서로 다르면 무게로 돌아간다")
    void fallsBackToGramsWhenUnitsDiffer() {
        // 레시피는 "고등어 120g", KAMIS 는 "1마리". 한 마리 무게를 알아야 푼다.
        Double ratio = convert.neededRatio(120.0, 120.0, "g", "1마리", "고등어");

        assertThat(ratio).as("고등어 한 마리 무게를 알고 있어야 한다").isNotNull();
        assertThat(ratio).isLessThan(1.0);
    }

    @Test
    @DisplayName("견줄 수 없으면 null 이다 (0 으로 치지 않는다)")
    void unknownConversionIsNull() {
        // 모르는 재료의 세는 단위. 0 으로 치면 합계가 조용히 작아진다.
        assertThat(convert.neededRatio(100.0, 1.0, "개", "1마리", "듣보잡재료")).isNull();
        assertThat(convert.neededRatio(null, 1.0, "큰술", "1kg", "고춧가루")).isNull();
    }

    @Test
    @DisplayName("가격은 1원 밑으로 내려가지 않는다")
    void priceNeverDropsBelowOne() {
        // 참깨 1작은술을 1kg 시세로 환산하면 0원에 가깝다. 0원으로 보여주면
        // 공짜처럼 읽힌다.
        assertThat(UnitConvert.scalePrice(10000, 0.00001)).isEqualTo(1);
        assertThat(UnitConvert.scalePrice(1000, 0.5)).isEqualTo(500);
    }

    @Test
    @DisplayName("0.5 는 \"0.5\", 2.0 은 \"2\" 로 적는다")
    void quantityFormatting() {
        assertThat(UnitConvert.formatQuantity(0.5)).isEqualTo("0.5");
        assertThat(UnitConvert.formatQuantity(2.0)).isEqualTo("2");
        assertThat(UnitConvert.formatQuantity(0.25)).isEqualTo("0.25");
        assertThat(UnitConvert.formatQuantity(30)).isEqualTo("30");
    }

    // ---------------------------------------------------------- 실제 KAMIS 호출

    @Test
    @DisplayName("GET /api/recipes/김치찌개 - 시세가 붙어 온다")
    void recipeCarriesPrices() throws Exception {
        MvcResult result = mvc.perform(get("/api/recipes/{name}", "김치찌개")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        JsonNode body = MAPPER.readTree(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        JsonNode prices = body.get("prices");

        // 재료 9개마다 한 줄씩. 값이 없는 재료도 줄은 있어야 한다 -
        // 없으면 화면이 재료 목록과 가격표의 줄 수가 안 맞는다.
        assertThat(prices.size())
                .as("재료 수와 가격 줄 수가 같아야 한다")
                .isEqualTo(body.get("ingredients_json").size());

        int surveyed = 0;
        int withOffers = 0;

        for (JsonNode item : prices) {
            // 칸이 하나도 빠지지 않아야 한다
            assertThat(item.propertyNames()).contains(
                    "name", "offers", "trend", "need_text",
                    "low_cost", "high_cost", "min_unit", "buy_text", "surveyed");

            // 이름에 분량이 붙어 있다 ("돼지고기 300g")
            assertThat(item.get("name").asString()).contains(" ");

            // need_text 는 조사가격이 있을 때만 채운다. 값은 알 수 있지만
            // 정답지가 그 모양이라 맞춘다 (PriceServiceImpl: 가격이 없으면 그 전에 돌려준다).
            if (item.get("offers").isEmpty()) {
                assertThat(item.get("need_text").isNull()).as("%s", item.get("name")).isTrue();
            } else {
                assertThat(item.get("need_text").isNull()).isFalse();
            }

            if (item.get("surveyed").asBoolean()) {
                surveyed++;
            }
            if (!item.get("offers").isEmpty()) {
                withOffers++;

                // 조사가격이 있으면 계산된 값도 있어야 한다
                assertThat(item.get("low_cost").isNull()).isFalse();
                assertThat(item.get("high_cost").isNull()).isFalse();
                assertThat(item.get("low_cost").asInt())
                        .isLessThanOrEqualTo(item.get("high_cost").asInt());

                JsonNode first = item.get("offers").get(0);
                assertThat(first.get("link").asString()).startsWith("https://www.kamis.or.kr");
                assertThat(first.get("unit").asString()).isNotBlank();
            }
        }

        // 두부 · 설탕 · 식용유는 가공식품이라 조사 대상이 아니다. 6개가 조사 대상이다.
        assertThat(surveyed).as("조사 대상 재료 수").isEqualTo(6);
        assertThat(withOffers).as("실제로 값을 받은 재료 수").isGreaterThan(0);
    }

    @Test
    @DisplayName("값이 없는 이유를 구분한다")
    void distinguishesWhyPriceIsMissing() throws Exception {
        MvcResult result = mvc.perform(get("/api/recipes/{name}", "김치찌개")).andReturn();

        JsonNode prices = MAPPER.readTree(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8)).get("prices");

        for (JsonNode item : prices) {
            String name = item.get("name").asString();

            // 두부는 콩 가공품이라 앞으로도 값이 없다.
            if (name.startsWith("두부")) {
                assertThat(item.get("surveyed").asBoolean())
                        .as("두부는 조사 대상이 아니다").isFalse();
                assertThat(item.get("offers").isEmpty()).isTrue();
            }

            // 돼지고기는 조사 대상이다. 값이 비었다면 이번에 못 받은 것이다.
            if (name.startsWith("돼지고기")) {
                assertThat(item.get("surveyed").asBoolean())
                        .as("돼지고기는 조사 대상이다").isTrue();
            }
        }
    }
}
