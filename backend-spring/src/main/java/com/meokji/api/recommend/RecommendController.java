package com.meokji.api.recommend;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.recommend.dto.RecommendRequest;
import com.meokji.api.recommend.dto.RecommendResponse;
import com.meokji.api.security.LoginUser;
import com.meokji.api.user.User;

import jakarta.validation.Valid;

/**
 * 음식 추천.
 *
 * <p>파이썬 쪽 {@code app/routers/recommendations.py} 의 추천 부분이다.
 * 레시피({@code GET /api/recipes/{이름}})는 아직 옮기지 않았다.
 *
 * <p>로그인해도 되고 안 해도 된다. 로그인했으면 못 먹는 재료(Ban)가 자동으로
 * 반영되고, 좌표를 안 보냈을 때 가입 주소의 좌표로 날씨를 본다.
 */
@RestController
@RequestMapping("/api")
public class RecommendController {

    private final RecommendService recommendService;

    public RecommendController(RecommendService recommendService) {
        this.recommendService = recommendService;
    }

    @PostMapping("/recommendations")
    public RecommendResponse recommend(
            @LoginUser(required = false) User me,
            @Valid @RequestBody RecommendRequest request) {

        return recommendService.recommend(me, request);
    }
}
