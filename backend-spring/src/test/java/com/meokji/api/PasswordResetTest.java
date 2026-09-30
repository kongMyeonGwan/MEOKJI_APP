package com.meokji.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

import com.meokji.api.dao.UserDAO;
import com.meokji.api.dao.VerificationCodeDAO;
import com.meokji.api.util.ResetCodeMailer;
import com.meokji.api.vo.UserVO;
import com.meokji.api.vo.VerificationCodeVO;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 비밀번호 재설정 세 단계를 확인한다 (STEP 8 · 32 · 33).
 *
 * <p>DB 에 붙는다. {@code minji@example.com} 계정이 있어야 한다 -
 * {@code gradlew seedDemo} 가 만들어준다 ({@link SeedDemoTest}).
 *
 * <h2>인증번호를 콘솔에서 주워온다</h2>
 *
 * <p>코드 원문은 DB 에 해시로만 남아서 되읽을 수 없다. {@code app.mail.provider=mock}
 * 일 때 {@code ResetCodeMailer} 가 콘솔에 찍는 값을 가로채 쓴다. 지저분하지만,
 * 이 방법이라야 발급 → 확인 → 교체를 <b>실제로 이어서</b> 돌려볼 수 있다.
 *
 * <h2>비밀번호를 원래 값으로 되돌려 놓는다</h2>
 *
 * <p>같은 계정을 {@code AuthFixtureTest} 가 로그인에 쓴다. 3단계에서 <b>같은
 * 비밀번호로</b> 바꿔서 다른 테스트가 깨지지 않게 한다. 재설정 경로에는 "이전과
 * 달라야 한다" 는 규칙이 없다 - 그 규칙은 로그인한 채로 바꾸는 쪽에만 있다.
 */
