package com.meokji.api.recommend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.recommend.vo.RecommendRequestVO;
import com.meokji.api.recommend.vo.RecommendVO;
import com.meokji.api.recommend.service.RecommendService;
import com.meokji.api.user.vo.UserVO;

import jakarta.validation.Valid;

/**
 * 음식 추천.  주소: POST /api/recommendations
 * 로그인 없이도 된다. 로그인했으면 못 먹는 재료를 빼고 추천한다.
 */
@RestController
@RequestMapping("/api")
public class RecommendController {

    @Autowired
    private RecommendService recommendService;

    @PostMapping("/recommendations")
    public RecommendVO recommend(
            // 로그인 안 했으면 null 이다. 이 주소는 SecurityConfig 에서 permitAll 이라
            // 손님도 들어온다 - 로그인했을 때만 얹어주는 정보가 있어서 받아만 둔다.
            @AuthenticationPrincipal UserVO loginUser,
            @Valid @RequestBody RecommendRequestVO request) {
        return recommendService.recommend(loginUser, request);
    }
}
