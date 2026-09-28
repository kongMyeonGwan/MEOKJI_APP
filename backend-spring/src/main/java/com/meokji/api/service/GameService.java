package com.meokji.api.service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meokji.api.common.ApiException;
import com.meokji.api.dto.game.GamePlayResponse;
import com.meokji.api.mapper.GameMapper;
import com.meokji.api.util.Times;
import com.meokji.api.vo.User;

/**
 * 미니게임 플레이 기록 (STEP 27).
 *
 * <p>하는 일은 키를 글자로 바꿔 한 줄 넣는 것뿐이다. 그래도 서비스를 두는 이유는
 * <b>컨트롤러 → 서비스 → 매퍼</b> 를 예외 없이 지키기 위해서다 - 어떤 컨트롤러는
 * 매퍼를 직접 부르고 어떤 것은 서비스를 거치면, 새로 들어온 사람이 규칙을 코드마다
 * 다르게 배운다.
 *
 * <p>트랜잭션도 여기서 연다. 컨트롤러는 HTTP 를 다루는 자리이고, "어디까지가 한
 * 덩어리인가" 는 업무 규칙이라 서비스가 정하는 것이 맞다.
 */
@Service
public class GameService {

    /** 화면의 게임 키 -> DB 에 넣을 글자. DDL 의 ck_game_histories_type 과 같아야 한다. */
    private static final Map<String, String> GAME_TYPES = Map.of(
            "worldcup", "A",   // 음식 월드컵 — 후보를 둘씩 비교해 좁힌다
            "balance", "B",    // 취향 밸런스 — 질문 다섯 개로 조건을 만든다
            "roulette", "C");  // 랜덤 돌림판 — 운에 맡긴다

    @Autowired
    private GameMapper games;

    /** 플레이 한 번을 기록한다. 모르는 게임 키면 404. */
    @Transactional
    public GamePlayResponse recordPlay(User me, String gameKey) {
        String gameType = GAME_TYPES.get(gameKey);

        if (gameType == null) {
            // 오타를 조용히 넘기지 않는다. 넘기면 화면이 게임을 바꿀 때
            // 기록이 한쪽으로만 쌓이는 것을 아무도 모른다.
            throw ApiException.notFound("그런 게임이 없습니다: " + gameKey);
        }

        games.insertPlay(me.getId(), gameType);

        // 방금 넣은 행을 다시 읽지 않는다. 이 표에는 PK 가 없어서 "방금 그 행" 을
        // 집을 수 없고, 화면도 이 시각을 쓰지 않는다.
        LocalDateTime now = LocalDateTime.ofInstant(Instant.now(), ZoneOffset.UTC);

        return new GamePlayResponse(gameKey, gameType, Times.toText(now));
    }
}