@SpringBootTest
@DisplayName("비밀번호 재설정")
class PasswordResetTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private static final String MEMBER_EMAIL = "minji@example.com";
    private static final String MEMBER_PASSWORD = "meokji1234!";

    /** mock 메일러가 찍는 " 인증번호  : 123456" 줄에서 여섯 자리만 뽑는다. */
    private static final Pattern CODE_LINE = Pattern.compile("인증번호\\s*:\\s*(\\d{6})");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private UserDAO users;

    @Autowired
    private VerificationCodeDAO codes;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    // ------------------------------------------------------------------ 도우미

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

    private MvcResult call(String path, String json) throws Exception {
        return mvc.perform(post("/api/auth/" + path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andReturn();
    }

    /** 이 회원에게 남아 있는 코드를 지운다. 재발송 제한(60초) 때문에 매번 치운다. */
    private UserVO cleanCodes() {
        UserVO me = users.findActiveByEmail(MEMBER_EMAIL);

        assertThat(me).as("seed_demo 로 만든 %s 계정이 있어야 한다", MEMBER_EMAIL).isNotNull();

        codes.deleteByUserAndPurpose(me.getId(), VerificationCodeVO.PURPOSE_PASSWORD_RESET);

        return me;
    }

    /** 인증번호를 발급받고, 메일러가 콘솔에 찍은 원문을 가로채 돌려준다. */
    private String sendAndCatchCode() throws Exception {
        PrintStream original = System.out;
        ByteArrayOutputStream caught = new ByteArrayOutputStream();

        MvcResult result;

        try {
            System.setOut(new PrintStream(caught, true, StandardCharsets.UTF_8));
            result = call("password-reset/send",
                    "{\"email\":\"" + MEMBER_EMAIL + "\"}");
        } finally {
            System.setOut(original);
        }

        assertThat(result.getResponse().getStatus()).isEqualTo(200);

        Matcher matcher = CODE_LINE.matcher(caught.toString(StandardCharsets.UTF_8));

        assertThat(matcher.find())
                .as("mock 메일러가 인증번호를 콘솔에 찍어야 한다 (app.mail.provider=mock)")
                .isTrue();

        return matcher.group(1);
    }

    // -------------------------------------------------------------------- 검사

    @Test
    @DisplayName("가입되지 않은 이메일은 404 이고 본문이 파이썬과 같다")
    void unknownEmailMatchesFixture() throws Exception {
        JsonNode want = fixture("password_reset_unknown");

        MvcResult result = call("password-reset/send",
                "{\"email\":\"no-such-user@example.com\"}");

        assertThat(result.getResponse().getStatus()).isEqualTo(want.get("status").asInt());
        assertThat(body(result)).isEqualTo(want.get("body"));
    }

    @Test
    @DisplayName("이메일 형식이 틀리면 422 다")
    void badEmailIsUnprocessable() throws Exception {
        MvcResult result = call("password-reset/send", "{\"email\":\"이메일아님\"}");

        assertThat(result.getResponse().getStatus()).isEqualTo(422);
        // 프론트는 detail 이 배열이면 칸별 오류로 나눈다.
        assertThat(body(result).get("detail").isArray()).isTrue();
    }

    @Test
    @DisplayName("인증번호가 6자리가 아니면 대조까지 가지 않고 422 다")
    void shortCodeIsUnprocessable() throws Exception {
        MvcResult result = call("password-reset/verify",
                "{\"email\":\"" + MEMBER_EMAIL + "\",\"code\":\"12345\"}");

        assertThat(result.getResponse().getStatus()).isEqualTo(422);
    }

    @Test
    @DisplayName("확인을 건너뛰고 바로 교체하면 400 이다")
    void changeWithoutVerifyIsRejected() throws Exception {
        cleanCodes();

        String code = sendAndCatchCode();

        // verify 를 부르지 않았으니 used_at 이 NULL 이다.
        MvcResult result = call("password-reset/change",
                "{\"email\":\"" + MEMBER_EMAIL + "\",\"code\":\"" + code + "\","
                + "\"new_password\":\"" + MEMBER_PASSWORD + "\"}");

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(body(result).get("detail").asString())
                .isEqualTo("인증번호가 올바르지 않거나 만료되었습니다.");
    }

    @Test
    @DisplayName("방금 보냈으면 429 와 Retry-After 가 나간다")
    void resendIsThrottled() throws Exception {
        cleanCodes();

        sendAndCatchCode();

        MvcResult result = call("password-reset/send",
                "{\"email\":\"" + MEMBER_EMAIL + "\"}");

        assertThat(result.getResponse().getStatus()).isEqualTo(429);

        // 화면이 문구에서 숫자를 뽑아내지 않고 이 헤더로 남은 시간을 센다.
        String retryAfter = result.getResponse().getHeader("Retry-After");

        assertThat(retryAfter).as("429 에는 Retry-After 가 붙어야 한다").isNotNull();
        assertThat(Integer.parseInt(retryAfter)).isBetween(1, 60);

        assertThat(body(result).get("detail").asString())
                .startsWith("인증번호를 방금 보냈습니다.");
    }

    @Test
    @DisplayName("틀린 인증번호는 400 이고 남은 시도 횟수를 알려준다")
    void wrongCodeTellsRemainingAttempts() throws Exception {
        cleanCodes();

        String code = sendAndCatchCode();
        String wrong = code.equals("000000") ? "111111" : "000000";

        MvcResult result = call("password-reset/verify",
                "{\"email\":\"" + MEMBER_EMAIL + "\",\"code\":\"" + wrong + "\"}");

        assertThat(result.getResponse().getStatus()).isEqualTo(400);
        assertThat(body(result).get("detail").asString())
                .isEqualTo("인증번호가 올바르지 않거나 만료되었습니다. (남은 시도 4회)");
    }

    @Test
    @DisplayName("발급 → 확인 → 교체가 이어서 되고, 다 쓴 코드는 두 번 통하지 않는다")
    void wholeFlowWorks() throws Exception {
        cleanCodes();

        String code = sendAndCatchCode();
        String json = "{\"email\":\"" + MEMBER_EMAIL + "\",\"code\":\"" + code + "\"}";

        MvcResult verified = call("password-reset/verify", json);

        assertThat(verified.getResponse().getStatus()).isEqualTo(200);
        assertThat(body(verified).get("message").asString())
                .isEqualTo("인증되었습니다. 새 비밀번호를 입력해주세요.");

        // 같은 비밀번호로 되돌려 놓는다. 위 클래스 주석 참고.
        String changeJson = "{\"email\":\"" + MEMBER_EMAIL + "\",\"code\":\"" + code + "\","
                + "\"new_password\":\"" + MEMBER_PASSWORD + "\"}";

        MvcResult changed = call("password-reset/change", changeJson);

        assertThat(changed.getResponse().getStatus()).isEqualTo(200);
        assertThat(body(changed).get("message").asString())
                .isEqualTo("비밀번호가 변경되었습니다. 다시 로그인해주세요.");

        // 바뀐 비밀번호로 로그인이 된다. 해시를 새로 만들어 넣었으니 이게 통해야
        // 다음 테스트(AuthFixtureTest)도 돈다.
        MvcResult login = call("login",
                "{\"email\":\"" + MEMBER_EMAIL + "\",\"password\":\"" + MEMBER_PASSWORD + "\"}");

        assertThat(login.getResponse().getStatus()).isEqualTo(200);

        // 코드는 교체와 함께 지워진다. 남겨두면 같은 코드로 계속 바꿀 수 있다.
        MvcResult again = call("password-reset/change", changeJson);

        assertThat(again.getResponse().getStatus()).isEqualTo(400);
    }

    @Test
    @DisplayName("비밀번호 재설정이 잠긴 계정을 푼다")
    void changeUnlocksAccount() throws Exception {
        UserVO me = cleanCodes();

        // 20회 실패한 상태를 직접 만든다. 로그인을 20번 시키는 것보다 빠르고,
        // 확인하려는 것은 "잠금이 풀리는가" 이지 "잠기는가" 가 아니다.
        users.lockAccount(me.getId());

        try {
            assertThat(users.findById(me.getId()).isLockedAccount())
                    .as("잠긴 상태를 먼저 만들어야 한다").isTrue();

            String code = sendAndCatchCode();

            call("password-reset/verify",
                    "{\"email\":\"" + MEMBER_EMAIL + "\",\"code\":\"" + code + "\"}");

            MvcResult changed = call("password-reset/change",
                    "{\"email\":\"" + MEMBER_EMAIL + "\",\"code\":\"" + code + "\","
                    + "\"new_password\":\"" + MEMBER_PASSWORD + "\"}");

            assertThat(changed.getResponse().getStatus()).isEqualTo(200);

            UserVO after = users.findById(me.getId());

            assertThat(after.isLockedAccount()).as("재설정이 잠금을 푸는 유일한 경로다").isFalse();
            assertThat(after.getLoginFailCount()).isZero();
        } finally {
            // 중간에 실패하면 데모 계정이 잠긴 채로 남는다. 다음 테스트도,
            // 그 계정으로 화면을 보던 사람도 로그인을 못 하게 된다.
            // 해시는 그대로 두고 잠금만 되돌린다.
            users.resetPasswordAndUnlock(me.getId(), me.getPasswordHash());
        }
    }
}
