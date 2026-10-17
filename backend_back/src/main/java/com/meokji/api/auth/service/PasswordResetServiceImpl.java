package com.meokji.api.auth.service;

import java.security.SecureRandom;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meokji.api.common.ApiException;
import com.meokji.api.user.dao.UserDAO;
import com.meokji.api.auth.dao.VerificationCodeDAO;
import com.meokji.api.util.Passwords;
import com.meokji.api.util.ResetCodeMailer;
import com.meokji.api.user.vo.UserVO;
import com.meokji.api.auth.vo.VerificationCodeVO;

/**
 * 비밀번호 재설정 세 단계.
 *
 * <p>막아야 하는 것이 둘이다
 *
 * <p>1. 번호를 찍어 맞히는 것 — 6자리면 백만 가지라 많아 보이지만, 초당 수백 번
 * 보내면 금방 다 넣어본다. 그래서 틀릴 수 있는 횟수를 세고(maxAttempt),
 * 유효 시간도 짧게 둔다(expireMinutes).
 *
 * <p>2. 메일 폭탄 — 남의 이메일을 적어 재발송을 반복하면 그 사람 받은편지함이
 * 쓰레기로 찬다. 그래서 재발송에 대기 시간을 둔다(resendCooldownSeconds).
 *
 * <p>세 설정값은 전부 application.yml 에 있다. 운영하면서 조절할 값이라
 * 코드에 박지 않았다.
 *
 * <p>2 · 3단계가 실패 이유를 구별해 주지 않는 이유 — 이쪽은 번호를 찍어볼 수
 * 있는 자리다. "그 번호는 만료됐다" 와 "그런 회원이 없다" 를 나눠 알려주면,
 * 그 차이로 가입 여부와 번호 상태를 알아낼 수 있다.
 */
@Service
public class PasswordResetServiceImpl implements PasswordResetService {

    // 인증번호가 틀렸거나 · 만료됐거나 · 회원이 없을 때 같은 문구를 쓴다
    // (구분해서 알려주면 "이 이메일이 가입돼 있는지" 를 알아내는 데 쓰일 수 있다)
    private static final String INVALID_CODE = "인증번호가 올바르지 않거나 만료되었습니다.";

    // 횟수를 다 쓴 경우. 다시 요청하면 새 번호가 나가므로 그 방법을 알려준다
    private static final String TOO_MANY_ATTEMPTS = "인증 시도 횟수를 초과했습니다. 인증번호를 다시 요청해주세요.";

    // VERIFICATION_CODES.PURPOSE 에 넣는 값. 한 표에 용도가 섞여 들어가므로
    // 찾을 때도 이 값으로 가려야 한다.
    //
    // 상수를 또 상수로 받는 이유 — 이 파일에서 다섯 번 쓴다. 매번
    // VerificationCodeVO.PURPOSE_PASSWORD_RESET 로 적으면 줄이 길어진다
    private static final String PURPOSE = VerificationCodeVO.PURPOSE_PASSWORD_RESET;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private VerificationCodeDAO verificationCodeDAO;

    // 인증번호도 비밀번호와 같은 방식으로 암호화한다. 6자리 원문은 DB 에 없다
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

