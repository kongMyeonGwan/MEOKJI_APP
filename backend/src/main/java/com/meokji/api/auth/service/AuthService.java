package com.meokji.api.auth.service;

import com.meokji.api.auth.vo.LoginVO;
import com.meokji.api.auth.vo.RegisterVO;
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

    // 로그인한 사람을 찾는 일은 여기 없다. Spring Security 의 JwtAuthenticationFilter 가
    // 요청 하나마다 한 번 하고, Controller 는 @AuthenticationPrincipal UserVO 로 받는다.
    //
    // 예전에는 getLoginUser(token) · findLoginUser(token) · getAdminUser(token) 세 개가
    // 여기 있었고 Controller 48곳이 그걸 직접 불렀다. 한 군데만 빠뜨려도 그 API 가
    // 인증 없이 열리는 구조라, 길목(필터)에서 한 번만 하도록 옮겼다.
}
