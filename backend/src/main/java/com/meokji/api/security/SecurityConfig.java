package com.meokji.api.security;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.meokji.api.user.dao.UserDAO;
import com.meokji.api.util.JwtUtil;

import lombok.extern.slf4j.Slf4j;

/**
 * Spring Security 설정. "어느 주소를 누구에게 열지" 를 한 곳에 모은다.
 *
 * <h3>예전에는 어떻게 했나</h3>
 *
 * Controller 마다 {@code @RequestHeader("Authorization") String token} 을 받아서
 * {@code authService.getLoginUser(token)} 을 직접 불렀다. 그 코드가 48곳까지 늘었고,
 * 새 API 를 만들면서 한 줄만 빠뜨리면 그 주소가 조용히 인증 없이 열렸다.
 * <b>빠뜨리는 쪽이 위험한</b> 구조였다.
 *
 * 지금은 반대다. {@code anyRequest().authenticated()} 때문에 <b>기본이 차단</b>이고,
 * 열고 싶은 주소만 아래 목록에 적는다. 적는 걸 빠뜨리면 그냥 401 이 나서 바로 눈에 띈다.
 *
 * <h3>요청이 지나가는 순서</h3>
 *
 * <pre>
 *   요청
 *     ↓
 *   CORS 검사            다른 주소(localhost:5173)에서 온 요청을 허용할지
 *     ↓
 *   JwtAuthenticationFilter   토큰을 풀어 "이 사람이다" 를 SecurityContext 에 올림
 *     ↓
 *   인가 검사 (아래 목록)      이 주소를 이 사람이 부를 수 있는지
 *     ├ 통과  → Controller → Service → DB
 *     ├ 로그인 안 함 → JwtAuthenticationEntryPoint → 401
 *     └ 권한 없음    → JwtAccessDeniedHandler      → 403
 * </pre>
 *
 * <h3>서버가 뜰 때 "Using generated security password" 가 찍히는 것에 대해</h3>
 *
 * 스프링 부트가 아이디/비밀번호 로그인용 기본 계정을 하나 만들면서 찍는 줄이다.
 * 우리는 폼 로그인을 아예 끄기 때문에 그 계정은 어디에도 쓰이지 않는다. 무시해도 된다.
 */
