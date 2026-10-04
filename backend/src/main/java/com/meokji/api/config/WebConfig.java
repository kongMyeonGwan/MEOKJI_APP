package com.meokji.api.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.cors-origins}")
    private String corsOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {

        System.out.println("========================================");
        System.out.println("CORS ORIGINS = [" + corsOrigins + "]");
        System.out.println("========================================");

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