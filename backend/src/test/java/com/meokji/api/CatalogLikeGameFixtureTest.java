package com.meokji.api;

import static org.assertj.core.api.Assertions.assertThat;
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
 * 카탈로그 · 미니게임 응답을 정답지(fixtures)와 대조한다.
 */
@SpringBootTest
@DisplayName("카탈로그 · 게임 정답지 대조")
class CatalogLikeGameFixtureTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;
    private String auth;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();

        MvcResult login = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"minji@example.com\","
                                + "\"password\":\"meokji1234!\"}"))
                .andReturn();

        auth = "Bearer " + body(login).get("accessToken").asString();
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
        assertThat(actual.get("gameKey").asString()).isEqualTo("worldcup");
        assertThat(actual.get("gameType").asString())
                .as("worldcup 은 DDL 의 ck_game_histories_type 에서 A 다").isEqualTo("A");
        assertThat(actual.get("playedAt").asString()).endsWith("Z");
    }

    @Test
    @DisplayName("세 게임 키가 모두 다른 글자로 간다")
    void allGameKeysMapToDistinctTypes() throws Exception {
        String[][] pairs = {{"worldcup", "A"}, {"balance", "B"}, {"roulette", "C"}};

        for (String[] pair : pairs) {
            MvcResult result = mvc.perform(post("/api/games/" + pair[0] + "/plays")
                            .header("Authorization", auth))
                    .andReturn();

            assertThat(body(result).get("gameType").asString())
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
    @DisplayName("게임은 로그인이 필요하다")
    void loginRequired() throws Exception {
        assertThat(mvc.perform(post("/api/games/worldcup/plays")).andReturn()
                .getResponse().getStatus()).isEqualTo(401);
    }
}
