package com.meokji.api.auth.service;

import com.meokji.api.auth.vo.LoginVO;
import com.meokji.api.auth.vo.RegisterVO;
import com.meokji.api.auth.vo.TokenVO;
import com.meokji.api.user.vo.UserVO;

/**
 * 회원가입 · 로그인 · 로그인한 사람 찾기.
 * 실제 코드는 AuthServiceImpl 에 있다.
 */
public interface AuthService {

    // 회원가입. 이메일이나 닉네임이 겹치면 409 에러
    UserVO register(RegisterVO request);

    // 로그인. 이메일 · 비밀번호가 맞으면 회원 정보를 돌려준다
    UserVO login(LoginVO request);

    /**
     * 로그인한 사람에게 토큰 두 개를 만들어준다. (로그인 성공 직후)
     *
     * <pre>
     *   Access Token  (30분) → 응답 JSON 으로 나간다. React 가 메모리에 둔다.
     *   Refresh Token (14일) → HttpOnly 쿠키로만 나간다. 해시를 USERS 에 저장한다.
     * </pre>
     *
     * 돌려주는 TokenVO 안에 Refresh Token 원문도 들어 있지만 @JsonIgnore 라 밖으로
     * 나가지 않는다. Controller 가 그걸 꺼내 쿠키에 담는다.
     *
     * @param keep 화면의 "로그인 상태 유지" 체크 여부. 쿠키 수명이 이걸로 갈린다.
     */
    TokenVO issueTokens(UserVO user, boolean keep);

    /**
     * Refresh Token 으로 Access Token 을 새로 발급한다.
     *
     * 다음을 모두 통과해야 한다. 하나라도 어긋나면 401 이다.
     * <ol>
     *   <li>토큰 자체가 멀쩡한가 (서명 · 만료 · 종류가 refresh 인지)</li>
     *   <li>그 회원이 지금도 쓸 수 있는 계정인가 (탈퇴 · 잠김)</li>
     *   <li>USERS 에 저장된 해시와 같은가 — 로그아웃했거나 다른 기기에서 다시
     *       로그인했으면 여기서 걸린다</li>
     *   <li>DB 의 만료 시각이 지나지 않았는가</li>
     * </ol>
     *
     * 통과하면 Access Token 뿐 아니라 <b>Refresh Token 도 새로 만든다</b>(회전).
     * 이유는 AuthServiceImpl 의 구현 주석 참고.
     *
     * @param refreshToken 쿠키에서 꺼낸 Refresh Token 원문
     */
    TokenVO refresh(String refreshToken, boolean keep);

    /**
     * 로그아웃. USERS 의 Refresh Token 을 지운다.
     *
     * 쿠키를 지우는 것만으로는 부족하다 - 쿠키 값을 미리 복사해둔 사람이 있으면
     * 그걸로 계속 재발급을 받을 수 있다. 서버 기록을 지워야 진짜로 끝난다.
     * (쿠키를 지우는 일은 Controller 가 따로 한다)
     *
     * @param userId 로그아웃할 회원 번호
     */
    void logout(Long userId);

    /**
     * Refresh Token 에서 회원 번호만 꺼낸다. 로그아웃이 "누구인지" 알아내는 데 쓴다.
     *
     * 로그아웃은 Access Token 이 이미 만료된 뒤에도 눌릴 수 있어서, 쿠키만으로도
     * 사람을 찾을 수 있어야 한다. 잘못된 토큰이면 null.
     */
    Long findUserIdByRefreshToken(String refreshToken);

    // 로그인한 사람을 찾는 일은 여기 없다. Spring Security 의 JwtAuthenticationFilter 가
    // 요청 하나마다 한 번 하고, Controller 는 @AuthenticationPrincipal UserVO 로 받는다.
    //
    // 예전에는 getLoginUser(token) · findLoginUser(token) · getAdminUser(token) 세 개가
    // 여기 있었고 Controller 48곳이 그걸 직접 불렀다. 한 군데만 빠뜨려도 그 API 가
    // 인증 없이 열리는 구조라, 길목(필터)에서 한 번만 하도록 옮겼다.
}
