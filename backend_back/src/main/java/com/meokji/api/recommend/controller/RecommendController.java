package com.meokji.api.recommend.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.recommend.vo.RecommendRequestVO;
import com.meokji.api.recommend.vo.RecommendVO;
import com.meokji.api.auth.service.AuthService;
import com.meokji.api.recommend.service.RecommendService;
import com.meokji.api.user.vo.UserVO;

import jakarta.validation.Valid;

/**
 * 음식 추천.  주소: POST /api/recommendations
 * 로그인 없이도 된다. 로그인했으면 못 먹는 재료를 빼고 추천한다.
 *
 * <p>GET 이 아니라 POST 인 이유
 *
 * <p>추천은 읽기인데 POST 를 쓴다. 조건이 많고(열 가지가 넘는다) 그 중에
 * 목록(exclude - 이미 보여준 메뉴)이 들어 있어서다. 주소줄에 담으면
 * ?exclude=김치찌개&amp;exclude=제육볶음&amp;... 으로 끝없이 길어지고,
 * 브라우저마다 주소 길이 한계가 달라 어느 선에서 잘린다.
 *
 * <p>GET 이면 브라우저가 캐시할 수도 있다. 추천은 "매번 다르게 나와야"
 * 하는 것이라 캐시되면 새로고침해도 같은 목록이 나온다.
 *
 * <p>메서드가 하나뿐이고 날씨 · 싼 재료도 이 응답에 같이 담긴다. 주소를
 * 나누지 않은 이유는 RecommendVO 주석에 적어 두었다.
 */
@RestController
@RequestMapping("/api")
public class RecommendController {

    @Autowired
    private AuthService authService;

    @Autowired
    private RecommendService recommendService;

    /**
     * 조건에 맞는 음식을 골라 돌려준다.
     *
     * 조건은 전부 선택이다. 빈 객체({})를 보내도 기본값(5개)으로 추천된다 -
     * 대시보드가 처음 뜰 때 그렇게 부른다.
     */
    @PostMapping("/recommendations")
    public RecommendVO recommend(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody RecommendRequestVO request) {
        // getLoginUser 가 아니라 findLoginUser 다. 추천은 로그인 없이도
        // 봐야 하는 첫 화면이라 토큰이 없을 때 에러를 내면 안 된다
        UserVO loginUser = authService.findLoginUser(token); // 로그인 안 했으면 null
        return recommendService.recommend(loginUser, request);
    }
}