@Slf4j
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** 허용할 프론트 주소. application.yml 의 app.cors-origins */
    @Value("${app.cors-origins}")
    private String corsOrigins;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserDAO userDAO;

    @Autowired
    private JwtAuthenticationEntryPoint authenticationEntryPoint;

    @Autowired
    private JwtAccessDeniedHandler accessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
                // ---------- CORS ----------
                // 프론트(localhost:5173)와 서버(localhost:8090)는 포트가 달라서
                // 브라우저가 "다른 출처" 로 본다. 허용 목록을 안 주면 모든 요청이 막힌다.
                // 설정 내용은 아래 corsConfigurationSource() 참고.
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))

                // ---------- CSRF ----------
                //
                // CSRF 는 "로그인한 사용자가 모르는 사이에 요청이 나가는" 공격이다.
                // 예를 들어 나쁜 사이트에 숨겨둔 폼이 우리 서버로 POST 를 쏘면,
                // 브라우저가 쿠키를 자동으로 붙여 보내기 때문에 서버는 본인 요청으로 착각한다.
                //
                // 여기서 끄는 이유 (그냥 관행이라서가 아니다)
                //
                //   1. API 인증은 Access Token 으로 한다. 이건 자바스크립트가
                //      Authorization 헤더에 직접 붙이는 값이라 브라우저가 자동으로
                //      붙여주지 않는다. 남의 사이트가 보낸 요청에는 애초에 안 실린다
                //      = CSRF 가 성립하지 않는다.
                //
                //   2. 자동으로 실려 나가는 건 Refresh Token 쿠키뿐인데, 그 쿠키는
                //      SameSite=Lax 라 남의 사이트가 보낸 POST 에는 안 붙는다
                //      (application.yml 의 app.refresh-cookie.same-site 참고).
                //      게다가 Path=/api/auth 로 묶어둬서 다른 주소에는 가지도 않는다.
                //
                //   3. 설령 /api/auth/refresh 가 불렸다 해도, 응답(새 Access Token)은
                //      위 CORS 설정 때문에 공격자 쪽 자바스크립트가 읽을 수 없다.
                //
                // 즉 쿠키를 쓰지만 쿠키로 "작업" 을 시키지는 않기 때문에 끈다.
                // 만약 나중에 Refresh 쿠키만으로 글쓰기 같은 걸 할 수 있게 바꾼다면,
                // 그때는 이 줄을 지우고 CSRF 토큰을 붙여야 한다.
                .csrf(AbstractHttpConfigurer::disable)

                // ---------- 세션 ----------
                //
                // STATELESS = 서버가 로그인 상태를 전혀 기억하지 않는다.
                //
                // 세션 방식은 서버 메모리에 "이 사람은 로그인했다" 를 적어두고
                // 쿠키의 세션 ID 로 찾아 쓴다. 그래서 서버를 재시작하면 전부 로그아웃되고,
                // 서버를 두 대로 늘리면 둘이 기억을 나눠 가져 문제가 생긴다.
                //
                // JWT 는 "로그인했다" 는 증거를 토큰 자체가 들고 다닌다. 서버는 서명만
                // 확인하면 되므로 기억할 게 없다. 그래서 세션을 아예 만들지 않는다.
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // ---------- 안 쓰는 로그인 방식 끄기 ----------
                // 스프링 부트는 기본으로 로그인 폼 화면과 브라우저 팝업 로그인을 켜준다.
                // 우리는 토큰으로만 인증하므로 둘 다 끈다. 안 끄면 인증 실패 때
                // 401 JSON 대신 로그인 화면으로 넘겨버린다(302).
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                // ---------- 인증 실패 · 권한 부족일 때 ----------
                // 기본값은 본문이 빈 응답이다. 프론트가 {"detail": "..."} 만 읽으므로
                // 그 모양으로 맞춰주는 우리 클래스를 끼운다.
                .exceptionHandling(handler -> handler
                        .authenticationEntryPoint(authenticationEntryPoint) // 401
                        .accessDeniedHandler(accessDeniedHandler)) // 403

                // ---------- 주소별 권한 ----------
                .authorizeHttpRequests(auth -> auth

                        // --- 브라우저가 본 요청 전에 보내는 확인 요청 ---
                        // CORS 를 쓸 때 브라우저가 "이 요청 보내도 되냐" 고 먼저 묻는다(preflight).
                        // 이건 토큰이 안 실려 오므로 막으면 안 된다.
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // --- 서버 상태 확인 ---
                        .requestMatchers("/api/health").permitAll()

                        // --- Swagger 문서 ---
                        // 개발 중에 API 를 눈으로 보는 화면이다. 운영에 올릴 때는
                        // 이 줄을 지우거나 관리자만 보게 막는 것을 고려해야 한다.
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**")
                        .permitAll()

                        // --- 로그인 · 회원가입 · 비밀번호 찾기 ---
                        // 로그인하려고 부르는 주소들이라 당연히 로그인이 필요 없다.
                        .requestMatchers(
                                "/api/auth/register",
                                "/api/auth/login",
                                "/api/auth/refresh",
                                "/api/auth/logout",
                                "/api/auth/password-reset/**")
                        .permitAll()

                        // /api/auth/logout 이 permitAll 인 이유
                        //   로그아웃은 Access Token 이 이미 만료된 뒤에도 눌릴 수 있다.
                        //   거기서 401 을 내면 "로그아웃조차 안 되는" 상태에 갇힌다.
                        //   누구인지는 Refresh 쿠키로 알아내므로 토큰이 없어도 된다.

                        // --- 재료 · 지역 목록 (고정된 참고 자료) ---
                        .requestMatchers(HttpMethod.GET, "/api/catalog", "/api/catalog/regions")
                        .permitAll()

                        // --- 추천 · 레시피 ---
                        // 로그인 안 해도 쓸 수 있다. 다만 로그인했으면 못 먹는 재료를
                        // 빼주므로, 토큰이 있으면 필터가 알아서 그 사람으로 인식한다.
                        .requestMatchers(HttpMethod.POST, "/api/recommendations").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/recipes/*").permitAll()

                        // --- 후기 게시판: 읽기는 누구나 ---
                        // 상세는 로그인했으면 "내가 좋아요를 눌렀는지" 까지 알려준다.
                        // 그래서 막지 않고, 토큰이 있으면 그 사람으로 보여준다.
                        .requestMatchers(HttpMethod.GET,
                                "/api/reviews", "/api/reviews/popular",
                                "/api/reviews/*", "/api/reviews/*/file")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/reviews/*/view").permitAll()

                        // --- 자유게시판: 읽기는 누구나 ---
                        .requestMatchers(HttpMethod.GET,
                                "/api/free-posts", "/api/free-posts/*", "/api/free-posts/*/file")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/free-posts/*/view").permitAll()

                        // --- 공지: 읽기는 누구나, 쓰기는 관리자만 ---
                        //
                        // 예전에는 Controller 에서 authService.getAdminUser(token) 을 불렀다.
                        // 이제는 여기 한 줄로 끝난다 - 필터가 관리자에게 ROLE_ADMIN 을
                        // 달아주고(JwtAuthenticationFilter 5번), 그걸 여기서 본다.
                        //
                        // hasRole("ADMIN") 은 내부에서 "ROLE_ADMIN" 을 찾는다.
                        // 접두사를 여기 적으면 안 된다 - "ROLE_ROLE_ADMIN" 을 찾게 된다.
                        .requestMatchers(HttpMethod.GET, "/api/notices", "/api/notices/*")
                        .permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/notices/*/view").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/notices").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/notices/*").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/notices/*").hasRole("ADMIN")

                        // --- 나머지는 전부 로그인해야 한다 ---
                        //
                        // 찜 · 식사 기록 · 못 먹는 재료 · 게임 · 내 정보 · 글쓰기가 여기 걸린다.
                        // 새 API 를 만들면 자동으로 이쪽이 되므로, 깜빡해서 열리는 일이 없다.
                        .anyRequest().authenticated())

                // ---------- 우리 필터 끼우기 ----------
                //
                // UsernamePasswordAuthenticationFilter 는 아이디/비밀번호 폼 로그인을
                // 처리하는 기본 필터다. 우리는 그걸 안 쓰지만, "인증을 담당하는 자리"가
                // 거기라서 그 앞에 끼운다. 그래야 인가 검사가 돌기 전에
                // SecurityContext 가 채워져 있다.
                //
                // new 로 만드는 이유는 JwtAuthenticationFilter 주석 참고
                // (빈으로 두면 톰캣에도 자동 등록돼서 체인 밖에서 한 번 더 돈다).
                .addFilterBefore(
                        new JwtAuthenticationFilter(jwtUtil, userDAO),
                        UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    /**
     * CORS 설정. 어느 주소에서 온 요청을 받아줄지 정한다.
     *
     * <h3>CORS 가 뭔가</h3>
     *
     * 브라우저는 "지금 보고 있는 페이지의 주소" 와 "요청을 보내는 주소" 가 다르면
     * 기본적으로 막는다. 나쁜 사이트가 내 은행 사이트에 몰래 요청을 보내지 못하게 하는
     * 안전장치다. 우리는 프론트(5173)와 서버(8090)가 포트가 달라서 여기에 걸린다.
     * 그래서 서버가 "이 주소는 괜찮다" 고 알려줘야 한다.
     *
     * <h3>왜 WebConfig 에서 여기로 옮겼나</h3>
     *
     * 원래는 WebConfig 의 {@code addCorsMappings} 에 있었다. 그런데 Spring Security
     * 필터는 Controller 보다 앞에서 도는데, 그 설정은 Controller 단계에서 적용된다.
     * 그러면 브라우저의 preflight(OPTIONS) 요청이 보안 필터에서 먼저 막혀서
     * 본 요청이 아예 출발하지 못한다. 그래서 보안 쪽으로 가져왔다.
     * 두 군데에 적어두면 응답 헤더가 겹칠 수 있어서 한 곳만 남긴다.
     *
     * <h3>allowCredentials 와 "*" 를 같이 쓸 수 없다</h3>
     *
     * {@code allowCredentials(true)} 는 "쿠키를 같이 보내도 된다" 는 뜻이다.
     * Refresh Token 이 쿠키라서 이게 반드시 켜져 있어야 한다.
     *
     * 그런데 이걸 켜면 {@code allowedOrigins("*")} 를 쓸 수 없다. 브라우저가 거부한다.
     * "아무 사이트나 + 쿠키까지" 는 그냥 모든 사이트에 로그인 정보를 넘기는 것과 같아서다.
     * 그래서 주소를 하나씩 적어둔다 (application.yml 의 app.cors-origins).
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        List<String> origins = Arrays.asList(corsOrigins.split(","));

        log.info("CORS 허용 주소 = {}", origins);

        CorsConfiguration config = new CorsConfiguration();

        // 허용할 프론트 주소. "*" 는 쓸 수 없다 (위 설명 참고).
        config.setAllowedOrigins(origins);

        config.setAllowedMethods(
                Arrays.asList("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));

        // 요청에 붙여 보낼 수 있는 헤더. Authorization 과 Content-Type 이 여기 걸린다.
        config.setAllowedHeaders(Arrays.asList("*"));

        // 자바스크립트가 응답에서 읽을 수 있는 헤더.
        // 기본으로는 몇 개만 읽히고 나머지는 숨겨진다. 파일 이름(Content-Disposition)과
        // 재시도 안내(Retry-After)는 화면에서 써야 해서 열어둔다.
        config.setExposedHeaders(Arrays.asList("Content-Disposition", "Retry-After"));

        // 쿠키를 주고받게 한다. Refresh Token 이 쿠키라서 반드시 true 여야 한다.
        // 프론트의 axios 에도 withCredentials: true 가 같이 켜져 있어야 짝이 맞는다.
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
