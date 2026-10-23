package com.meokji.api.game.vo;

import lombok.Data;

/**
 * 게임 기록 응답. 모양: { "gameKey": "worldcup", "gameType": "A", "played_at": "..." }
 *
 * GAME_PLAYS 표에는 회원 번호와 게임 코드만 들어간다. 화면은 방금 무엇을
 * 기록했는지만 알면 되므로 칸이 다르다.
 */
@Data
public class GamePlayVO {

    private String gameKey;
    private String gameType;
    private String playedAt;
}
