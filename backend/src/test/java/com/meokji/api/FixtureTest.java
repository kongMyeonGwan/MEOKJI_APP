package com.meokji.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * 서버 응답을 정답지(fixtures)와 글자 단위로 대조한다.
 *
 * <p>정답지는 {@code src/test/resources/fixtures/*.json} 이고, 화면이 기대하는 응답
 * 모양이다. 서버가 같은 JSON 을 내는지 여기서 확인한다. 이게 없으면 응답 모양이
 * 바뀌어도 "돌아가는 것 같다" 로 끝난다.
 *
 * <p>DB 에 붙는다. 엔드포인트가 늘어날 때마다 이 파일에 대조 하나를 추가한다.
 */
@SpringBootTest
@DisplayName("응답 정답지 대조")
class FixtureTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    private MockMvc mvc() {
        if (mvc == null) {
            // .apply(springSecurity()) 는 보안 필터를 붙인다. 이 줄이 없으면 MockMvc 가
            // Controller 를 바로 불러서 JwtAuthenticationFilter 를 건너뛴다. 그러면
            // @AuthenticationPrincipal 이 늘 null 이고 로그인이 필요한 주소도 통과해서,
            // 테스트는 통과하는데 실제 서버와는 다른 길을 재는 꼴이 된다.
            mvc = MockMvcBuilders.webAppContextSetup(context)
                    .apply(springSecurity())
                    .build();
        }
        return mvc;
    }

    /** 로그인해서 Access Token 을 받아온다. 로그인이 필요한 주소를 부를 때 쓴다. */
    private String loginAndGetToken() throws Exception {
        MvcResult result = mvc().perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"minji@example.com\",\"password\":\"meokji1234!\"}"))
                .andReturn();

        assertThat(result.getResponse().getStatus()).as("데모 계정으로 로그인").isEqualTo(200);

        return MAPPER.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8))
                .get("access_token").asString();
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
    @DisplayName("GET /api/health - 상태코드와 본문이 정답지와 같다")
    void healthMatchesFixture() throws Exception {
        JsonNode want = fixture("health");

        MvcResult result = mvc().perform(get("/api/health")).andReturn();
        String raw = result.getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertThat(result.getResponse().getStatus()).isEqualTo(want.get("status").asInt());

        JsonNode actual = MAPPER.readTree(raw);
        assertThat(actual).isEqualTo(want.get("body"));
    }

    @Test
    @DisplayName("없는 주소는 로그인한 사람에게 404 이고 본문이 {\"detail\": \"Not Found\"} 다")
    void unknownPathIsNotFound() throws Exception {
        // 이걸 확인하는 이유: @ExceptionHandler(Exception.class) 가 스프링의 404 까지
        // 삼켜서 500 으로 내보낸 적이 있다. 서버가 고장난 것과 주소를 잘못 부른 것이
        // 구분되지 않는다.
        //
        // 토큰을 붙여서 부른다. Spring Security 를 들인 뒤로 없는 주소는 인가 검사에
        // 먼저 걸려서, 로그인하지 않으면 404 까지 가지 않는다 (아래 테스트 참고).
        MvcResult result = mvc().perform(get("/api/no-such-path")
                        .header("Authorization", "Bearer " + loginAndGetToken()))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(404);

        JsonNode body = MAPPER.readTree(
                result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(body.get("detail").asString()).isEqualTo("Not Found");
    }

    @Test
    @DisplayName("없는 주소를 로그인 없이 부르면 404 가 아니라 401 이다")
    void unknownPathIsUnauthorizedForGuest() throws Exception {
        // Spring Security 를 들이면서 달라진 점이다. 전에는 404 가 나왔다.
        //
        // SecurityConfig 의 anyRequest().authenticated() 는 "적어두지 않은 주소는
        // 전부 로그인 필요" 라는 뜻이고, 존재하지 않는 주소도 거기 걸린다.
        // 인가 검사가 Controller 찾기보다 앞에 있어서 그렇다.
        //
        // 오히려 이 편이 낫다. 404 와 401 이 갈리면 로그인하지 않은 사람이 주소를
        // 하나씩 넣어보는 것만으로 "이 서버에 어떤 API 가 있는지" 를 알아낼 수 있다.
        MvcResult result = mvc().perform(get("/api/no-such-path")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
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
