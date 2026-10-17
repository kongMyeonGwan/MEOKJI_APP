package com.meokji.api.recipe.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.recipe.vo.RecipeVO;
import com.meokji.api.auth.service.AuthService;
import com.meokji.api.recipe.service.RecipeService;
import com.meokji.api.user.vo.UserVO;

import jakarta.validation.constraints.Size;

/**
 * 레시피 + 재료 가격.  주소: GET /api/recipes/김치찌개
 * 로그인 없이도 된다. 로그인했으면 못 먹는 재료를 빼거나 다른 재료로 바꿔준다.
 *
 * <p>주소에 음식 번호가 아니라 이름이 들어간다. 레시피가 표가 아니라
 * resources/data/recipes.json 에 있어서 번호가 없고, 추천 · 검색 · 찜이
 * 전부 이름으로 음식을 가리키기 때문이다.
 *
 * <p>메서드가 하나뿐이다. 레시피는 읽기만 하고, 사용자가 고치거나 새로
 * 쓰는 것이 아니다. "내가 만들어 먹었다" 는 기록은 history 기능이 맡는다.
 *
 * <p>&#64;RequestMapping 이 /api 인 점에 주의 — 다른 컨트롤러처럼
 * /api/recipes 로 묶지 않았다. 주소가 하나뿐이라 묶을 것이 없다.
 */
@RestController
@RequestMapping("/api")
public class RecipeController {

    @Autowired
    private AuthService authService;

    @Autowired
    private RecipeService recipeService;

    /**
     * 음식 이름으로 레시피와 재료 가격을 받는다.
     *
     * 로그인 여부로 응답이 달라진다
     *   비로그인 : 레시피 그대로
     *   로그인   : 못 먹는 재료를 빼거나 다른 재료로 바꾼 레시피 +
     *              무엇을 왜 바꿨는지(substitutes_json)
     *
     * &#64;Size(min = 1, max = 150) 을 거는 이유 — 이름이 그대로
     * recipes.json 을 찾는 열쇠가 된다. 아주 긴 글자가 와도 어차피
     * 못 찾지만, 찾아보기 전에 막는 쪽이 낫다. 150 은 음식 이름 칸의
     * DB 길이와 맞춘 값이다.
     */
    @GetMapping("/recipes/{foodName}")
    public RecipeVO getRecipe(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable @Size(min = 1, max = 150) String foodName) {
        // getLoginUser 가 아니라 findLoginUser 다. 레시피는 로그인 없이도
        // 봐야 하므로 토큰이 없을 때 에러를 내면 안 된다
        UserVO loginUser = authService.findLoginUser(token); // 로그인 안 했으면 null
        // 준비된 레시피가 없는 음식이면 서비스가 404 를 던진다
        return recipeService.getRecipe(loginUser, foodName);
    }
}
