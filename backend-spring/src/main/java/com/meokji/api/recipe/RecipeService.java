package com.meokji.api.recipe;

import java.util.List;

import org.springframework.stereotype.Service;

import com.meokji.api.ban.BanMapper;
import com.meokji.api.catalog.FoodCatalog;
import com.meokji.api.common.ApiException;
import com.meokji.api.recipe.dto.PriceItem;
import com.meokji.api.recipe.dto.RecipeResponse;
import com.meokji.api.user.User;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ObjectNode;

/**
 * 레시피 한 건을 만들어 준다.
 *
 * <p>파이썬 쪽 {@code app/services/ai_service.py} 의 {@code make_recipe} 자리다.
 *
 * <h2>캐시를 두지 않았다</h2>
 *
 * <p>파이썬에는 6시간짜리 레시피 캐시가 있다. AI 를 부르던 시절에 7~10초를 아끼려고
 * 둔 것인데, 지금은 미리 읽어둔 파일에서 꺼내는 일이라 캐시로 아낄 것이 없다.
 * 느린 쪽은 KAMIS 시세이고 그건 {@code price} 쪽에 따로 캐시가 있다.
 */
@Service
public class RecipeService {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private final RecipeBook recipeBook;
    private final FoodCatalog catalog;
    private final BanMapper bans;

    public RecipeService(RecipeBook recipeBook, FoodCatalog catalog, BanMapper bans) {
        this.recipeBook = recipeBook;
        this.catalog = catalog;
        this.bans = bans;
    }

    /**
     * 음식 이름으로 레시피를 만든다. 가격은 붙이지 않는다 - 부르는 쪽이 붙인다.
     *
     * @throws ApiException 카탈로그에 없는 음식이면 404
     */
    public RecipeResponse make(User me, String rawFoodName) {
        String foodName = rawFoodName == null ? "" : rawFoodName.trim();

        if (foodName.isEmpty()) {
            throw ApiException.unprocessable("음식 이름이 없습니다.");
        }

        List<String> allergies = banNames(me, "ALLERGY");
        List<String> dislikes = banNames(me, "DISLIKE");

        RecipeBook.Built built = recipeBook.build(foodName, allergies, dislikes);

        if (built == null) {
            // 지어내지 않는다. 빈 화면보다 틀린 레시피가 나쁘다 - 요리는 따라
            // 만드는 글이라 사실이 아닌 순서는 재료를 실제로 버리게 한다.
            throw ApiException.notFound(
                    "'" + foodName + "'" + josa(foodName, "은", "는")
                    + " 준비된 레시피가 없는 메뉴입니다. "
                    + "추천이나 검색에서 고른 메뉴로 다시 시도해주세요.");
        }

        // 조리 정보는 한 덩어리로 묶어 보낸다. 이 값이 그대로
        // POST /api/histories 의 recipe_json 으로 되돌아간다.
        ObjectNode recipeJson = MAPPER.createObjectNode();
        recipeJson.put("cook_time_minutes", built.cookTimeMinutes());
        recipeJson.put("servings", built.servings());
        recipeJson.put("difficulty", built.difficulty());
        recipeJson.set("steps", built.steps());

        JsonNode known = catalog.findFood(foodName);

        return new RecipeResponse(
                foodName,
                built.recipeName(),
                recipeJson,
                built.ingredients(),
                built.substitutes(),
                built.estimatedCost(),
                known == null ? null : known.path("category").asString(null),
                known == null ? null : known.path("art").asString(null),
                known == null ? null : textOrNull(known.get("image_url")),
                // 가격은 컨트롤러가 붙인다. 시세 조회는 바깥을 부르는 일이라
                // 레시피 만들기와 섞지 않는다.
                List.of());
    }

    /** 만든 레시피에 가격만 갈아 끼운다. */
    public static RecipeResponse withPrices(RecipeResponse recipe, List<PriceItem> prices) {
        return new RecipeResponse(
                recipe.foodName(),
                recipe.recipeName(),
                recipe.recipeJson(),
                recipe.ingredientsJson(),
                recipe.substitutesJson(),
                recipe.estimatedCost(),
                recipe.category(),
                recipe.art(),
                recipe.imageUrl(),
                prices);
    }

    private List<String> banNames(User me, String banType) {
        if (me == null) {
            return List.of();
        }

        return bans.findActiveNames(me.getId(), banType);
    }

    private static String textOrNull(JsonNode node) {
        return node == null || node.isNull() ? null : node.asString();
    }

    /**
     * 받침에 맞는 조사를 고른다.
     *
     * <p>"돼지고기 을(를)" 처럼 둘 다 적으면 기계가 쓴 글로 읽힌다. 한글 음절은
     * {@code 0xAC00} 부터 28개씩 묶여 있어서, 그 나머지가 0 이 아니면 받침이 있다.
     */
    static String josa(String word, String withFinal, String withoutFinal) {
        if (word == null || word.isEmpty()) {
            return withoutFinal;
        }

        char last = word.charAt(word.length() - 1);

        if (last < 0xAC00 || last > 0xD7A3) {
            // 한글이 아니면 받침을 알 수 없다. 영문·숫자로 끝나는 이름은
            // 조사를 붙이지 않는 쪽이 덜 어색하다.
            return withoutFinal;
        }

        return (last - 0xAC00) % 28 != 0 ? withFinal : withoutFinal;
    }
}
