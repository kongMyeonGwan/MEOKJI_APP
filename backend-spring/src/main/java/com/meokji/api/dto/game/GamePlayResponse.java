package com.meokji.api.dto.game;

/**
 * 게임 시작을 기록한 결과.
 *
 * <p>{@code gameKey} 는 화면이 쓰는 이름(worldcup)이고 {@code gameType} 은 DB 에
 * 들어간 글자(A)다. 둘을 같이 주는 이유는 화면이 보낸 값이 그대로 돌아왔는지
 * 확인할 수 있게 하려는 것이다.
 */
public record GamePlayResponse(
        String gameKey,
        String gameType,
        String playedAt) {
}
