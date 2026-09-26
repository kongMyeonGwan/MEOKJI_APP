package com.meokji.api.history.dto;

import tools.jackson.databind.JsonNode;

/**
 * 식사 기록 한 건.
 *
 * <p>{@code userId} 는 내려주지 않는다. "내 기록" 만 조회되므로 필요 없다.
 *
 * <p>{@code sharedPostId} 와 {@code art} 는 DB 컬럼이 아니다. 앞은 후기와 JOIN 해서
 * 계산하고, 뒤는 카탈로그에서 가져온다. 칸 순서는 파이썬 응답과 같게 둔다.
 */
public record MealHistoryResponse(
        Long id,
        String foodName,
        String recipeName,
        JsonNode recipeJson,
        JsonNode ingredientsJson,
        JsonNode substitutesJson,
        Integer estimatedCost,
        String selectedAt,
        Long sharedPostId,
        String art) {
}
