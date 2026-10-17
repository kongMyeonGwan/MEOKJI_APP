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
 *
 * <p>왜 세션이 아니라 토큰인가 — 세션은 서버가 "누가 로그인했는지" 를
 * 들고 있어야 한다. 토큰은 그 정보가 토큰 안에 들어 있어서 서버가
 * 기억할 것이 없다. 서버를 다시 켜도 로그인이 풀리지 않는다.
 *
 * <p>토큰 안에 회원 번호만 넣는 이유 — 닉네임이나 이메일을 넣으면,
 * 정보를 고친 뒤에도 토큰에는 옛 값이 남는다. 번호만 넣고 나머지는
 * 그때그때 DB 에서 읽는다.
 *
 * <p>토큰 내용은 암호화되지 않는다. 서명만 붙는다 — 누구나 열어 읽을 수
 * 있지만, 내용을 바꾸면 서명이 안 맞아 들통난다. 그래서 비밀번호처럼
 * 감춰야 할 값은 넣지 않는다.
 *
 * <p>로그아웃이 없다. 토큰을 서버가 기억하지 않으니 "이 토큰은 이제
 * 무효" 라고 할 곳도 없다. 화면이 localStorage 에서 지우는 것이 전부이고,
 * 토큰 자체는 만료될 때까지(기본 30분) 유효하다.
 */
@Component
public class JwtUtil {

    // application.yml 의 app.jwt.secret-key (32글자 이상이어야 한다)
    //
    // 32글자가 넘어야 하는 이유 — HMAC-SHA256 서명에 256비트(32바이트)
    // 이상의 키가 필요하다. 짧으면 서버가 뜰 때 Keys.hmacShaKeyFor 가 막는다.
    //
    // 코드에 박지 않고 환경변수로 받는다. 이 값이 새어나가면 누구나
    // 아무 회원 번호로 토큰을 만들 수 있다 - 비밀번호를 몰라도 로그인된다
    @Value("${app.jwt.secret-key}")
    private String secretKey;

    // application.yml 의 app.jwt.expire-minutes (토큰 유효 시간, 분)
    @Value("${app.jwt.expire-minutes}")
    private int expireMinutes;

    // 비밀키 글자를 서명용 키로 바꾼다
    //
    // 만들어 두고 재사용하지 않고 부를 때마다 만든다. 만드는 일이
    // 가볍고, 필드로 들고 있으면 서버가 뜰 때 secretKey 가 아직
    // 안 채워진 상태에서 만들어질 수 있다
    private SecretKey getKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    // 토큰 만들기
    //
    // Date 를 쓰는 이유 — JJWT 라이브러리가 Date 를 받는다.
    // 프로젝트의 다른 곳은 LocalDateTime 을 쓰지만 여기는 맞춰야 한다
    public String createToken(Long userId) {
        Date now = new Date();
        Date expireAt = new Date(now.getTime() + getExpiresInSeconds() * 1000);

        return Jwts.builder()
                .subject(String.valueOf(userId)) // 회원 번호
                // subject 는 JWT 가 정해둔 "이 토큰이 누구 것인가" 칸이다.
                // 글자만 담을 수 있어서 번호를 글자로 바꿔 넣는다
                .issuedAt(now) // 만든 시각
                .expiration(expireAt) // 만료 시각
                .signWith(getKey()) // 비밀키로 서명
                .compact();
    }

    // 토큰에서 회원 번호 꺼내기. 만료됐거나 잘못된 토큰이면 null
    //
    // 예외를 던지지 않고 null 을 주는 이유 — 부르는 쪽(AuthServiceImpl)이
    // 401 을 던질지 null 로 넘길지 정한다. 로그인 없이도 되는 화면에서는
    // 토큰이 잘못돼도 에러가 아니다
    public Long getUserId(String token) {
        try {
            Claims claims = Jwts.parser()
                    // 서명을 확인한다. 이 줄이 없으면 내용을 아무렇게나
                    // 바꾼 토큰도 통과한다 - 회원 번호를 1 로 고치면
                    // 관리자가 될 수 있다
                    .verifyWith(getKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return Long.valueOf(claims.getSubject());
        } catch (JwtException | IllegalArgumentException e) {
            // JwtException        : 서명이 안 맞거나 만료됨
            // IllegalArgumentException : 토큰이 비었거나 모양이 아님,
            //                            또는 subject 가 숫자가 아님
            //
            // 둘을 나눠 다루지 않는 이유 — 어느 쪽이든 "이 토큰으로는
            // 누구인지 알 수 없다" 로 결과가 같다. 이유를 알려주면
            // 토큰을 꾸며보는 쪽에 단서가 된다
            return null;
        }
    }

    // 토큰 유효 시간(초). 로그인 응답의 expires_in 에 들어간다.
    //
    // 60L 로 적는 이유 — expireMinutes 가 int 라 60 을 곱하면 int 끼리의
    // 계산이 된다. L 을 붙이면 long 으로 계산해서, 큰 값을 넣어도
    // 넘치지 않는다
    public long getExpiresInSeconds() {
        return expireMinutes * 60L;
    }
}
