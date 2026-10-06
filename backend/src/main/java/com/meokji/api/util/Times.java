package com.meokji.api.util;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/**
 * DB 의 시각을 응답에 쓸 문자열로 바꾼다.
 *
 * 왜 문자열로 내보내나
 *
 *   "created_at": "2026-09-11T21:29:42.955000Z"
 *
 * Jackson 에 Instant 를 그냥 넘기면 .955Z 로 뒤의 0 을 떼어낸다.
 * new Date() 로 읽는 프론트에는 둘 다 같지만, 두 서버의 응답을 기계적으로
 * 비교하는 동안에는 글자가 달라지면 매번 사람이 눈으로 확인해야 한다.
 *
 * 왜 LocalDateTime 을 받나
 *
 * DB 컬럼이 TIMESTAMP(6) 이고 시간대를 담지 않는다. 대신 넣는 값이
 * 항상 UTC 라는 약속으로 맞춰뒀다(DDL 의 SYS_EXTRACT_UTC, 커넥션의
 * ALTER SESSION SET TIME_ZONE='+00:00'). 그래서 읽어온 값에 시간대가 없고,
 * 그것을 UTC 로 읽어 Z 를 붙이는 것이 여기서 하는 일이다.
 */
public final class Times {

    private static final DateTimeFormatter WITH_MICROS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'");

    private static final DateTimeFormatter WITHOUT_FRACTION =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private Times() {
    }

    /**
     * 지금 시각 (UTC).
     *
     * {@code LocalDateTime.now()} 를 쓰면 안 된다 - 그건 PC 의 시간대를 따라가서
     * 한국에서는 9시간 앞선 값이 나온다. DB 의 모든 시각 컬럼이 UTC 로 저장돼 있으므로
     * (DDL 의 SYS_EXTRACT_UTC), 자바에서 만든 값과 비교하거나 저장하려면 여기를 거쳐야 한다.
     *
     * Refresh Token 만료 시각을 만들고 비교하는 데 쓴다.
     */
    public static LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    /** DB 에서 읽은 시각을 응답용 문자열로. 값이 없으면 null 을 그대로 돌려준다. */
    public static String toText(LocalDateTime utc) {
        if (utc == null) {
            return null;
        }

        // 나노초를 마이크로초로 내린다. 오라클 TIMESTAMP(6) 이므로 뒤 세 자리는 0 이다.
        int micros = utc.getNano() / 1000;

        return micros == 0
                ? utc.format(WITHOUT_FRACTION)
                : utc.format(WITH_MICROS);
    }
}
