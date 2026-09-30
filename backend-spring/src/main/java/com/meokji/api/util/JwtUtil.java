package com.meokji.api.util;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

/**
 * 로그인 토큰(JWT) 만들기 · 읽기.
 *
 * 토큰 안에는 "회원 번호(id)" 와 "만료 시각" 이 들어 있다.
 * 비밀키로 서명해서, 누가 토큰 내용을 바꾸면 읽을 때 실패한다.
 *
 *   로그인 성공 → createToken(회원 id)  → 프론트가 localStorage 에 저장
 *   API 요청    → getUserId(토큰)       → 누가 보낸 요청인지 안다
 */
@Component
public class JwtUtil {

    // application.yml 의 app.jwt.secret-key (32글자 이상이어야 한다)
    @Value("${app.jwt.secret-key}")
    private String secretKey;

    // application.yml 의 app.jwt.expire-minutes (토큰 유효 시간, 분)
    @Value("${app.jwt.expire-minutes}")
    private int expireMinutes;

    // 비밀키 글자를 서명용 키로 바꾼다
    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    // 토큰 만들기
    public String createToken(Long userId) {
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + getExpiresInSeconds() * 1000);

        return Jwts.builder()
                .subject(String.valueOf(userId)) // 회원 번호
                .issuedAt(now) // 만든 시각
                .expiration(expireAt) // 만료 시각
                .signWith(getKey()) // 비밀키로 서명
                .compact();
    }

    // 토큰에서 회원 번호 꺼내기. 만료됐거나 잘못된 토큰이면 null
    public Long getUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return Long.valueOf(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    // 토큰 유효 시간(초). 로그인 응답의 expires_in 에 들어간다.
    public long getExpiresInSeconds() {
        return expireMinutes * 60L;
    }
}
