package com.meokji.api.like.service;

import com.meokji.api.like.vo.LikeListVO;
import com.meokji.api.like.vo.RecipeLikeVO;
import com.meokji.api.user.vo.UserVO;

/** 찜한 레시피. 실제 코드는 LikeServiceImpl 에 있다. */
public interface LikeService {

    /**
     * 내 찜 목록 (음식 이름들)
     *
     * 이름만 돌려주지 않고 카탈로그에서 찾은 음식 정보(items)도 같이 담는다.
     * 화면이 카드를 그리려면 분류 · 조리 시간 · 난이도가 필요하다.
     */
    LikeListVO getList(UserVO user);

    /**
     * 하트 누르기. 찜돼 있으면 풀고, 아니면 찜한다
     *
     * 화면이 "켜라/꺼라" 를 정하지 않는다. 서버가 지금 상태를 읽어 뒤집는다 —
     * 화면이 정하면 다른 기기에서 이미 푼 찜을 또 풀려고 보낼 수 있다.
     *
     * @param foodName 앞뒤 공백이 없는 이름. 다듬는 일은 컨트롤러가 한다
     * @return 바뀐 뒤의 상태(liked)와 내 찜 전체 개수(count)
     */
    RecipeLikeVO toggle(UserVO user, String foodName);

    /**
     * 찜 전부 지우기
     *
     * @return 비운 뒤의 상태(빈 목록 · count 0). items 는 null 이다
     */
    LikeListVO clear(UserVO user);
}
