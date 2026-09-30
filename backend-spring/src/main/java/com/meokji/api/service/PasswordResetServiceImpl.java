package com.meokji.api.service;

import java.security.SecureRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meokji.api.common.ApiException;
import com.meokji.api.dao.UserDAO;
import com.meokji.api.dao.VerificationCodeDAO;
import com.meokji.api.util.Passwords;
import com.meokji.api.util.ResetCodeMailer;
import com.meokji.api.vo.UserVO;
import com.meokji.api.vo.VerificationCodeVO;

@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    // 인증번호가 틀렸거나 · 만료됐거나 · 회원이 없을 때 같은 문구를 쓴다
    // (구분해서 알려주면 "이 이메일이 가입돼 있는지" 를 알아내는 데 쓰일 수 있다)
    private static final String INVALID_CODE = "인증번호가 올바르지 않거나 만료되었습니다.";

    private static final String TOO_MANY_ATTEMPTS = "인증 시도 횟수를 초과했습니다. 인증번호를 다시 요청해주세요.";

    private static final String PURPOSE = VerificationCodeVO.PURPOSE_PASSWORD_RESET;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private VerificationCodeDAO verificationCodeDAO;

    @Autowired
    private BCryptPasswordEncoder passwordEncoder;

    @Autowired
    private ResetCodeMailer resetCodeMailer;

    @Value("${app.otp.expire-minutes}")
    private int expireMinutes; // 인증번호 유효 시간(분)

    @Value("${app.otp.max-attempt}")
    private int maxAttempt; // 인증번호를 틀릴 수 있는 횟수

    @Value("${app.otp.resend-cooldown-seconds}")
    private int resendCooldownSeconds; // 다시 보내기까지 기다려야 하는 시간(초)

    // ===================== 1단계: 인증번호 보내기 =====================

    @Override
    @Transactional
    public void send(String email) {
        UserVO user = userDAO.findActiveByEmail(email);
        if (user == null) {
            throw ApiException.notFound("가입되지 않은 회원입니다.");
        }

        // 방금 보냈으면 잠시 기다리게 한다 (429)
        Integer waitSeconds = verificationCodeDAO.findResendWaitSeconds(user.getId(), PURPOSE, resendCooldownSeconds);
        if (waitSeconds != null && waitSeconds > 0) {
            throw ApiException.tooManyRequests(
                    "인증번호를 방금 보냈습니다. " + waitSeconds + "초 후에 다시 요청해주세요.", waitSeconds);
        }

        // 예전에 보낸 인증번호는 지운다 (가장 최근 것 하나만 쓸 수 있게)
        verificationCodeDAO.deleteByUserAndPurpose(user.getId(), PURPOSE);

        // 6자리 숫자 만들기 (예: "042917")
        String code = String.format("%06d", new SecureRandom().nextInt(1000000));

        // DB 에는 인증번호를 암호화해서 저장한다
        VerificationCodeVO row = new VerificationCodeVO();
        row.setUserId(user.getId());
        row.setCodeHash(passwordEncoder.encode(code));
        row.setPurpose(PURPOSE);
        verificationCodeDAO.insert(row, expireMinutes);

        // 메일 보내기 (설정이 mock 이면 콘솔에 찍기만 한다)
        resetCodeMailer.send(user.getEmail(), code, expireMinutes);
    }

    // ===================== 2단계: 인증번호 확인 =====================

    @Override
    @Transactional
    public void verify(String email, String code) {
        UserVO user = userDAO.findActiveByEmail(email);
        if (user == null) {
            throw ApiException.badRequest(INVALID_CODE);
        }

        // 아직 확인 안 했고, 만료 안 된 인증번호 찾기
        VerificationCodeVO row = verificationCodeDAO.findUsable(user.getId(), PURPOSE);
        if (row == null) {
            throw ApiException.badRequest(INVALID_CODE);
        }

        int attempts = row.getAttemptCount() == null ? 0 : row.getAttemptCount();
        if (attempts >= maxAttempt) {
            throw ApiException.tooManyRequests(TOO_MANY_ATTEMPTS);
        }

        // 틀렸으면 틀린 횟수를 올린다
        if (!passwordEncoder.matches(code, row.getCodeHash())) {
            verificationCodeDAO.increaseAttempt(row.getId());

            int remaining = maxAttempt - (attempts + 1);
            if (remaining <= 0) {
                throw ApiException.tooManyRequests(TOO_MANY_ATTEMPTS);
            }
            throw ApiException.badRequest(INVALID_CODE + " (남은 시도 " + remaining + "회)");
        }

        // 맞았으면 "확인됨" 표시
        verificationCodeDAO.markUsed(row.getId());
    }

    // ===================== 3단계: 새 비밀번호로 바꾸기 =====================

    @Override
    @Transactional
    public void change(String email, String code, String newPassword) {
        UserVO user = userDAO.findActiveByEmail(email);
        if (user == null) {
            throw ApiException.badRequest(INVALID_CODE);
        }

        // 2단계에서 확인을 마친 인증번호 찾기
        VerificationCodeVO row = verificationCodeDAO.findVerified(user.getId(), PURPOSE);
        if (row == null || !passwordEncoder.matches(code, row.getCodeHash())) {
            throw ApiException.badRequest(INVALID_CODE);
        }

        Passwords.checkBytes(newPassword);

        // 비밀번호를 바꾸고 계정 잠금도 푼다
        userDAO.resetPasswordAndUnlock(user.getId(), passwordEncoder.encode(newPassword));

        // 다 쓴 인증번호는 지운다
        verificationCodeDAO.deleteById(row.getId());
    }
}
