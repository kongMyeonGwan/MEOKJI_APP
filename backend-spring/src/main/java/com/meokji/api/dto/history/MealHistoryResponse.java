package com.meokji.api.dto.history;

import lombok.Data;
import tools.jackson.databind.JsonNode;

// 식사 기록 한 건 응답
@Data
public class MealHistoryResponse {

    private Long id;
    private String foodName;
    private String recipeName;
    private JsonNode recipeJson;
    private JsonNode ingredientsJson;
    private JsonNode substitutesJson;
    private Integer estimatedCost;
    private String selectedAt; // 저장한 시각
    private Long sharedPostId; // 이 기록으로 쓴 커뮤니티 글 번호 (없으면 null)
    private String art; // 썸네일 그림 종류
}
