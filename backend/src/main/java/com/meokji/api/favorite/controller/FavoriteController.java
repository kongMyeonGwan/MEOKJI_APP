package com.meokji.api.favorite.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.favorite.service.FavoriteService;

/**
 * 찜한 레시피.  주소: /api/favorites
 *
 * '좋아요(like)' 와 헷갈리지 않게 찜은 favorite 으로 부른다.
 *   찜        = favorite  음식에 누르는 ♡. 나만 보는 보관함 (이 패키지, RECIPE_LIKES 표)
 *   후기 좋아요 = like      남이 쓴 후기 글에 누르는 반응 (review 패키지, REVIEW_LIKES 표)
 * 표 이름은 예전 그대로 RECIPE_LIKES 다. 엔티티에서 @Table(name = "recipe_likes") 로 잇는다.
 *
 * TODO: JPA 로 구현한다 (구조는 free 패키지를 따라간다: entity · repository · dto · service).
 * 프론트(frontend/src/api/favorites.js)가 기대하는 모양
 *   GET    /api/favorites   내 찜 목록   { "names": ["김치찌개", ...], "items": [음식 정보...], "count": 2 }
 *   PUT    /api/favorites   찜 토글      요청 { "food_name": "김치찌개" }
 *                                       응답 { "food_name": "김치찌개", "favorited": true, "count": 3 }
 *   DELETE /api/favorites   찜 전체 비우기
 * 셋 다 로그인이 필요하다.
 */
@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    // 내 찜 목록  GET /api/favorites
    @GetMapping
    public void getList() {

    }
}
