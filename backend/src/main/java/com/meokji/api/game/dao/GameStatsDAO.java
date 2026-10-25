package com.meokji.api.game.dao;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.meokji.api.game.vo.GameStatVO;

/** GAME_HISTORIES 표를 세는 SQL. 기록을 넣는 쪽은 GameDAO 에 있다. */
@Mapper
public interface GameStatsDAO {

    // 게임 종류(A · B · C)별 플레이 횟수. 한 번도 안 한 게임은 줄이 없다
    List<GameStatVO> selectPlayCounts();
}
