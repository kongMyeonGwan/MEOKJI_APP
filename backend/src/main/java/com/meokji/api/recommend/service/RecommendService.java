package com.meokji.api.recommend.service;

import com.meokji.api.recommend.vo.RecommendRequestVO;
import com.meokji.api.recommend.vo.RecommendVO;
import com.meokji.api.user.vo.UserVO;

/**
 * 음식 추천. 실제 코드는 RecommendServiceImpl 에 있다.
 *
 * <p>메서드가 하나인데 안에서 여러 가지를 한다 — 조건으로 후보를 걸러내고,
 * 점수를 매겨 고르고, 날씨와 싼 재료를 (요청에 따라) 같이 붙인다.
 * 화면이 한 번만 부르면 되도록 묶어 두었다.
 */
public interface RecommendService {

    /**
     * 조건에 맞는 음식을 골라준다. loginUser 는 로그인 안 했으면 null
     *
     * @param loginUser null 이면 못 먹는 재료를 빼지 않고, 주소로 날씨도 못 찾는다
     * @param request   조건. 전부 선택이라 빈 객체여도 된다
     */
    RecommendVO recommend(UserVO loginUser, RecommendRequestVO request);
}
