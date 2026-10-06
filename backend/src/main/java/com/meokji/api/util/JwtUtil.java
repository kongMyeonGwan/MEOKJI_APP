package com.meokji.api.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
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
 * <h3>토큰이 두 종류인 이유</h3>
 *
 * <pre>
 *   Access Token  (30분)  API 를 부를 때마다 Authorization 헤더에 실어 보낸다.
 *                         짧다. 새어도 30분 뒤면 쓸모가 없다.
 *                         DB 에 저장하지 않는다 - 서명만 맞으면 믿는다.
 *
 *   Refresh Token (14일)  Access Token 을 새로 받아오는 데에만 쓴다.
 *                         길다. 그래서 HttpOnly 쿠키에만 담고, DB 에도
 *                         해시를 남겨 서버가 언제든 무효로 만들 수 있게 한다.
 * </pre>
 *
 * 하나로 합치면 둘 중 하나를 포기해야 한다 - 30분짜리 하나면 30분마다 다시
 * 로그인해야 하고, 14일짜리 하나면 그 값이 새는 순간 2주 동안 남의 계정이 열린다.
 * 나눠두면 "자주 돌아다니는 값은 짧게, 긴 값은 꽁꽁 숨겨서" 둘 다 가져갈 수 있다.
 *
 * <h3>type 칸을 넣는 이유</h3>
 *
 * 두 토큰은 같은 비밀키로 서명한다. 그래서 서명 검사만 해서는 구별이 안 되고,
 * 14일짜리 Refresh Token 을 Authorization 헤더에 넣어 API 를 부르면 그냥 통과해버린다.
 * 그걸 막으려고 토큰 안에 {@code type} 을 적어두고, 꺼낼 때 종류까지 확인한다.
 *
 * <pre>
 *   로그인 성공  → createAccessToken(id) · createRefreshToken(id)
 *   API 요청     → getUserId(토큰)          누가 보냈는지 (Access 만 통과)
 *   토큰 재발급  → getRefreshUserId(토큰)   누가 보냈는지 (Refresh 만 통과)
 * </pre>
 */
@Component
public class JwtUtil {

    /** 토큰 안에 적어두는 종류 이름. 이 글자가 다르면 거절한다. */
    private static final String TYPE_ACCESS = "access";
    private static final String TYPE_REFRESH = "refresh";

    /** 종류를 담는 칸 이름. JWT 안에서 {"type": "access"} 모양이 된다. */
    private static final String CLAIM_TYPE = "type";

    // application.yml 의 app.jwt.secret-key (32글자 이상이어야 한다)
    @Value("${app.jwt.secret-key}")
    private String secretKey;

    // application.yml 의 app.jwt.expire-minutes (Access Token 유효 시간, 분)
    @Value("${app.jwt.expire-minutes}")
    private int expireMinutes;

    // application.yml 의 app.jwt.refresh-expire-days (Refresh Token 유효 시간, 일)
    @Value("${app.jwt.refresh-expire-days}")
    private int refreshExpireDays;

    // 비밀키 글자를 서명용 키로 바꾼다
    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    // ===================== 토큰 만들기 =====================

    /** Access Token 만들기 (30분). API 를 부를 때 쓰는 토큰이다. */
    public String createAccessToken(Long userId) {
        return createToken(userId, TYPE_ACCESS, getExpiresInSeconds());
    }

    /**
     * 예전 이름. 지금은 {@link #createAccessToken} 과 같다.
     *
     * 이미 여러 곳에서 이 이름으로 부르고 있어서 남겨둔다.
     */
    public String createToken(Long userId) {
        return createAccessToken(userId);
    }

    /** Refresh Token 만들기 (14일). Access Token 을 새로 받아올 때만 쓴다. */
    public String createRefreshToken(Long userId) {
        return createToken(userId, TYPE_REFRESH, getRefreshExpiresInSeconds());
    }

    // 두 토큰이 똑같이 거치는 부분. 다른 건 type 과 유효 시간뿐이다.
    private String createToken(Long userId, String type, long expiresInSeconds) {
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + expiresInSeconds * 1000);

