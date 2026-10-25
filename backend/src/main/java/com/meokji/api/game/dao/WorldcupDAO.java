package com.meokji.api.game.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.meokji.api.game.vo.WorldcupRankVO;

/** WORLDCUP_WINS 표에 보내는 SQL. 쌓기만 하는 로그라 넣기 · 세기만 한다. */
@Mapper
public interface WorldcupDAO {

    void insertWin(@Param("userId") Long userId,
                   @Param("foodName") String foodName,
                   @Param("runnerUp") String runnerUp,
                   @Param("roundSize") int roundSize);

    // 우승 횟수가 많은 음식부터 limit 개. 같으면 결승 진출이 많은 순, 그다음 이름순
    List<WorldcupRankVO> selectRankings(@Param("limit") int limit);

    // 지금까지 끝난 판 수
    long countWins();
}
