package com.meokji.api.game.vo;

import lombok.Data;

/**
 * 월드컵 우승 기록 요청. 모양: { "foodName": "김치찌개", "runnerUp": "된장찌개", "roundSize": 32 }
 */
@Data
public class WorldcupWinVO {

    private String foodName;
    private String runnerUp;
    private Integer roundSize;
}
