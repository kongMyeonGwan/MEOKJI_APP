package com.meokji.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 추천 결과를 정답지(fixtures)와 대조한다.
 *
 * <p>점수 계산이 이 포팅에서 가장 조용히 틀릴 수 있는 부분이다. 결과가 나오기는
 * 하는데 순서가 다르면 아무도 모른다. 그래서 <b>고른 메뉴와 순서까지</b> 본다.
 *
 * <p>날씨는 기본값이 {@code mock}(28도 맑음)이라 이 테스트에서는 바깥을 부르지
 * 않는다. 정답지는 실제 OpenWeather 로 뜬 것이라 기온이 달라서, 날씨를 켠 경우는
 * 값 대신 <b>규칙</b>을 확인한다 - 더운 날엔 시원한 것이 위로 온다.
 */
@SpringBootTest
@DisplayName("추천 정답지 대조")
class RecommendFixtureTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private JsonNode fixture(String name) throws Exception {
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("fixtures/" + name + ".json")) {
            assertThat(in).as("정답지가 없다: %s", name).isNotNull();
            return MAPPER.readTree(in);
        }
    }

    private JsonNode recommend(String body) throws Exception {
        MvcResult result = mvc.perform(post("/api/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        return MAPPER.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private static List<String> names(JsonNode response) {
        List<String> names = new ArrayList<>();

        for (JsonNode item : response.get("items")) {
            names.add(item.get("food_name").asString());
        }

        return names;
    }

    @Test
    @DisplayName("조건 없음 - 고른 메뉴와 순서가 정답지와 같다")
    void plainRecommendMatchesFixture() throws Exception {
        JsonNode want = fixture("recommend_plain").get("body");

        JsonNode actual = recommend("{\"limit\":5,\"use_weather\":false}");

        // 점수가 같으면 싼 순 -> 이름순으로 정렬한다. 그 규칙까지 맞아야
        // 순서가 같다 - 순서가 다르면 정렬 기준을 잘못 옮긴 것이다.
        assertThat(names(actual))
                .as("고른 메뉴와 순서")
                .isEqualTo(names(want));

        assertThat(actual.get("condition").asString())
                .isEqualTo(want.get("condition").asString());
        assertThat(actual.get("weather").isNull()).isTrue();
        assertThat(actual.get("bargains").isEmpty()).isTrue();
    }

    @Test
    @DisplayName("항목 칸이 정답지와 같다")
    void itemFieldsMatchFixture() throws Exception {
        JsonNode want = fixture("recommend_plain").get("body").get("items").get(0);

        JsonNode actual = recommend("{\"limit\":5,\"use_weather\":false}").get("items").get(0);

        assertThat(actual.propertyNames())
                .containsExactlyInAnyOrderElementsOf(want.propertyNames());
        assertThat(actual).isEqualTo(want);
    }

    @Test
    @DisplayName("카테고리 필터 - 한식만 나온다")
    void categoryFilterMatchesFixture() throws Exception {
        JsonNode want = fixture("recommend_category").get("body");

        JsonNode actual = recommend(
                "{\"limit\":5,\"use_weather\":false,\"category\":\"한식\"}");

        assertThat(names(actual)).isEqualTo(names(want));
        assertThat(actual.get("condition").asString()).isEqualTo("한식");

        for (JsonNode item : actual.get("items")) {
            assertThat(item.get("category").asString()).isEqualTo("한식");
        }
    }

    @Test
    @DisplayName("카탈로그에 없는 카테고리는 0건이다 (404 가 아니다)")
    void emptyCategoryReturnsEmptyList() throws Exception {
        JsonNode want = fixture("recommend_empty_category").get("body");

        JsonNode actual = recommend(
                "{\"limit\":5,\"use_weather\":false,\"category\":\"양식\"}");

        // 카탈로그가 한식 76 · 국탕 15 로 쏠려 있어서 양식은 0종이다.
        // 화면은 빈 상태를 견딘다 - 404 를 내면 화면이 깨진 것처럼 보인다.
        assertThat(actual.get("items").isEmpty()).isTrue();
        assertThat(names(actual)).isEqualTo(names(want));
    }

    @Test
    @DisplayName("예산 필터 - 범위 안의 것만 나오고 문구에 적힌다")
    void budgetFilterMatchesFixture() throws Exception {
        JsonNode want = fixture("recommend_budget").get("body");

        JsonNode actual = recommend(
                "{\"limit\":5,\"use_weather\":false,\"budget_min\":3000,\"budget_max\":8000}");

        assertThat(names(actual)).isEqualTo(names(want));
        assertThat(actual.get("condition").asString())
                .as("천 단위 쉼표까지 같아야 한다")
                .isEqualTo(want.get("condition").asString());

        for (JsonNode item : actual.get("items")) {
            assertThat(item.get("estimated_cost").asInt()).isBetween(3000, 8000);
        }
    }

    @Test
    @DisplayName("검색어 - 점수 순서가 정답지와 같다")
    void queryScoringMatchesFixture() throws Exception {
        JsonNode want = fixture("recommend_query").get("body");

        JsonNode actual = recommend(
                "{\"limit\":5,\"use_weather\":false,\"query\":\"얼큰한 국물\"}");

        // 이름 10점 · 카테고리/태그 6점 · 재료 4점 · 설명 2점. 배점이 하나라도
        // 다르면 순서가 달라진다.
        assertThat(names(actual)).isEqualTo(names(want));
        assertThat(actual.get("condition").asString()).isEqualTo("\"얼큰한 국물\"");
    }

    @Test
    @DisplayName("날씨를 켜면 mock(28도 맑음) 기준으로 시원한 것이 위로 온다")
    void weatherScoringPrefersColdWhenHot() throws Exception {
        JsonNode actual = recommend("{\"limit\":5,\"use_weather\":true}");

        JsonNode weather = actual.get("weather");
        assertThat(weather.isNull()).as("use_weather 를 켜면 날씨가 실려야 한다").isFalse();

        // 기본값이 mock 이라 바깥을 부르지 않는다. 28도 맑음이 고정으로 온다.
        assertThat(weather.get("temp").asInt()).isEqualTo(28);
        assertThat(weather.get("condition").asString()).isEqualTo("CLEAR");
        assertThat(weather.get("comment").asString()).contains("더운");
        assertThat(weather.get("pick_lead").asString()).contains("시원한");

        // 더운 날 점수에서 hot_weather_picks 가 8점으로 가장 강하다.
        // 첫 번째는 그 목록에 있거나 '시원한음식'·'면' 태그를 가져야 한다.
        JsonNode first = actual.get("items").get(0);
        assertThat(first.get("tags").toString() + first.get("food_name").asString())
                .as("더운 날 1등: %s", first.get("food_name").asString())
                .containsAnyOf("시원한음식", "면", "냉", "국수");

        // 문구에 날씨가 들어간다
        assertThat(actual.get("condition").asString()).contains("28°");
    }

    @Test
    @DisplayName("exclude 에 넣은 메뉴는 다시 나오지 않는다")
    void excludeIsRespected() throws Exception {
        JsonNode first = recommend("{\"limit\":3,\"use_weather\":false}");
        List<String> shown = names(first);

        StringBuilder excluded = new StringBuilder();
        for (String name : shown) {
            if (excluded.length() > 0) {
                excluded.append(',');
            }
            excluded.append('"').append(name).append('"');
        }

        JsonNode second = recommend(
                "{\"limit\":3,\"use_weather\":false,\"exclude\":[" + excluded + "]}");

        assertThat(names(second)).doesNotContainAnyElementsOf(shown);
    }

    @Test
    @DisplayName("예산이 너무 좁으면 3천원 넓혀서 한 번 더 본다")
    void budgetIsRelaxedWhenEmpty() throws Exception {
        // 1,600원(감자전)이 가장 싸다. 1,000원 이하로 걸면 0건이 나와야 하는데,
        // 3,000원을 넓히면 4,000원 이하가 되어 후보가 생긴다.
        JsonNode actual = recommend(
                "{\"limit\":5,\"use_weather\":false,\"budget_max\":1000}");

        assertThat(actual.get("items").isEmpty())
                .as("넓혀서 다시 보므로 비어 있지 않아야 한다").isFalse();

        for (JsonNode item : actual.get("items")) {
            assertThat(item.get("estimated_cost").asInt()).isLessThanOrEqualTo(4000);
        }

        // 문구에는 원래 예산이 적혀 있다. 넓힌 것을 적으면 사용자가 요청한 조건과
        // 달라 보인다.
        assertThat(actual.get("condition").asString()).isEqualTo("1,000원 이하");
    }

    @Test
    @DisplayName("로그인하지 않아도 추천이 된다")
    void guestCanRecommend() throws Exception {
        MvcResult result = mvc.perform(post("/api/recommendations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("Ban 재료가 추천에 반영된다")
    void bansAreApplied() throws Exception {
        MvcResult login = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"minji@example.com\","
                                + "\"password\":\"meokji1234!\"}"))
                .andReturn();

        String auth = "Bearer " + MAPPER.readTree(
                login.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("access_token").asString();

        // 알레르기로 돼지고기를 등록한다
        MvcResult created = mvc.perform(post("/api/bans")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ingredient_name\":\"돼지고기\",\"ban_type\":\"ALLERGY\"}"))
                .andReturn();

        long banId = MAPPER.readTree(
                created.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("id").asLong();

        try {
            MvcResult result = mvc.perform(post("/api/recommendations")
                            .header("Authorization", auth)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"limit\":12,\"use_weather\":false}"))
                    .andReturn();

            JsonNode actual = MAPPER.readTree(
                    result.getResponse().getContentAsString(StandardCharsets.UTF_8));

            // 알레르기 재료가 든 메뉴는 통째로 빠진다
            for (JsonNode item : actual.get("items")) {
                assertThat(item.get("food_name").asString())
                        .as("알레르기 재료가 든 메뉴는 빠져야 한다")
                        .doesNotContain("돼지고기");
            }

            assertThat(actual.get("condition").asString()).contains("못 먹는 재료 1건 제외");
        } finally {
            // 되돌려 놓는다. 남겨두면 다음 실행의 추천 결과가 달라진다.
            mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                            .delete("/api/bans/" + banId)
                            .header("Authorization", auth))
                    .andReturn();
        }
    }
}
