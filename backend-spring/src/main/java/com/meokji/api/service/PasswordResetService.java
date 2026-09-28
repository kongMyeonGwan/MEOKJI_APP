package com.meokji.api.service;

import java.security.SecureRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.meokji.api.common.ApiException;
import com.meokji.api.config.AppProperties;
import com.meokji.api.mapper.UserMapper;
import com.meokji.api.mapper.VerificationCodeMapper;
import com.meokji.api.util.Passwords;
import com.meokji.api.util.ResetCodeMailer;
import com.meokji.api.vo.User;
import com.meokji.api.vo.VerificationCode;

/**
 * 비밀번호 재설정 (이메일 OTP · STEP 8 · 32 · 33).
 *
 * <p>파이썬 쪽 {@code app/services/password_reset_service.py} 자리다. 세 단계로 나뉜다.
 *
 * <pre>
 *   send   → 6자리 코드를 발급해 메일로 보낸다 (60초에 한 번)
 *   verify → 코드를 확인하고 used_at 을 찍는다 (5회까지 틀릴 수 있다)
 *   change → 확인을 마친 코드로 비밀번호를 바꾸고 계정 잠금을 푼다
 * </pre>
 *
 * <h2>왜 verify 와 change 의 실패 문구가 같은가</h2>
 *
 * <p>"코드가 틀렸다" 와 "만료됐다" 와 "그런 회원이 없다" 를 구분해서 알려주면,
 * 이 화면이 <b>가입 여부를 확인하는 도구</b>가 된다. 그래서 셋을 한 문구로 묶었다.
 * 다만 send 단계만은 404 로 가입 여부를 알려준다 - 오타를 친 사람이 오지 않는
 * 메일을 5분 기다리게 두는 쪽이 더 나쁘다고 판단했다(STEP 32).
 */
@Service
public class PasswordResetService {

    /** verify · change 단계의 실패 문구. 위 설명대로 하나로 통일한다. */
    private static final String INVALID_CODE = "인증번호가 올바르지 않거나 만료되었습니다.";

    private static final String NOT_REGISTERED = "가입되지 않은 회원입니다.";

    private static final String TOO_MANY_ATTEMPTS =
            "인증 시도 횟수를 초과했습니다. 인증번호를 다시 요청해주세요.";

    private static final int OTP_LENGTH = 6;

    /** {@code Math.random()} 이 아니라 암호학적 난수를 쓴다. 코드를 예측당하면 안 된다. */
    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private UserMapper users;
    @Autowired
    private VerificationCodeMapper codes;
    @Autowired
    private PasswordEncoder passwordEncoder;
    @Autowired
    private ResetCodeMailer mailer;
    @Autowired
    private AppProperties app;

    /**
     * 발급 부분만 트랜잭션으로 묶는다.
     *
     * <p>{@code @Transactional} 을 쓰지 않는 이유는, 같은 클래스 안에서 부르면
     * 프록시를 거치지 않아 애노테이션이 <b>조용히 아무 일도 하지 않기</b> 때문이다.
     * 여기서는 "이 블록만 묶고 그 뒤에 메일을 보낸다" 가 핵심이라 눈에 보이게 쓴다.
     *
     * <p>스프링부트가 만들어 둔 빈을 그대로 받는다. 직접 {@code new} 하던 것을
     * 주입으로 바꾼 것뿐이고, 기본 전파 · 격리 수준도 같다.
     */
    @Autowired
    private TransactionTemplate transaction;

    /**
     * 1단계 — 인증번호를 발급하고 메일로 보낸다.
     *
     * <p>메일 발송은 <b>커밋 이후에</b> 한다. 발송에 성공했는데 DB 저장이 롤백되면
     * 사용자는 코드를 받았는데 서버는 모르는 상태가 된다. 그래서 저장까지를
     * {@link #issueCode} 에 가둬 먼저 끝내고, 발송은 트랜잭션 밖에서 한다.
     */
    public void send(String email) {
        User user = findActive(email);

        if (user == null) {
            throw ApiException.notFound(NOT_REGISTERED);
        }

        raiseIfTooSoon(user.getId());

        String code = transaction.execute(status -> issueCode(user.getId()));

        mailer.send(user.getEmail(), code, app.getOtp().getExpireMinutes());
    }

