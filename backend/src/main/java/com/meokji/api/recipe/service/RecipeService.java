package com.meokji.api.recipe.service;

import com.meokji.api.recipe.vo.RecipeVO;
import com.meokji.api.user.vo.UserVO;

/**
 * 레시피. 실제 코드는 RecipeServiceImpl 에 있다.
 *
 * <p>메서드가 하나인데 하는 일은 셋이다 — 레시피를 찾고, 못 먹는 재료를
 * 처리하고, 재료 가격을 붙인다. 화면이 한 번만 부르면 되도록 묶어 두었다.
 * 나누면 화면이 세 번 부르고, 그 사이에 재료가 바뀌면 가격이 안 맞는다.
 */
public interface RecipeService {

    /**
     * 음식 이름으로 레시피 + 재료 가격을 만든다. 준비된 레시피가 없는 음식이면 404 에러
     *
     * @param loginUser 로그인 안 했으면 null. 그때는 못 먹는 재료를 빼지 않는다
     * @param foodName  recipes.json 의 음식 이름과 글자가 같아야 찾을 수 있다
     */
    RecipeVO getRecipe(UserVO loginUser, String foodName);
}
