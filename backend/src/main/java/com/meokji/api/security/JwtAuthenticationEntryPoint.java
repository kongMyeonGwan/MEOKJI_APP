package com.meokji.api.security;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 로그인이 필요한 주소에 로그인 없이 들어왔을 때 응답을 쓴다. (401)
 *
 * <h3>언제 불리나</h3>
 *
 * {@link JwtAuthenticationFilter} 가 인증을 못 올렸는데, 그 주소가
 * {@code .authenticated()} 로 묶여 있을 때 Spring Security 가 여기를 부른다.
 * 로그인 없이도 되는 주소(레시피 · 게시판 목록)는 여기까지 오지 않는다.
 *
 * <h3>왜 직접 만들었나</h3>
 *
 * 기본 동작은 본문이 비어 있는 401 이다. 그런데 이 프로젝트의 프론트는 모든 에러를
 * {@code {"detail": "문장"}} 모양으로 읽어서 화면에 띄운다. 기본값대로 두면 화면에
 * 아무 문구도 안 뜨고 "무슨 일이 난 건지 모르겠는" 상태가 된다.
 *
 * <h3>문구를 두 가지로 나누는 이유</h3>
 *
 * <pre>
 *   토큰을 안 보냄        → "인증이 필요합니다."
 *   토큰이 썩었거나 만료  → "토큰이 유효하지 않거나 만료되었습니다."
 * </pre>
 *
 * 프론트가 이 둘을 다르게 처리한다. 뒤쪽이면 Refresh Token 으로 재발급을 시도해볼
 * 여지가 있고, 앞쪽이면 그냥 로그인 화면으로 보내면 된다.
 *
 * 사유는 필터가 {@link SecurityErrors#remember} 로 적어둔 것을 읽는다. 필터가 그
 * 자리에서 401 을 쓰지 못하는 이유는 {@link JwtAuthenticationFilter} 주석 참고.
 */
@Component
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException) throws IOException {

        // 필터가 적어둔 사유를 꺼낸다. 없으면 = 토큰을 아예 안 보낸 것이다.
        String message = SecurityErrors.rememberedMessage(request);
        if (message == null) {
            message = "인증이 필요합니다.";
        }

        // 계정이 잠긴 경우만 423 이고, 나머지는 401 이다.
        // (잠금은 "로그인을 20회 틀려서 막혔다" 는 뜻이라 화면 안내가 달라야 한다)
        int status = SecurityErrors.rememberedStatus(request, HttpStatus.UNAUTHORIZED.value());

        SecurityErrors.write(response, status, message);
    }
}
