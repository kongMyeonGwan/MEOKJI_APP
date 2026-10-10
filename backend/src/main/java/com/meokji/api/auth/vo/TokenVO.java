package com.meokji.api.auth.vo;

import com.meokji.api.user.vo.UserVO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 로그인 성공 응답.
 * 모양: { "access_token": "eyJ...", "token_type": "bearer", "expires_in": 1800, "user": {...} }
 *
 * <p>칸 이름이 accessToken 인데 JSON 으로는 access_token 으로 나간다.
 * application.yml 의 property-naming-strategy: SNAKE_CASE 가 바꿔준다.
 *
 * <p>토큰만 주지 않고 넷을 같이 보내는 이유
 *
 * <p>화면이 로그인 직후에 해야 할 일이 셋이다 — 토큰을 저장하고, 닉네임을
 * 띄우고, 언제 만료되는지 알아 두는 것. 토큰만 주면 닉네임을 얻으려고
 * /me 를 한 번 더 불러야 하고, 만료 시각은 토큰을 직접 풀어봐야 안다.
 * 서버가 방금 다 알고 있던 값이라 같이 실어 보낸다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenVO {

    // 로그인 토큰. 화면은 이것을 localStorage 에 두고, 요청마다
    // Authorization: Bearer <이 값> 으로 붙여 보낸다
    private String accessToken;

    // 항상 "bearer". 지금은 한 종류뿐이지만 토큰 방식이 바뀔 수 있어
    // 화면이 종류를 보고 붙이도록 같이 보낸다
    private String tokenType;

    // 토큰 유효 시간(초). 만료 시각이 아니라 "지금부터 몇 초" 다.
    //
    // 시각으로 주면 브라우저 시계가 서버와 다를 때 어긋난다. 초로 주면
    // 화면이 받은 순간을 기준으로 재므로 시계가 틀려도 상관없다.
    // 기본 1800(30분) — application.yml 의 app.jwt.expire-minutes 가 정한다
    private long expiresIn;

    // 로그인한 회원 정보. 비밀번호 칸은 UserVO 에서 @JsonIgnore 로 막혀 있다
    private UserVO user;
}
