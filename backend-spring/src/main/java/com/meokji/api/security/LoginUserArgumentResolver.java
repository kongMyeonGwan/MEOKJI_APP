package com.meokji.api.security;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.MethodParameter;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import com.meokji.api.common.ApiException;
import com.meokji.api.mapper.UserMapper;
import com.meokji.api.vo.User;

import jakarta.servlet.http.HttpServletRequest;

/**
 * {@code Authorization: Bearer ...} 를 읽어 {@link LoginUser} 가 붙은 인자에 회원을 넣는다.
 *
 * <p>파이썬 쪽 {@code app/core/deps.py} 자리다. 필터가 아니라 인자 리졸버로 둔 이유는,
 * "이 엔드포인트에 로그인이 필요한가" 를 컨트롤러 선언부에서 읽을 수 있게 하려는
 * 것이다. 필터에 주소 목록을 적어두면 라우터가 늘 때 두 곳을 고쳐야 한다.
 */
@Component
public class LoginUserArgumentResolver implements HandlerMethodArgumentResolver {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    @Autowired
    private JwtProvider jwt;
    @Autowired
    private UserMapper users;


    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(LoginUser.class)
                && User.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(
            MethodParameter parameter,
            ModelAndViewContainer container,
            NativeWebRequest request,
            WebDataBinderFactory binderFactory) {

        LoginUser marker = parameter.getParameterAnnotation(LoginUser.class);
        boolean required = marker == null || marker.required();
        boolean adminOnly = marker != null && marker.admin();

        String token = readToken(request);

        if (token == null) {
            if (!required) {
                // 게스트도 볼 수 있는 화면. 로그인 안 한 사람으로 취급한다.
                return null;
            }
            throw ApiException.unauthorized("인증이 필요합니다.");
        }

        User user;
        try {
            user = findByToken(token);
        } catch (ApiException exc) {
            // required=false 일 때는 토큰이 깨져 있어도 화면을 막지 않는다.
            // 파이썬 get_optional_user 가 예외를 삼키고 None 을 주던 것과 같다.
            if (!required) {
                return null;
            }
            throw exc;
        }

        if (adminOnly && !user.isAdmin()) {
            // 404 로 숨기지 않고 403 을 낸다. 있는 기능을 권한 때문에 못 쓰는 것과
            // 없는 주소를 부른 것은 다른 이야기다.
            throw ApiException.forbidden("관리자만 할 수 있는 작업입니다.");
        }

        return user;
    }

    private String readToken(NativeWebRequest request) {
        HttpServletRequest raw = request.getNativeRequest(HttpServletRequest.class);
        if (raw == null) {
            return null;
        }

        String header = raw.getHeader(HEADER);
        if (header == null || !header.startsWith(PREFIX)) {
            return null;
        }

        String token = header.substring(PREFIX.length()).trim();

        return token.isEmpty() ? null : token;
    }

    /** 토큰 문자열로 회원을 찾는다. 실패하면 401(잠긴 계정은 423). */
    private User findByToken(String token) {
        Long userId = jwt.readUserId(token);

        if (userId == null) {
            throw ApiException.unauthorized("토큰이 유효하지 않거나 만료되었습니다.");
        }

        User user = users.findById(userId);

        if (user == null || !"Y".equals(user.getUseYn())) {
            // 토큰을 받은 뒤에 탈퇴한 경우. 서명은 멀쩡한데 주인이 없다.
            throw ApiException.unauthorized("존재하지 않는 사용자입니다.");
        }

        if (user.isLockedAccount()) {
            // 로그인한 뒤에 잠긴 경우. 이미 받아둔 토큰으로 계속 쓰지 못하게 막는다.
            throw ApiException.locked("계정이 잠겼습니다.");
        }

        return user;
    }
}
