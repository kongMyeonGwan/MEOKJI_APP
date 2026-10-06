package com.meokji.api.game.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.game.vo.GamePlayVO;
import com.meokji.api.game.service.GameService;
import com.meokji.api.user.vo.UserVO;

/**
 * 메뉴 게임 기록.  주소: /api/games/{게임이름}/plays
 * "누가 어떤 게임을 했는지" 남긴다. 로그인이 필요하다.
 */
@RestController
@RequestMapping("/api/games")
public class GameController {

    @Autowired
    private GameService gameService;

    // 게임 시작 기록  POST /api/games/worldcup/plays
    @PostMapping("/{gameKey}/plays")
    @ResponseStatus(HttpStatus.CREATED) // 201
    public GamePlayVO recordPlay(
            @AuthenticationPrincipal UserVO loginUser,
            @PathVariable String gameKey) {
        return gameService.recordPlay(loginUser, gameKey);
    }
}
