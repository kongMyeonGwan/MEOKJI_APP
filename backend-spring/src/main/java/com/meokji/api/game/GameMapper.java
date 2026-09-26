package com.meokji.api.game;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * GAME_HISTORIES 표에 보내는 SQL.
 *
 * <p>이 표는 PK 도 {@code use_yn} 도 없다. 수정 · 삭제하지 않는 누적 로그라서
 * 넣기만 한다. 그래서 넣은 뒤 그 행을 다시 읽어올 방법이 없다 - "방금 그 행" 을
 * 정확히 집을 열이 없다.
 */
@Mapper
public interface GameMapper {

    void insertPlay(@Param("userId") Long userId, @Param("gameType") String gameType);
}
