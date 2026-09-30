package com.meokji.api.like.service;

import com.meokji.api.like.vo.LikeListVO;
import com.meokji.api.like.vo.LikeToggleVO;
import com.meokji.api.user.vo.UserVO;

/** 찜한 레시피. 실제 코드는 LikeServiceImpl 에 있다. */
public interface LikeService {

    // 내 찜 목록 (음식 이름들)
    LikeListVO getList(UserVO user);

    // 하트 누르기. 찜돼 있으면 풀고, 아니면 찜한다
    LikeToggleVO toggle(UserVO user, String foodName);

    // 찜 전부 지우기
    LikeListVO clear(UserVO user);
}
