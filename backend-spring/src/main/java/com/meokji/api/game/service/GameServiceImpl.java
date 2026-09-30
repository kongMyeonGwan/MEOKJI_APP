package com.meokji.api.game.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meokji.api.common.ApiException;
import com.meokji.api.game.dao.GameDAO;
import com.meokji.api.game.dto.GamePlayResponse;
import com.meokji.api.util.Times;
import com.meokji.api.user.vo.UserVO;

@Service
public class GameServiceImpl implements GameService {

    @Autowired
    private GameDAO gameDAO;

    @Override
    @Transactional
    public GamePlayResponse recordPlay(UserVO user, String gameKey) {
        // 게임 이름 → DB 에 저장하는 한 글자 코드
        String gameType;
        if (gameKey.equals("worldcup")) {
            gameType = "A"; // 음식 월드컵
        } else if (gameKey.equals("balance")) {
            gameType = "B"; // 취향 밸런스
        } else if (gameKey.equals("roulette")) {
            gameType = "C"; // 룰렛
        } else {
            throw ApiException.notFound("그런 게임이 없습니다: " + gameKey);
        }

        gameDAO.insertPlay(user.getId(), gameType);

        GamePlayResponse response = new GamePlayResponse();
        response.setGameKey(gameKey);
        response.setGameType(gameType);
        response.setPlayedAt(Times.toText(LocalDateTime.now(ZoneOffset.UTC))); // 지금 시각 (UTC)
        return response;
    }
}