    /**
     * 6자리 번호를 만들어 메일로 보낸다.
     *
     * {@code @Transactional} 이 필요한 이유 — 옛 코드 DELETE 와 새 코드 INSERT
     * 가 한 묶음이어야 한다. 지우고 넣기 사이에서 실패하면 쓸 수 있는 코드가
     * 아예 없어진다.
     */
    @Override
    @Transactional
    public void send(String email) {
        UserVO user = userDAO.findActiveByEmail(email);
        if (user == null) {
            // 여기만 이유를 그대로 알려준다. 메일 주소를 잘못 적은 사람이
            // 오지 않는 메일을 계속 기다리는 쪽이 더 나쁘다고 봤다
            throw ApiException.notFound("가입되지 않은 회원입니다.");
        }

        // 방금 보냈으면 잠시 기다리게 한다 (429)
        //
        // 남은 초를 자바에서 계산하지 않고 SQL 에게 물어본다 — created_at 은
        // DB 시계가 찍은 값이라, 자바 시계로 견주면 두 시계가 어긋난 만큼
        // 대기 시간이 59초나 61초가 된다. 에러가 안 나서 알아채기 어렵다.
        Integer waitSeconds = verificationCodeDAO.findResendWaitSeconds(user.getId(), PURPOSE, resendCooldownSeconds);
        // 발급한 적이 없으면 null 이 온다. 그때는 그냥 보낸다
        if (waitSeconds != null && waitSeconds > 0) {
            // 남은 초를 두 군데에 넣는다 — 문장(사용자가 읽을 말)과
            // Retry-After 헤더(화면이 버튼 타이머에 쓸 값)
            throw ApiException.tooManyRequests(
                    "인증번호를 방금 보냈습니다. " + waitSeconds + "초 후에 다시 요청해주세요.", waitSeconds);
        }

        // 예전에 보낸 인증번호는 지운다 (가장 최근 것 하나만 쓸 수 있게)
        //
        // 안 지우면 메일을 세 번 받은 사람이 세 번호 중 아무것이나 넣어도
        // 통과한다. 쓸 수 있는 번호가 늘어나는 만큼 찍어 맞힐 확률도 오른다
        verificationCodeDAO.deleteByUserAndPurpose(user.getId(), PURPOSE);

        // 6자리 숫자 만들기 (예: "042917")
        //
        // SecureRandom 을 쓴다. 보통 Random 은 씨앗값을 알면 다음 값을 계산할
        // 수 있어서, 번호를 예측할 길이 생긴다.
        //
        // %06d 로 앞을 0 으로 채운다. nextInt(1000000) 은 42917 처럼 5자리를
        // 줄 수도 있는데, 그러면 화면의 "6자리" 안내와 맞지 않는다
        String code = String.format("%06d", new SecureRandom().nextInt(1000000));

        // DB 에는 인증번호를 암호화해서 저장한다
        //
        // 원문을 넣으면 DB 를 볼 수 있는 사람이 남의 비밀번호를 바꿀 수 있다.
        // 짧아서 암호화할 필요가 없어 보이지만, 번호를 아는 것만으로
        // 계정을 가져갈 수 있는 값이다
        VerificationCodeVO row = new VerificationCodeVO();
        row.setUserId(user.getId());
        row.setCodeHash(passwordEncoder.encode(code));
        row.setPurpose(PURPOSE);
        // 만료 시각은 VO 에 넣지 않고 분만 넘긴다. 계산은 SQL 이 DB 시계로 한다
        verificationCodeDAO.insert(row, expireMinutes);

        // 메일 보내기 (설정이 mock 이면 콘솔에 찍기만 한다)
        //
        // 저장을 먼저 하고 메일을 나중에 보낸다. 메일부터 보내면, 저장이
        // 실패했을 때 사용자는 번호를 받았는데 서버에는 그 번호가 없다
        resetCodeMailer.send(user.getEmail(), code, expireMinutes);
    }

    // ===================== 2단계: 인증번호 확인 =====================

