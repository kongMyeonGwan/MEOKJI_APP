package com.meokji.api.game.service;

import com.meokji.api.game.vo.GamePlayVO;
import com.meokji.api.user.vo.UserVO;

/**
 * 메뉴 게임 기록. 실제 코드는 GameServiceImpl 에 있다.
 *
 * <p>메서드가 하나뿐인데 인터페이스를 둔 이유 — 다른 기능과 같은 모양
 * (인터페이스 + Impl)을 지키기 위한 것이다. 기능마다 구조가 다르면
 * 코드를 찾을 때 매번 확인해야 한다.
 */
public interface GameService {

    /**
     * 게임을 시작했다고 기록한다. 없는 게임 이름이면 404 에러
     *
     * @param gameKey "worldcup" · "balance" · "roulette" 중 하나.
     *                DB 에는 한 글자 코드(A · B · C)로 바꿔 넣는다
     * @return 방금 기록한 내용. DB 에서 다시 읽은 값이 아니라 서비스가 만든 값이다
     */
    GamePlayVO recordPlay(UserVO user, String gameKey);
}
