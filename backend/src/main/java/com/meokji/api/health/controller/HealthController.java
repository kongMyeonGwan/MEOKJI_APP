package com.meokji.api.health.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.health.service.HealthService;

import java.util.Map;

/**
 * 서버와 DB 가 살아있는지 확인.  주소: GET /api/health
 * 마이페이지 아래의 "개발용 연결 상태" 카드가 부른다.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @Autowired
    private HealthService healthService;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = healthService.getStatus();

        // DB 가 안 되면 503 으로 응답한다
        if (body.get("database").equals("ok")) {
            return ResponseEntity.ok(body);
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}
