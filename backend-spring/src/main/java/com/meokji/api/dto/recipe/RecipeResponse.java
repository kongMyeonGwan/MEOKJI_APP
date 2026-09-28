package com.meokji.api.dto.recipe;

import java.util.List;

import tools.jackson.databind.JsonNode;

/**
 * 레시피 한 건 (아직 저장 전).
 *
 * <p>칸 이름이 {@code recipeJson} · {@code ingredientsJson} 처럼 어색한 것은,
 * 이 값이 그대로 {@code POST /api/histories} 로 되돌아가 DB 의 같은 이름 컬럼에
 * 저장되기 때문이다. 화면이 받은 것을 그대로 보낼 수 있어야 한다.
 *
 * <p>{@code prices} 는 DB 와 무관하다. 매번 KAMIS 에서 받아온다.
 */
public record RecipeResponse(
        String foodName,
        String recipeName,
        JsonNode recipeJson,
        JsonNode ingredientsJson,
        JsonNode substitutesJson,
        Integer estimatedCost,
        String category,
        String art,
        String imageUrl,
        List<PriceItem> prices) {
}
