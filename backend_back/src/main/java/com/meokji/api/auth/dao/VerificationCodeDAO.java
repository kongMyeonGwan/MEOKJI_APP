package com.meokji.api.auth.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.meokji.api.auth.vo.VerificationCodeVO;

/**
 * VERIFICATION_CODES 표에 보내는 SQL.
 *
 * 시각 비교를 전부 SQL 에서 하는 이유
 *
 * 만료와 재발송 제한은 "지금" 을 기준으로 판단한다. 그 "지금" 을 자바에서
 * 만들면, DDL 의 DEFAULT SYS_EXTRACT_UTC(SYSTIMESTAMP) 가 찍은
 * created_at(DB 시계)과 자바 시계를 견주게 된다. 두 시계가 조금만 어긋나도
 * 재발송 제한이 59초가 되거나 61초가 되는데, 에러가 나지 않아 알아채기 어렵다.
 * 한쪽 시계만 쓰도록 비교를 여기로 모았다.
 *
 * <p>인터페이스만 있고 구현 클래스가 없는 이유 — {@code @Mapper} 가 붙어 있으면
 * MyBatis 가 서버 시작 때 구현을 만들어 끼워 넣는다. 실제 SQL 은
 * resources/mapper/VerificationCodeMapper.xml 에 있고, 메서드 이름과 XML 의
 * id 가 같은 것끼리 이어진다. 그래서 이름을 고칠 때는 둘을 같이 고쳐야 한다
 * (한쪽만 고치면 서버가 뜰 때 "찾을 수 없다" 로 막힌다).
 *
 * <p>{@code @Param} 을 붙이는 이유 — 안 붙이면 XML 에서 param1 · param2 로
 * 적어야 한다. 순서만 바꿔도 조용히 엉뚱한 값이 들어간다.
 *
 * <p>purpose 를 매번 받는 이유 — 이 표는 비밀번호 재설정 말고도 쓸 수 있게
 * 만들어 두었다(DDL 이 EMAIL_VERIFY · ACCOUNT_UNLOCK 도 허용한다). 가리지
 * 않으면 다른 용도로 발급한 코드까지 같이 걸린다.
 */
@Mapper
public interface VerificationCodeDAO {

    /**
     * 재발송까지 남은 시간(초).
     *
     * 가장 최근에 발급한 코드의 created_at 에 cooldownSeconds 를 더한 시각과
     * 지금을 견주어, 남은 초를 돌려준다. 계산은 SQL 안에서 한다.
     *
     * @param cooldownSeconds 설정값(app.otp.resend-cooldown-seconds). SQL 에
     *                        박지 않고 넘기는 이유는 운영하면서 조절할 값이기 때문이다
     * @return 0 이하면 지금 보내도 된다. 발급한 적이 없으면 null
     */
    Integer findResendWaitSeconds(
            @Param("userId") Long userId,
            @Param("purpose") String purpose,
            @Param("cooldownSeconds") int cooldownSeconds);

    /**
     * 아직 확인하지 않았고 만료되지도 않은 가장 최근 코드.
     *
     * 2단계(번호 확인)가 쓴다. "확인 전" 조건이 들어 있어서, 이미 확인을
     * 마친 코드로는 같은 단계를 또 통과할 수 없다.
     */
    VerificationCodeVO findUsable(
            @Param("userId") Long userId, @Param("purpose") String purpose);

    /**
     * 확인까지 마쳤고 아직 만료되지 않은 코드. 비밀번호 교체 단계가 쓴다.
     *
     * findUsable 과 조건이 반대다(used_at 이 NULL 이 아닌 것). 3단계에서
     * findUsable 을 쓰면 2단계를 건너뛰고 비밀번호를 바꿀 수 있게 된다.
     */
    VerificationCodeVO findVerified(
            @Param("userId") Long userId, @Param("purpose") String purpose);

    /**
     * 이 회원의 같은 용도 코드를 모두 지운다.
     *
     * 새로 발급하기 직전에 부른다. 남겨두면 옛날 코드로도 인증이 되어
     * 유효한 코드가 여러 개 살아있게 된다.
     */
    void deleteByUserAndPurpose(
            @Param("userId") Long userId, @Param("purpose") String purpose);

    /**
     * 코드 한 건을 넣는다. expires_at 은 SQL 이 계산한다.
     *
     * 인자가 둘이라 둘 다 {@code @Param} 이 필요하다. code 는 VO 통째로
     * 넘기므로 XML 에서 #{code.userId} 처럼 한 겹 들어가 꺼낸다.
     *
     * @param expireMinutes 유효 시간(분). SQL 이 "지금 + 이 분" 으로 만료 시각을 만든다
     */
    void insert(
            @Param("code") VerificationCodeVO code,
            @Param("expireMinutes") int expireMinutes);

    /**
     * 틀렸을 때 시도 횟수를 하나 올린다.
     *
     * 자바에서 읽어 +1 해 UPDATE 하지 않고 SQL 안에서 올린다
     * (attempt_count = attempt_count + 1). 읽고 쓰는 사이에 다른 요청이
     * 끼어들면 둘 다 같은 값을 읽어 한 번만 오르게 된다.
     */
    void increaseAttempt(@Param("id") Long id);

    /** 확인에 성공했다고 표시한다. 이 코드는 다시 확인에 쓸 수 없다. */
    void markUsed(@Param("id") Long id);

    /** 다 쓴 코드를 지운다. 남겨두면 같은 코드로 비밀번호를 계속 바꿀 수 있다. */
    void deleteById(@Param("id") Long id);
}
