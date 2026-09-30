package com.meokji.api.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.dao.HealthDAO;

/**
 * 서버와 DB 가 살아있는지 확인.  주소: GET /api/health
 * 마이페이지 아래의 "개발용 연결 상태" 카드가 부른다.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @Autowired
    private HealthDAO healthDAO;

    @Value("${app.name}")
    private String appName;

    @Value("${app.version}")
    private String appVersion;

    @Value("${app.env}")
    private String appEnv;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        // DB 에 아주 간단한 SQL(SELECT 1 FROM DUAL)을 보내본다
        String database = "ok";
        String databaseError = null;
        try {
            healthDAO.ping();
        } catch (Exception e) {
            database = "error";
            databaseError = e.getClass().getSimpleName(); // 에러 종류만 (자세한 내용은 보안상 숨긴다)
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", database.equals("ok") ? "ok" : "degraded");
        body.put("app", appName);
        body.put("version", appVersion);
        body.put("env", appEnv);
        body.put("database", database);
        if (databaseError != null) {
            body.put("database_error", databaseError);
        }

        // DB 가 안 되면 503 으로 응답한다
        if (database.equals("ok")) {
            return ResponseEntity.ok(body);
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}