        return Jwts.builder()
                .subject(String.valueOf(userId)) // 회원 번호
                .claim(CLAIM_TYPE, type) // 종류 (access / refresh)
                .issuedAt(now) // 만든 시각
                .expiration(expireAt) // 만료 시각
                .signWith(getKey()) // 비밀키로 서명
                .compact();
    }

    // ===================== 토큰 읽기 =====================

    /**
     * Access Token 에서 회원 번호 꺼내기.
     *
     * 다음이면 전부 null 이다 - 부르는 쪽은 null 하나만 보면 된다.
     * <ul>
     *   <li>서명이 안 맞는다 (누가 내용을 고쳤다)</li>
     *   <li>만료됐다</li>
     *   <li>Refresh Token 이다 (API 호출에 쓸 수 없다)</li>
     * </ul>
     */
    public Long getUserId(String token) {
        return getUserId(token, TYPE_ACCESS);
    }

    /**
     * Refresh Token 에서 회원 번호 꺼내기. 잘못됐으면 null.
     *
     * Access Token 을 넣으면 null 이다. 30분짜리로 14일 효과를 내려는 시도를 막는다.
     */
    public Long getRefreshUserId(String token) {
        return getUserId(token, TYPE_REFRESH);
    }

    // 실제 검사. 서명 · 만료는 라이브러리가 보고, 종류는 여기서 본다.
    private Long getUserId(String token, String expectedType) {
        if (token == null || token.isBlank()) {
            return null;
        }

        try {
            // parseSignedClaims 안에서 두 가지를 본다.
            //   1. 서명이 우리 비밀키로 만든 게 맞는지  → 아니면 JwtException
            //   2. 만료 시각이 지나지 않았는지          → 지났으면 JwtException
            Claims claims = Jwts.parser()
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            // 3. 종류가 맞는지. 이건 라이브러리가 모르는 우리 규칙이라 직접 본다.
            String type = claims.get(CLAIM_TYPE, String.class);
            if (!expectedType.equals(type)) {
                return null;
            }

            return Long.valueOf(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            // JwtException      : 서명 불일치 · 만료 · 모양이 JWT 가 아님
            // IllegalArgumentException : subject 가 숫자가 아님
            return null;
        }
    }

    // ===================== 유효 시간 =====================

    /** Access Token 유효 시간(초). 로그인 응답의 expires_in 에 들어간다. */
    public long getExpiresInSeconds() {
        return expireMinutes * 60L;
    }

    /** Refresh Token 유효 시간(초). 쿠키의 Max-Age 에도 이 값을 쓴다. */
    public long getRefreshExpiresInSeconds() {
        return refreshExpireDays * 24L * 60L * 60L;
    }

    /**
     * Refresh Token 이 만료되는 시각. DB 의 refresh_expires_at 에 넣는 값이다.
     *
     * UTC 로 만든다. 이 스키마의 모든 시각 컬럼이 UTC 고정이라서다
     * (db/01_schema.sql 상단 참고). 서버 시간대를 따라가면 PC 설정에 따라
     * 만료 시각이 9시간씩 어긋난다.
     */
    public LocalDateTime getRefreshExpiresAt() {
        return Times.nowUtc().plusSeconds(getRefreshExpiresInSeconds());
    }

    // ===================== 해시 =====================

    /**
     * Refresh Token 을 SHA-256 으로 요약한다. DB 에는 이 값만 저장한다.
     *
     * <h3>왜 원문을 저장하지 않나</h3>
     *
     * Refresh Token 은 14일 동안 "이 사람이 맞다" 를 증명하는 값이다. 원문을 그대로
     * 넣어두면 DB 가 한 번 새는 순간, 그 값을 복사해 쿠키에 넣는 것만으로 남의 계정에
     * 들어갈 수 있다. 비밀번호를 평문으로 저장하지 않는 것과 똑같은 이유다.
     *
     * 해시는 되돌릴 수 없다. 서버는 "쿠키로 들어온 토큰을 해시해서 DB 값과 같은지" 만
     * 비교하면 되므로, 원문이 없어도 검증에 아무 지장이 없다.
     *
     * <h3>왜 BCrypt 가 아니라 SHA-256 인가</h3>
     *
     * BCrypt 는 일부러 느리게 만든 함수다. 사람이 고른 비밀번호("1234")는 짧고 뻔해서
     * 공격자가 전부 넣어볼 수 있고, 그래서 한 번 계산에 시간을 끌어 대입을 막아야 한다.
     *
     * Refresh Token 은 서버가 만든 JWT 라 길고 무작위다. 전부 넣어보는 공격이 애초에
     * 불가능해서 느리게 만들 이유가 없고, /refresh 를 부를 때마다 BCrypt 를 돌리면
     * 그만큼 응답만 느려진다.
     *
     * @return 16진수 소문자 64글자 (SHA-256 은 32바이트 = 16진수 64글자로 길이가 늘 같다)
     */
    public String hashRefreshToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));

            // 바이트 32개를 16진수 글자 64개로 바꾼다.
            // 바이트 하나가 글자 두 개(00 ~ ff)가 된다.
            StringBuilder hex = new StringBuilder();
            for (int i = 0; i < bytes.length; i++) {
                // & 0xff : 자바 byte 는 -128~127 이라 음수가 나온다. 0~255 로 되돌린다.
                String one = Integer.toHexString(bytes[i] & 0xff);
                if (one.length() == 1) {
                    hex.append('0'); // "f" 처럼 한 글자면 "0f" 로 채운다
                }
                hex.append(one);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 은 자바가 반드시 가지고 있다. 여기로 올 일이 없다.
            throw new IllegalStateException("SHA-256 을 쓸 수 없다", e);
        }
    }
}
