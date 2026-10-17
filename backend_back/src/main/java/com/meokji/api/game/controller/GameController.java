package com.meokji.api.game.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.game.vo.GamePlayVO;
import com.meokji.api.auth.service.AuthService;
import com.meokji.api.game.service.GameService;
import com.meokji.api.user.vo.UserVO;

/**
 * 메뉴 게임 기록.  주소: /api/games/{게임이름}/plays
 * "누가 어떤 게임을 했는지" 남긴다. 로그인이 필요하다.
 *
 * <p>게임 자체는 서버가 돌리지 않는다. 월드컵 대진 · 룰렛 회전 · 밸런스 문항은
 * 전부 브라우저에서 처리되고, 문항과 룰렛 조각은 /api/catalog 로 미리 받아간다.
 * 서버가 하는 일은 "했다" 는 사실만 남기는 것이다.
 *
 * <p>그래서 메서드가 하나뿐이다. 게임 목록은 catalog 기능이, 고른 메뉴는
 * history 기능이 따로 들고 있다.
 *
 * <p>결과(어떤 메뉴를 골랐는지)를 안 받는 이유 — 그건 식사 기록이라
 * MEAL_HISTORIES 로 가야 한다. 게임 쪽에 또 저장하면 같은 사실이 두 표에
 * 남고, 한쪽만 지우는 일이 생긴다.
 */
@RestController
@RequestMapping("/api/games")
public class GameController {

    @Autowired
    private AuthService authService;

    @Autowired
    private GameService gameService;

    // 게임 시작 기록  POST /api/games/worldcup/plays
    //
    // 게임 이름을 주소에 넣는 이유 — /api/games/plays 로 두고 본문에 이름을
    // 보내게 하면 주소만 봐서는 무엇을 하는지 알 수 없다. 주소에 있으면
    // 서버 접근 로그에도 어느 게임인지 남는다.
    //
    // 요청 본문이 없다. "시작했다" 는 사실 말고 보낼 값이 없기 때문이다.
    // 누가 했는지는 토큰에서, 무엇을 했는지는 주소에서, 언제는 서버 시계에서 온다.
    @PostMapping("/{gameKey}/plays")
    @ResponseStatus(HttpStatus.CREATED) // 201
    public GamePlayVO recordPlay(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable String gameKey) {
        UserVO loginUser = authService.getLoginUser(token);
        // 없는 게임 이름이면 서비스가 404 를 던진다.
        // @Pattern 으로 주소를 막지 않은 이유는 서비스 쪽 주석에 적어 두었다
        return gameService.recordPlay(loginUser, gameKey);
    }
}
