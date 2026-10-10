package com.meokji.api.game.vo;

import lombok.Data;

/**
 * 게임 기록 응답. 모양: { "game_key": "worldcup", "game_type": "A", "played_at": "..." }
 *
 * GAME_HISTORIES 표에는 회원 번호와 게임 코드만 들어간다. 화면은 방금 무엇을
 * 기록했는지만 알면 되므로 칸이 다르다.
 *
 * <p>다른 VO 들과 달리 DB 한 행을 담는 용도가 전혀 아니다. 응답 전용이다.
 * 그래서 @NotBlank 같은 검사도 없고(받는 값이 아니다), @JsonIgnore 로
 * 감출 칸도 없다(전부 나가는 값이다).
 *
 * <p>칸 셋 중 둘은 사실 화면이 이미 아는 값이다 — gameKey 는 화면이 보낸 것이고
 * gameType 은 그걸 바꾼 것이다. 그래도 같이 보내는 이유는, 화면이 "서버가
 * 내 요청을 이렇게 알아들었다" 를 확인할 수 있게 하려는 것이다.
 */
@Data
public class GamePlayVO {

    /** 화면과 주소에서 쓰는 이름. "worldcup" · "balance" · "roulette" */
    private String gameKey;

    /** DB 에 저장한 한 글자 코드. A · B · C */
    private String gameType;

    /**
     * 기록한 시각(UTC). 글자로 나간다. 예) "2026-10-11T12:30:00Z"
     *
     * LocalDateTime 이 아니라 String 인 이유 — 이 값은 DB 에서 읽은 것이
     * 아니라 서비스가 자바 시계로 만든 것이고, 만든 즉시 Times.toText 로
     * 글자가 된다. 중간 모양을 둘 이유가 없다.
     */
    private String playedAt;
}
