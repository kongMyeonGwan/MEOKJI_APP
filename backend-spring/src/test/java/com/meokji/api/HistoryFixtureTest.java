package com.meokji.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

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

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 식사 기록 응답을 파이썬 정답지와 대조한다.
 *
 * <p>확정한 기록은 테스트 끝에 지운다. 데모 계정을 그대로 쓰기 때문에, 남겨두면
 * 요리 횟수와 누적 비용이 계속 올라가 다음 실행의 대조가 어긋난다.
 */
@SpringBootTest
@DisplayName("식사 기록 정답지 대조")
class HistoryFixtureTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private static final String MEMBER_EMAIL = "minji@example.com";
    private static final String MEMBER_PASSWORD = "meokji1234!";

    /** 확정 요청 본문. 레시피와 재료는 카탈로그가 주는 모양을 그대로 흉내냈다. */
    private static final String CREATE_BODY = """
            {
              "food_name": "테스트확정용음식",
              "recipe_name": "테스트 레시피",
              "recipe_json": {
                "cook_time_minutes": 25,
                "servings": 2,
                "difficulty": "쉬움",
                "steps": ["재료를 썬다.", "끓인다."]
              },
              "ingredients_json": [
                {"name": "돼지고기", "quantity": 300, "unit": "g", "grams": 300}
              ],
              "substitutes_json": [],
              "estimated_cost": 6900
            }
            """;

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;
    private String auth;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();

        MvcResult login = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + MEMBER_EMAIL + "\","
                                + "\"password\":\"" + MEMBER_PASSWORD + "\"}"))
                .andReturn();

        auth = "Bearer " + body(login).get("access_token").asString();
    }

    private JsonNode fixture(String name) throws Exception {
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("fixtures/" + name + ".json")) {
            assertThat(in).as("정답지가 없다: %s", name).isNotNull();
            return MAPPER.readTree(in);
        }
    }

    private JsonNode body(MvcResult result) throws Exception {
        return MAPPER.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("GET /api/histories - 페이지 칸과 항목 칸이 파이썬과 같다")
    void listMatchesFixture() throws Exception {
        JsonNode want = fixture("histories_list").get("body");

        MvcResult result = mvc.perform(get("/api/histories").header("Authorization", auth))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        JsonNode actual = body(result);

        // 페이지 칸 네 개 + items
        assertThat(actual.propertyNames())
                .containsExactlyInAnyOrderElementsOf(want.propertyNames());
        assertThat(actual.get("size").asInt()).isEqualTo(want.get("size").asInt());
        assertThat(actual.get("page").asInt()).isEqualTo(1);

        JsonNode items = actual.get("items");
        assertThat(items.isArray()).isTrue();
        assertThat(items).as("데모 기록이 있어야 한다 (seed_demo)").isNotEmpty();

        JsonNode wantItem = want.get("items").get(0);
        JsonNode item = items.get(0);

        assertThat(item.propertyNames())
                .containsExactlyInAnyOrderElementsOf(wantItem.propertyNames());

        // JSON 칸이 문자열이 아니라 JSON 이어야 한다. 문자열로 나가면 화면이
        // recipe_json.steps 를 읽지 못한다.
        assertThat(item.get("recipe_json").isObject())
                .as("recipe_json 은 객체여야 한다").isTrue();
        assertThat(item.get("ingredients_json").isArray())
                .as("ingredients_json 은 배열이어야 한다").isTrue();
        assertThat(item.get("recipe_json").has("steps")).isTrue();

        // 시각 형식이 파이썬과 같아야 한다 (뒤에 Z, 마이크로초 여섯 자리)
        assertThat(item.get("selected_at").asString()).endsWith("Z");
    }

    @Test
    @DisplayName("GET /api/histories/stats - 세 칸이 있고 전체 기준이다")
    void statsMatchesFixture() throws Exception {
        JsonNode want = fixture("histories_stats").get("body");

        MvcResult result = mvc.perform(
                        get("/api/histories/stats").header("Authorization", auth))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        JsonNode actual = body(result);
        assertThat(actual.propertyNames())
                .containsExactlyInAnyOrderElementsOf(want.propertyNames());

        // 목록의 total 과 요약의 count 가 같아야 한다. 페이징 전에는 요약이 한
        // 페이지만 세고 있어서 2페이지부터 숫자가 틀렸다(STEP 31).
        MvcResult listed = mvc.perform(get("/api/histories").header("Authorization", auth))
                .andReturn();

        assertThat(actual.get("count").asInt()).isEqualTo(body(listed).get("total").asInt());
    }

    @Test
    @DisplayName("범위를 벗어난 페이지는 404 가 아니라 빈 목록이다")
    void pageBeyondRangeIsEmpty() throws Exception {
        JsonNode want = fixture("histories_page2").get("body");

        MvcResult result = mvc.perform(get("/api/histories?page=2&size=2")
                        .header("Authorization", auth))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        JsonNode actual = body(result);
        assertThat(actual.get("page").asInt()).isEqualTo(2);
        assertThat(actual.get("size").asInt()).isEqualTo(2);
        assertThat(actual.get("items").isArray()).isTrue();

        // 정답지는 당시 DB 기준이라 건수가 다를 수 있다. 칸 모양만 맞춘다.
        assertThat(actual.propertyNames())
                .containsExactlyInAnyOrderElementsOf(want.propertyNames());
    }

    @Test
    @DisplayName("size 상한을 넘기면 422 다")
    void sizeOverLimitIsRejected() throws Exception {
        MvcResult result = mvc.perform(get("/api/histories?size=999")
                        .header("Authorization", auth))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(422);
        assertThat(body(result).get("detail").isArray()).isTrue();
    }

    @Test
    @DisplayName("shared 필터가 목록과 건수에 같이 걸린다")
    void sharedFilterAppliesToBoth() throws Exception {
        MvcResult all = mvc.perform(get("/api/histories").header("Authorization", auth))
                .andReturn();
        MvcResult notShared = mvc.perform(get("/api/histories?shared=false")
                        .header("Authorization", auth))
                .andReturn();
        MvcResult onlyShared = mvc.perform(get("/api/histories?shared=true")
                        .header("Authorization", auth))
                .andReturn();

        int total = body(all).get("total").asInt();
        int no = body(notShared).get("total").asInt();
        int yes = body(onlyShared).get("total").asInt();

        // COUNT 와 목록이 같은 조건을 봐야 한다. 한쪽에만 필터를 붙이면
        // "3페이지가 있다는데 열면 비어 있는" 일이 생긴다.
        assertThat(no + yes).as("공유한 것 + 안 한 것 = 전부").isEqualTo(total);
        assertThat(body(notShared).get("items").size()).isLessThanOrEqualTo(no);
    }

    @Test
    @DisplayName("확정 -> 목록에 보임 -> 삭제 -> 사라짐")
    void createAndDelete() throws Exception {
        MvcResult created = mvc.perform(post("/api/histories")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CREATE_BODY))
                .andReturn();

        assertThat(created.getResponse().getStatus()).isEqualTo(201);

        JsonNode item = body(created);
        long historyId = item.get("id").asLong();

        assertThat(item.get("food_name").asString()).isEqualTo("테스트확정용음식");
        assertThat(item.get("shared_post_id").isNull())
                .as("방금 만든 기록은 아직 공유했을 수 없다").isTrue();
        assertThat(item.get("art").isNull())
                .as("카탈로그에 없는 이름이라 썸네일이 없다").isTrue();

        // 넣은 JSON 이 그대로 돌아와야 한다. CLOB 왕복에서 깨지지 않는지 본다.
        assertThat(item.get("recipe_json").get("steps").size()).isEqualTo(2);
        assertThat(item.get("ingredients_json").get(0).get("name").asString())
                .isEqualTo("돼지고기");

        // 목록에 보인다
        MvcResult listed = mvc.perform(get("/api/histories").header("Authorization", auth))
                .andReturn();
        assertThat(body(listed).toString()).contains("테스트확정용음식");

        // 삭제
        MvcResult deleted = mvc.perform(delete("/api/histories/" + historyId)
                        .header("Authorization", auth))
                .andReturn();
        assertThat(deleted.getResponse().getStatus()).isEqualTo(204);

        // 사라졌다 (use_yn='N' 이라 행은 남아 있다)
        MvcResult after = mvc.perform(get("/api/histories").header("Authorization", auth))
                .andReturn();
        assertThat(body(after).toString()).doesNotContain("테스트확정용음식");

        // 없는 것을 또 지우면 404
        MvcResult again = mvc.perform(delete("/api/histories/" + historyId)
                        .header("Authorization", auth))
                .andReturn();
        assertThat(again.getResponse().getStatus()).isEqualTo(404);
        assertThat(body(again).get("detail").asString()).isEqualTo("기록을 찾을 수 없습니다.");
    }

    @Test
    @DisplayName("빈 음식 이름은 422 다")
    void blankFoodNameIsRejected() throws Exception {
        MvcResult result = mvc.perform(post("/api/histories")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"food_name\":\"  \",\"recipe_json\":{},"
                                + "\"ingredients_json\":[]}"))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(422);
        assertThat(body(result).get("detail").isArray()).isTrue();
    }

    @Test
    @DisplayName("로그인 없이 부르면 401 이다")
    void loginRequired() throws Exception {
        for (String path : new String[] {"/api/histories", "/api/histories/stats"}) {
            MvcResult result = mvc.perform(get(path)).andReturn();

            assertThat(result.getResponse().getStatus()).as(path).isEqualTo(401);
        }
    }
}
