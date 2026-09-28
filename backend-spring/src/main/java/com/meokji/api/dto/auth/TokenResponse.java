package com.meokji.api.dto.auth;

/**
 * 로그인 성공 응답.
 *
 * <p>{@code tokenType} 은 소문자 "bearer" 다. 프론트가 이 값을 그대로
 * {@code Authorization} 헤더에 붙이므로 대소문자를 바꾸면 안 된다.
 *
 * <p>{@code expiresIn} 은 초 단위다. 프론트가 만료 시각을 계산할 때 쓴다.
 */
public record TokenResponse(
        String accessToken,
        String tokenType,
        long expiresIn,
        UserResponse user) {

    public static TokenResponse of(String accessToken, long expiresIn, UserResponse user) {
        return new TokenResponse(accessToken, "bearer", expiresIn, user);
    }
}
