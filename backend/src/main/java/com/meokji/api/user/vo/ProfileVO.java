package com.meokji.api.user.vo;

import java.math.BigDecimal;

import lombok.Data;

/**
 * 마이페이지 응답. 주소: GET / PATCH /api/users/me
 *
 * USERS 표 한 행이 아니다. streakDays(연속 기록일) · cookCount(요리 횟수)는
 * 컬럼이 아니라 식사 기록을 **세어서** 만든다. 그래서 UserVO 와 칸이 다르고,
 * 한 클래스로 합치면 로그인 응답에도 "요리 0회" 가 따라 나간다.
 *
 * 나가는 모양: { "id", "email", "nickname", "created_at", "gender",
 *               "address_sido", "address_sigungu", "latitude", "longitude",
 *               "streak_days", "cook_count" }
 *
 * <p>응답 전용이다. 수정 요청은 ProfileUpdateVO 가 받는다 - 받는 칸과
 * 내보내는 칸이 많이 달라서(이메일 · 가입일 · 좌표 · 통계는 못 고친다)
 * 하나로 겸하면 "보내도 반영되나" 를 칸마다 확인해야 한다.
 *
 * <p>비밀번호 칸이 아예 없다. UserVO 는 @JsonIgnore 로 막고 있지만,
 * 이쪽은 칸 자체를 두지 않아 실수로 열릴 자리가 없다.
 */
@Data
public class ProfileVO {

    private Long id;

    /** 로그인에 쓰는 이메일. 화면에서 고칠 수 없다 */
    private String email;

    private String nickname;

    private String createdAt; // 가입일 (예: "2026-09-11T21:29:42Z")
    // LocalDateTime 이 아니라 String 이다. 서비스가 Times.toText 로
    // 바꿔 넣는다 - 이 VO 는 DB 한 행이 아니라 응답 모양이라
    // 중간 타입을 둘 이유가 없다

    private String gender;
    private String addressSido;
    private String addressSigungu;

    /** 위도 · 경도 */
    // 화면에 보여주지는 않는다. 날씨를 부를 때 쓸 수 있게 같이 내려준다 -
    // 추천을 부를 때 lat · lon 을 실어 보내면 저장된 주소 기준 날씨가 된다
    //
    // BigDecimal 인 이유 — double 로 담으면 37.5177 이 37.51769999... 로
    // 떠서, 보낼 때마다 자릿수가 지저분해진다
    private BigDecimal latitude;
    private BigDecimal longitude;

    /** 연속 기록일 */
    // 컬럼이 아니다. 식사 기록의 날짜를 세어서 만든다 (UserServiceImpl).
    // USERS 에 쌓아두지 않는 이유 — 기록을 지우면 이 값도 같이 줄어야
    // 하는데, 그러려면 기록을 건드리는 모든 자리에서 이 칸을 고쳐야 한다
    private int streakDays;

    /** 요리 횟수 */
    private int cookCount;
}
