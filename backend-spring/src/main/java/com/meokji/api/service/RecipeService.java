package com.meokji.api.service;

import com.meokji.api.dto.recipe.RecipeResponse;
import com.meokji.api.vo.UserVO;

/** 레시피. 실제 코드는 RecipeServiceImpl 에 있다. */
public interface RecipeService {

    // 음식 이름으로 레시피 + 재료 가격을 만든다. 준비된 레시피가 없는 음식이면 404 에러
    RecipeResponse getRecipe(UserVO loginUser, String foodName);
}