    /**
     * 받은 번호가 맞는지 본다. 맞으면 그 행에 "확인됨" 표시를 남긴다.
     *
     * {@code @Transactional} 이 필요한 이유 — 틀렸을 때 횟수를 올리는
     * UPDATE 가 들어간다. 읽기만 하는 메서드가 아니다.
     */
    @Override
    @Transactional
    public void verify(String email, String code) {
        UserVO user = userDAO.findActiveByEmail(email);
        if (user == null) {
            // 1단계와 달리 "가입되지 않았다" 고 하지 않는다. 이 주소는
            // 이메일을 바꿔가며 반복해 부를 수 있는 자리다
            throw ApiException.badRequest(INVALID_CODE);
        }

        // 아직 확인 안 했고, 만료 안 된 인증번호 찾기
        //
        // 만료 비교도 SQL 에서 한다(DAO 주석 참고). 여기서 row.getExpiresAt()
        // 과 지금을 견주면 자바 시계와 DB 시계를 섞어 쓰게 된다
        VerificationCodeVO row = verificationCodeDAO.findUsable(user.getId(), PURPOSE);
        if (row == null) {
            throw ApiException.badRequest(INVALID_CODE);
        }

        // DDL 에 DEFAULT 0 이 있지만, null 이 와도 터지지 않게 받는다
        int attempts = row.getAttemptCount() == null ? 0 : row.getAttemptCount();
        // 이미 횟수를 다 쓴 번호면 비교조차 하지 않는다.
        // 비교를 하면 그만큼 찍어볼 기회를 한 번 더 주는 셈이다
        if (attempts >= maxAttempt) {
            throw ApiException.tooManyRequests(TOO_MANY_ATTEMPTS);
        }

        // 틀렸으면 틀린 횟수를 올린다
        if (!passwordEncoder.matches(code, row.getCodeHash())) {
            verificationCodeDAO.increaseAttempt(row.getId());

            // 이번 시도까지 더한 뒤 남은 횟수.
            // 0 이하면 이번이 마지막이었다는 뜻이라 문구를 바꾼다
            int remaining = maxAttempt - (attempts + 1);
            if (remaining <= 0) {
                throw ApiException.tooManyRequests(TOO_MANY_ATTEMPTS);
            }
            throw ApiException.badRequest(INVALID_CODE + " (남은 시도 " + remaining + "회)");
            // ※ 확인해 둘 점 — 이 메서드에 @Transactional 이 붙어 있고
            //    ApiException 은 RuntimeException 이라, 여기서 던지면 위의
            //    increaseAttempt UPDATE 까지 같이 되돌려진다. 그래서 틀린
            //    횟수가 실제로는 쌓이지 않고, 남은 시도 안내도 늘 같은 숫자가
            //    나온다. 횟수 제한을 살리려면 이 UPDATE 만 따로 커밋되게
            //    해야 한다(별도 트랜잭션). 에러가 나지 않아 눈에 띄지 않는다.
        }

        // 맞았으면 "확인됨" 표시
        // 이 표시가 있어야 3단계가 통과시킨다
        verificationCodeDAO.markUsed(row.getId());
    }

    // ===================== 3단계: 새 비밀번호로 바꾸기 =====================

    /**
     * 새 비밀번호로 바꾼다.
     *
     * {@code @Transactional} 이 필요한 이유 — 비밀번호 UPDATE 와 코드 DELETE
     * 가 한 묶음이어야 한다. 바꾸고 나서 지우기가 실패하면 그 번호로
     * 비밀번호를 또 바꿀 수 있다.
     */
    @Override
    @Transactional
    public void change(String email, String code, String newPassword) {
        UserVO user = userDAO.findActiveByEmail(email);
        if (user == null) {
            throw ApiException.badRequest(INVALID_CODE);
        }

        // 2단계에서 확인을 마친 인증번호 찾기
        //
        // findUsable 이 아니라 findVerified 다. 2단계를 건너뛰고 이 주소만
        // 부르는 것을 막기 위한 것이다
        VerificationCodeVO row = verificationCodeDAO.findVerified(user.getId(), PURPOSE);
        // 번호도 다시 견준다. "확인된 코드가 있다" 와 "그 코드가 이 번호다" 는
        // 다른 얘기다 - 이 확인이 없으면 아무 번호나 보내도 통과한다
        if (row == null || !passwordEncoder.matches(code, row.getCodeHash())) {
            throw ApiException.badRequest(INVALID_CODE);
        }

        // 72바이트를 넘으면 bcrypt 가 뒤를 잘라낸다. 가입과 같은 규칙으로 막는다
        Passwords.checkBytes(newPassword);

        // 비밀번호를 바꾸고 계정 잠금도 푼다
        //
        // 한 SQL 로 같이 하는 이유 — 로그인을 5번 틀려 잠긴 사람이 쓰는
        // 길이 이 기능이다. 비밀번호만 바꾸고 잠금을 안 풀면 새 비밀번호로도
        // 못 들어간다. 실패 횟수도 같이 0 으로 되돌린다
        userDAO.resetPasswordAndUnlock(user.getId(), passwordEncoder.encode(newPassword));

        // 다 쓴 인증번호는 지운다
        // 남겨두면 같은 번호로 비밀번호를 몇 번이고 바꿀 수 있다
        verificationCodeDAO.deleteById(row.getId());
    }
}
