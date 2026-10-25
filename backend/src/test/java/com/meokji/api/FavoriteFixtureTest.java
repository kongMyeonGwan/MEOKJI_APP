package com.meokji.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 찜하기 응답을 정답지(fixtures)와 대조한다.
 *
 * <p>데모 계정(minji)은 찜이 0개인 상태가 기본이다. 정답지도 그걸 전제로
 * 적혀 있어서(favorites_list 의 count 가 0) 각 테스트는 <b>비우고 시작해서
 * 비우고 끝낸다.</b> 남겨두면 다음 실행의 정답지가 어긋난다.
 */
@SpringBootTest
@DisplayName("찜하기 정답지 대조")
class FavoriteFixtureTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private static final String MEMBER_EMAIL = "minji@example.com";
    private static final String MEMBER_PASSWORD = "meokji1234!";

    private static final String FOOD = "김치찌개";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;
    private String auth;
    private Long userId;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();

        MvcResult logged = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + MEMBER_EMAIL + "\","
                                + "\"password\":\"" + MEMBER_PASSWORD + "\"}"))
                .andReturn();

        auth = "Bearer " + body(logged).get("accessToken").asString();
        userId = body(logged).get("user").get("id").asLong();

        // 비우고 시작한다
        mvc.perform(delete("/api/favorites").header("Authorization", auth)).andReturn();
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

    private MvcResult toggle(String foodName) throws Exception {
        return mvc.perform(put("/api/favorites")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"foodName\":\"" + foodName + "\"}"))
                .andReturn();
    }

    /** 그 음식으로 남아 있는 행 수 (켜진 것 · 꺼진 것 전부) */
    private int rowCount(String foodName) {
        return jdbc.queryForObject(
                "SELECT COUNT(*) FROM recipe_likes WHERE user_id = ? AND food_name = ?",
                Integer.class, userId, foodName);
    }

    @Test
    @DisplayName("PUT /api/favorites - 누를 때마다 뒤집히고 개수가 따라온다")
    void toggleMatchesFixture() throws Exception {
        JsonNode wantOn = fixture("favorites_toggle_on").get("body");
        JsonNode wantOff = fixture("favorites_toggle_off").get("body");

        MvcResult on = toggle(FOOD);
        assertThat(on.getResponse().getStatus()).isEqualTo(200);
        assertThat(body(on)).as("찜 켜기").isEqualTo(wantOn);

        MvcResult off = toggle(FOOD);
        assertThat(off.getResponse().getStatus()).isEqualTo(200);
        assertThat(body(off)).as("찜 끄기").isEqualTo(wantOff);

        // 한 번 더 켜도 같은 모양이어야 한다
        assertThat(body(toggle(FOOD))).as("다시 켜기").isEqualTo(wantOn);

        toggle(FOOD); // 끄고 끝낸다
    }

    @Test
    @DisplayName("껐다 켰다 해도 행은 하나다 - 되살리고 새로 넣지 않는다")
    void toggleRevivesInsteadOfInserting() throws Exception {
        toggle(FOOD);
        toggle(FOOD);
        toggle(FOOD);
        toggle(FOOD);

        assertThat(rowCount(FOOD))
                .as("uq_recipe_likes 는 use_yn='Y' 인 행만 막는다. 새로 INSERT 하면 죽은 행이 쌓인다")
                .isEqualTo(1);
    }

    @Test
    @DisplayName("GET /api/favorites - 찜한 음식의 정보가 items 에 담겨 온다")
    void listCarriesFoodInfo() throws Exception {
        JsonNode empty = fixture("favorites_list").get("body");

        MvcResult before = mvc.perform(get("/api/favorites").header("Authorization", auth)).andReturn();
        assertThat(body(before).get("names")).as("비운 뒤 목록").isEqualTo(empty.get("names"));
        assertThat(body(before).get("count").asInt()).isEqualTo(0);

        toggle(FOOD);

        JsonNode listed = body(mvc.perform(get("/api/favorites").header("Authorization", auth)).andReturn());

        assertThat(listed.get("count").asInt()).isEqualTo(1);
        assertThat(listed.get("names").get(0).asString()).isEqualTo(FOOD);

        // items 는 표에 없는 칸이다. 이름으로 카탈로그에서 찾아 채운다
        JsonNode item = listed.get("items").get(0);
        assertThat(item.get("foodName").asString()).isEqualTo(FOOD);
        assertThat(item.get("category").asString()).as("카탈로그에서 채운 칸").isNotBlank();
        assertThat(item.get("cookTimeMinutes").asInt()).isGreaterThan(0);
        assertThat(item.get("estimatedCost").asInt()).isGreaterThan(0);

        toggle(FOOD);
    }

    @Test
    @DisplayName("카탈로그에 없는 이름도 찜할 수 있다. 그때는 foodName 만 온다")
    void unknownFoodKeepsNameOnly() throws Exception {
        String unknown = "테스트찜용없는음식";

        toggle(unknown);

        JsonNode listed = body(mvc.perform(get("/api/favorites").header("Authorization", auth)).andReturn());
        JsonNode item = listed.get("items").get(0);

        assertThat(item.get("foodName").asString()).isEqualTo(unknown);
        assertThat(item.get("category").isNull()).as("카탈로그에 없으니 빈 칸").isTrue();

        toggle(unknown);

        // 되돌린다 - 꺼진 행까지 지워서 다음 실행에 영향을 주지 않는다
        jdbc.update("DELETE FROM recipe_likes WHERE user_id = ? AND food_name = ?", userId, unknown);
    }

    @Test
    @DisplayName("DELETE /api/favorites - 전부 비운다")
    void clearEmptiesAll() throws Exception {
        toggle(FOOD);
        toggle("된장찌개");

        JsonNode cleared = body(mvc.perform(delete("/api/favorites")
                        .header("Authorization", auth))
                .andReturn());

        assertThat(cleared.get("count").asInt()).isEqualTo(0);
        assertThat(cleared.get("names")).isEmpty();

        JsonNode listed = body(mvc.perform(get("/api/favorites").header("Authorization", auth)).andReturn());
        assertThat(listed.get("count").asInt()).isEqualTo(0);
    }

    @Test
    @DisplayName("로그인 없이 부르면 401")
    void loginRequired() throws Exception {
        assertThat(mvc.perform(get("/api/favorites")).andReturn().getResponse().getStatus())
                .isEqualTo(401);

        assertThat(mvc.perform(put("/api/favorites")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"foodName\":\"" + FOOD + "\"}"))
                .andReturn().getResponse().getStatus())
                .isEqualTo(401);

        assertThat(mvc.perform(delete("/api/favorites")).andReturn().getResponse().getStatus())
                .isEqualTo(401);
    }

    /**
     * 음식 이름이 비면 422.
     *
     * 토큰 없이 빈 이름을 보내도 401 이 아니라 422 다. @Valid 가 컨트롤러에
     * 들어가기 전에 돌아서, 로그인 확인까지 가지 않기 때문이다.
     */
    @Test
    @DisplayName("음식 이름이 비면 422")
    void blankNameRejected() throws Exception {
        MvcResult result = mvc.perform(put("/api/favorites")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"foodName\":\"  \"}"))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(422);
    }
}
