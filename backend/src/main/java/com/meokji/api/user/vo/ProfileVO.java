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
 *               "address_sido", "address_sigungu", "address_dong", "latitude", "longitude",
 *               "streak_days", "cook_count" }
 */
@Data
public class ProfileVO {

    private Long id;
    private String email;
    private String nickname;
    private String createdAt; // 가입일 (예: "2026-09-11T21:29:42Z")
    private String gender;
    private String addressSido;
    private String addressSigungu;

    private String addressDong;

    /** 위도 · 경도 */
    private BigDecimal latitude;
    private BigDecimal longitude;

    /** 연속 기록일 */
    private int streakDays;

    /** 요리 횟수 */
    private int cookCount;
}
