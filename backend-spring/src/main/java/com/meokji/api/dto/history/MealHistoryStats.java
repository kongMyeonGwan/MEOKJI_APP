package com.meokji.api.dto.history;

import lombok.Data;

// 식사 기록 요약. 모양: { "count": 12, "total_cost": 95000, "streak_days": 3 }
@Data
public class MealHistoryStats {

    private int count; // 총 기록 수
    private long totalCost; // 누적 재료비
    private int streakDays; // 연속 기록일
}
