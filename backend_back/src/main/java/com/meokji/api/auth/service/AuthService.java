package com.meokji.api.auth.service;

import com.meokji.api.auth.vo.LoginVO;
import com.meokji.api.auth.vo.RegisterVO;
import com.meokji.api.user.vo.UserVO;

/**
 * 회원가입 · 로그인 · 로그인한 사람 찾기.
 * 실제 코드는 AuthServiceImpl 에 있다.
 *
 * <p>아래 세 개(getLoginUser · findLoginUser · getAdminUser)는 auth 기능뿐 아니라
 * 거의 모든 컨트롤러가 쓴다. 글을 쓰려면 "누가 쓰는지" 를 알아야 하고, 그 판단은
 * 토큰을 풀어 회원을 찾는 일이라 auth 의 일이다. 기능마다 토큰을 직접 풀면
 * 만료 처리나 잠긴 계정 처리를 빠뜨리는 곳이 생긴다.
 */
public interface AuthService {

    // 회원가입. 이메일이나 닉네임이 겹치면 409 에러
    UserVO register(RegisterVO request);

    // 로그인. 이메일 · 비밀번호가 맞으면 회원 정보를 돌려준다
    // 틀리면 401, 잠긴 계정이면 423
    UserVO login(LoginVO request);

    // 로그인한 사람 찾기. 토큰이 없거나 잘못됐으면 401 에러
    //   token : 요청 헤더의 Authorization 값 ("Bearer eyJhbGci...")
    //
    // 로그인이 꼭 필요한 자리에서 쓴다 (글쓰기 · 내 정보 · 좋아요 등).
    // 에러를 던지므로 부른 다음 줄에서는 회원이 있다고 믿어도 된다.
    UserVO getLoginUser(String token);

    // 로그인한 사람 찾기. 로그인 안 했으면 에러 대신 null (로그인 없이도 되는 화면에서 쓴다)
    //
    // 게시글 목록처럼 누구나 볼 수 있지만, 로그인했으면 "내가 좋아요를
    // 눌렀는지" 를 같이 표시해야 하는 자리에서 쓴다. 토큰이 없으면 null 을
    // 받아 그 표시만 빼고 그린다.
    UserVO findLoginUser(String token);

    // 관리자 찾기. 관리자가 아니면 403 에러
    //
    // 공지 작성 · 사용자 정지처럼 관리자만 되는 자리에서 쓴다.
    // getLoginUser 로 찾은 뒤 role 을 각자 확인하게 두면 한 곳에서
    // 빠뜨리게 되므로, 확인까지 묶어 하나의 메서드로 만들었다.
    UserVO getAdminUser(String token);
}
