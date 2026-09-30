package com.meokji.api.game.service;

import com.meokji.api.game.dto.GamePlayResponse;
import com.meokji.api.user.vo.UserVO;

/** 메뉴 게임 기록. 실제 코드는 GameServiceImpl 에 있다. */
public interface GameService {

    // 게임을 시작했다고 기록한다. 없는 게임 이름이면 404 에러
    GamePlayResponse recordPlay(UserVO user, String gameKey);
}
