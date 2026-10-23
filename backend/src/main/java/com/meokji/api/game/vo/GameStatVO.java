package com.meokji.api.game.vo;

import lombok.Data;

/**
 * 게임 하나의 플레이 횟수. 모양: { "gameKey": "worldcup", "gameType": "A", "plays": 120 }
 *
 * DB 에서 읽을 때는 game_type 과 plays 만 채워지고, game_key 는
 * GameStatsServiceImpl 이 글자(A · B · C)를 보고 채운다.
 */
@Data
public class GameStatVO {

    private String gameKey;
    private String gameType;
    private long plays;
}
