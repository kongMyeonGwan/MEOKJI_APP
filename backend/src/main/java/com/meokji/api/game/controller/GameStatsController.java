package com.meokji.api.game.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.game.service.GameStatsService;
import com.meokji.api.game.vo.GameStatVO;

/**
 * 메뉴 게임 통계.  주소: /api/games/stats
 * 전체 회원이 게임마다 몇 번 했는지 센다. 로그인 없이도 볼 수 있다.
 */
@RestController
@RequestMapping("/api/games")
public class GameStatsController {

    @Autowired
    private GameStatsService gameStatsService;

    // 게임별 플레이 횟수  GET /api/games/stats
    // { "items": [{ "game_key": "worldcup", "game_type": "A", "plays": 120 }, ...], "total": 245 }
    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        List<GameStatVO> items = gameStatsService.getPlayCounts();

        long total = 0;
        for (GameStatVO item : items) {
            total += item.getPlays();
        }

        Map<String, Object> response = new HashMap<>();
        response.put("items", items);
        response.put("total", total);
        return response;
    }
}
