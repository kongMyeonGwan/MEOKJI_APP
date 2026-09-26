package com.meokji.api.common;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * DB 의 시각을 응답에 쓸 문자열로 바꾼다.
 *
 * <h2>왜 문자열로 내보내나</h2>
 *
 * <p>파이썬(Pydantic)이 내던 모양을 글자까지 맞춰야 한다. 정답지를 보면 이렇다.
 *
 * <pre>
 *   "created_at": "2026-09-11T21:29:42.955000Z"
 * </pre>
 *
 * <p>Jackson 에 {@code Instant} 를 그냥 넘기면 {@code .955Z} 로 뒤의 0 을 떼어낸다.
 * {@code new Date()} 로 읽는 프론트에는 둘 다 같지만, 두 서버의 응답을 기계적으로
 * 비교하는 동안에는 글자가 달라지면 매번 사람이 눈으로 확인해야 한다.
 *
 * <p>규칙도 파이썬과 같게 맞춘다 - 마이크로초가 0 이면 소수점 자리를 아예 쓰지
 * 않는다({@code 2026-09-11T21:29:42Z}). Pydantic 의 isoformat 이 그렇게 동작한다.
 *
 * <h2>왜 LocalDateTime 을 받나</h2>
 *
 * <p>DB 컬럼이 {@code TIMESTAMP(6)} 이고 시간대를 담지 않는다. 대신 <b>넣는 값이
 * 항상 UTC</b> 라는 약속으로 맞춰뒀다(DDL 의 {@code SYS_EXTRACT_UTC}, 커넥션의
 * {@code ALTER SESSION SET TIME_ZONE='+00:00'}). 그래서 읽어온 값에 시간대가 없고,
 * 그것을 UTC 로 읽어 {@code Z} 를 붙이는 것이 여기서 하는 일이다.
 */
public final class Times {

    private static final DateTimeFormatter WITH_MICROS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'");

    private static final DateTimeFormatter WITHOUT_FRACTION =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private Times() {
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
