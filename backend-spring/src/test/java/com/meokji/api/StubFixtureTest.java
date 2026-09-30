package com.meokji.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 담당자에게 넘길 501 스텁이 제대로 서 있는지 본다.
 *
 * <p>스텁이라고 확인을 건너뛰면 담당자가 <b>틀린 주소</b>를 받는다. 주소 하나가
 * 어긋나면 프론트를 붙일 때야 드러나고, 그때는 누가 틀렸는지 찾는 데 시간이 든다.
 */
@SpringBootTest
@DisplayName("게시판 · 공지 501 스텁")
class StubFixtureTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    /**
     * 담당자가 채울 주소 20개. 프론트가 이 주소를 그대로 부른다.
     *
     * <p><b>후기 쪽은 파이썬과 주소가 다르다.</b> 파이썬은 {@code /api/posts} 였는데
     * 표 이름이 {@code recipe_reviews} 라서 {@code /api/reviews} 로 바꿨다 - "post" 와
     * "review" 가 섞여 있으면 어느 쪽이 무엇인지 매번 확인해야 한다. 공지(notices)는
     * 표 이름이 그대로라 손대지 않았다.
     */
    private static final List<String> EXPECTED = List.of(
            "GET    /api/reviews",
            "POST   /api/reviews",
            "GET    /api/reviews/popular",
            "GET    /api/reviews/{reviewId}",
            "PUT    /api/reviews/{reviewId}",
            "DELETE /api/reviews/{reviewId}",
            "POST   /api/reviews/{reviewId}/view",
            "PUT    /api/reviews/{reviewId}/like",
            "DELETE /api/reviews/{reviewId}/like",
            "POST   /api/reviews/{reviewId}/comments",
            "PUT    /api/reviews/{reviewId}/comments/{commentId}",
            "DELETE /api/reviews/{reviewId}/comments/{commentId}",
            "POST   /api/reviews/{reviewId}/file",
            "GET    /api/reviews/{reviewId}/file",
            "DELETE /api/reviews/{reviewId}/file",
            "GET    /api/notices",
            "POST   /api/notices",
            "POST   /api/notices/{noticeId}/view",
            "PUT    /api/notices/{noticeId}",
            "DELETE /api/notices/{noticeId}");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private RequestMappingHandlerMapping handlerMapping;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private JsonNode body(MvcResult result) throws Exception {
        return MAPPER.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("게시판 · 공지 주소 20개가 파이썬과 같다")
    void stubRoutesMatchPython() {
        List<String> actual = new ArrayList<>();

        handlerMapping.getHandlerMethods().forEach((info, method) -> {
            String path = firstPattern(info);

            if (path == null
                    || !(path.startsWith("/api/reviews") || path.startsWith("/api/notices"))) {
                return;
            }

            for (var httpMethod : new TreeSet<>(info.getMethodsCondition().getMethods())) {
                actual.add(String.format("%-6s %s", httpMethod.name(), path));
            }
        });

        assertThat(actual)
                .as("스프링이 들고 있는 게시판·공지 주소")
                .containsExactlyInAnyOrderElementsOf(EXPECTED);
    }

    private static String firstPattern(RequestMappingInfo info) {
        if (info.getPathPatternsCondition() == null) {
            return null;
        }

        return info.getPathPatternsCondition().getPatterns().stream()
                .findFirst()
                .map(Object::toString)
                .orElse(null);
    }

    @Test
    @DisplayName("GET /api/reviews - 501 이고 문구가 파이썬과 같다")
    void postsListIsNotImplemented() throws Exception {
        JsonNode want = fixture("reviews_not_implemented");

        MvcResult result = mvc.perform(get("/api/reviews")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(want.get("status").asInt());
        assertThat(body(result)).isEqualTo(want.get("body"));
    }

    @Test
    @DisplayName("GET /api/notices - 501 이고 문구가 파이썬과 같다")
    void noticesListIsNotImplemented() throws Exception {
        JsonNode want = fixture("notices_not_implemented");

        MvcResult result = mvc.perform(get("/api/notices")).andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(want.get("status").asInt());
        assertThat(body(result)).isEqualTo(want.get("body"));
    }

    @Test
    @DisplayName("게스트도 볼 수 있는 조회는 401 이 아니라 501 이다")
    void guestReadableStubsReturn501() throws Exception {
        String[] paths = {"/api/reviews", "/api/reviews/1", "/api/reviews/popular", "/api/notices"};

        for (String path : paths) {
            MvcResult result = mvc.perform(get(path)).andReturn();

            // 로그인 없이도 501 이어야 한다. 401 이 나오면 컨트롤러가
            // findLoginUser(token) 대신 getLoginUser(token) 를 쓴 것이고,
            // 담당자가 구현한 뒤에 게스트가 목록을 못 보게 된다.
            assertThat(result.getResponse().getStatus())
                    .as("%s 는 게스트도 볼 수 있어야 한다", path)
                    .isEqualTo(501);
        }
    }

    @Test
    @DisplayName("로그인이 필요한 스텁은 토큰이 없으면 501 이 아니라 401 이다")
    void loginRequiredStubsReturn401() throws Exception {
        // 글쓰기는 로그인이 필요하다. 구현 전이라도 인증은 먼저 걸려야 한다 -
        // 여기서 501 이 나오면 authService.getLoginUser(token) 를 안 부른 것이다.
        MvcResult result = mvc.perform(post("/api/reviews")
                        .contentType("application/json")
                        .content("{\"meal_history_id\":1,\"title\":\"ㄱ\",\"content\":\"ㄴ\"}"))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(401);
        assertThat(body(result).get("detail").asString()).isEqualTo("인증이 필요합니다.");
    }

    @Test
    @DisplayName("공지 작성은 관리자가 아니면 403 이다")
    void noticeCreateNeedsAdmin() throws Exception {
        MvcResult login = mvc.perform(post("/api/auth/login")
                        .contentType("application/json")
                        .content("{\"email\":\"minji@example.com\","
                                + "\"password\":\"meokji1234!\"}"))
                .andReturn();

        String auth = "Bearer " + body(login).get("access_token").asString();

        MvcResult result = mvc.perform(post("/api/notices")
                        .header("Authorization", auth)
                        .contentType("application/json")
                        .content("{\"title\":\"ㄱ\",\"content\":\"ㄴ\"}"))
                .andReturn();

        // 일반 회원이라 403. 구현 전이라도 권한 검사가 먼저 걸려야 한다.
        assertThat(result.getResponse().getStatus()).isEqualTo(403);
        assertThat(body(result).get("detail").asString())
                .isEqualTo("관리자만 할 수 있는 작업입니다.");
    }

    private JsonNode fixture(String name) throws Exception {
        try (InputStream in = getClass().getClassLoader()
                .getResourceAsStream("fixtures/" + name + ".json")) {
            assertThat(in).as("정답지가 없다: %s", name).isNotNull();
            return MAPPER.readTree(in);
        }
    }
}
