package com.meokji.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.Cookie;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Access Token · Refresh Token 흐름을 확인한다.
 *
 * <p>확인하는 것
 *
 * <pre>
 *   로그인      Access Token 은 JSON 으로, Refresh Token 은 쿠키로만 나간다
 *   정상 호출   Authorization 헤더 → 필터 → Controller → 200
 *   토큰 없음   401
 *   위조 토큰   401
 *   만료 토큰   401 이고, 그 뒤 재발급으로 살아난다
 *   재발급      쿠키만으로 새 Access Token 을 받는다 (F5 복원이 이 길이다)
 *   회전        재발급에 쓴 Refresh Token 은 그 즉시 못 쓴다
 *   로그아웃    DB 기록이 지워져서 쿠키가 남아 있어도 재발급이 안 된다
 *   상태 유지   체크 여부로 쿠키 수명이 갈린다
 * </pre>
 *
 * <p>DB 에 붙는다. 데모 계정(minji@example.com)이 있어야 한다 - {@code gradlew seedDemo}
 */
@SpringBootTest
@DisplayName("로그인 토큰 (Access · Refresh)")
class RefreshTokenTest {

    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    private static final String EMAIL = "minji@example.com";
    private static final String PASSWORD = "meokji1234!";

    /** application.yml 의 app.refresh-cookie.name */
    private static final String COOKIE = "meokji_refresh";

    @Autowired
    private WebApplicationContext context;

    /** 만료된 토큰 · 종류가 틀린 토큰을 직접 만들어 보려고 비밀키를 받아둔다. */
    @Value("${app.jwt.secret-key}")
    private String secretKey;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    // ===================== 거들기 =====================

