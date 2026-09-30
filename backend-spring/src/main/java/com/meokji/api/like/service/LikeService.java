package com.meokji.api.like.service;

import com.meokji.api.like.dto.LikeListResponse;
import com.meokji.api.like.dto.LikeToggleResponse;
import com.meokji.api.user.vo.UserVO;

/** 찜한 레시피. 실제 코드는 LikeServiceImpl 에 있다. */
public interface LikeService {

    // 내 찜 목록 (음식 이름들)
    LikeListResponse getList(UserVO user);

    // 하트 누르기. 찜돼 있으면 풀고, 아니면 찜한다
    LikeToggleResponse toggle(UserVO user, String foodName);

    // 찜 전부 지우기
    LikeListResponse clear(UserVO user);
}
