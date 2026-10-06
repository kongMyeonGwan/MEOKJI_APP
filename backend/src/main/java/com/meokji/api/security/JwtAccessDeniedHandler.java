package com.meokji.api.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 로그인은 했는데 그 일을 할 권한이 없을 때 응답을 쓴다. (403)
 *
 * <h3>401 과 403 의 차이</h3>
 *
 * <pre>
 *   401 Unauthorized  "네가 누군지 모르겠다"  → 로그인하면 될 수도 있다
 *   403 Forbidden     "누군지는 알겠는데 안 된다" → 로그인해도 소용없다
 * </pre>
 *
 * 이 프로젝트에서 403 이 나는 곳은 공지 쓰기 · 수정 · 삭제다. 일반 회원으로
 * 로그인한 사람이 관리자 전용 주소를 부른 경우다.
 *
 * <h3>글 주인 확인은 여기서 안 한다</h3>
 *
 * "남의 글은 못 고친다" 도 403 이지만 그건 서비스가
 * {@code throw ApiException.forbidden(...)} 으로 던지고 ApiExceptionHandler 가 받는다.
 * 주소만 보고는 그 글이 누구 것인지 알 수 없어서 DB 를 봐야 하기 때문이다.
 * Spring Security 는 "주소 + 권한" 처럼 DB 없이 판단되는 것만 맡는다.
 */
@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException) throws IOException {

        // 예전에 AuthService.getAdminUser() 가 쓰던 문구를 그대로 쓴다.
        // 화면에 이 문장이 뜨고 있어서, 바꾸면 사용자가 보는 글이 달라진다.
        SecurityErrors.write(response, HttpStatus.FORBIDDEN.value(), "관리자만 할 수 있는 작업입니다.");
    }
}
