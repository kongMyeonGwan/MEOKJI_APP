package com.meokji.api.game.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * GAME_HISTORIES 표에 보내는 SQL.
 *
 * 이 표는 PK 도 use_yn 도 없다. 수정 · 삭제하지 않는 누적 로그라서
 * 넣기만 한다. 그래서 넣은 뒤 그 행을 다시 읽어올 방법이 없다 - "방금 그 행" 을
 * 정확히 집을 열이 없다.
 *
 * <p>PK 를 안 둔 이유 — 한 사람이 같은 게임을 여러 번 할 수 있고, 그 기록을
 * 하나씩 고칠 일도 지울 일도 없다. 번호를 매길 시퀀스와 인덱스를 두면
 * 쓰지도 않는 것을 유지하게 된다.
 *
 * <p>대신 값이 하나 치러진다 — 응답을 만들 때 DB 에서 다시 읽을 수 없어서,
 * 서비스가 시각을 자바 시계로 만들어 넣는다(GameServiceImpl 참고).
 *
 * <p>VO 를 안 쓰고 값 두 개를 바로 받는 이유 — 넣는 칸이 둘뿐이다.
 * GamePlayVO 는 응답 모양이라 칸이 다르고(gameKey · playedAt), 그걸
 * 넘기면 XML 에서 쓰지 않는 칸이 섞여 보인다.
 */
@Mapper
public interface GameDAO {

    /**
     * 게임 기록 한 줄 넣기. created_at 은 DDL 의 DEFAULT 가 찍는다.
     *
     * @param gameType 한 글자 코드. A(월드컵) · B(밸런스) · C(룰렛).
     *                 이름을 코드로 바꾸는 일은 서비스가 한다
     */
    void insertPlay(@Param("userId") Long userId, @Param("gameType") String gameType);
}
