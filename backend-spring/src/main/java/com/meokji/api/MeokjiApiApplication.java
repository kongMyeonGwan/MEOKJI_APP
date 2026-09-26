package com.meokji.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * 뭐먹지? API 진입점.
 *
 * <p>파이썬 쪽 {@code backend/app/main.py} 를 옮긴 것이다. 그쪽은 라우터를 하나씩
 * {@code include_router} 로 붙였지만, 스프링은 {@code @RestController} 를 스스로
 * 찾아 붙이므로 여기 적을 것이 없다.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class MeokjiApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeokjiApiApplication.class, args);
    }
}
