package com.meokji.api.game.service;

import java.util.List;

import com.meokji.api.game.vo.GameStatVO;

/** 메뉴 게임 통계. 실제 코드는 GameStatsServiceImpl 에 있다. */
public interface GameStatsService {

    // 전체 회원의 게임별 플레이 횟수. 게임 셋이 늘 같은 순서(월드컵 · 밸런스 · 룰렛)로 온다
    List<GameStatVO> getPlayCounts();
}
