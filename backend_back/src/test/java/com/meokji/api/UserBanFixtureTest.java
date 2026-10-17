package com.meokji.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
 * 내 정보 · 알림 · Ban 응답을 정답지(fixtures)와 대조한다.
 *
 * <p>값을 바꾸는 테스트는 <b>바꾼 것을 되돌려 놓는다.</b> 데모 계정을 그대로
 * 쓰기 때문에 남겨두면 다음 실행의 정답지가 어긋난다.
 */
@SpringBootTest
@DisplayName("내 정보 · Ban 정답지 대조")
class UserBanFixtureTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private static final String MEMBER_EMAIL = "minji@example.com";
    private static final String MEMBER_PASSWORD = "meokji1234!";

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
    @DisplayName("GET /api/users/me - 칸 이름과 값이 정답지와 같다")
    void profileMatchesFixture() throws Exception {
        JsonNode want = fixture("users_me").get("body");

        MvcResult result = mvc.perform(get("/api/users/me").header("Authorization", auth))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        JsonNode actual = body(result);

        // 칸 이름이 하나도 빠지지 않아야 한다. snake_case 설정이 빠지면 여기서 걸린다.
        assertThat(actual.propertyNames()).containsExactlyInAnyOrderElementsOf(
                want.propertyNames());

        // 좌표는 NUMBER(9,6) 이라 소수 여섯 자리가 그대로 나와야 한다.
        assertThat(actual.get("latitude")).isEqualTo(want.get("latitude"));
        assertThat(actual.get("longitude")).isEqualTo(want.get("longitude"));

        // id · created_at 은 DB 가 정하는 값이라 모양만 본다 (MemberDbValues 참고)
        MemberDbValues.assertShape(actual);
        assertThat(actual.get("email")).isEqualTo(want.get("email"));
        assertThat(actual.get("nickname")).isEqualTo(want.get("nickname"));
        assertThat(actual.get("gender")).isEqualTo(want.get("gender"));
        assertThat(actual.get("address_sido")).isEqualTo(want.get("address_sido"));
        assertThat(actual.get("address_sigungu")).isEqualTo(want.get("address_sigungu"));

        // 요리 횟수와 연속 방문일은 DB 상태에 따라 달라지므로 형만 본다.
        assertThat(actual.get("cook_count").isNumber()).isTrue();
        assertThat(actual.get("streak_days").isNumber()).isTrue();
        assertThat(actual.get("cook_count").asInt()).isGreaterThanOrEqualTo(0);
    }

    @Test
    @DisplayName("GET /api/users/me/notifications - 게시판이 501 이어도 500 이 아니라 빈 배열")
    void notificationsAreEmptyArray() throws Exception {
        JsonNode want = fixture("users_me_notifications");

        MvcResult result = mvc.perform(
                        get("/api/users/me/notifications").header("Authorization", auth))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(want.get("status").asInt());

        // 게시판 표를 읽는 조회다. 담당자가 게시판을 만들기 전에는 비어 있는 것이
        // 정상이고, 여기서 500 이 나면 JOIN 이 잘못된 것이다.
        JsonNode actual = body(result);
        assertThat(actual.isArray()).isTrue();
        assertThat(actual).isEqualTo(want.get("body"));
    }

    @Test
    @DisplayName("Ban 추가 -> 목록 -> 해제가 정답지와 같은 모양이다")
    void banRoundTripMatchesFixture() throws Exception {
        JsonNode wantCreate = fixture("bans_create");

        // 1) 추가. 201 이고 user_id 는 내려주지 않는다.
        MvcResult created = mvc.perform(post("/api/bans")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ingredient_name\":\"픽스처확인용재료\","
                                + "\"ban_type\":\"DISLIKE\"}"))
                .andReturn();

        assertThat(created.getResponse().getStatus())
                .isEqualTo(wantCreate.get("status").asInt());

        JsonNode createdBody = body(created);
        assertThat(createdBody.propertyNames()).containsExactlyInAnyOrderElementsOf(
                wantCreate.get("body").propertyNames());
        assertThat(createdBody.get("ingredient_name").asString()).isEqualTo("픽스처확인용재료");
        assertThat(createdBody.get("ban_type").asString()).isEqualTo("DISLIKE");

        long banId = createdBody.get("id").asLong();

        // 2) 같은 재료를 또 넣으면 409
        MvcResult duplicated = mvc.perform(post("/api/bans")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ingredient_name\":\"픽스처확인용재료\","
                                + "\"ban_type\":\"DISLIKE\"}"))
                .andReturn();

        assertThat(duplicated.getResponse().getStatus()).isEqualTo(409);
        assertThat(body(duplicated).get("detail").asString()).isEqualTo("이미 등록한 재료입니다.");

        // 3) 목록에 보인다
        MvcResult listed = mvc.perform(get("/api/bans").header("Authorization", auth))
                .andReturn();

        assertThat(listed.getResponse().getStatus()).isEqualTo(200);
        assertThat(body(listed).toString()).contains("픽스처확인용재료");

        // 4) 해제. 204 이고 본문이 없다
        MvcResult deleted = mvc.perform(
                        delete("/api/bans/" + banId).header("Authorization", auth))
                .andReturn();

        assertThat(deleted.getResponse().getStatus()).isEqualTo(204);

        // 5) 목록에서 사라졌다. use_yn='N' 이라 행은 남아 있다.
        MvcResult after = mvc.perform(get("/api/bans").header("Authorization", auth))
                .andReturn();

        assertThat(body(after).toString()).doesNotContain("픽스처확인용재료");

        // 6) 없는 것을 또 지우면 404
        MvcResult again = mvc.perform(
                        delete("/api/bans/" + banId).header("Authorization", auth))
                .andReturn();

        assertThat(again.getResponse().getStatus()).isEqualTo(404);
        assertThat(body(again).get("detail").asString()).isEqualTo("등록되지 않은 재료입니다.");
    }

    @Test
    @DisplayName("빈 재료명은 422 다 (오라클이 빈 문자열을 NULL 로 보기 전에 막는다)")
    void blankIngredientIsRejected() throws Exception {
        MvcResult result = mvc.perform(post("/api/bans")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ingredient_name\":\"   \"}"))
                .andReturn();

        assertThat(result.getResponse().getStatus()).isEqualTo(422);
        assertThat(body(result).get("detail").isArray()).isTrue();
    }

    @Test
    @DisplayName("PATCH /api/users/me - 보내지 않은 칸은 그대로 둔다")
    void patchKeepsUntouchedFields() throws Exception {
        MvcResult before = mvc.perform(get("/api/users/me").header("Authorization", auth))
                .andReturn();
        JsonNode original = body(before);

        // 성별만 보낸다. 닉네임 · 주소 · 좌표는 건드리지 않아야 한다.
        MvcResult patched = mvc.perform(patch("/api/users/me")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gender\":\"OTHER\"}"))
                .andReturn();

        assertThat(patched.getResponse().getStatus()).isEqualTo(200);

        JsonNode actual = body(patched);
        assertThat(actual.get("gender").asString()).isEqualTo("OTHER");
        assertThat(actual.get("nickname")).isEqualTo(original.get("nickname"));
        assertThat(actual.get("address_sido")).isEqualTo(original.get("address_sido"));
        assertThat(actual.get("address_sigungu")).isEqualTo(original.get("address_sigungu"));
        assertThat(actual.get("latitude")).isEqualTo(original.get("latitude"));

        // 되돌려 놓는다. 안 그러면 다음 실행의 정답지 대조가 어긋난다.
        mvc.perform(patch("/api/users/me")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"gender\":\"" + original.get("gender").asString() + "\"}"))
                .andReturn();
    }

    @Test
    @DisplayName("비밀번호 변경은 현재 비밀번호를 확인한다")
    void changePasswordChecksCurrent() throws Exception {
        MvcResult wrong = mvc.perform(post("/api/users/me/password")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"current_password\":\"not-my-password\","
                                + "\"new_password\":\"Whatever1234!\"}"))
                .andReturn();

        assertThat(wrong.getResponse().getStatus()).isEqualTo(400);
        assertThat(body(wrong).get("detail").asString())
                .isEqualTo("현재 비밀번호가 올바르지 않습니다.");

        // 같은 값으로 바꾸려는 것도 막는다.
        MvcResult same = mvc.perform(post("/api/users/me/password")
                        .header("Authorization", auth)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"current_password\":\"" + MEMBER_PASSWORD + "\","
                                + "\"new_password\":\"" + MEMBER_PASSWORD + "\"}"))
                .andReturn();

        assertThat(same.getResponse().getStatus()).isEqualTo(400);
        assertThat(body(same).get("detail").asString())
                .isEqualTo("지금 쓰는 비밀번호와 다른 값을 입력해주세요.");
    }

    @Test
    @DisplayName("로그인 없이 부르면 401 이다 (내 정보 · Ban 전부)")
    void loginRequiredEverywhere() throws Exception {
        String[] paths = {"/api/users/me", "/api/users/me/notifications", "/api/bans"};

        for (String path : paths) {
            MvcResult result = mvc.perform(get(path)).andReturn();

            assertThat(result.getResponse().getStatus())
                    .as("%s 는 로그인이 필요하다", path)
                    .isEqualTo(401);
            assertThat(body(result).get("detail").asString()).isEqualTo("인증이 필요합니다.");
        }
    }
}
