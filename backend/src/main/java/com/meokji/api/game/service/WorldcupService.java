package com.meokji.api.game.service;

import java.util.List;

import com.meokji.api.game.vo.WorldcupRankVO;
import com.meokji.api.game.vo.WorldcupWinVO;
import com.meokji.api.user.vo.UserVO;

/** 음식 이상형 월드컵 우승 기록 · 명예의 전당. 실제 코드는 WorldcupServiceImpl 에 있다. */
public interface WorldcupService {

    // 우승 한 판을 남긴다. 음식 이름이 비었거나 라운드 크기가 이상하면 400 에러
    void recordWin(UserVO user, WorldcupWinVO win);

    // 우승 횟수 순위 limit 개 (1 ~ 20)
    List<WorldcupRankVO> getRankings(int limit);

    // 지금까지 끝난 판 수
    long countWins();
}
