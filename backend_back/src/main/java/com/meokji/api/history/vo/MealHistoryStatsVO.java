package com.meokji.api.history.vo;

import lombok.Data;

/**
 * 식사 기록 요약. 모양: { "count": 12, "total_cost": 95000, "streak_days": 3 }
 *
 * 표 한 행이 아니라 여러 행을 세어서 만든 값이라 MEAL_HISTORIES 와 칸이 다르다.
 *
 * <p>응답 전용이다. 화면이 보낼 값이 아니라 서버가 계산해 주는 값이라
 * @NotBlank 같은 검사도, @JsonIgnore 로 감출 칸도 없다.
 *
 * <p>Map 으로 돌려주지 않고 VO 를 만든 이유 — 칸이 셋으로 고정이고 타입도
 * 정해져 있다. Map 이면 칸 이름을 서비스에서 손으로 적게 되고,
 * "total_cost" 를 "totalCost" 로 적어도 컴파일이 된다.
 */
@Data
public class MealHistoryStatsVO {

    private int count; // 총 기록 수

    // 누적 재료비. int 가 아니라 long 인 이유 — 한 끼가 2만원이라 치면
    // int 의 한계(약 21억)까지 10만 끼가 넘어야 하지만, 돈을 더하는
    // 칸에서 넘칠 걱정을 하지 않으려고 long 으로 둔다
    private long totalCost;

    // 연속 기록일. 오늘부터 거꾸로 날짜가 하루씩 이어지는 동안의 일수.
    // 하루라도 비면 거기서 끊긴다. 계산은 HistoryServiceImpl 이 한다
    private int streakDays;
}
