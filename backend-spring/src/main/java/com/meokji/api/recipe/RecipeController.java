package com.meokji.api.recipe;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.price.PriceService;
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

    @Autowired
    private RecipeService recipeService;
    @Autowired
    private PriceService priceService;


    @GetMapping("/recipes/{foodName}")
    public RecipeResponse read(
            @PathVariable @Size(min = 1, max = 150) String foodName,
            @LoginUser(required = false) User me) {

        RecipeResponse recipe = recipeService.make(me, foodName);

        // 레시피를 만드는 일과 시세를 받아오는 일을 섞지 않는다. 앞은 파일에서
        // 꺼내는 일이고 뒤는 바깥을 부르는 일이라, 한 메서드에 두면 무엇 때문에
        // 느린지 · 무엇 때문에 실패했는지 가려낼 수 없다.
        return RecipeService.withPrices(
                recipe, priceService.forIngredients(recipe.ingredientsJson()));
    }
}
