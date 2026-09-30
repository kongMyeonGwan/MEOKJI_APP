package com.meokji.api.auth.vo;

import com.meokji.api.user.vo.UserVO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 로그인 성공 응답.
 * 모양: { "access_token": "eyJ...", "token_type": "bearer", "expires_in": 1800, "user": {...} }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenVO {

    private String accessToken; // 로그인 토큰
    private String tokenType; // 항상 "bearer"
    private long expiresIn; // 토큰 유효 시간(초)
    private UserVO user; // 로그인한 회원 정보
}
