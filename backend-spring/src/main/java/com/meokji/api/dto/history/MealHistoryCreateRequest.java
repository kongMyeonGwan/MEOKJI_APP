package com.meokji.api.dto.history;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;
import tools.jackson.databind.JsonNode;

// 식사 기록 저장 요청 ("이걸로 먹을래요").
// 레시피 화면이 받은 레시피를 그대로 보낸다. JsonNode 는 "모양이 정해지지 않은 JSON" 을 담는 타입이다.
@Data
public class MealHistoryCreateRequest {

    @NotBlank(message = "음식 이름을 입력해주세요")
    @Size(max = 150, message = "음식 이름이 너무 깁니다")
    private String foodName;

    @Size(max = 200)
    private String recipeName;

    @NotNull(message = "레시피가 없습니다")
    private JsonNode recipeJson;

    @NotNull(message = "재료 목록이 없습니다")
    private JsonNode ingredientsJson;

    private JsonNode substitutesJson; // 대체 재료 (없어도 된다)

    @Min(value = 0, message = "예상 비용은 0원 이상이어야 합니다")
    @Max(value = 100000000, message = "예상 비용이 너무 큽니다")
    private Integer estimatedCost;
}
