package com.meokji.api.health.service;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.meokji.api.health.dao.HealthDAO;

/**
 * 마이페이지 아래의 "개발용 연결 상태" 카드가 쓰는 기능.
 *
 * <p>왜 필요한가 — 화면이 안 뜰 때 원인이 셋이다. 프론트가 안 떴거나,
 * 서버가 안 떴거나, DB 가 안 됐거나. 이 주소가 응답하면 앞의 둘은 괜찮다는
 * 뜻이고, database 칸이 그 셋째를 알려준다. 하나씩 짚어보지 않아도 된다.
 *
 * <p>VO 를 안 만들고 Map 을 쓰는 이유 — 칸이 상황에 따라 다르다
 * (database_error 는 에러일 때만 붙는다). VO 로 만들면 정상일 때도
 * "database_error": null 이 같이 나간다.
 */
@Service
public class HealthServiceImpl implements HealthService {

    @Autowired
    private HealthDAO healthDAO;

    // 아래 셋은 application.yml 의 app.name · app.version · app.env 다.
    //
    // 내려보내는 이유 — 서버가 여러 개 떠 있을 때 "지금 내가 보고 있는 게
    // 어느 서버인지" 를 알려준다. env 가 local 인데 배포 화면을 보고 있으면
    // 프론트가 엉뚱한 서버를 보고 있다는 뜻이다.
    @Value("${app.name}")
    private String appName;

    @Value("${app.version}")
    private String appVersion;

    @Value("${app.env}")
    private String appEnv;

    /**
     * 서버 정보와 DB 연결 상태를 모아 돌려준다.
     *
     * {@code @Transactional} 이 없다. 읽기만 하고, 실패해도 되돌릴 것이 없다.
     * 오히려 붙이면 DB 가 죽었을 때 트랜잭션을 열려다 먼저 터져서,
     * 아래 try-catch 가 잡지 못하고 500 이 나간다.
     */
    @Override
    public Map<String, Object> getStatus() {
        // DB 에 아주 간단한 SQL(SELECT 1 FROM DUAL)을 보내본다
        //
        // 표를 읽지 않는 이유 — 어느 표를 읽으면 그 표가 없거나 권한이
        // 없을 때도 "DB 가 죽었다" 로 보이게 된다. 확인하려는 것은
        // "연결이 되는가" 뿐이다.
        String database = "ok";
        String databaseError = null;
        try {
            healthDAO.ping();
        } catch (Exception e) {
            // 예외를 올려보내지 않고 삼킨다. DB 가 죽은 것은 이 기능의
            // "에러" 가 아니라 "확인 결과" 다. 500 이 나가면 서버가 죽은
            // 것과 구별이 안 된다
            database = "error";
            databaseError = e.getClass().getSimpleName(); // 에러 종류만 (자세한 내용은 보안상 숨긴다)
            // getMessage() 를 쓰면 접속 주소 · 계정 이름 · SQL 이 그대로
            // 바깥으로 나간다. 클래스 이름만으로도 원인은 대개 짐작된다
            // (CannotGetJdbcConnection = 접속 자체가 안 됨 등)
        }

        // LinkedHashMap 이라 넣은 순서대로 응답에 나간다.
        // 보통 HashMap 이면 칸 순서가 뜰 때마다 바뀌어서 눈으로 비교하기 어렵다
        Map<String, Object> body = new LinkedHashMap<>();
        // 전체 상태를 맨 앞에 둔다. 감시 도구는 이 칸만 보면 된다.
        // "degraded" 는 "서버는 살아있는데 일부가 안 된다" 는 뜻이다
        body.put("status", database.equals("ok") ? "ok" : "degraded");
        body.put("app", appName);
        body.put("version", appVersion);
        body.put("env", appEnv);
        body.put("database", database);
        // 에러일 때만 칸을 붙인다. 정상일 때 null 로 나가면 화면이
        // "값이 없는 것" 과 "칸이 없는 것" 을 구별해야 한다
        if (databaseError != null) {
            body.put("database_error", databaseError);
        }

        return body;
    }
}
