package com.meokji.api.auth.service;

import com.meokji.api.auth.dto.LoginRequest;
import com.meokji.api.auth.dto.RegisterRequest;
import com.meokji.api.user.vo.UserVO;

/**
 * 회원가입 · 로그인 · 로그인한 사람 찾기.
 * 실제 코드는 AuthServiceImpl 에 있다.
 */
public interface AuthService {

    // 회원가입. 이메일이나 닉네임이 겹치면 409 에러
    UserVO register(RegisterRequest request);

    // 로그인. 이메일 · 비밀번호가 맞으면 회원 정보를 돌려준다
    UserVO login(LoginRequest request);

    // 로그인한 사람 찾기. 토큰이 없거나 잘못됐으면 401 에러
    //   token : 요청 헤더의 Authorization 값 ("Bearer eyJhbGci...")
    UserVO getLoginUser(String token);

    // 로그인한 사람 찾기. 로그인 안 했으면 에러 대신 null (로그인 없이도 되는 화면에서 쓴다)
    UserVO findLoginUser(String token);

    // 관리자 찾기. 관리자가 아니면 403 에러
    UserVO getAdminUser(String token);
}
