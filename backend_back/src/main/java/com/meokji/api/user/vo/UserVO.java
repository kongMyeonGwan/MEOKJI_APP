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

    /** 로그인에 쓰는 이메일. UNIQUE 라 같은 값으로 두 번 가입할 수 없다 */
    private String email;

    /** 절대 내보내지 않는다. */
    // bcrypt 로 만든 글자다. 원문은 어디에도 저장하지 않는다.
    //
    // @JsonIgnore 가 없으면 로그인 응답에 그대로 실려 나간다 - 해시라
    // 바로 풀 수는 없지만, 가져가서 시간을 들여 맞혀볼 수는 있다
    @JsonIgnore
    private String passwordHash;

    /** 글쓴이 이름으로 화면에 보이는 값. 이것도 UNIQUE 다 */
    private String nickname;

    /** 1=관리자 2=일반. 가입은 전부 2 이고, 1 은 DB 에서 직접 올린다. 화면에는 is_admin 으로 나간다. */
    @JsonIgnore
    private Integer userLevel;

    /** MALE · FEMALE · OTHER. 안 고르면 null */
    private String gender;

    /** 주소. 아래 좌표를 찾는 근거이기도 하다 */
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
    // 화면에 내보내지 않는다. 남은 횟수는 로그인이 실패했을 때
    // 에러 문장에만 적어 보낸다 - 평소에 "3번 틀렸음" 을 알려줄 이유가 없다
    //
    // Integer 인 이유 — DDL 에 DEFAULT 0 이 있지만, MyBatis 가
    // NULL 을 int 에 넣으려 하면 에러가 난다
    @JsonIgnore
    private Integer loginFailCount;

    /** 0=정상 1=잠김. 컬럼이 NUMBER(1) 이라 boolean 이 아니다. */
    @JsonIgnore
    private Integer isLocked;

    /** 잠긴 시각(UTC). 지금은 기록용이다 - 일정 시간 뒤 자동 해제는 없다 */
    @JsonIgnore
    private LocalDateTime lockedAt;

    /** 'N' = 탈퇴 회원. 행은 남기고 조회에서만 뺀다. */
    @JsonIgnore
    private String useYn;

    /** 가입일(UTC). 아래 getCreatedAtText 로 글자가 되어 나간다 */
    @JsonIgnore
    private LocalDateTime createdAt;

    /** 마지막으로 정보를 고친 시각. 화면에 내보내지 않는다 */
    @JsonIgnore
    private LocalDateTime updatedAt;

    // ---------- 화면이 읽는 모양 ----------

    /** 관리자인지. 응답의 is_admin 이 이 값이다. */
    // userLevel 을 그대로 내보내지 않는 이유 — 화면은 "관리자인가" 만
    // 알면 된다. 숫자를 보내면 화면마다 1 과 비교하는 코드를 적게 되고,
    // 등급이 늘어날 때 그 비교가 전부 틀어진다
    //
    // 칸이 아니라 메서드라서 userLevel 과 어긋날 자리가 없다
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
