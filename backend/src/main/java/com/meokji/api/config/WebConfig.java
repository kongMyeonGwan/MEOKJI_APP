package com.meokji.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 웹 관련 공통 설정.
 *
 * <p>CORS 설정은 여기 있었는데 {@link com.meokji.api.security.SecurityConfig} 로 옮겼다.
 * Spring Security 필터가 Controller 보다 앞에서 도는데, 여기(WebMvcConfigurer)에 적은
 * CORS 는 Controller 단계에서 적용된다. 그러면 브라우저가 본 요청 전에 보내는
 * 확인 요청(OPTIONS)이 보안 필터에서 먼저 막혀 본 요청이 출발하지 못한다.
 * 두 군데에 적어두면 응답 헤더가 겹칠 수도 있어서 한 곳만 남겼다.
 */
@Configuration
public class WebConfig {

    /**
     * 비밀번호 암호화기.
     *
     * <p>회원가입 때 비밀번호를 해시로 바꿔 저장하고, 로그인 때 입력값과 대조한다.
     * 같은 비밀번호라도 매번 다른 해시가 나오므로(소금을 섞는다) DB 만 보고는
     * 두 사람이 같은 비밀번호를 쓰는지도 알 수 없다.
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
