package com.meokji.api.game.dto;

import lombok.Data;

// 게임 기록 응답. 모양: { "game_key": "worldcup", "game_type": "A", "played_at": "..." }
@Data
public class GamePlayResponse {

    private String gameKey;
    private String gameType;
    private String playedAt;
}
