package com.meokji.api.auth.vo;

import lombok.Data;
import java.time.LocalDateTime;

import com.meokji.api.util.Times;

/**
 * VERIFICATION_CODES 표 한 행 (이메일 OTP).
 *
 * 6자리 원문은 저장하지 않는다. 메일로만 나가고 DB 에는 bcrypt 해시만
 * 남는다 - DB 가 유출돼도 코드를 그대로 읽어 계정을 가져갈 수 없게 하려는 것이다.
 *
 * 시각은 전부 UTC 다. 컬럼이 TIMESTAMP(6) 이라 시간대를 담지 않고,
 * "넣는 값은 항상 UTC" 라는 약속으로 맞춰뒀다({@link Times} 참고).
 *
 * <p>이 VO 는 화면에 나가지 않는다. 다른 VO 들과 달리 응답으로 쓰이는 자리가
 * 없고, DAO 가 DB 한 행을 담아 서비스에 넘기는 데만 쓴다. 코드 해시 ·
 * 시도 횟수는 사용자가 알 필요도, 알아서도 안 되는 값이다.
 *
 * <p>한 사람이 같은 용도의 코드를 여러 개 갖지 않는다. 새로 발급할 때
 * 서비스가 옛 코드를 먼저 지운다(VerificationCodeDAO.deleteByUserAndPurpose).
 * 남겨두면 메일을 여러 번 받은 사람이 어느 번호를 넣어도 통과한다.
 */
@Data
public class VerificationCodeVO {

    /** 시퀀스로 받는 번호. 시도 횟수를 올리거나 코드를 지울 때 이걸로 가리킨다 */
    private Long id;

    /** 누구의 코드인지. USERS.ID */
    private Long userId;

    /** 6자리 인증번호의 bcrypt 해시. */
    private String codeHash;

    /** DDL 의 ck_vc_purpose 가 허용하는 값만 들어간다. 지금 쓰는 것은 하나뿐이다. */
    private String purpose;

    /**
     * 만료 시각(UTC). 지금이 이 시각을 넘으면 못 쓴다.
     *
     * 몇 분짜리인지를 여기 적지 않고 SQL 이 계산해 넣는다 - 자바 시계와
     * DB 시계가 어긋나면 만료가 몇 초씩 밀리는데, 에러가 안 나서 알아채기 어렵다.
     */
    private LocalDateTime expiresAt;

    /** 이 코드에 몇 번 틀렸는지. 무차별 대입을 끊는 기준이다. */
    private Integer attemptCount;

    /** 인증에 성공한 시각. NULL 이면 아직 확인 전이다 (1회용). */
    private LocalDateTime usedAt;

    /**
     * 발급 시각(UTC). DDL 의 DEFAULT SYS_EXTRACT_UTC(SYSTIMESTAMP) 가 찍는다.
     *
     * 재발송 제한을 재는 기준이기도 하다 - "이 시각으로부터 60초" 를
     * SQL 안에서 계산한다(VerificationCodeDAO.findResendWaitSeconds).
     */
    private LocalDateTime createdAt;

    /**
     * 지금 쓰는 유일한 용도. DDL 은 EMAIL_VERIFY · ACCOUNT_UNLOCK 도 허용한다.
     *
     * 상수로 둔 이유 — "PASSWORD_RESET" 을 서비스와 DAO 가 각각 적으면
     * 한쪽에 오타가 나도 컴파일이 된다. 그러면 발급한 코드를 찾지 못해
     * "번호가 틀렸다" 만 계속 나오고, 원인을 찾기 어렵다.
     *
     * attemptCount 가 int 가 아니라 Integer 인 것도 DB 사정이다 -
     * DDL 에 DEFAULT 0 이 있어 실제로 null 이 오지는 않지만,
     * MyBatis 가 NULL 을 int 에 넣으려 하면 에러가 난다.
     */
    public static final String PURPOSE_PASSWORD_RESET = "PASSWORD_RESET";
}
