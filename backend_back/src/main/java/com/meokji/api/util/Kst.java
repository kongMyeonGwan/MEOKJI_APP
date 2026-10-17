package com.meokji.api.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Set;

/**
 * 한국 시각(KST) 도우미.
 *
 * DB 에는 시각을 UTC(세계 표준시)로 저장한다. 한국은 UTC 보다 9시간 빠르다.
 * 날짜를 셀 때 이 차이를 안 맞추면 새벽 0시~9시에 만든 기록이 전날로 세어진다.
 *
 * <p>왜 DB 에 UTC 로 저장하나 — 서버가 어느 나라에 있든 같은 값이 되도록
 * 하려는 것이다. 한국 시각으로 저장하면 서버를 옮겼을 때 과거 기록이
 * 통째로 9시간씩 틀어진다.
 *
 * <p>대신 "보여줄 때 한국 시각으로 바꾸는" 일이 생긴다. 그 일을 여기 모았다 -
 * 자리마다 +9 를 더하면 한 곳에서 빠뜨려도 에러가 안 난다. 날짜만 하루
 * 어긋난 채로 돌아간다.
 *
 * <p>Times 와 나눈 기준 — Times 는 "응답에 쓸 글자" 로 바꾸고(UTC 그대로),
 * 이쪽은 "한국 날짜로 세는" 일을 한다. 둘 다 시각을 다루지만 쓰는 데가 다르다.
 */
public class Kst {

    public static final ZoneId ZONE = ZoneOffset.ofHours(9); // 한국 = UTC + 9시간
    // ZoneId.of("Asia/Seoul") 이 아니라 고정 오프셋을 쓴다. 한국은
    // 서머타임이 없어서 늘 +9 이고, 지역 이름으로 두면 그 표(tzdata)가
    // JVM 마다 다를 수 있다

    // 오늘 날짜 (한국 기준)
    //
    // LocalDate.now() 만 쓰면 서버가 선 지역의 날짜가 된다. 연속 기록일을
    // 셀 때 기준이 되는 날이라, 틀리면 전체가 하루씩 밀린다
    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    // UTC 시각 → 한국 날짜
    //
    // 세 단계를 거친다
    //   atOffset(UTC)       : 시간대가 없던 값에 "이건 UTC" 를 붙인다
    //   atZoneSameInstant   : 같은 순간을 한국 시각으로 다시 읽는다
    //   toLocalDate         : 시 · 분을 버리고 날짜만 남긴다
    //
    // atOffset 을 먼저 하는 것이 중요하다. LocalDateTime 에는 시간대가
    // 없어서, 안 붙이면 "이 값이 어디 시각인지" 를 자바가 모른다
    public static LocalDate toDate(LocalDateTime utc) {
        if (utc == null) {
            return null;
        }
        return utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(ZONE).toLocalDate();
    }

    // 오늘(또는 어제)부터 거꾸로 며칠 연속으로 기록이 있는지 센다.
    // 어제부터도 세는 이유: 오늘 아직 요리를 안 했다고 연속 기록이 0 이 되면 안 되니까.
    //   days : 기록이 있는 날짜들
    public static int countStreak(Set<LocalDate> days) {
        LocalDate day = today();

        if (!days.contains(day)) {
            day = day.minusDays(1); // 오늘 기록이 없으면 어제부터 센다
            // 어제도 없으면 아래 while 이 한 번도 안 돌아 0 이 된다.
            // 연속이 끊긴 것이 맞다
        }

        // 하루씩 거꾸로 가면서 기록이 있는 동안 센다.
        // 하루라도 비면 while 이 끝난다 - 그게 "연속" 의 뜻이다
        int streak = 0;
        while (days.contains(day)) {
            streak++;
            day = day.minusDays(1);
        }
        return streak;
        // Set 을 받는 이유 — contains 로 찾는 일만 한다. 목록이면
        // 날마다 전체를 훑어야 해서, 기록이 많을수록 느려진다
    }
}