    /** 코드를 만들어 저장하고 원문을 돌려준다. 위 트랜잭션 안에서만 부른다. */
    private String issueCode(Long userId) {
        // 이전에 발급한 코드는 전부 버린다. 남겨두면 옛날 코드로도 인증이 되어
        // 유효한 코드가 여러 개 살아있게 된다.
        codes.deleteByUserAndPurpose(userId, VerificationCode.PURPOSE_PASSWORD_RESET);

        String code = randomCode();

        VerificationCode row = new VerificationCode();
        row.setUserId(userId);
        // 원문은 메일로만 나간다. DB 에는 해시만 남긴다.
        row.setCodeHash(passwordEncoder.encode(code));
        row.setPurpose(VerificationCode.PURPOSE_PASSWORD_RESET);

        codes.insert(row, app.getOtp().getExpireMinutes());

        return code;
    }

    /** 2단계 — 인증번호를 확인하고 used_at 을 찍는다. */
    @Transactional
    public void verify(String email, String code) {
        User user = findActive(email);

        if (user == null) {
            throw ApiException.badRequest(INVALID_CODE);
        }

        // 없거나 · 이미 확인했거나 · 만료됐으면 여기서 null 이 온다. 셋 다 같은 문구다.
        VerificationCode row = codes.findUsable(
                user.getId(), VerificationCode.PURPOSE_PASSWORD_RESET);

        if (row == null) {
            throw ApiException.badRequest(INVALID_CODE);
        }

        int limit = app.getOtp().getMaxAttempt();

        if (row.attempts() >= limit) {
            // 100만 분의 1을 무작위로 맞히려는 시도를 여기서 끊는다.
            throw ApiException.tooManyRequests(TOO_MANY_ATTEMPTS);
        }

        if (!passwordEncoder.matches(code, row.getCodeHash())) {
            codes.increaseAttempt(row.getId());

            int remaining = limit - (row.attempts() + 1);

            if (remaining <= 0) {
                // "남은 시도 0회" 라고만 하면 다음에 뭘 해야 할지 알 수 없다.
                throw ApiException.tooManyRequests(TOO_MANY_ATTEMPTS);
            }

            throw ApiException.badRequest(INVALID_CODE + " (남은 시도 " + remaining + "회)");
        }

        codes.markUsed(row.getId());
    }

    /**
     * 3단계 — 비밀번호를 바꾸고 계정 잠금을 푼다.
     *
     * <p>비밀번호 재설정이 <b>계정 잠금(STEP 7)을 푸는 유일한 경로</b>다. 20회 틀려
     * 잠긴 계정은 여기를 지나야만 다시 열린다.
     */
    @Transactional
    public void change(String email, String code, String newPassword) {
        User user = findActive(email);

        if (user == null) {
            throw ApiException.badRequest(INVALID_CODE);
        }

        // 확인을 건너뛰고 바로 여기를 부른 경우(used_at 이 NULL)도 null 로 온다.
        VerificationCode row = codes.findVerified(
                user.getId(), VerificationCode.PURPOSE_PASSWORD_RESET);

        if (row == null || !passwordEncoder.matches(code, row.getCodeHash())) {
            throw ApiException.badRequest(INVALID_CODE);
        }

        Passwords.checkBytes(newPassword);

        users.resetPasswordAndUnlock(user.getId(), passwordEncoder.encode(newPassword));

        // 코드를 지운다. 남겨두면 used_at 이 찍힌 채로 남아
        // 같은 코드로 비밀번호를 계속 바꿀 수 있다.
        codes.deleteById(row.getId());
    }

    /** 탈퇴한 회원에게는 재설정 메일을 보내지 않는다. */
    private User findActive(String email) {
        return users.findActiveByEmail(email);
    }

    /** 방금 보냈으면 429 로 끊는다 (STEP 33). */
    private void raiseIfTooSoon(Long userId) {
        Integer wait = codes.findResendWaitSeconds(
                userId,
                VerificationCode.PURPOSE_PASSWORD_RESET,
                app.getOtp().getResendCooldownSeconds());

        if (wait == null || wait <= 0) {
            return;
        }

        throw ApiException.tooManyRequests(
                "인증번호를 방금 보냈습니다. " + wait + "초 후에 다시 요청해주세요.", wait);
    }

    /** 6자리 숫자. 앞자리가 0 이어도 자리 수를 채운다. */
    private static String randomCode() {
        return String.format("%0" + OTP_LENGTH + "d", RANDOM.nextInt(1_000_000));
    }
}
