package com.meokji.api.recipe.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import tools.jackson.databind.JsonNode;

/**
 * 레시피 한 건 (아직 저장 전).
 *
 * 칸 이름이 recipeJson · ingredientsJson 처럼 어색한 것은,
 * 이 값이 그대로 POST /api/histories 로 되돌아가 DB 의 같은 이름 컬럼에
 * 저장되기 때문이다. 화면이 받은 것을 그대로 보낼 수 있어야 한다.
 *
 * prices 는 DB 와 무관하다. 매번 KAMIS 에서 받아온다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeResponse {

    private String foodName;

    private String recipeName;

    private JsonNode recipeJson;

    private JsonNode ingredientsJson;

    private JsonNode substitutesJson;

    private Integer estimatedCost;

    private String category;

    private String art;

    private String imageUrl;

    private List<PriceItem> prices;
}
