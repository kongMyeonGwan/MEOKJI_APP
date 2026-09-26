package com.meokji.api.game;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.common.ApiException;
import com.meokji.api.common.Times;
import com.meokji.api.game.dto.GamePlayResponse;
import com.meokji.api.security.LoginUser;
import com.meokji.api.user.User;

/**
 * 미니게임 플레이 기록 (STEP 27).
 *
 * <p>게임 화면이 열릴 때 한 번 부른다. 점수나 결과는 받지 않는다 - 무엇을 골랐는지는
 * 화면이 들고 있고, 서버는 "몇 번 했는지" 만 센다.
 *
 * <p>서비스 클래스를 따로 두지 않았다. 하는 일이 키를 글자로 바꿔 한 줄 넣는 것뿐인데
 * 그 사이에 클래스를 하나 더 두면 읽을 곳만 늘어난다.
 */
@RestController
@RequestMapping("/api/games")
public class GameController {

    /** 화면의 게임 키 -> DB 에 넣을 글자. DDL 의 ck_game_histories_type 과 같아야 한다. */
    private static final Map<String, String> GAME_TYPES = Map.of(
            "worldcup", "A",   // 음식 월드컵 — 후보를 둘씩 비교해 좁힌다
            "balance", "B",    // 취향 밸런스 — 질문 다섯 개로 조건을 만든다
            "roulette", "C");  // 랜덤 돌림판 — 운에 맡긴다

    private final GameMapper games;

    public GameController(GameMapper games) {
        this.games = games;
    }

    @PostMapping("/{gameKey}/plays")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public GamePlayResponse recordPlay(
            @LoginUser User me,
            @PathVariable String gameKey) {

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
