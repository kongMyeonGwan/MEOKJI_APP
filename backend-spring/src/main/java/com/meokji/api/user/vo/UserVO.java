package com.meokji.api.user.vo;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * USERS 표 한 행.
 */
@Data
public class UserVO {

    private Long id;
    private String email;

    private String passwordHash;

    private String nickname;

    /** 1=관리자 2=일반. 가입은 전부 2 이고, 1 은 DB 에서 직접 올린다. */
    private Integer userLevel;

    private String gender;
    private String addressSido;
    private String addressSigungu;

    /** 위도 · 경도. 날씨 API 를 실제로 부르는 값이다. */
    private BigDecimal latitude;
    private BigDecimal longitude;

    /** 연속 로그인 실패 횟수. 성공하면 0 으로 되돌린다. */
    private Integer loginFailCount;

    /** 0=정상 1=잠김. 컬럼이 NUMBER(1) 이라 boolean 이 아니다. */
    private Integer isLocked;

    private LocalDateTime lockedAt;

    /** 'N' = 탈퇴 회원. 행은 남기고 조회에서만 뺀다. */
    private String useYn;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    /** 관리자인지. 응답의 is_admin 이 이 값이다. */
    public boolean isAdmin() {
        return userLevel != null && userLevel == 1;
    }

    /** 계정이 잠겼는지. 컬럼이 숫자라서 여기서 한 번만 뒤집어 쓴다. */
    public boolean isLockedAccount() {
        return isLocked != null && isLocked == 1;
    }
}
