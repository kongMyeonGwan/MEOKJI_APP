package com.meokji.api.recommend.service;

import com.meokji.api.recommend.dto.RecommendRequest;
import com.meokji.api.recommend.dto.RecommendResponse;
import com.meokji.api.user.vo.UserVO;

/** 음식 추천. 실제 코드는 RecommendServiceImpl 에 있다. */
public interface RecommendService {

    // 조건에 맞는 음식을 골라준다. loginUser 는 로그인 안 했으면 null
    RecommendResponse recommend(UserVO loginUser, RecommendRequest request);
}
