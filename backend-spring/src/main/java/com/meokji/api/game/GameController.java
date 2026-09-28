package com.meokji.api.game;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.game.dto.GamePlayResponse;
import com.meokji.api.security.LoginUser;
import com.meokji.api.user.User;

/**
 * 미니게임 플레이 기록 (STEP 27).
 *
 * <p>게임 화면이 열릴 때 한 번 부른다. 점수나 결과는 받지 않는다 - 무엇을 골랐는지는
 * 화면이 들고 있고, 서버는 "몇 번 했는지" 만 센다.
 *
 * <p>게임 키를 글자로 바꾸는 표와 트랜잭션은 {@link GameService} 에 있다.
 */
@RestController
@RequestMapping("/api/games")
public class GameController {

    @Autowired
    private GameService gameService;

    @PostMapping("/{gameKey}/plays")
    @ResponseStatus(HttpStatus.CREATED)
    public GamePlayResponse recordPlay(
            @LoginUser User me,
            @PathVariable String gameKey) {

        return gameService.recordPlay(me, gameKey);
    }
}
