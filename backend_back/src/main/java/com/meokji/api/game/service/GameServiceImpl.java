package com.meokji.api.game.service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.meokji.api.common.ApiException;
import com.meokji.api.game.dao.GameDAO;
import com.meokji.api.game.vo.GamePlayVO;
import com.meokji.api.util.Times;
import com.meokji.api.user.vo.UserVO;

/**
 * 게임을 시작했다는 기록 남기기.
 *
 * <p>하는 일이 셋이다 — 게임 이름을 DB 코드로 바꾸고, 한 줄 넣고, 방금 넣은
 * 내용을 응답 모양으로 만들어 돌려준다.
 *
 * <p>이름과 코드를 나눠 쓰는 이유
 *
 * <p>DB 의 GAME_TYPE 은 한 글자(CHAR(1))다. 표를 만들 때 정해진 모양이라
 * "worldcup" 을 그대로 넣을 수 없다. 반대로 화면과 주소에는 "worldcup" 이
 * 쓰인다 — URL 에 /api/games/A/plays 라고 적혀 있으면 무슨 게임인지 알 수 없다.
 * 그 사이를 바꿔주는 자리가 여기다.
 */
@Service
public class GameServiceImpl implements GameService {

    @Autowired
    private GameDAO gameDAO;

    /**
     * 게임 이름을 받아 한 줄 기록하고, 방금 기록한 내용을 돌려준다.
     *
     * {@code @Transactional} 이 붙어 있지만 INSERT 하나뿐이다. 되돌릴 짝이
     * 없어도 붙여두는 이유는, 나중에 줄을 더할 때 빠뜨리지 않게 하려는 것이다.
     */
    @Override
    @Transactional
    public GamePlayVO recordPlay(UserVO user, String gameKey) {
        // 게임 이름 → DB 에 저장하는 한 글자 코드
        //
        // Map 이나 enum 으로 두지 않고 if 로 늘어놓은 이유 — 세 개뿐이고,
        // "없는 이름" 처리까지 같은 자리에서 끝난다. Map 으로 두면
        // get 결과가 null 인지 또 확인해야 한다.
        //
        // 주소에 @Pattern 을 걸어 막지 않은 이유 — 막으면 422 가 나가는데,
        // "그런 게임이 없다" 는 404 가 맞다. 값의 모양이 틀린 게 아니라
        // 가리키는 것이 없는 경우다.
        String gameType;
        if (gameKey.equals("worldcup")) {
            gameType = "A"; // 음식 월드컵
        } else if (gameKey.equals("balance")) {
            gameType = "B"; // 취향 밸런스
        } else if (gameKey.equals("roulette")) {
            gameType = "C"; // 룰렛
        } else {
            // 어떤 이름을 보냈는지 문장에 붙여준다. 화면 쪽 오타를 찾기 쉽다
            throw ApiException.notFound("그런 게임이 없습니다: " + gameKey);
        }

        // 이 표는 넣기만 한다 (수정 · 삭제 없음). DAO 주석 참고
        gameDAO.insertPlay(user.getId(), gameType);

        // 넣은 행을 다시 읽지 않고 응답을 직접 만든다.
        //
        // 다른 기능(ban · free 등)은 insert 뒤에 findById 로 다시 읽어서
        // 돌려준다. 여기는 그럴 수 없다 — 이 표에 PK 가 없어서 "방금 넣은
        // 그 행" 을 집을 열이 없다.
        GamePlayVO response = new GamePlayVO();
        response.setGameKey(gameKey);
        response.setGameType(gameType);
        // DB 의 DEFAULT 가 찍은 시각을 읽어올 수 없으므로 자바 시계로 만든다.
        // 두 시계가 조금 어긋날 수 있지만, 이 값은 화면에 보여주는 용도뿐이고
        // 무언가를 판단하는 데 쓰지 않는다.
        //
        // ZoneOffset.UTC 를 꼭 넣는다. LocalDateTime.now() 만 쓰면 서버가
        // 선 지역 시계(KST)가 되어 DB 에 쌓인 UTC 값과 9시간 어긋난다
        response.setPlayedAt(Times.toText(LocalDateTime.now(ZoneOffset.UTC))); // 지금 시각 (UTC)
        return response;
    }
}
