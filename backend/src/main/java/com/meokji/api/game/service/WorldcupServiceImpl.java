package com.meokji.api.game.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meokji.api.common.ApiException;
import com.meokji.api.game.dao.WorldcupDAO;
import com.meokji.api.game.vo.WorldcupRankVO;
import com.meokji.api.game.vo.WorldcupWinVO;
import com.meokji.api.user.vo.UserVO;

@Service
public class WorldcupServiceImpl implements WorldcupService {

    private static final int MAX_NAME = 150; // WORLDCUP_WINS.food_name 의 길이

    @Autowired
    private WorldcupDAO worldcupDAO;

    @Override
    @Transactional
    public void recordWin(UserVO user, WorldcupWinVO win) {
        String foodName = trim(win.getFoodName());
        String runnerUp = trim(win.getRunnerUp());
        Integer roundSize = win.getRoundSize();

        if (foodName == null) {
            throw ApiException.badRequest("우승 음식 이름이 없습니다.");
        }
        if (foodName.length() > MAX_NAME || (runnerUp != null && runnerUp.length() > MAX_NAME)) {
            throw ApiException.badRequest("음식 이름이 너무 깁니다.");
        }
        // 월드컵은 2 · 4 · 8 ... 처럼 둘씩 짝지어 줄어드는 크기만 있다
        if (roundSize == null || roundSize < 2 || roundSize > 128 || Integer.bitCount(roundSize) != 1) {
            throw ApiException.badRequest("라운드 크기가 올바르지 않습니다.");
        }

        worldcupDAO.insertWin(user.getId(), foodName, runnerUp, roundSize);
    }

    @Override
    public List<WorldcupRankVO> getRankings(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 20));
        List<WorldcupRankVO> items = worldcupDAO.selectRankings(safeLimit);

        // 등수: 우승 횟수가 같으면 같은 등수다 (5회 · 5회 · 3회 → 1 · 1 · 3위)
        for (int i = 0; i < items.size(); i++) {
            WorldcupRankVO item = items.get(i);
            if (i > 0 && items.get(i - 1).getWins() == item.getWins()) {
                item.setRank(items.get(i - 1).getRank());
            } else {
                item.setRank(i + 1);
            }
        }
        return items;
    }

    @Override
    public long countWins() {
        return worldcupDAO.countWins();
    }

    // 앞뒤 빈칸을 자른다. 비었으면 null
    private String trim(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
