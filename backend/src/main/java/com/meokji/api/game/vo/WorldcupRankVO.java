package com.meokji.api.game.vo;

import lombok.Data;

/**
 * 명예의 전당 한 줄. 모양: { "rank": 1, "foodName": "김치찌개", "wins": 5, "finals": 7 }
 *
 * DB 에서 읽을 때는 food_name · wins · finals 만 채워지고,
 * rank 는 WorldcupServiceImpl 이 우승 횟수를 보고 채운다.
 */
@Data
public class WorldcupRankVO {

    private int rank;
    private String foodName;
    private long wins;   // 우승 횟수
    private long finals; // 결승 진출 횟수 (우승 + 준우승)
}
