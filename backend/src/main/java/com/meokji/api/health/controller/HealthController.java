package com.meokji.api.health.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.health.service.HealthService;

/**
 * 서버와 DB 가 살아있는지 확인.  주소: GET /api/health
 * 마이페이지 아래의 "개발용 연결 상태" 카드가 부른다.
 *
 * <p>로그인이 필요 없다. 로그인은 DB 를 봐야 하는데, DB 가 죽었는지 보려고
 * 부르는 주소라서 로그인을 요구하면 "DB 가 죽어서 확인도 못 하는" 상태가 된다.
 *
 * <p>@RequestMapping 이 /api 인 것에 주의 — 다른 컨트롤러는 기능 이름까지
 * 적는데(/api/bans), 여기는 /api 만 적고 메서드에 /health 를 붙였다.
 * 주소가 /api/health 하나뿐이라 묶을 것이 없다.
 *
 * <p>이 컨트롤러만 ResponseEntity 를 쓴다. 다른 곳은 VO 를 그대로 돌려주고
 * 상태 코드는 @ResponseStatus 로 고정하는데, 여기는 결과를 보고 200 과 503 을
 * 가려야 해서 코드를 실행 중에 정해야 한다.
 */
@RestController
@RequestMapping("/api")
public class HealthController {

    @Autowired
    private HealthService healthService;

    /**
     * 상태 확인.
     *
     * 응답 모양 (DB 가 되는 경우) — 200
     *   { "status": "ok", "app": "뭐먹지? API", "version": "0.1.0",
     *     "env": "local", "database": "ok" }
     *
     * DB 가 안 되는 경우 — 503. database_error 칸이 하나 더 붙는다
     *   { "status": "degraded", ..., "database": "error",
     *     "database_error": "CannotGetJdbcConnectionException" }
     *
     * 에러일 때도 본문을 주는 이유 — 503 만 받으면 화면은 "뭐가 안 되는지"
     * 를 알 수 없다. 앱 이름 · 버전은 서버가 떠 있다는 뜻이므로, DB 만
     * 문제라는 것을 그 둘이 알려준다.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = healthService.getStatus();

        // DB 가 안 되면 503 으로 응답한다
        //
        // 200 으로 주고 본문만 "error" 로 두면, 상태만 긁어가는 감시 도구가
        // 정상으로 읽는다. 상태 코드로도 드러나야 한다.
        //
        // body 에서 다시 꺼내 보는 모양인 이유 — 서비스가 Map 하나만
        // 돌려주기 때문이다. "DB 가 되는지" 를 따로 돌려주게 하면
        // 서비스 메서드가 둘이 되거나 전용 VO 가 하나 더 필요해진다
        if (body.get("database").equals("ok")) {
            return ResponseEntity.ok(body);
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }
}
