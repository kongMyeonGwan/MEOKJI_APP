package com.meokji.api.dto.history;

/**
 * History 화면 위쪽 요약.
 *
 * <p>세 값 모두 <b>전체 기준</b>이다. 페이지 안의 건수가 아니다 - 목록이 페이징되기
 * 전에는 한 페이지만 세고 있어서 2페이지부터 숫자가 틀렸다(STEP 31에서 고쳤다).
 */
public record MealHistoryStats(
        int count,
        long totalCost,
        int streakDays) {
}
