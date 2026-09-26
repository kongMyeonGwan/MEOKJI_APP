package com.meokji.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * 파이썬 서버의 응답(정답지)과 글자 단위로 대조한다.
 *
 * <p>정답지는 {@code src/test/resources/fixtures/*.json} 이고,
 * {@code backend/} 의 FastAPI 서버에서 떠온 것이다. 스프링이 같은 JSON 을 내는지
 * 여기서 확인한다. 이게 없으면 포팅이 "돌아가는 것 같다" 로 끝난다.
 *
 * <p>DB 에 붙는다. 파이썬 쪽 {@code tests/test_api.py} 와 같은 성격이다.
 * 옮기는 엔드포인트가 늘어날 때마다 이 파일에 대조 하나를 추가한다.
 */
@SpringBootTest
@DisplayName("파이썬 응답 정답지 대조")
class FixtureTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    private MockMvc mvc() {
        if (mvc == null) {
            mvc = MockMvcBuilders.webAppContextSetup(context).build();
        }
        return mvc;
    }

    /** 정답지 파일 하나를 읽는다. */
    private JsonNode fixture(String name) throws Exception {
        String path = "fixtures/" + name + ".json";
        try (InputStream in = getClass().getClassLoader().getResourceAsStream(path)) {
            assertThat(in).as("정답지가 없다: %s", path).isNotNull();
            return MAPPER.readTree(in);
        }
    }

    @Test
    @DisplayName("GET /api/health - 상태코드와 본문이 파이썬과 같다")
    void healthMatchesFixture() throws Exception {
        JsonNode want = fixture("health");

        MvcResult result = mvc().perform(get("/api/health")).andReturn();
        String raw = result.getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(result.getResponse().getStatus()).isEqualTo(want.get("status").asInt());

        JsonNode actual = MAPPER.readTree(raw);
        assertThat(actual).isEqualTo(want.get("body"));
    }

    @Test
    @DisplayName("없는 주소는 404 이고 본문이 {\"detail\": \"Not Found\"} 다")
    void unknownPathIsNotFound() throws Exception {
        // 이걸 확인하는 이유: @ExceptionHandler(Exception.class) 가 스프링의 404 까지
        // 삼켜서 500 으로 내보낸 적이 있다. 서버가 고장난 것과 주소를 잘못 부른 것이
        // 구분되지 않는다.
        MvcResult result = mvc().perform(get("/api/no-such-path")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(404);

        JsonNode body = MAPPER.readTree(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(body.get("detail").asString()).isEqualTo("Not Found");
    }

    @Test
    @DisplayName("허용되지 않은 메서드는 405 다")
    void wrongMethodIsNotAllowed() throws Exception {
        MvcResult result = mvc().perform(post("/api/health")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(405);

        JsonNode body = MAPPER.readTree(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(body.get("detail").asString()).isEqualTo("Method Not Allowed");
    }

    @Test
    @DisplayName("정답지 38개가 그대로 있다")
    void fixturesArePresent() throws Exception {
        JsonNode index = fixture("_index");

        assertThat(index.get("count").asInt()).isEqualTo(37);
        assertThat(index.get("files")).isNotEmpty();

        // 아직 안 옮긴 것까지 세어둔다. 남은 일의 크기가 숫자로 보이게 하려는 것이다.
        System.out.printf("정답지 %d개 · 지금 대조하는 것 1개 (health)%n",
                index.get("count").asInt());
    }
}
