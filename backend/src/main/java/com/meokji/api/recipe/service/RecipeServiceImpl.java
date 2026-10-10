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
 *
 * <p>왜 레시피가 표가 아니라 파일인가 — 사용자가 쓰는 값이 아니고 운영자도
 * 거의 안 고친다. 표로 두면 레시피 100개의 INSERT 문을 관리해야 하고,
 * 조리 순서가 배열이라 표 하나로는 담기지 않아 표를 여럿 만들어야 한다.
 *
 * <p>DB 를 보는 자리는 한 곳뿐이다 — 못 먹는 재료(BanDAO). 그것만 사람마다
 * 다르고, 나머지는 누가 봐도 같은 값이다.
 *
 * <p>한 메서드가 길지만 하는 일이 순서대로 다섯이다. 번호를 매겨 두었다.
 *
 * <p>&#64;Transactional 이 없다. 읽기만 하고, 바깥 API(KAMIS)를 부르는 일이
 * 들어 있어서 트랜잭션을 열어두면 응답을 기다리는 동안 DB 연결을 붙잡고 있게 된다.
 */
@Service
public class RecipeServiceImpl implements RecipeService {

    @Autowired
    private RecipeBook recipeBook; // recipes.json 을 읽어둔 것

    @Autowired
    private FoodCatalog foodCatalog; // catalog.json 을 읽어둔 것 (음식 분류, 그림)

    @Autowired
    private PriceService priceService; // 재료 가격

    // 못 먹는 재료. 이 기능이 DB 를 보는 유일한 곳이다.
    //
    // ban 기능의 BanService 가 아니라 DAO 를 바로 쓴다 - 필요한 것이
    // "이름 목록" 뿐이고, BanService 에는 그 모양으로 주는 메서드가 없다
    @Autowired
    private BanDAO banDAO;

