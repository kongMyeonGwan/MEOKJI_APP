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
 */
public class Kst {

    public static final ZoneId ZONE = ZoneOffset.ofHours(9); // 한국 = UTC + 9시간

    // 오늘 날짜 (한국 기준)
    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    // UTC 시각 → 한국 날짜
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
        }

        int streak = 0;
        while (days.contains(day)) {
            streak++;
            day = day.minusDays(1);
        }
        return streak;
    }
}
