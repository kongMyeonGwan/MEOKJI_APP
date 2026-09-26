package com.meokji.api.auth.dto;

import com.meokji.api.common.Times;
import com.meokji.api.user.User;

/**
 * 회원 정보 응답.
 *
 * <p><b>password_hash 를 절대 담지 않는다.</b> 담을 자리를 아예 두지 않는 것이
 * "담지 말아야 한다" 는 주석보다 안전하다.
 *
 * <p>필드는 카멜로 쓰고 나가는 JSON 은 snake 다
 * (application.yml 의 jackson property-naming-strategy).
 *
 * <p>{@code createdAt} 이 문자열인 이유는 {@link Times} 참고 - 파이썬이 내던
 * {@code 2026-09-11T21:29:42.955000Z} 와 글자까지 맞추기 위한 것이다.
 */
public record UserResponse(
        Long id,
        String email,
        String nickname,
        String createdAt,
        String gender,
        String addressSido,
        String addressSigungu,
        boolean isAdmin) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                Times.toText(user.getCreatedAt()),
                user.getGender(),
                user.getAddressSido(),
                user.getAddressSigungu(),
                user.isAdmin());
    }
}
