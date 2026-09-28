package com.meokji.api.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.meokji.api.security.LoginUserArgumentResolver;

/**
 * {@code @LoginUser} 인자를 채워주는 리졸버를 등록한다.
 *
 * <p>이 등록을 빼먹으면 컨트롤러의 {@code @LoginUser User me} 가 조용히 null 로
 * 들어온다 - 로그인 검사가 사라진 채로 서버가 정상 동작하는 것처럼 보인다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private LoginUserArgumentResolver loginUserResolver;


    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        resolvers.add(loginUserResolver);
    }
}
