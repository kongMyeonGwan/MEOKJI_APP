package com.meokji.api;

import static org.assertj.core.api.Assertions.assertThat;
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

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 인증 응답을 정답지(fixtures)와 대조한다.
 *
 * <p>DB 에 붙는다. {@code minji@example.com} 계정이 있어야 한다 -
 * {@code gradlew seedDemo} 가 만들어준다 ({@link SeedDemoTest}).
 *
 * <p>토큰 문자열 자체는 비교하지 않는다. 발급 시각이 들어가서 매번 달라지기
 * 때문이다. 대신 <b>모양</b>(칸 이름 · 형 · 회원 정보)을 본다.
 */
@SpringBootTest
@DisplayName("인증 응답 정답지 대조")
class AuthFixtureTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private static final String MEMBER_EMAIL = "minji@example.com";
    private static final String MEMBER_PASSWORD = "meokji1234!";

    @Autowired
    private WebApplicationContext context;

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

    private JsonNode body(MvcResult result) throws Exception {
        return MAPPER.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    private String loginAndGetToken() throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + MEMBER_EMAIL + "\","
                                + "\"password\":\"" + MEMBER_PASSWORD + "\"}"))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        return body(result).get("access_token").asString();
    }

    @Test
    @DisplayName("POST /api/auth/login - 회원 정보가 정답지와 같다")
    void loginMatchesFixture() throws Exception {
        JsonNode want = fixture("auth_login").get("body");

        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + MEMBER_EMAIL + "\","
                                + "\"password\":\"" + MEMBER_PASSWORD + "\"}"))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        JsonNode actual = body(result);

        // 토큰은 매번 달라지므로 모양만 본다.
        assertThat(actual.get("access_token").asString()).isNotBlank();
        assertThat(actual.get("token_type").asString())
                .as("프론트가 이 값을 Authorization 헤더에 그대로 붙인다")
                .isEqualTo(want.get("token_type").asString());
        assertThat(actual.get("expires_in").asInt())
                .isEqualTo(want.get("expires_in").asInt());

        // 회원 정보는 글자까지 같아야 한다. bcrypt 해시가 그대로 통한다는 확인도 된다 -
        // 이 계정의 비밀번호 해시는 다른 bcrypt 구현($2b$)이 만든 것이다.
        assertThat(actual.get("user")).isEqualTo(want.get("user"));
    }

    @Test
    @DisplayName("GET /api/auth/me - 토큰으로 내 정보를 읽는다")
    void meMatchesFixture() throws Exception {
        JsonNode want = fixture("auth_me").get("body");

        MvcResult result = mvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + loginAndGetToken()))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(body(result)).isEqualTo(want);
    }

    @Test
    @DisplayName("토큰이 없으면 401 이고 문구도 같다")
    void meWithoutTokenIsUnauthorized() throws Exception {
        JsonNode want = fixture("auth_me_no_token");

        MvcResult result = mvc.perform(get("/api/auth/me")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(want.get("status").asInt());
        assertThat(body(result)).isEqualTo(want.get("body"));
    }

    @Test
    @DisplayName("위조 토큰도 401 이고, 어느 쪽이 틀렸는지 밝히지 않는다")
    void meWithBadTokenIsUnauthorized() throws Exception {
        JsonNode want = fixture("auth_me_bad_token");

        MvcResult result = mvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer not.a.real.token"))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(want.get("status").asInt());
        assertThat(body(result)).isEqualTo(want.get("body"));
    }

    @Test
    @DisplayName("비밀번호가 틀리면 401 + 남은 시도 횟수")
    void wrongPasswordTellsRemaining() throws Exception {
        JsonNode want = fixture("auth_login_wrong");

        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + MEMBER_EMAIL + "\","
                                + "\"password\":\"wrong-password-1234\"}"))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(want.get("status").asInt());

        // 남은 횟수는 그동안의 실패 기록에 따라 달라지므로 문구 모양만 본다.
        String detail = body(result).get("detail").asString();
        assertThat(detail).startsWith("이메일 또는 비밀번호가 올바르지 않습니다.");
        assertThat(detail).contains("남은 시도");

        // 다음 테스트에 실패 기록을 남기지 않는다. 성공 로그인이 0 으로 되돌린다.
        loginAndGetToken();
    }

    @Test
    @DisplayName("이미 있는 이메일로 가입하면 409")
    void duplicateEmailIsConflict() throws Exception {
        JsonNode want = fixture("register_duplicate");

        MvcResult result = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + MEMBER_EMAIL + "\","
                                + "\"password\":\"Test1234!\",\"nickname\":\"중복확인\"}"))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(want.get("status").asInt());
        assertThat(body(result)).isEqualTo(want.get("body"));
    }

    @Test
    @DisplayName("검증 실패는 422 이고 detail 이 배열이다 (프론트가 칸별로 나눠 쓴다)")
    void invalidRegisterIsUnprocessable() throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"1\",\"nickname\":\"\"}"))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(422);

        JsonNode detail = body(result).get("detail");
        assertThat(detail.isArray()).as("배열이어야 프론트가 칸별 오류로 읽는다").isTrue();

        // 프론트는 loc 의 마지막 값을 칸 이름으로 쓴다. 세 칸이 다 걸려야 한다.
        StringBuilder fields = new StringBuilder();
        for (JsonNode item : detail) {
            JsonNode loc = item.get("loc");
            fields.append(loc.get(loc.size() - 1).asString()).append(' ');
            assertThat(item.get("msg").asString()).isNotBlank();
        }

        assertThat(fields.toString()).contains("email").contains("password").contains("nickname");
    }
}
