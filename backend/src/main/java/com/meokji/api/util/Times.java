package com.meokji.api.util;

import java.time.LocalDateTime;
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
 *
 * <p>Kst 와 나눈 기준 — 이쪽은 "응답에 쓸 글자" 로 바꾸고(UTC 그대로),
 * Kst 는 "한국 날짜로 세는" 일을 한다. 둘 다 시각을 다루지만 쓰는 데가 다르다.
 *
 * <p>왜 한국 시각으로 바꿔 보내지 않나 — 보내는 값에 Z(=UTC)가 붙어 있으면
 * 브라우저가 보는 사람의 시간대로 알아서 바꿔 그린다. 서버가 미리 한국
 * 시각으로 바꿔 보내면, 그 값에 시간대 표시가 없어서 브라우저가 또
 * 자기 시간대로 읽어 9시간이 더 밀린다.
 *
 * <p>쓰는 곳 — 날짜를 응답에 담는 VO 들이 전부 이것을 거친다
 * (UserBanVO · MealHistoryVO · NoticeVO · 댓글 VO 들 ...).
 */
public final class Times {

    // 모양을 둘 둔다. 소수점 이하가 있을 때와 없을 때다.
    //
    // 패턴 안의 'T' 와 'Z' 에 따옴표가 붙은 이유 — 따옴표가 없으면
    // 그 글자를 날짜 기호로 읽는다. 'Z' 는 "시간대" 라는 뜻이 되어
    // LocalDateTime(시간대가 없는 값)에는 쓸 수 없어 터진다.
    //
    // static final 인 이유 — DateTimeFormatter 를 만드는 일이 가볍지
    // 않다. 목록 20건을 내보낼 때마다 20번 만들 이유가 없고,
    // 한 번 만들면 상태가 바뀌지 않아 같이 써도 안전하다
    private static final DateTimeFormatter WITH_MICROS =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'");

    private static final DateTimeFormatter WITHOUT_FRACTION =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss'Z'");

    // 바깥에서 만들지 못하게 막는다. 전부 static 메서드다
    private Times() {
    }

    /** DB 에서 읽은 시각을 응답용 문자열로. 값이 없으면 null 을 그대로 돌려준다. */
    public static String toText(LocalDateTime utc) {
        if (utc == null) {
            return null;
        }

        // 나노초를 마이크로초로 내린다. 오라클 TIMESTAMP(6) 이므로 뒤 세 자리는 0 이다.
        //
        // 이 값을 보고 모양을 고른다. 0 이면 ".000000" 을 붙일 이유가 없다 -
        // DDL 의 DEFAULT 로 찍힌 시각은 대개 소수점이 있고,
        // 자바에서 만든 시각은 0 인 경우가 있다
        int micros = utc.getNano() / 1000;

        return micros == 0
                ? utc.format(WITHOUT_FRACTION)
                : utc.format(WITH_MICROS);
    }
}
