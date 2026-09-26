package com.meokji.api.user.dto;

import java.math.BigDecimal;

import com.meokji.api.common.Times;
import com.meokji.api.user.User;

/**
 * 마이페이지 · 사이드바 · 상단바가 함께 쓰는 내 정보.
 *
 * <p>{@code streakDays} 와 {@code cookCount} 는 DB 컬럼이 아니다. 조회할 때 세서
 * 계산한다 - 비정규화 컬럼은 동기화 버그 위험이 성능 이득보다 크다는 판단이
 * {@code docs/PROGRESS.md} 에 적혀 있다.
 *
 * <p>좌표를 {@code BigDecimal} 로 내보낸다. 컬럼이 {@code NUMBER(9,6)} 인데
 * {@code double} 로 바꾸면 37.5177 이 37.51769999... 로 떠서 파이썬 응답과 값이
 * 달라진다. Jackson 은 BigDecimal 을 따옴표 없는 숫자로 쓰므로 화면에서 계산도 된다.
 */
public record ProfileResponse(
        Long id,
        String email,
        String nickname,
        String createdAt,
        String gender,
        String addressSido,
        String addressSigungu,
        BigDecimal latitude,
        BigDecimal longitude,
        int streakDays,
        int cookCount) {

    public static ProfileResponse of(User user, int streakDays, int cookCount) {
        return new ProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                Times.toText(user.getCreatedAt()),
                user.getGender(),
                user.getAddressSido(),
                user.getAddressSigungu(),
                user.getLatitude(),
                user.getLongitude(),
                streakDays,
                cookCount);
    }
}
