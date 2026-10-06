package com.meokji.api.recipe.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.recipe.vo.RecipeVO;
import com.meokji.api.recipe.service.RecipeService;
import com.meokji.api.user.vo.UserVO;

import jakarta.validation.constraints.Size;

/**
 * 레시피 + 재료 가격.  주소: GET /api/recipes/김치찌개
 * 로그인 없이도 된다. 로그인했으면 못 먹는 재료를 빼거나 다른 재료로 바꿔준다.
 */
@RestController
@RequestMapping("/api")
public class RecipeController {

    @Autowired
    private RecipeService recipeService;

    @GetMapping("/recipes/{foodName}")
    public RecipeVO getRecipe(
            // 로그인 안 했으면 null 이다. 이 주소는 SecurityConfig 에서 permitAll 이라
            // 손님도 들어온다 - 로그인했을 때만 얹어주는 정보가 있어서 받아만 둔다.
            @AuthenticationPrincipal UserVO loginUser,
            @PathVariable @Size(min = 1, max = 150) String foodName) {
        return recipeService.getRecipe(loginUser, foodName);
    }
}
