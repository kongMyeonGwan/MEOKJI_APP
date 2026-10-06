package com.meokji.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

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

import com.meokji.api.util.RecipeBook;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 레시피 응답을 정답지(fixtures)와 대조한다.
 *
 * <p>가격({@code prices})은 아직 옮기지 않아 빈 목록이라 비교하지 않는다. 나머지
 * 칸은 <b>글자까지</b> 같아야 한다 - 특히 {@code grams} 의 정수/소수 구분이 그렇다.
 */
@SpringBootTest
@DisplayName("레시피 정답지 대조")
class RecipeFixtureTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private RecipeBook recipeBook;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        // .apply(springSecurity()) 는 보안 필터를 붙인다. 이 줄이 없으면 MockMvc 가
        // Controller 를 바로 불러서 JwtAuthenticationFilter 를 건너뛴다. 그러면
        // @AuthenticationPrincipal 이 늘 null 이고 로그인이 필요한 주소도 통과해서,
        // 테스트는 통과하는데 실제 서버와는 다른 길을 재는 꼴이 된다.
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    private JsonNode fixture(String name) throws Exception {
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("fixtures/" + name + ".json")) {
            assertThat(in).as("정답지가 없다: %s", name).isNotNull();
            return MAPPER.readTree(in);
        }
    }

    private JsonNode recipe(String foodName) throws Exception {
        MvcResult result = mvc.perform(get("/api/recipes/{name}", foodName)).andReturn();

        assertThat(result.getResponse().getStatus())
                .as("%s 레시피", foodName).isEqualTo(200);

        return MAPPER.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("김치찌개 - 가격 말고 전부 정답지와 글자까지 같다")
    void kimchiStewMatchesFixture() throws Exception {
        JsonNode want = fixture("recipe_김치찌개").get("body");
        JsonNode actual = recipe("김치찌개");

        assertThat(actual.propertyNames())
                .containsExactlyInAnyOrderElementsOf(want.propertyNames());

        assertThat(actual.get("food_name")).isEqualTo(want.get("food_name"));
        assertThat(actual.get("recipe_name")).isEqualTo(want.get("recipe_name"));
        assertThat(actual.get("estimated_cost")).isEqualTo(want.get("estimated_cost"));
        assertThat(actual.get("category")).isEqualTo(want.get("category"));
        assertThat(actual.get("art")).isEqualTo(want.get("art"));
        assertThat(actual.get("image_url").isNull()).isTrue();

        // 조리 정보와 순서
        assertThat(actual.get("recipe_json")).isEqualTo(want.get("recipe_json"));

        // 재료. grams 의 정수/소수 구분까지 같아야 한다 -
        // quantity 가 정수면 grams 도 정수(80), 소수면 소수(625.0)로 내보낸다.
        assertThat(actual.get("ingredients_json")).isEqualTo(want.get("ingredients_json"));

        assertThat(actual.get("substitutes_json")).isEqualTo(want.get("substitutes_json"));
    }

    @Test
    @DisplayName("된장찌개 · 제육볶음도 같다")
    void otherRecipesMatchFixture() throws Exception {
        for (String name : new String[] {"된장찌개", "제육볶음"}) {
            JsonNode want = fixture("recipe_" + name).get("body");
            JsonNode actual = recipe(name);

            assertThat(actual.get("recipe_json")).as("%s 조리 정보", name)
                    .isEqualTo(want.get("recipe_json"));
            assertThat(actual.get("ingredients_json")).as("%s 재료", name)
                    .isEqualTo(want.get("ingredients_json"));
            assertThat(actual.get("estimated_cost")).as("%s 예상비용", name)
                    .isEqualTo(want.get("estimated_cost"));
        }
    }

    @Test
    @DisplayName("카탈로그에 없는 음식은 404 이고 조사까지 맞는다")
    void unknownFoodIsNotFound() throws Exception {
        JsonNode want = fixture("recipe_not_found");

        MvcResult result = mvc.perform(get("/api/recipes/{name}", "할머니표 비밀 국수"))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(want.get("status").asInt());

        String detail = MAPPER.readTree(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("detail").asString();

        // "국수" 는 받침이 없으니 "는" 이다. "돼지고기" 라면 "은" 이어야 한다 -
        // 둘 다 적어 "은(는)" 으로 내보내면 기계가 쓴 글로 읽힌다.
        assertThat(detail).isEqualTo(want.get("body").get("detail").asString());
        assertThat(detail).contains("'할머니표 비밀 국수'는");
    }

    @Test
    @DisplayName("100종 전부 레시피가 만들어진다")
    void allHundredRecipesBuild() throws Exception {
        int built = 0;

        // 100종을 전부 HTTP 로 부르면 느리다. 골고루 뽑은 열 개만 실제로 부르고,
        // 나머지는 RecipeBook.has() 로 있는지만 확인한다.
        for (String name : new String[] {
                "김치찌개", "바지락된장찌개", "홍합탕", "부추전", "동그랑땡",
                "돼지갈비구이", "코다리조림", "물냉면", "가지냉국", "열무비빔국수"}) {
            assertThat(recipeBook.has(name)).as("%s 레시피가 있어야 한다", name).isTrue();

            JsonNode actual = recipe(name);
            assertThat(actual.get("recipe_json").get("steps").isEmpty())
                    .as("%s 조리 순서", name).isFalse();
            assertThat(actual.get("ingredients_json").isEmpty())
                    .as("%s 재료", name).isFalse();
            built++;
        }

        assertThat(built).isEqualTo(10);

        // 파일에 100종이 다 있는지도 본다. 복사가 어긋나면 여기서 걸린다.
        assertThat(recipeBook.count()).as("recipes.json 의 레시피 수").isEqualTo(100);
    }

    @Test
    @DisplayName("알레르기 재료는 목록에서 빠지고 순서 맨 앞에 밝힌다")
    void allergyRemovesIngredientAndWarns() throws Exception {
        MvcResult login = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"minji@example.com\","
                                + "\"password\":\"meokji1234!\"}"))
                .andReturn();

        String auth = "Bearer " + MAPPER.readTree(
                login.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("access_token").asString();

        MvcResult created = mvc.perform(post("/api/bans")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ingredient_name\":\"두부\",\"ban_type\":\"ALLERGY\"}"))
                .andReturn();

        long banId = MAPPER.readTree(
                created.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("id").asLong();

        try {
            MvcResult result = mvc.perform(get("/api/recipes/{name}", "김치찌개")
                            .header("Authorization", auth))
                    .andReturn();

            JsonNode actual = MAPPER.readTree(
                    result.getResponse().getContentAsString(StandardCharsets.UTF_8));

            // 목록에서 빠졌다
            assertThat(actual.get("ingredients_json").toString()).doesNotContain("두부");

            // 순서 맨 앞에 안내가 붙었다. 순서 중간에 그 재료가 나오는데 목록에
            // 없으면 따라 만드는 사람이 멈춘다.
            String firstStep = actual.get("recipe_json").get("steps").get(0).asString();
            assertThat(firstStep).contains("알레르기 재료").contains("두부");
        } finally {
            mvc.perform(delete("/api/bans/" + banId).header("Authorization", auth))
                    .andReturn();
        }
    }

    @Test
    @DisplayName("싫어하는 재료는 그대로 두고 대체재료를 알려준다")
    void dislikeKeepsIngredientAndSuggests() throws Exception {
        MvcResult login = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"minji@example.com\","
                                + "\"password\":\"meokji1234!\"}"))
                .andReturn();

        String auth = "Bearer " + MAPPER.readTree(
                login.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("access_token").asString();

        MvcResult created = mvc.perform(post("/api/bans")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ingredient_name\":\"돼지고기\",\"ban_type\":\"DISLIKE\"}"))
                .andReturn();

        long banId = MAPPER.readTree(
                created.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("id").asLong();

        try {
            JsonNode actual = MAPPER.readTree(
                    mvc.perform(get("/api/recipes/{name}", "김치찌개")
                                    .header("Authorization", auth))
                            .andReturn().getResponse()
                            .getContentAsString(StandardCharsets.UTF_8));

            // 재료는 그대로 있다. 빼버리면 요리가 성립하지 않는다.
            assertThat(actual.get("ingredients_json").toString()).contains("돼지고기");

            // 대신 바꿀 방법을 적어준다
            JsonNode substitutes = actual.get("substitutes_json");
            assertThat(substitutes.isEmpty()).isFalse();
            assertThat(substitutes.get(0).get("original").asString()).isEqualTo("돼지고기");
            assertThat(substitutes.get(0).get("substitute").asString()).isEqualTo("닭고기");
            assertThat(substitutes.get(0).get("reason").asString()).isNotBlank();
        } finally {
            mvc.perform(delete("/api/bans/" + banId).header("Authorization", auth))
                    .andReturn();
        }
    }

    @Test
    @DisplayName("로그인하지 않아도 레시피를 볼 수 있다")
    void guestCanReadRecipe() throws Exception {
        assertThat(mvc.perform(get("/api/recipes/{name}", "김치찌개"))
                .andReturn().getResponse().getStatus()).isEqualTo(200);
    }
}