    @Override
    public RecipeVO getRecipe(UserVO loginUser, String foodName) {
        // 앞뒤 공백을 떼어낸다. recipes.json 의 이름과 글자가 똑같아야 찾는다
        foodName = foodName.trim();
        // 컨트롤러의 @Size(min = 1) 은 공백만 있는 이름("  ")을 통과시킨다.
        // trim 한 뒤에 비면 여기서 막는다
        if (foodName.isEmpty()) {
            throw ApiException.unprocessable("음식 이름이 없습니다.");
        }

        // 1. 로그인했으면 못 먹는 재료를 가져온다
        // 빈 목록으로 시작한다. 비로그인이면 그대로 넘어가서, 아래 build 가
        // "뺄 재료가 없다" 로 받는다. null 을 넘기면 build 쪽에서 또 확인해야 한다
        List<String> allergies = new ArrayList<>();
        List<String> dislikes = new ArrayList<>();
        if (loginUser != null) {
            // 둘을 따로 읽는다. 알레르기는 대체재료로도 넣으면 안 되고,
            // 싫어함은 다른 것으로 바꿔주면 된다 - 취급이 달라서다
            allergies = banDAO.findActiveNames(loginUser.getId(), "ALLERGY");
            dislikes = banDAO.findActiveNames(loginUser.getId(), "DISLIKE");
        }

        // 2. 레시피 파일에서 꺼낸다 (못 먹는 재료는 빼거나 다른 재료로 바꿔준다)
        // 재료를 빼고 바꾸고 재료비를 계산하는 일은 RecipeBook 이 한다.
        // 그 계산이 길어서 util 로 뺐다
        RecipeBook.Built built = recipeBook.build(foodName, allergies, dislikes);
        // 레시피가 없는 음식이면 null 이 온다. 사용자가 주소에 아무 이름이나
        // 넣어볼 수 있으므로 정상적인 경우로 본다
        if (built == null) {
            // 문장에 음식 이름을 넣고 조사까지 맞춘다. "김치찌개은" 처럼
            // 어색하게 나가면 사용자가 서버 문장을 읽다 걸린다
            throw ApiException.notFound("'" + foodName + "'" + josa(foodName, "은", "는")
                    + " 준비된 레시피가 없는 메뉴입니다. 추천이나 검색에서 고른 메뉴로 다시 시도해주세요.");
        }

        // 3. 조리 정보(시간, 인분, 난이도, 순서)를 JSON 한 덩어리로 묶는다
        // 왜 묶나 — 이 덩어리가 그대로 POST /api/histories 의 recipe_json 으로
        // 되돌아간다. 화면이 받은 것을 그대로 보낼 수 있어야 해서, 저장할
        // 모양으로 미리 만들어 둔다.
        //
        // ObjectNode 는 "고칠 수 있는 JSON 객체" 다. JsonNode 는 읽기만 되므로
        // put 으로 칸을 넣으려면 이쪽이어야 한다
        ObjectNode recipeJson = JsonMapper.builder().build().createObjectNode();
        recipeJson.put("cook_time_minutes", built.getCookTimeMinutes());
        recipeJson.put("servings", built.getServings());
        recipeJson.put("difficulty", built.getDifficulty());
        // put 이 아니라 set 이다. 조리 순서는 숫자나 글자가 아니라 배열이라서
        // put 으로는 넣을 수 없다
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
        // recipes.json 에는 있지만 catalog.json 에는 없는 음식이 있을 수 있다.
        // 그때는 분류 · 그림 없이 레시피만 보여준다
        JsonNode food = foodCatalog.findFood(foodName);
        if (food != null) {
            // path 는 칸이 없어도 터지지 않는다. asString(null) 은
            // "값이 없으면 null" 이라는 뜻이다
            response.setCategory(food.path("category").asString(null));
            response.setArt(food.path("art").asString(null));
            // 사진은 get 으로 꺼내 직접 확인한다. 100개 중 사진이 없는 음식도
            // 있어서, 빈 글자가 아니라 아예 null 로 둬야 화면이 art 로 떨어진다
            JsonNode imageUrl = food.get("image_url");
            if (imageUrl != null && !imageUrl.isNull()) {
                response.setImageUrl(imageUrl.asString());
            }
        }

        // 5. 재료 가격을 붙인다 (정부 시세 API 를 부른다)
        // 맨 마지막에 부른다. 바깥 API 라 가장 느리고, 실패할 수도 있다.
        // 앞의 것들을 먼저 만들어 두면 가격만 비어도 레시피는 보여줄 수 있다
        //
        // built.getIngredients() 를 넘긴다 - 못 먹는 재료를 빼고 바꾼 뒤의
        // 목록이다. 원래 목록을 넘기면 먹지 않을 재료의 값까지 합산된다
        response.setPrices(priceService.forIngredients(built.getIngredients()));

        return response;
    }

    // 받침이 있으면 "은", 없으면 "는" 을 고른다.  예) 김치찌개는, 된장국은
    // 한글 글자는 0xAC00 부터 28개씩 묶여 있어서, 나머지가 0 이 아니면 받침이 있다.
    //
    // 왜 28개씩인가 — 한글 완성형은 (초성 19) x (중성 21) x (종성 28) 순서로
    // 늘어서 있다. 종성이 가장 안쪽이라 28로 나눈 나머지가 곧 종성이고,
    // 0 이면 받침이 없다는 뜻이다.
    //
    // "은/는" 만 쓰는데 인자로 받는 모양인 이유 — "이/가", "을/를" 도 규칙이
    // 같다. 나중에 쓸 자리가 생기면 이 메서드를 그대로 쓴다
    private String josa(String word, String withFinal, String withoutFinal) {
        char last = word.charAt(word.length() - 1);
        // 0xAC00 ~ 0xD7A3 이 한글 완성형이 들어 있는 범위다.
        // 영어 · 숫자로 끝나는 이름이면 "는" 쪽이 덜 어색하다
        if (last < 0xAC00 || last > 0xD7A3) {
            return withoutFinal; // 한글이 아니면
        }
        if ((last - 0xAC00) % 28 != 0) {
            return withFinal;
        }
        return withoutFinal;
    }
}
