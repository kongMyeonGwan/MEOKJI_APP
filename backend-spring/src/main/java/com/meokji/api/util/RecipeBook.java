package com.meokji.api.util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;


import jakarta.annotation.PostConstruct;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * 손으로 쓴 레시피 100종을 꺼내 쓴다 (STEP 41).
 *
 * <p>파이썬 쪽 {@code app/integrations/recipe_book.py} 자리다. 조리 순서를 만드는
 * 코드({@code steps.py} 1,004줄)는 옮기지 않았다 - {@code recipes.json} 에 순서가
 * 이미 들어 있고, 그 파일을 만들 때 쓴 코드라 조회에는 필요하지 않다.
 *
 * <h2>왜 AI 를 안 부르나</h2>
 *
 * <p>카탈로그 100종은 목록이 고정돼 있다. 바뀌지 않는 음식의 레시피를 열 때마다
 * 새로 만들면 느리고(7~10초), 열 때마다 재료와 순서가 달라지고, 돈이 든다.
 * 어제 보고 오늘 다시 연 사람에게는 다른 요리가 된다.
 */
@Component
public class RecipeBook {

    private static final String PATH = "data/recipes.json";

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    /** 음식 이름 -> 미리 써둔 레시피. */
    private final Map<String, JsonNode> recipes = new LinkedHashMap<>();

    @Autowired
    private IngredientRules rules;


    /**
     * Ban 을 반영해 다듬은 레시피 한 건.
     *
     * @param steps           조리 순서. 알레르기 재료를 뺐으면 맨 앞에 안내가 붙는다
     * @param ingredients     [{name, quantity, unit, grams}, ...]
     * @param substitutes     [{original, substitute, reason}, ...]
     */
    public record Built(
            String recipeName,
            int cookTimeMinutes,
            int servings,
            String difficulty,
            int estimatedCost,
            ArrayNode steps,
            ArrayNode ingredients,
            ArrayNode substitutes) {
    }

    @PostConstruct
    void load() {
        try (InputStream in = new ClassPathResource(PATH).getInputStream()) {
            JsonNode root = MAPPER.readTree(in);

            root.path("recipes").properties()
                    .forEach(entry -> recipes.put(entry.getKey(), entry.getValue()));
        } catch (Exception exc) {
            throw new IllegalStateException(PATH + " 을 읽지 못했습니다.", exc);
        }

        if (recipes.isEmpty()) {
            throw new IllegalStateException(PATH + " 의 recipes 가 비어 있습니다.");
        }
    }

    /** 들고 있는 레시피 수. 파일 복사가 어긋났는지 확인할 때 쓴다. */
    public int count() {
        return recipes.size();
    }

    /** 미리 써둔 레시피가 있는 음식인가. */
    public boolean has(String foodName) {
        return foodName != null && recipes.containsKey(foodName.trim());
    }

    /**
     * 미리 써둔 레시피를 Ban 재료에 맞춰 다듬어 돌려준다. 없으면 null.
     *
     * <p>알레르기 재료는 목록에서 <b>아예 뺀다.</b> 싫어하는 재료는 그대로 두되
     * 바꿀 방법을 함께 적는다 - 빼버리면 요리가 성립하지 않을 수 있고, 대체재료를
     * 알려주면 사용자가 정할 수 있다.
     */
    public Built build(String foodName, List<String> allergies, List<String> dislikes) {
        JsonNode found = recipes.get(foodName == null ? "" : foodName.trim());

        if (found == null) {
            return null;
        }

        ArrayNode ingredients = MAPPER.createArrayNode();
        ArrayNode substitutes = MAPPER.createArrayNode();
        List<String> removed = new ArrayList<>();

        for (JsonNode row : found.path("ingredients")) {
            String name = row.path("name").asString();

            if (IngredientRules.matchesAny(name, allergies)) {
                removed.add(name);
                continue;
            }

            if (IngredientRules.matchesAny(name, dislikes)) {
                IngredientRules.Substitute replacement = rules.substituteFor(name);

                ObjectNode substitute = substitutes.addObject();
                substitute.put("original", name);
                substitute.put("substitute", replacement.substitute());
                substitute.put("reason", replacement.reason());
            }

            JsonNode quantity = row.path("quantity");
            String unit = row.path("unit").asString();

            ObjectNode item = ingredients.addObject();
            item.put("name", name);
            // 분량은 파일에 적힌 값을 그대로 옮긴다. 정수면 정수로, 소수면 소수로.
            item.set("quantity", quantity);
            item.put("unit", unit);
            putGrams(item, quantity, unit, name);
        }

        ArrayNode steps = MAPPER.createArrayNode();

        // 뺀 재료가 있으면 순서 맨 앞에 밝힌다. 순서 중간에 그 재료가 나오는데
        // 목록에 없으면 따라 만드는 사람이 멈춘다.
        if (!removed.isEmpty()) {
            steps.add("알레르기 재료 " + String.join(" · ", removed)
                    + "을(를) 뺀 레시피입니다. 아래 순서에 이 재료가 나오면 넣지 말고 "
                    + "다음 단계로 넘어가세요.");
        }

        for (JsonNode step : found.path("steps")) {
            steps.add(step.asString());
        }

        return new Built(
                found.path("recipe_name").asString(),
                found.path("cook_time_minutes").asInt(),
                found.path("servings").asInt(),
                found.path("difficulty").asString(),
                found.path("estimated_cost").asInt(),
                steps,
                ingredients,
                substitutes);
    }

    /**
     * 그램을 넣는다. <b>정수와 소수를 가려서 넣는다.</b>
     *
     * <p>파이썬이 그렇게 내보내고 있어서 맞춘다. {@code round(1 * 80, 1)} 은 둘 다
     * 정수라 {@code 80} 이고, {@code round(0.25 * 2500, 1)} 은 {@code 625.0} 이다.
     * 언제나 소수로 내보내면 {@code "grams": 80.0} 이 되어 정답지와 글자가 달라진다.
     *
     * <p>화면에서는 같은 값이지만, 두 서버의 응답을 기계적으로 비교하는 동안에는
     * 글자가 달라지면 매번 사람이 눈으로 확인해야 한다.
     */
    private void putGrams(ObjectNode item, JsonNode quantity, String unit, String name) {
        double grams = rules.gramsFor(name, quantity.asDouble(), unit);

        if (quantity.isIntegralNumber()) {
            item.put("grams", Math.round(grams));
        } else {
            item.put("grams", grams);
        }
    }
}
