package com.meokji.api.recipe;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.recipe.dto.RecipeResponse;
import com.meokji.api.security.LoginUser;
import com.meokji.api.user.User;

import jakarta.validation.constraints.Size;

/**
 * 레시피 + 재료 가격.
 *
 * <p>파이썬 쪽 {@code app/routers/recommendations.py} 의 {@code read_recipe} 자리다.
 * 주소가 {@code /api/recipes/{음식이름}} 이라 한글이 경로에 들어간다.
 *
 * <p>로그인해도 되고 안 해도 된다. 로그인했으면 못 먹는 재료(Ban)가 반영된다 -
 * 알레르기 재료는 목록에서 빠지고, 싫어하는 재료는 대체재료가 함께 온다.
 */
@RestController
@RequestMapping("/api")
public class RecipeController {

    private final RecipeService recipeService;

    public RecipeController(RecipeService recipeService) {
        this.recipeService = recipeService;
    }

    @GetMapping("/recipes/{foodName}")
    public RecipeResponse read(
            @PathVariable @Size(min = 1, max = 150) String foodName,
            @LoginUser(required = false) User me) {

        // 가격은 아직 붙이지 않는다. KAMIS 시세를 옮기면 여기서 갈아 끼운다
        // (RecipeService.withPrices).
        return recipeService.make(me, foodName);
    }
}
