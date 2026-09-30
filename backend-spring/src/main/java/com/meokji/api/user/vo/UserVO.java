package com.meokji.api.user.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.meokji.api.util.Times;

import lombok.Data;

/**
 * USERS 표 한 행.
 *
 * 화면으로 나갈 모양도 이 클래스가 겸한다.
 *   POST /api/auth/login  로그인 응답 안의 user
 *   GET  /api/auth/me     로그인 확인
 *
 * 마이페이지(GET /api/users/me)는 여기에 없다. 요리 횟수 · 연속 기록일을
 * **세어서** 만드는 모양이라 표와 칸이 달라서 ProfileVO 로 따로 뒀다.
 *
 * **비밀번호 해시처럼 나가면 안 되는 칸에는 @JsonIgnore 를 붙였다.**
 * 이 표시가 없으면 로그인 응답에 password_hash 가 그대로 실려 나간다.
 * 칸을 새로 넣을 때 화면에 보여도 되는 값인지 먼저 따져야 한다.
 *
 * 나가는 모양: { "id", "email", "nickname", "created_at", "gender",
 *               "address_sido", "address_sigungu", "is_admin" }
 */
@Data
public class UserVO {

    private Long id;
    private String email;

    /** 절대 내보내지 않는다. */
    @JsonIgnore
    private String passwordHash;

    private String nickname;

    /** 1=관리자 2=일반. 가입은 전부 2 이고, 1 은 DB 에서 직접 올린다. 화면에는 is_admin 으로 나간다. */
    @JsonIgnore
    private Integer userLevel;

    private String gender;
    private String addressSido;
    private String addressSigungu;

    /**
     * 위도 · 경도. 날씨 API 를 실제로 부르는 값이다.
     * 로그인 응답에는 넣지 않는다 - 마이페이지(ProfileVO)에서만 보여준다.
     */
    @JsonIgnore
    private BigDecimal latitude;

    @JsonIgnore
    private BigDecimal longitude;

    /** 연속 로그인 실패 횟수. 성공하면 0 으로 되돌린다. */
    @JsonIgnore
    private Integer loginFailCount;

    /** 0=정상 1=잠김. 컬럼이 NUMBER(1) 이라 boolean 이 아니다. */
    @JsonIgnore
    private Integer isLocked;

    @JsonIgnore
    private LocalDateTime lockedAt;

    /** 'N' = 탈퇴 회원. 행은 남기고 조회에서만 뺀다. */
    @JsonIgnore
    private String useYn;

    @JsonIgnore
    private LocalDateTime createdAt;

    @JsonIgnore
    private LocalDateTime updatedAt;

    // ---------- 화면이 읽는 모양 ----------

    /** 관리자인지. 응답의 is_admin 이 이 값이다. */
    @JsonProperty("is_admin")
    public boolean isAdmin() {
        return userLevel != null && userLevel == 1;
    }

    /** 계정이 잠겼는지. 컬럼이 숫자라서 여기서 한 번만 뒤집어 쓴다. (화면에는 안 나간다) */
    @JsonIgnore
    public boolean isLockedAccount() {
        return isLocked != null && isLocked == 1;
    }

    /** 화면이 읽는 created_at (가입일). 예) "2026-09-11T21:29:42Z" */
    @JsonProperty("created_at")
    public String getCreatedAtText() {
        return Times.toText(createdAt);
    }
}
