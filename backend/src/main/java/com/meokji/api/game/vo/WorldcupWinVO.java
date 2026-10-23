package com.meokji.api.game.vo;

import lombok.Data;

/**
 * 월드컵 우승 기록 요청. 모양: { "food_name": "김치찌개", "runner_up": "된장찌개", "round_size": 32 }
 */
@Data
public class WorldcupWinVO {

    private String foodName;
    private String runnerUp;
    private Integer roundSize;
}
