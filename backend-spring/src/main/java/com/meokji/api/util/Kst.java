package com.meokji.api.util;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;

/**
 * 한국 시각(KST)과 UTC 사이를 오간다.
 *
 * <p>파이썬 쪽 {@code app/core/kst.py} 자리다. 모아둔 이유도 같다 - 같은 변환이
 * 연속 방문일 계산과 인기 레시피 기간 검색 두 곳에서 필요해서, 각자 계산하면
 * 한쪽만 고치는 실수가 난다.
 *
 * <p>DB 에는 UTC 만 저장하는데 사용자가 보는 날짜는 한국 날짜다. 이 9시간을 안
 * 맞추면 경계에서 결과가 어긋난다 - 새벽 0시~9시에 만든 기록이 전날 것으로
 * 세어지거나 아예 빠진다.
 */
public final class Kst {

    /** 한국은 서머타임이 없어서 1년 내내 +9시간 고정이다. */
    public static final ZoneId ZONE = ZoneOffset.ofHours(9);

    private Kst() {
    }

    /** 오늘 날짜를 한국 기준으로. */
    public static LocalDate today() {
        return LocalDate.now(ZONE);
    }

    /**
     * UTC 로 저장된 시각을 한국 날짜로 바꾼다.
     *
     * <p>컬럼에 시간대가 없으므로 "이 값은 UTC 다" 라는 약속을 여기서 적용한다.
     */
    public static LocalDate toDate(LocalDateTime utc) {
        if (utc == null) {
            return null;
        }

        return utc.atOffset(ZoneOffset.UTC).atZoneSameInstant(ZONE).toLocalDate();
    }
}
