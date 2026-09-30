package com.meokji.api.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.dto.recommend.RecommendRequest;
import com.meokji.api.dto.recommend.RecommendResponse;
import com.meokji.api.service.AuthService;
import com.meokji.api.service.RecommendService;
import com.meokji.api.vo.UserVO;

import jakarta.validation.Valid;

/**
 * 음식 추천.  주소: POST /api/recommendations
 * 로그인 없이도 된다. 로그인했으면 못 먹는 재료를 빼고 추천한다.
 */
@RestController
@RequestMapping("/api")
public class RecommendController {

    @Autowired
    private AuthService authService;

    @Autowired
    private RecommendService recommendService;

    @PostMapping("/recommendations")
    public RecommendResponse recommend(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody RecommendRequest request) {
        UserVO loginUser = authService.findLoginUser(token); // 로그인 안 했으면 null
        return recommendService.recommend(loginUser, request);
    }
}
