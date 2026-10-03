package com.meokji.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 서버 전체 설정.
 *
 * 1. CORS : 리액트(5173)와 이 서버(8090)는 주소가 달라서, 브라우저가 요청을 막지 않게 허락해준다.
 * 2. BCryptPasswordEncoder : 비밀번호를 암호화하는 도구. 서비스에서 @Autowired 로 받아 쓴다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    // application.yml 의 app.cors-origins 값 (예: http://localhost:5173)
    @Value("${app.cors-origins}")
    private String corsOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins(corsOrigins.split(","))
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Content-Disposition", "Retry-After")
                .allowCredentials(true);
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
