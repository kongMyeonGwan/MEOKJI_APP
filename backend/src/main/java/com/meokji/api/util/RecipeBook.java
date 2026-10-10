package com.meokji.api.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
 * 왜 AI 를 안 부르나
 *
 * 카탈로그 100종은 목록이 고정돼 있다. 바뀌지 않는 음식의 레시피를 열 때마다
 * 새로 만들면 느리고(7~10초), 열 때마다 재료와 순서가 달라지고, 돈이 든다.
 * 어제 보고 오늘 다시 연 사람에게는 다른 요리가 된다.
 *
 * 대신 치르는 것 — 카탈로그에 없는 음식(사용자가 직접 검색한 이름)에는
 * 레시피가 없다. 그때는 RecipeServiceImpl 이 404 를 던지고, 화면이
 * "추천이나 검색에서 고른 메뉴로 다시 시도해주세요" 를 띄운다.
 *
 * 여기서 하는 일이 둘이다
 *   1. 파일에서 레시피 한 건을 꺼낸다
 *   2. 못 먹는 재료를 빼거나 바꿀 방법을 적는다 (아래 build)
 *
 * 2번을 서비스가 아니라 여기서 하는 이유 — 재료 목록을 손보면서
 * 그램도 다시 계산해야 한다(IngredientRules). 레시피를 아는 쪽에서
 * 같이 하는 편이 낫다.
 */
@Component
public class RecipeBook {

    private static final String PATH = "data/recipes.json";

    // static final 로 하나만 둔다. 만드는 일이 가볍지 않고, 만들고 나면
    // 상태가 바뀌지 않아 같이 써도 안전하다
    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    /** 음식 이름 -> 미리 써둔 레시피. */
    // 파일이 336KB 다. 서버가 뜰 때 한 번 읽어 들고 있는다 -
    // 레시피를 열 때마다 다시 읽고 파싱할 이유가 없다
    private final Map<String, JsonNode> recipes = new LinkedHashMap<>();

    // 그램 계산과 대체재료를 맡는다. 재료를 손보는 데 둘 다 필요하다
    @Autowired
    private IngredientRules rules;

    /**
     * Ban 을 반영해 다듬은 레시피 한 건.
     *
     * @param steps           조리 순서. 알레르기 재료를 뺐으면 맨 앞에 안내가 붙는다
     * @param ingredients     [{name, quantity, unit, grams}, ...]
     * @param substitutes     [{original, substitute, reason}, ...]
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Built {

        private String recipeName;

        private int cookTimeMinutes;

        private int servings;

        private String difficulty;

        private int estimatedCost;

        private ArrayNode steps;

        private ArrayNode ingredients;

        private ArrayNode substitutes;
    }

    /** 서버가 뜰 때 한 번 파일을 읽어 메모리에 올린다. */
    @PostConstruct
    void load() {
        try (InputStream in = new ClassPathResource(PATH).getInputStream()) {
            JsonNode root = MAPPER.readTree(in);

            root.path("recipes").properties()
                    .forEach(entry -> recipes.put(entry.getKey(), entry.getValue()));
        } catch (Exception exc) {
            // 이 파일이 없으면 레시피를 아무것도 못 보여준다. 조용히
            // 빈 목록으로 뜨면 모든 음식이 404 가 되므로 시작 자체를 막는다
            throw new IllegalStateException(PATH + " 을 읽지 못했습니다.", exc);
        }

        // 파일은 읽었는데 안이 비어 있는 경우. 읽기가 성공했으니
        // 위 catch 에 안 걸리지만 결과는 같다
        if (recipes.isEmpty()) {
            throw new IllegalStateException(PATH + " 의 recipes 가 비어 있습니다.");
        }
    }

    /** 들고 있는 레시피 수. 파일 복사가 어긋났는지 확인할 때 쓴다. */
    // 테스트(DataFileTest)가 쓴다. 100 이 아니면 파일이 덜 복사된 것이다
    public int count() {
        return recipes.size();
    }

    /** 미리 써둔 레시피가 있는 음식인가. */
    // trim 하는 이유 — 파일의 이름과 글자가 똑같아야 찾는다.
    // " 김치찌개" 로 물으면 못 찾는다
    public boolean has(String foodName) {
        return foodName != null && recipes.containsKey(foodName.trim());
    }

