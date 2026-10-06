package com.meokji.api.auth.vo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.meokji.api.user.vo.UserVO;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 로그인 · 토큰 재발급 성공 응답.
 *
 * 나가는 모양: { "access_token": "eyJ...", "token_type": "bearer", "expires_in": 1800, "user": {...} }
 *
 * <h3>Refresh Token 은 왜 이 응답에 없나</h3>
 *
 * 들어 있기는 한데({@link #refreshToken}) 밖으로 나가지 않는다. {@code @JsonIgnore} 가
 * 붙어 있어서다.
 *
 * Refresh Token 은 14일짜리라, 자바스크립트가 들고 있으면 XSS 한 번에 2주짜리 열쇠가
 * 통째로 털린다. 그래서 JSON 이 아니라 <b>HttpOnly 쿠키</b>로만 보낸다. 자바스크립트는
 * 그 쿠키를 읽을 수조차 없고, 브라우저가 알아서 요청에 붙여준다.
 *
 * 그러면 이 칸은 왜 있나 — Controller 가 쿠키를 만들려면 토큰 글자가 필요하기 때문이다.
 * 서비스가 여기에 담아 넘기면 Controller 가 꺼내 쿠키에 넣고, 응답 JSON 에는 안 실린다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TokenVO {

    private String accessToken; // API 를 부를 때 쓰는 토큰 (30분)
    private String tokenType; // 항상 "bearer"
    private long expiresIn; // accessToken 유효 시간(초)
    private UserVO user; // 로그인한 회원 정보

    /**
     * Refresh Token 원문 (14일).
     *
     * 절대 내보내지 않는다. Controller 가 쿠키로 옮겨 담는 데에만 쓴다.
     */
    @JsonIgnore
    private String refreshToken;

    /**
     * "로그인 상태 유지" 체크 여부. 쿠키를 얼마나 남길지 Controller 에 알려준다.
     *
     * 이것도 내보내지 않는다 - 화면이 이미 아는 값이다.
     */
    @JsonIgnore
    private boolean keep;
}
