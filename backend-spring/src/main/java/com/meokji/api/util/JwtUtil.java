package com.meokji.api.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.meokji.api.config.AppProperties;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;

/**
 * Access Token 발급 · 해석.
 *
 * <p>파이썬 쪽 {@code app/core/security.py} 의 JWT 부분이다. Refresh Token 은
 * 쓰지 않는다 - 그쪽과 같은 결정이다.
 *
 * <p><b>파이썬이 발급한 토큰과 서로 통한다.</b> 같은 HS256, 같은 비밀키,
 * {@code sub} 에 회원 id 를 문자열로 담는 것까지 같다. 그래서 두 서버를 같이
 * 띄워놓고 한쪽에서 로그인해 받은 토큰으로 다른 쪽을 부를 수 있다 - 포팅 중에
 * 응답을 비교할 때 이게 편하다.
 */
@Component
public class JwtProvider {

    @Autowired
    private AppProperties app;

    private SecretKey key;
    private long expireSeconds;

    /**
     * 비밀키를 서명에 쓸 수 있는 형태로 만들어 둔다.
     *
     * <p>생성자가 아니라 {@code @PostConstruct} 인 이유는 필드 주입이라서다 -
     * 생성자가 도는 시점에는 {@code app} 이 아직 null 이다. 주입이 끝난 뒤에
     * 스프링이 이 메서드를 불러준다.
     *
     * <p>여기서 예외가 나면 <b>서버가 아예 뜨지 않는다.</b> 그게 맞다 - 토큰을
     * 만들 때마다 실패하는 것보다 시작할 때 멈추는 편이 낫다.
     */
    @PostConstruct
    void init() {
        String secret = app.getJwt().getSecretKey();

        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "app.jwt.secret-key 가 비어 있습니다. "
                    + "application-local.yml.example 을 복사해서 채우세요.");
        }

        // HS256 은 32바이트 이상을 요구한다. 짧으면 여기서 WeakKeyException 이 난다.
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expireSeconds = app.getJwt().getExpiresInSeconds();
    }

    /** 회원 id 를 담은 토큰을 만든다. */
    public String createAccessToken(Long userId) {
        Instant now = Instant.now();

        return Jwts.builder()
                // RFC 7519 상 sub 는 문자열이어야 한다. 파이썬 쪽도 str(user_id) 로 넣는다.
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(expireSeconds)))
                .signWith(key)
                .compact();
    }

    /**
     * 토큰에서 회원 id 를 꺼낸다.
     *
     * <p>만료됐든 서명이 안 맞든 형식이 깨졌든 <b>전부 null</b> 을 돌려준다.
     * 어느 쪽인지 응답에 밝히지 않는 것이 파이썬 쪽 결정이고, 공격자에게 힌트가
     * 되기 때문이다. 부르는 쪽은 null 이면 401 을 낸다.
     */
    public Long readUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String subject = claims.getSubject();
            if (subject == null || !subject.chars().allMatch(Character::isDigit)) {
                return null;
            }

            return Long.valueOf(subject);
        } catch (JwtException | IllegalArgumentException exc) {
            return null;
        }
    }

    /** 응답의 expires_in 에 그대로 들어가는 값(초). */
    public long getExpiresInSeconds() {
        return expireSeconds;
    }
}
