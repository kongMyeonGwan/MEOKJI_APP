package com.meokji.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 카탈로그 · 찜 · 미니게임 응답을 정답지(fixtures)와 대조한다.
 */
@SpringBootTest
@DisplayName("카탈로그 · 찜 · 게임 정답지 대조")
class CatalogLikeGameFixtureTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;
    private String auth;

    @BeforeEach
    void setUp() throws Exception {
        // .apply(springSecurity()) 는 보안 필터를 붙인다. 이 줄이 없으면 MockMvc 가
        // Controller 를 바로 불러서 JwtAuthenticationFilter 를 건너뛴다. 그러면
        // @AuthenticationPrincipal 이 늘 null 이고 로그인이 필요한 주소도 통과해서,
        // 테스트는 통과하는데 실제 서버와는 다른 길을 재는 꼴이 된다.
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        MvcResult login = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"minji@example.com\","
                                + "\"password\":\"meokji1234!\"}"))
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
    @DisplayName("GET /api/catalog - 파일에서 그대로 내려주므로 본문이 같다")
    void catalogMatchesFixture() throws Exception {
        JsonNode want = fixture("catalog").get("body");

        MvcResult result = mvc.perform(get("/api/catalog")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        // catalog.json 을 그대로 복사했으므로 글자까지 같아야 한다.
        // 다르면 복사가 어긋났거나 내려보내는 칸이 바뀐 것이다.
        assertThat(body(result)).isEqualTo(want);
    }

    @Test
    @DisplayName("GET /api/catalog/regions - 시·도 17개와 시·군·구 목록이 같다")
    void regionsMatchFixture() throws Exception {
        JsonNode want = fixture("catalog_regions").get("body");

        MvcResult result = mvc.perform(get("/api/catalog/regions")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(body(result)).isEqualTo(want);
    }

    @Test
    @DisplayName("카탈로그는 로그인 없이 볼 수 있다")
    void catalogIsPublic() throws Exception {
        assertThat(mvc.perform(get("/api/catalog")).andReturn().getResponse().getStatus())
                .isEqualTo(200);
        assertThat(mvc.perform(get("/api/catalog/regions")).andReturn()
                .getResponse().getStatus()).isEqualTo(200);
    }

    @Test
    @DisplayName("찜하기 -> 목록 -> 다시 눌러 해제 -> 비우기")
    void likeToggleRoundTrip() throws Exception {
        JsonNode wantToggle = fixture("likes_toggle_on").get("body");

        // 1) 켜기
        MvcResult on = mvc.perform(put("/api/likes")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"food_name\":\"김치찌개\"}"))
                .andReturn();

        assertThat(on.getResponse().getStatus()).isEqualTo(200);

        JsonNode onBody = body(on);
        assertThat(onBody.propertyNames())
                .containsExactlyInAnyOrderElementsOf(wantToggle.propertyNames());
        assertThat(onBody.get("food_name").asString()).isEqualTo("김치찌개");
        assertThat(onBody.get("liked").asBoolean()).isTrue();
        assertThat(onBody.get("count").asInt()).isGreaterThanOrEqualTo(1);

        // 2) 목록에 보인다
        MvcResult listed = mvc.perform(get("/api/likes").header("Authorization", auth))
                .andReturn();
        assertThat(body(listed).get("names").toString()).contains("김치찌개");

        // 3) 같은 이름을 다시 눌러 해제
        MvcResult off = mvc.perform(put("/api/likes")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"food_name\":\"김치찌개\"}"))
                .andReturn();

        assertThat(body(off).get("liked").asBoolean())
                .as("같은 이름을 다시 누르면 뒤집힌다").isFalse();

        // 4) 해제한 것을 또 켤 수 있다. 행을 새로 만들지 않고 되살린다 -
        //    안 그러면 같은 음식의 행이 쌓인다.
        MvcResult again = mvc.perform(put("/api/likes")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"food_name\":\"김치찌개\"}"))
                .andReturn();
        assertThat(body(again).get("liked").asBoolean()).isTrue();

        // 5) 비우기
        MvcResult cleared = mvc.perform(delete("/api/likes").header("Authorization", auth))
                .andReturn();

        assertThat(cleared.getResponse().getStatus()).isEqualTo(200);

        JsonNode clearedBody = body(cleared);
        assertThat(clearedBody.get("count").asInt()).isZero();
        assertThat(clearedBody.get("names").isEmpty()).isTrue();

        // 비운 뒤에 목록도 비어 있다
        MvcResult after = mvc.perform(get("/api/likes").header("Authorization", auth))
                .andReturn();
        assertThat(body(after).get("count").asInt()).isZero();
    }

    @Test
    @DisplayName("빈 음식 이름으로 찜하면 422 다")
    void blankFoodNameIsRejected() throws Exception {
        MvcResult result = mvc.perform(put("/api/likes")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"food_name\":\"  \"}"))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(422);
    }

    @Test
    @DisplayName("POST /api/games/{key}/plays - 게임 키가 DB 글자로 바뀐다")
    void gamePlayMatchesFixture() throws Exception {
        JsonNode want = fixture("games_play").get("body");

        MvcResult result = mvc.perform(post("/api/games/worldcup/plays")
                        .header("Authorization", auth))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(201);

        JsonNode actual = body(result);
        assertThat(actual.propertyNames())
                .containsExactlyInAnyOrderElementsOf(want.propertyNames());
        assertThat(actual.get("game_key").asString()).isEqualTo("worldcup");
        assertThat(actual.get("game_type").asString())
                .as("worldcup 은 DDL 의 ck_game_histories_type 에서 A 다").isEqualTo("A");
        assertThat(actual.get("played_at").asString()).endsWith("Z");
    }

    @Test
    @DisplayName("세 게임 키가 모두 다른 글자로 간다")
    void allGameKeysMapToDistinctTypes() throws Exception {
        String[][] pairs = {{"worldcup", "A"}, {"balance", "B"}, {"roulette", "C"}};

        for (String[] pair : pairs) {
            MvcResult result = mvc.perform(post("/api/games/" + pair[0] + "/plays")
                            .header("Authorization", auth))
                    .andReturn();

            assertThat(body(result).get("game_type").asString())
                    .as("%s -> %s", pair[0], pair[1]).isEqualTo(pair[1]);
        }
    }

    @Test
    @DisplayName("없는 게임 키는 404 다")
    void unknownGameKeyIsNotFound() throws Exception {
        JsonNode want = fixture("games_bad_key");

        MvcResult result = mvc.perform(post("/api/games/no-such-game/plays")
                        .header("Authorization", auth))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(want.get("status").asInt());
        assertThat(body(result).get("detail").asString())
                .startsWith("그런 게임이 없습니다:");
    }

    @Test
    @DisplayName("찜 · 게임은 로그인이 필요하다")
    void loginRequired() throws Exception {
        assertThat(mvc.perform(get("/api/likes")).andReturn().getResponse().getStatus())
                .isEqualTo(401);
        assertThat(mvc.perform(post("/api/games/worldcup/plays")).andReturn()
                .getResponse().getStatus()).isEqualTo(401);
    }
}