    private JsonNode body(MvcResult result) throws Exception {
        return MAPPER.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    /** 로그인한다. keep 은 "로그인 상태 유지" 체크 여부다. */
    private MvcResult login(boolean keep) throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\","
                                + "\"password\":\"" + PASSWORD + "\","
                                + "\"keep\":" + keep + "}"))
                .andReturn();

        assertThat(result.getResponse().getStatus()).as("로그인").isEqualTo(200);
        return result;
    }

    /** 서버가 직접 만드는 것과 같은 방식으로, 수명과 종류만 바꾼 토큰을 만든다. */
    private String forgeToken(String type, long expiresInSeconds) {
        SecretKey key = Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
        Date now = new Date();

        return Jwts.builder()
                .subject("1")
                .claim("type", type)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expiresInSeconds * 1000))
                .signWith(key)
                .compact();
    }

    // ===================== 1. 로그인 =====================

    @Test
    @DisplayName("로그인하면 Access Token 은 JSON 으로, Refresh Token 은 쿠키로만 나온다")
    void loginGivesAccessTokenInJsonAndRefreshTokenInCookie() throws Exception {
        MvcResult result = login(true);

        // Access Token 은 본문에 있다. 프론트가 메모리에 담는 값이다.
        assertThat(body(result).get("access_token").asString()).isNotBlank();
        assertThat(body(result).get("token_type").asString()).isEqualTo("bearer");
        assertThat(body(result).get("expires_in").asLong()).isEqualTo(30 * 60L);

        // Refresh Token 은 본문에 없어야 한다. 있으면 자바스크립트가 읽을 수 있다는 뜻이다.
        assertThat(body(result).has("refresh_token"))
                .as("Refresh Token 이 JSON 으로 새어 나오면 안 된다")
                .isFalse();

        // 쿠키로만 나간다
        Cookie cookie = result.getResponse().getCookie(COOKIE);
        assertThat(cookie).as("Refresh 쿠키").isNotNull();
        assertThat(cookie.getValue()).isNotBlank();
        assertThat(cookie.isHttpOnly())
                .as("HttpOnly 여야 자바스크립트가 못 읽는다")
                .isTrue();
    }

    @Test
    @DisplayName("'로그인 상태 유지' 체크 여부로 쿠키 수명이 갈린다")
    void keepDecidesCookieLifetime() throws Exception {
        // 체크함 → 14일 동안 남는다. 브라우저를 껐다 켜도 로그인이 복원된다.
        Cookie kept = login(true).getResponse().getCookie(COOKIE);
        assertThat(kept.getMaxAge()).isEqualTo(14 * 24 * 60 * 60);

        // 체크 안 함 → 수명을 안 적는다(-1 = 세션 쿠키). 브라우저를 닫으면 사라진다.
        Cookie notKept = login(false).getResponse().getCookie(COOKIE);
        assertThat(notKept.getMaxAge())
                .as("수명이 없어야 브라우저를 닫을 때 사라진다")
                .isEqualTo(-1);
    }

    // ===================== 2~4. Access Token =====================

    @Test
    @DisplayName("Access Token 을 붙이면 로그인 전용 API 가 200 이다")
    void accessTokenOpensProtectedApi() throws Exception {
        String accessToken = body(login(true)).get("access_token").asString();

        MvcResult result = mvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(body(result).get("email").asString()).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("토큰이 없으면 401, 위조 토큰도 401")
    void missingOrForgedTokenIsUnauthorized() throws Exception {
        // 토큰 없음
        MvcResult none = mvc.perform(get("/api/likes")).andReturn();
        assertThat(none.getResponse().getStatus()).isEqualTo(401);
        assertThat(body(none).get("detail").asString()).isEqualTo("인증이 필요합니다.");

        // 서명이 안 맞는 토큰 (내용을 고쳤거나 남이 만든 것)
        MvcResult forged = mvc.perform(get("/api/likes")
                        .header("Authorization", "Bearer not.a.real.token"))
                .andReturn();
        assertThat(forged.getResponse().getStatus()).isEqualTo(401);
        assertThat(body(forged).get("detail").asString())
                .isEqualTo("토큰이 유효하지 않거나 만료되었습니다.");
    }

    @Test
    @DisplayName("만료된 Access Token 은 401 이고, 그 뒤 재발급으로 살아난다")
    void expiredAccessTokenIsUnauthorizedButRefreshable() throws Exception {
        MvcResult loggedIn = login(true);
        Cookie refreshCookie = loggedIn.getResponse().getCookie(COOKIE);

        // 이미 만료된 Access Token (서명은 멀쩡하고 시간만 지났다)
        String expired = forgeToken("access", -60);

        MvcResult rejected = mvc.perform(get("/api/likes")
                        .header("Authorization", "Bearer " + expired))
                .andReturn();
        assertThat(rejected.getResponse().getStatus()).isEqualTo(401);

        // 여기서 끝이 아니다. 쿠키가 살아 있으면 새 Access Token 을 받아 다시 쓸 수 있다.
        MvcResult refreshed = mvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
                .andReturn();
        assertThat(refreshed.getResponse().getStatus()).isEqualTo(200);

        String fresh = body(refreshed).get("access_token").asString();
        assertThat(mvc.perform(get("/api/likes").header("Authorization", "Bearer " + fresh))
                .andReturn().getResponse().getStatus())
                .isEqualTo(200);
    }

    @Test
    @DisplayName("Refresh Token 으로는 일반 API 를 부를 수 없다")
    void refreshTokenCannotCallNormalApi() throws Exception {
        // 두 토큰은 같은 비밀키로 서명한다. 종류(type)를 안 보면 14일짜리 Refresh Token
        // 으로 API 를 부를 수 있게 되고, 그러면 토큰을 나눈 의미가 없어진다.
        String refreshToken = forgeToken("refresh", 60 * 60);

        MvcResult result = mvc.perform(get("/api/likes")
                        .header("Authorization", "Bearer " + refreshToken))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
    }

    // ===================== 6~8. 재발급 =====================

    @Test
    @DisplayName("쿠키만 있으면 새 Access Token 을 받는다 (F5 · 브라우저 재시작 복원)")
    void refreshWithCookieOnly() throws Exception {
        Cookie refreshCookie = login(true).getResponse().getCookie(COOKIE);

        // Authorization 헤더를 안 붙인다. React 메모리의 Access Token 이 사라진 상황이다.
        MvcResult result = mvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);
        assertThat(body(result).get("access_token").asString()).isNotBlank();

        // 회원 정보도 같이 온다. 프론트가 /api/auth/me 를 한 번 더 부르지 않아도 된다.
        assertThat(body(result).get("user").get("email").asString()).isEqualTo(EMAIL);
    }

    @Test
    @DisplayName("쿠키가 없으면 재발급은 401 이다")
    void refreshWithoutCookieIsUnauthorized() throws Exception {
        MvcResult result = mvc.perform(post("/api/auth/refresh")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        assertThat(body(result).get("detail").asString()).isEqualTo("로그인이 필요합니다.");
    }

    @Test
    @DisplayName("만료된 Refresh Token 은 401 이다 (14일이 지난 경우)")
    void expiredRefreshTokenIsUnauthorized() throws Exception {
        Cookie expired = new Cookie(COOKIE, forgeToken("refresh", -60));

        MvcResult result = mvc.perform(post("/api/auth/refresh").cookie(expired)).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("재발급에 쓴 Refresh Token 은 그 즉시 못 쓴다 (회전)")
    void usedRefreshTokenIsRejected() throws Exception {
        Cookie first = login(true).getResponse().getCookie(COOKIE);

        // 1차 재발급 - 성공하고, 새 쿠키가 내려온다
        MvcResult refreshed = mvc.perform(post("/api/auth/refresh").cookie(first)).andReturn();
        assertThat(refreshed.getResponse().getStatus()).isEqualTo(200);

        Cookie second = refreshed.getResponse().getCookie(COOKIE);
        assertThat(second.getValue())
                .as("재발급할 때마다 Refresh Token 도 새로 만든다")
                .isNotEqualTo(first.getValue());

        // 옛 쿠키로 다시 시도 - 토큰 자체는 아직 14일 안 지났지만 DB 가 새 것으로
        // 바뀌어서 해시 대조에서 걸린다. 훔친 토큰을 오래 쓰지 못하게 하는 장치다.
        MvcResult reused = mvc.perform(post("/api/auth/refresh").cookie(first)).andReturn();
        assertThat(reused.getResponse().getStatus()).isEqualTo(401);

        // 새 쿠키는 정상이다
        assertThat(mvc.perform(post("/api/auth/refresh").cookie(second))
                .andReturn().getResponse().getStatus())
                .isEqualTo(200);
    }

    // ===================== 9. 로그아웃 =====================

    @Test
    @DisplayName("로그아웃하면 쿠키가 남아 있어도 재발급이 안 된다")
    void logoutRevokesRefreshToken() throws Exception {
        MvcResult loggedIn = login(true);
        String accessToken = body(loggedIn).get("access_token").asString();
        Cookie refreshCookie = loggedIn.getResponse().getCookie(COOKIE);

        MvcResult loggedOut = mvc.perform(post("/api/auth/logout")
                        .header("Authorization", "Bearer " + accessToken)
                        .cookie(refreshCookie))
                .andReturn();

        assertThat(loggedOut.getResponse().getStatus()).isEqualTo(200);

        // 쿠키를 지우라는 응답이 같이 온다 (Max-Age 0)
        assertThat(loggedOut.getResponse().getCookie(COOKIE).getMaxAge())
                .as("브라우저가 쿠키를 버리게 한다")
                .isEqualTo(0);

        // 브라우저가 쿠키를 버리지 않았다 해도(혹은 누가 값을 복사해뒀다 해도) 소용없다.
        // DB 기록이 지워져서 해시 대조에서 걸린다. 이게 "서버 로그아웃" 의 핵심이다.
        MvcResult afterLogout =
                mvc.perform(post("/api/auth/refresh").cookie(refreshCookie)).andReturn();

        assertThat(afterLogout.getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    @DisplayName("Access Token 이 만료된 뒤에도 로그아웃은 된다")
    void logoutWorksWithExpiredAccessToken() throws Exception {
        Cookie refreshCookie = login(true).getResponse().getCookie(COOKIE);

        // 30분이 지나 Access Token 이 만료된 상황. 여기서 401 이 나면 사용자는
        // "로그아웃조차 안 되는" 상태에 갇힌다. 그래서 쿠키만으로도 되게 해뒀다.
        MvcResult result = mvc.perform(post("/api/auth/logout").cookie(refreshCookie))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        // 실제로 폐기됐는지 확인
        assertThat(mvc.perform(post("/api/auth/refresh").cookie(refreshCookie))
                .andReturn().getResponse().getStatus())
                .isEqualTo(401);
    }
}
