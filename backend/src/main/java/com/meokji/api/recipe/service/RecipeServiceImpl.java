package com.meokji.api.recipe.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.meokji.api.common.ApiException;
import com.meokji.api.ban.dao.BanDAO;
import com.meokji.api.recipe.vo.RecipeVO;
import com.meokji.api.util.FoodCatalog;
import com.meokji.api.util.RecipeBook;
import com.meokji.api.user.vo.UserVO;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * 레시피.
 * 미리 준비된 레시피 파일(recipes.json)에서 꺼내고, 재료 가격은 PriceService 가 붙인다.
 */
@Service
public class RecipeServiceImpl implements RecipeService {

    @Autowired
    private RecipeBook recipeBook; // recipes.json 을 읽어둔 것

    @Autowired
    private FoodCatalog foodCatalog; // catalog.json 을 읽어둔 것 (음식 분류, 그림)

    @Autowired
    private PriceService priceService; // 재료 가격

    @Autowired
    private BanDAO banDAO;

    @Override
    public RecipeVO getRecipe(UserVO loginUser, String foodName) {
        foodName = foodName.trim();
        if (foodName.isEmpty()) {
            throw ApiException.unprocessable("음식 이름이 없습니다.");
        }

        // 1. 로그인했으면 못 먹는 재료를 가져온다
        List<String> allergies = new ArrayList<>();
        List<String> dislikes = new ArrayList<>();
        if (loginUser != null) {
            allergies = banDAO.findActiveNames(loginUser.getId(), "ALLERGY");
            dislikes = banDAO.findActiveNames(loginUser.getId(), "DISLIKE");
        }

        // 2. 레시피 파일에서 꺼낸다 (못 먹는 재료는 빼거나 다른 재료로 바꿔준다)
        RecipeBook.Built built = recipeBook.build(foodName, allergies, dislikes);
        if (built == null) {
            throw ApiException.notFound("'" + foodName + "'" + josa(foodName, "은", "는")
                    + " 준비된 레시피가 없는 메뉴입니다. 추천이나 검색에서 고른 메뉴로 다시 시도해주세요.");
        }

        // 3. 조리 정보(시간, 인분, 난이도, 순서)를 JSON 한 덩어리로 묶는다
        ObjectNode recipeJson = JsonMapper.builder().build().createObjectNode();
        recipeJson.put("cook_time_minutes", built.getCookTimeMinutes());
        recipeJson.put("servings", built.getServings());
        recipeJson.put("difficulty", built.getDifficulty());
        recipeJson.set("steps", built.getSteps());

        // 4. 응답 만들기
        RecipeVO response = new RecipeVO();
        response.setFoodName(foodName);
        response.setRecipeName(built.getRecipeName());
        response.setRecipeJson(recipeJson);
        response.setIngredientsJson(built.getIngredients());
        response.setSubstitutesJson(built.getSubstitutes());
        response.setEstimatedCost(built.getEstimatedCost());

        // 음식 목록(catalog.json)에 있는 음식이면 분류 · 그림 정보도 붙인다
        JsonNode food = foodCatalog.findFood(foodName);
        if (food != null) {
            response.setCategory(food.path("category").asString(null));
            response.setArt(food.path("art").asString(null));
            JsonNode imageUrl = food.get("image_url");
            if (imageUrl != null && !imageUrl.isNull()) {
                response.setImageUrl(imageUrl.asString());
            }
        }

        // 5. 재료 가격을 붙인다 (정부 시세 API 를 부른다)
        response.setPrices(priceService.forIngredients(built.getIngredients()));

        return response;
    }

    // 받침이 있으면 "은", 없으면 "는" 을 고른다.  예) 김치찌개는, 된장국은
    // 한글 글자는 0xAC00 부터 28개씩 묶여 있어서, 나머지가 0 이 아니면 받침이 있다.
    private String josa(String word, String withFinal, String withoutFinal) {
        char last = word.charAt(word.length() - 1);
        if (last < 0xAC00 || last > 0xD7A3) {
            return withoutFinal; // 한글이 아니면
        }
        if ((last - 0xAC00) % 28 != 0) {
            return withFinal;
        }
        return withoutFinal;
    }
}
