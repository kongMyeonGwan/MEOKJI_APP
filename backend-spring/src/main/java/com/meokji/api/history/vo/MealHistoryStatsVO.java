package com.meokji.api.history.vo;

import lombok.Data;

/**
 * 식사 기록 요약. 모양: { "count": 12, "total_cost": 95000, "streak_days": 3 }
 *
 * 표 한 행이 아니라 여러 행을 세어서 만든 값이라 MEAL_HISTORIES 와 칸이 다르다.
 */
@Data
public class MealHistoryStatsVO {

    private int count; // 총 기록 수
    private long totalCost; // 누적 재료비
    private int streakDays; // 연속 기록일
}
