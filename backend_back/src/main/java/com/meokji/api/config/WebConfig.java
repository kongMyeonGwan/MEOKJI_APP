package com.meokji.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 서버 전체 설정. 지금 하는 일은 두 가지다 — CORS 허용과 비밀번호 암호화 도구 등록.
 *
 * <p>왜 설정 클래스가 따로 있나
 *
 * <p>이 두 가지는 어느 기능에도 속하지 않는다. CORS 는 모든 주소에 걸리고,
 * 비밀번호 암호화는 회원가입 · 로그인 · 비밀번호 변경 · 탈퇴가 같이 쓴다.
 * 기능 폴더(auth/, user/ ...) 어디에 둬도 "왜 여기 있지?" 가 되므로
 * config 폴더에 모았다.
 *
 * <p>{@code @Configuration} 은 "서버가 뜰 때 이 클래스를 먼저 읽어라" 는 표시다.
 * {@code WebMvcConfigurer} 를 구현하면 스프링이 주소 처리 설정을 물어보러 오는데,
 * 그때 아래 addCorsMappings 가 불린다.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    /**
     * 접속을 허용할 프론트 주소. 쉼표로 여러 개를 적는다.
     *
     * application.yml 의 app.cors-origins 에서 읽어온다. 기본값은
     * http://localhost:5173,http://localhost:5174 (Vite 개발 서버)이고,
     * 배포할 때는 CORS_ORIGINS 환경변수로 실제 도메인을 넣는다.
     *
     * 코드에 주소를 박지 않는 이유는 개발 중에는 localhost, 배포하면 실제
     * 도메인이라 값이 달라지기 때문이다. 값이 달라지는 것은 설정 파일로 뺀다.
     */
    @Value("${app.cors-origins}")
    private String corsOrigins;

    /**
     * CORS 허용 설정.
     *
     * CORS 가 왜 필요한가 — 프론트(localhost:5173)와 서버(localhost:8090)는
     * 포트가 달라서 브라우저가 "다른 사이트" 로 본다. 그냥 두면 브라우저가
     * 요청을 보내기도 전에 막아버리고, 콘솔에만 CORS 에러가 뜬다. 서버는
     * 요청을 받은 적도 없으니 서버 로그에는 아무것도 남지 않는다.
     * 그래서 "이 주소에서 오는 요청은 받아도 된다" 를 여기서 알려준다.
     */
    @Override
    public void addCorsMappings(CorsRegistry registry) {

        // 서버가 뜰 때 허용 주소를 한 번 찍어둔다.
        // CORS 에러는 브라우저 콘솔에만 나오고 서버 로그에는 안 남기 때문에,
        // 막혔을 때 "설정값이 뭐였는지" 를 확인할 곳이 필요하다.
        System.out.println("========================================");
        System.out.println("CORS ORIGINS = [" + corsOrigins + "]");
        System.out.println("========================================");

        registry.addMapping("/**") // 모든 주소에 적용
                // 쉼표로 적힌 값을 잘라 목록으로 넘긴다
                .allowedOrigins(corsOrigins.split(","))
                // OPTIONS 를 빼면 안 된다. 브라우저는 PUT · DELETE 를 보내기 전에
                // OPTIONS 로 "보내도 되냐" 를 먼저 물어보기 때문이다(preflight).
                .allowedMethods("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
                // Authorization · Content-Type 등 요청 헤더는 전부 허용
                .allowedHeaders("*")
                // 응답 헤더는 기본적으로 자바스크립트가 못 읽는다. 이 둘만 열어준다.
                //   Content-Disposition : 첨부파일 내려받을 때 원래 파일명
                //   Retry-After         : 429(너무 자주 요청) 일 때 몇 초 뒤에 되는지
                .exposedHeaders("Content-Disposition", "Retry-After")
                // 쿠키 · 인증 헤더를 실어 보낼 수 있게 한다
                .allowCredentials(true);
    }

    /**
     * 비밀번호 암호화 도구.
     *
     * {@code @Bean} 으로 등록해 두면 {@code @Autowired} 로 어디서나 꺼내 쓸 수 있다.
     * 쓰는 곳은 회원가입 · 로그인 · 비밀번호 변경 · 탈퇴다({@code Passwords} 를 거친다).
     *
     * BCrypt 를 쓰는 이유 — 같은 비밀번호를 넣어도 매번 다른 글자가 나온다.
     * 안에 무작위 값(salt)을 섞어 넣기 때문이다. 그래서 DB 가 새어 나가도
     * 같은 비밀번호를 쓴 회원끼리 묶이지 않고, 미리 계산해 둔 표로 되돌리기도 어렵다.
     * 맞는지 확인할 때는 되돌리는 게 아니라 matches 로 견주어 본다.
     */
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