    /**
     * 미리 써둔 레시피를 Ban 재료에 맞춰 다듬어 돌려준다. 없으면 null.
     *
     * 알레르기 재료는 목록에서 아예 뺀다. 싫어하는 재료는 그대로 두되
     * 바꿀 방법을 함께 적는다 - 빼버리면 요리가 성립하지 않을 수 있고, 대체재료를
     * 알려주면 사용자가 정할 수 있다.
     */
    public Built build(String foodName, List<String> allergies, List<String> dislikes) {
        // null 이면 빈 글자로 찾는다. get(null) 은 LinkedHashMap 에서
        // 터지지 않지만, 찾는 열쇠를 null 로 두는 모양을 남기지 않는다
        JsonNode found = recipes.get(foodName == null ? "" : foodName.trim());

        // 레시피가 없는 음식. 사용자가 주소에 아무 이름이나 넣어볼 수
        // 있으므로 정상적인 경우로 본다 (예외가 아니다)
        if (found == null) {
            return null;
        }

        // 파일의 레시피를 고치지 않고 새 배열에 담는다. 파일에서 읽은
        // JsonNode 를 건드리면 메모리에 들고 있는 원본이 바뀌어,
        // 다음 사람이 재료가 빠진 레시피를 받는다
        ArrayNode ingredients = MAPPER.createArrayNode();
        ArrayNode substitutes = MAPPER.createArrayNode();
        // 뺀 재료 이름. 조리 순서 맨 앞에 안내를 붙이는 데 쓴다
        List<String> removed = new ArrayList<>();

        for (JsonNode row : found.path("ingredients")) {
            String name = row.path("name").asString();

            // 알레르기는 아예 뺀다. continue 라서 아래 ingredients 에
            // 담기지도 않고, 대체재료도 제안하지 않는다 - 대체재료에
            // 같은 성분이 들어 있을 수 있다
            if (IngredientRules.matchesAny(name, allergies)) {
                removed.add(name);
                continue;
            }

            // 싫어하는 재료는 목록에 그대로 두고 바꿀 방법만 적는다.
            // 빼버리면 요리가 성립하지 않을 수 있고, 사용자가 "그냥 먹겠다" 를
            // 고를 수도 있어야 한다
            if (IngredientRules.matchesAny(name, dislikes)) {
                IngredientRules.Substitute replacement = rules.substituteFor(name);

                ObjectNode substitute = substitutes.addObject();
                substitute.put("original", name);
                substitute.put("substitute", replacement.getSubstitute());
                substitute.put("reason", replacement.getReason());
            }

            JsonNode quantity = row.path("quantity");
            String unit = row.path("unit").asString();

            // addObject 는 배열에 새 객체를 넣으면서 그 객체를 돌려준다.
            // 따로 만들어 add 하는 것보다 한 줄 짧다
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

        // 파일의 순서를 그대로 옮긴다. 뺀 재료가 나오는 단계를 찾아
        // 고치지 않는 이유 — 글을 자동으로 고치면 어색해지고,
        // "넣지 말고 넘어가세요" 라고 미리 알려주는 쪽이 분명하다
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
     * 그램을 넣는다. 정수와 소수를 가려서 넣는다.
     *
     * 화면에서는 같은 값이지만, 두 서버의 응답을 기계적으로 비교하는 동안에는
     * 글자가 달라지면 매번 사람이 눈으로 확인해야 한다.
     */
    private void putGrams(ObjectNode item, JsonNode quantity, String unit, String name) {
        // 재료 이름까지 넘긴다. "고등어 1마리" 는 재료별 무게를 봐야 하고,
        // "간장 2큰술" 은 단위별 무게로 충분하다 (IngredientRules 참고)
        double grams = rules.gramsFor(name, quantity.asDouble(), unit);

        // 분량이 정수면 그램도 정수로 적는다. 1마리 -> 400 (400.0 이 아니라).
        // 소수면 소수로 둔다 - 0.5개 -> 150.0 처럼 계산 결과가 소수일 수 있다
        if (quantity.isIntegralNumber()) {
            item.put("grams", Math.round(grams));
        } else {
            item.put("grams", grams);
        }
    }
}
