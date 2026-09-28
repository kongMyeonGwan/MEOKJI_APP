package com.meokji.api.dto.history;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import tools.jackson.databind.JsonNode;

/**
 * '이걸로 먹을래요' 를 눌렀을 때 보내는 값.
 *
 * <p>JSON 세 칸을 {@link JsonNode} 로 받는다. DDL 이 CLOB + {@code CHECK(... IS JSON)}
 * 이라 모양이 깨지면 DB 가 거부하는데, 그러면 500 이 난다. 여기서 형을 먼저 확인해
 * 422 로 끊는다.
 */
public record MealHistoryCreateRequest(

        @NotBlank(message = "음식 이름을 입력해주세요")
        @Size(max = 150, message = "음식 이름이 너무 깁니다")
        String foodName,

        @Size(max = 200) String recipeName,

        @NotNull(message = "레시피가 없습니다")
        JsonNode recipeJson,

        @NotNull(message = "재료 목록이 없습니다")
        JsonNode ingredientsJson,

        JsonNode substitutesJson,

        @Min(value = 0, message = "예상 비용은 0원 이상이어야 합니다")
        @Max(value = 100_000_000, message = "예상 비용이 너무 큽니다")
        Integer estimatedCost) {

    /** 앞뒤 공백을 떼어낸 음식 이름. */
    public String trimmedFoodName() {
        return foodName == null ? null : foodName.trim();
    }
}
