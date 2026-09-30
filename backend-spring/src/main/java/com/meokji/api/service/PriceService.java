package com.meokji.api.service;

import java.util.List;

import com.meokji.api.dto.recipe.PriceItem;

import tools.jackson.databind.JsonNode;

/** 재료 가격 (정부 KAMIS 시장 조사가격). 실제 코드는 PriceServiceImpl 에 있다. */
public interface PriceService {

    // 레시피 재료 목록으로 재료별 가격을 가져온다
    //   ingredientsJson : [{ "name": "돼지고기", "quantity": 300, "unit": "g", "grams": 300 }, ...]
    List<PriceItem> forIngredients(JsonNode ingredientsJson);
}
