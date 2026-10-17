package com.meokji.api.health.service;

import java.util.Map;

/** 서버 · DB 상태. 실제 코드는 HealthServiceImpl 에 있다. */
public interface HealthService {

    /**
     * 상태 정보. { status, app, version, env, database, database_error }
     *
     * DB 가 되는지는 database 칸이 "ok" 인지로 알 수 있다.
     * 컨트롤러는 그걸 보고 200 으로 줄지 503 으로 줄지 정한다.
     *
     * <p>database_error 는 에러일 때만 들어 있다. 정상일 때 null 로 담지
     * 않는 이유는 구현 쪽 주석에 적어 두었다.
     *
     * <p>DB 가 안 돼도 예외를 던지지 않는다. 그게 이 메서드가 알아내려는
     * 것이기 때문이다 — 던지면 컨트롤러가 결과를 받을 수 없다.
     *
     * @return 칸 이름 -> 값. 넣은 순서가 유지된다(LinkedHashMap)
     */
    Map<String, Object> getStatus();
}
