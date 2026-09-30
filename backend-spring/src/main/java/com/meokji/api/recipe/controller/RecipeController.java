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
 */
@RestController
@RequestMapping("/api")
public class RecipeController {

    @Autowired
    private AuthService authService;

    @Autowired
    private RecipeService recipeService;

    @GetMapping("/recipes/{foodName}")
    public RecipeVO getRecipe(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable @Size(min = 1, max = 150) String foodName) {
        UserVO loginUser = authService.findLoginUser(token); // 로그인 안 했으면 null
        return recipeService.getRecipe(loginUser, foodName);
    }
}
