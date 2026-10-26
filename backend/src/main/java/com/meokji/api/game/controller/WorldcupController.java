package com.meokji.api.game.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.auth.service.AuthService;
import com.meokji.api.game.service.WorldcupService;
import com.meokji.api.game.vo.WorldcupRankVO;
import com.meokji.api.game.vo.WorldcupWinVO;
import com.meokji.api.user.vo.UserVO;

/**
 * 음식 이상형 월드컵 우승 기록 · 명예의 전당.  주소: /api/games/worldcup/...
 * 우승을 남길 때는 로그인이 필요하고, 순위는 로그인 없이도 볼 수 있다.
 */
@RestController
@RequestMapping("/api/games/worldcup")
public class WorldcupController {

    @Autowired
    private AuthService authService;

    @Autowired
    private WorldcupService worldcupService;

    // 우승 기록  POST /api/games/worldcup/wins
    // { "foodName": "김치찌개", "runnerUp": "된장찌개", "roundSize": 32 }
    @PostMapping("/wins")
    @ResponseStatus(HttpStatus.CREATED) // 201
    public Map<String, Object> recordWin(
            @RequestHeader(value = "Authorization", required = false) String token,
            @RequestBody WorldcupWinVO win) {
        UserVO loginUser = authService.getLoginUser(token);
        worldcupService.recordWin(loginUser, win);

        Map<String, Object> response = new HashMap<>();
        response.put("foodName", win.getFoodName());
        return response;
    }

    // 명예의 전당  GET /api/games/worldcup/rankings?limit=5
    // { "items": [{ "rank": 1, "foodName": "김치찌개", "wins": 5, "finals": 7 }, ...], "total": 12 }
    @GetMapping("/rankings")
    public Map<String, Object> getRankings(@RequestParam(defaultValue = "5") int limit) {
        List<WorldcupRankVO> items = worldcupService.getRankings(limit);

        Map<String, Object> response = new HashMap<>();
        response.put("items", items);
        response.put("total", worldcupService.countWins());
        return response;
    }
}
