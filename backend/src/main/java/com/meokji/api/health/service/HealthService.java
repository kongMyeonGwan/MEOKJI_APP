package com.meokji.api.health.service;

import java.util.Map;

/** 서버 · DB 상태. 실제 코드는 HealthServiceImpl 에 있다. */
public interface HealthService {

    /**
     * 상태 정보. { status, app, version, env, database, database_error }
     *
     * DB 가 되는지는 database 칸이 "ok" 인지로 알 수 있다.
     * 컨트롤러는 그걸 보고 200 으로 줄지 503 으로 줄지 정한다.
     */
    Map<String, Object> getStatus();
}
