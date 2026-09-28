package com.meokji.api.controller;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.config.AppProperties;

/**
 * 서버와 DB 가 살아있는지 확인한다.
 *
 * <p>파이썬 쪽 {@code app/routers/health.py} 와 <b>응답이 한 글자도 다르지 않아야</b>
 * 한다. 프론트의 ApiStatus 컴포넌트가 이걸 보고 화면 상단에 상태를 띄운다.
 * 정답지는 src/test/resources/fixtures/health.json 이다.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @Autowired
    private JdbcTemplate jdbc;
    @Autowired
    private AppProperties app;


    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        String databaseStatus = "ok";
        String databaseError = null;

        try {
            // 오라클은 FROM 절 없는 SELECT 를 허용하지 않는다. DUAL 이 필수다.
            jdbc.queryForObject("SELECT 1 FROM DUAL", Integer.class);
        } catch (Exception exc) {
            databaseStatus = "error";
            // 예외 타입만 남긴다. 메시지에는 커넥션 문자열이 섞여 나올 수 있다.
            databaseError = exc.getClass().getSimpleName();
        }

        // DB 가 안 되면 앱은 떠 있어도 정상 서비스는 못 하는 상태다.
        // 그래서 상태 문구와 HTTP 상태 코드를 같이 바꿔준다.
        boolean healthy = "ok".equals(databaseStatus);

        // LinkedHashMap 으로 칸 순서를 고정한다. 정답지와 글자 단위로 비교하려면
        // status · app · version · env · database 순서가 유지돼야 한다.
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", healthy ? "ok" : "degraded");
        body.put("app", app.getName());
        body.put("version", app.getVersion());
        body.put("env", app.getEnv());
        body.put("database", databaseStatus);
        if (databaseError != null) {
            body.put("database_error", databaseError);
        }

        HttpStatus status = healthy ? HttpStatus.OK : HttpStatus.SERVICE_UNAVAILABLE;

        return ResponseEntity.status(status).body(body);
    }
}
