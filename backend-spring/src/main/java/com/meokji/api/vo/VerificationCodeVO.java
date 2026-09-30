package com.meokji.api.vo;

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
 * "넣는 값은 항상 UTC" 라는 약속으로 맞춰뒀다(common/Times 참고).
 */
@Data
public class VerificationCodeVO {

    private Long id;
    private Long userId;

    /** 6자리 인증번호의 bcrypt 해시. */
    private String codeHash;

    /** DDL 의 ck_vc_purpose 가 허용하는 값만 들어간다. 지금 쓰는 것은 하나뿐이다. */
    private String purpose;

    private LocalDateTime expiresAt;

    /** 이 코드에 몇 번 틀렸는지. 무차별 대입을 끊는 기준이다. */
    private Integer attemptCount;

    /** 인증에 성공한 시각. NULL 이면 아직 확인 전이다 (1회용). */
    private LocalDateTime usedAt;

    private LocalDateTime createdAt;

    /** 지금 쓰는 유일한 용도. DDL 은 EMAIL_VERIFY · ACCOUNT_UNLOCK 도 허용한다. */
    public static final String PURPOSE_PASSWORD_RESET = "PASSWORD_RESET";

    /** null 을 0 으로 읽는다. DDL 에 DEFAULT 0 이 있어 실제로는 null 이 오지 않는다. */
}
