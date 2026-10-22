package com.meokji.api.favorite.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.meokji.api.auth.service.AuthService;
import com.meokji.api.favorite.dto.FavoriteRequest;
import com.meokji.api.favorite.dto.FavoriteToggleResponse;
import com.meokji.api.favorite.dto.FavoritesResponse;
import com.meokji.api.favorite.service.FavoriteService;
import com.meokji.api.user.vo.UserVO;

import jakarta.validation.Valid;

/**
 * 찜한 레시피. 주소: /api/favorites
 *
 * '좋아요(like)' 와 헷갈리지 않게 찜은 favorite 으로 부른다.
 *   찜 = favorite        음식에 누르는 ♡. 나만 보는 보관함 (이 패키지, RECIPE_LIKES 표)
 *   후기 좋아요 = like   남이 쓴 후기 글에 누르는 반응 (review 패키지, REVIEW_LIKES 표)
 * 표 이름은 예전 그대로 RECIPE_LIKES 다. 엔티티에서 @Table 로 잇는다.
 *
 * 프론트(frontend/src/api/favorites.js)와 주고받는 모양
 *   GET    /api/favorites  내 찜 목록 { "names": [...], "items": [...], "count": 2 }
 *   PUT    /api/favorites  찜 토글   요청 { "foodName": "김치찌개" }
 *                                    응답 { "foodName": "김치찌개", "favorited": true, "count": 3 }
 *   DELETE /api/favorites  찜 전체 비우기
 * 셋 다 로그인이 필요하다.
 */
@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    @Autowired
    private FavoriteService favoriteService;

    @Autowired
    private AuthService authService;

    /** 내 찜 목록 */
    @GetMapping
    public FavoritesResponse getList(
            @RequestHeader(value = "Authorization", required = false) String token) {

        UserVO loginUser = authService.getLoginUser(token); // 로그인 안 했으면 401

        return favoriteService.getList(loginUser.getId());
    }

    /**
     * 찜하기 · 해제. 같은 이름을 다시 보내면 뒤집힌다.
     *
     * PUT 인데 멱등이 아니다. 하트 버튼 하나로 켜고 끄는 동작이라 화면이 지금
     * 상태를 몰라도 되게 만든 것인데, 그 대신 같은 요청을 두 번 보내면 원래대로
     * 돌아온다. 화면이 재시도하지 않는다는 전제가 깔려 있다.
     */
    @PutMapping
    public FavoriteToggleResponse toggle(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody FavoriteRequest request) {

        UserVO loginUser = authService.getLoginUser(token);

        return favoriteService.toggle(loginUser.getId(), request.getFoodName());
    }

    /** 찜 전체 비우기 (설정 화면) */
    @DeleteMapping
    public FavoritesResponse clear(
            @RequestHeader(value = "Authorization", required = false) String token) {

        UserVO loginUser = authService.getLoginUser(token);

        return favoriteService.clear(loginUser.getId());
    }
}
