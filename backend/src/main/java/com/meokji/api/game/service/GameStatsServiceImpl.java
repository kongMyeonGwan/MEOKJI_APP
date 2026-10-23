package com.meokji.api.game.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.meokji.api.game.dao.GameStatsDAO;
import com.meokji.api.game.vo.GameStatVO;

@Service
public class GameStatsServiceImpl implements GameStatsService {

    // DB 글자 → 게임 이름. GameServiceImpl.recordPlay 의 짝과 같아야 한다
    private static final String[][] GAMES = {
        { "A", "worldcup" }, // 음식 월드컵
        { "B", "balance" },  // 취향 밸런스
        { "C", "roulette" }, // 룰렛
    };

    @Autowired
    private GameStatsDAO gameStatsDAO;

    @Override
    public List<GameStatVO> getPlayCounts() {
        List<GameStatVO> counted = gameStatsDAO.selectPlayCounts();

        // 한 번도 안 한 게임은 DB 에 줄이 없어서, 0 으로 채워 셋 다 내려준다
        List<GameStatVO> result = new ArrayList<>();
        for (String[] game : GAMES) {
            GameStatVO stat = new GameStatVO();
            stat.setGameType(game[0]);
            stat.setGameKey(game[1]);
            for (GameStatVO row : counted) {
                if (game[0].equals(row.getGameType())) {
                    stat.setPlays(row.getPlays());
                }
            }
            result.add(stat);
        }
        return result;
    }
}
