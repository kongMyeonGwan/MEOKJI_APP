package com.meokji.api.auth;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * VERIFICATION_CODES 표에 보내는 SQL.
 *
 * <h2>시각 비교를 전부 SQL 에서 하는 이유</h2>
 *
 * <p>만료와 재발송 제한은 "지금" 을 기준으로 판단한다. 그 "지금" 을 자바에서
 * 만들면, DDL 의 {@code DEFAULT SYS_EXTRACT_UTC(SYSTIMESTAMP)} 가 찍은
 * {@code created_at}(DB 시계)과 자바 시계를 견주게 된다. 두 시계가 조금만 어긋나도
 * <b>재발송 제한이 59초가 되거나 61초가 되는데, 에러가 나지 않아 알아채기 어렵다.</b>
 * 한쪽 시계만 쓰도록 비교를 여기로 모았다.
 */
@Mapper
public interface VerificationCodeMapper {

    /**
     * 재발송까지 남은 시간(초).
     *
     * @return 0 이하면 지금 보내도 된다. 발급한 적이 없으면 {@code null}
     */
    Integer findResendWaitSeconds(
            @Param("userId") Long userId,
            @Param("purpose") String purpose,
            @Param("cooldownSeconds") int cooldownSeconds);

    /**
     * 아직 확인하지 않았고 만료되지도 않은 가장 최근 코드.
     *
     * <p>파이썬은 "가장 최근 행" 을 꺼낸 뒤 사용 여부 · 만료를 따로 검사하는데,
     * 셋 다 같은 400 문구로 끝난다. {@link #deleteByUserAndPurpose} 가 발급 때마다
     * 이전 코드를 지워서 행이 하나뿐이므로, 조건을 SQL 로 합쳐도 결과가 같다.
     */
    VerificationCode findUsable(
            @Param("userId") Long userId, @Param("purpose") String purpose);

    /** 확인까지 마쳤고 아직 만료되지 않은 코드. 비밀번호 교체 단계가 쓴다. */
    VerificationCode findVerified(
            @Param("userId") Long userId, @Param("purpose") String purpose);

    /**
     * 이 회원의 같은 용도 코드를 모두 지운다.
     *
     * <p>새로 발급하기 직전에 부른다. 남겨두면 옛날 코드로도 인증이 되어
     * 유효한 코드가 여러 개 살아있게 된다.
     */
    void deleteByUserAndPurpose(
            @Param("userId") Long userId, @Param("purpose") String purpose);

    /** 코드 한 건을 넣는다. {@code expires_at} 은 SQL 이 계산한다. */
    void insert(
            @Param("code") VerificationCode code,
            @Param("expireMinutes") int expireMinutes);

    /** 틀렸을 때 시도 횟수를 하나 올린다. */
    void increaseAttempt(@Param("id") Long id);

    /** 확인에 성공했다고 표시한다. 이 코드는 다시 확인에 쓸 수 없다. */
    void markUsed(@Param("id") Long id);

    /** 다 쓴 코드를 지운다. 남겨두면 같은 코드로 비밀번호를 계속 바꿀 수 있다. */
    void deleteById(@Param("id") Long id);
}
