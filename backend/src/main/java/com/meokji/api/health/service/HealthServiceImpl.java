package com.meokji.api.health.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.meokji.api.health.dao.HealthDAO;

/** 마이페이지 아래의 "개발용 연결 상태" 카드가 쓰는 기능. */
@Service
public class HealthServiceImpl implements HealthService {

    @Autowired
    private HealthDAO healthDAO;

    @Value("${app.name}")
    private String appName;

    @Value("${app.version}")
    private String appVersion;

    @Value("${app.env}")
    private String appEnv;

    @Override
    public Map<String, Object> getStatus() {
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

        return body;
    }
}
