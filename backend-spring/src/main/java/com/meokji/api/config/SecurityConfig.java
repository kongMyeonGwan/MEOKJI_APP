package com.meokji.api.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * 인증 · CORS 설정.
 *
 * <p>Spring Security 를 쓰지만 세션과 로그인 폼은 끈다. 이 서비스는 JWT 하나로
 * 판단하고(Refresh Token 도 쓰지 않는다), 화면은 React 라서 서버가 그릴 로그인
 * 페이지가 없다.
 *
 * <p><b>권한 검사를 여기서 URL 로 하지 않는다.</b> 파이썬 쪽이 라우터마다
 * {@code Depends(get_current_user)} 로 걸었던 것과 같은 자리를 지킨다 - 주소 목록을
 * 여기에 또 적어두면 라우터를 추가할 때 두 곳을 고쳐야 하고, 한쪽만 고치면
 * 조용히 열린다. 토큰은 필터가 읽어 넣고, 로그인 필요 여부는 컨트롤러가 정한다.
 */
@Configuration
public class SecurityConfig {

    @Autowired
    private AppProperties app;


    /**
     * bcrypt. 파이썬 쪽 passlib 와 같은 알고리즘이라 <b>기존 회원의 비밀번호가
     * 그대로 통한다</b> - 저장된 해시($2b$...)를 다시 만들 필요가 없다.
     *
     * <p>주의: bcrypt 는 72바이트가 상한이다. 한글 비밀번호는 24자가 최대이고,
     * 넘으면 조용히 잘린다. 검증은 바이트 기준으로 해야 한다
     * (docs/PROGRESS.md 의 오라클·환경 함정 8번).
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                // 토큰으로만 판단하므로 CSRF 토큰이 필요 없다. 쿠키를 쓰지 않는다.
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // 브라우저 기본 인증 창과 로그인 폼을 끈다. 둘 다 React 화면과 충돌한다.
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .logout(logout -> logout.disable())
                // 위에 적은 이유로 URL 별 권한을 여기서 정하지 않는다.
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());

        return http.build();
    }

    /**
     * React(Vite) 개발 서버는 5173, 이 서버는 8080 이라 출처가 다르다.
     *
     * <p>{@code allowedOrigins} 를 {@code *} 로 두지 않는다. 자격 증명을 허용하면
     * 브라우저가 와일드카드를 거부한다.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(app.getCorsOriginList());
        config.setAllowedMethods(List.of(
                HttpMethod.GET.name(),
                HttpMethod.POST.name(),
                HttpMethod.PUT.name(),
                HttpMethod.PATCH.name(),
                HttpMethod.DELETE.name(),
                HttpMethod.OPTIONS.name()));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        // 첨부파일 내려받기에서 원본 파일명을 읽어야 한다.
        config.setExposedHeaders(List.of("Content-Disposition"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }
}
