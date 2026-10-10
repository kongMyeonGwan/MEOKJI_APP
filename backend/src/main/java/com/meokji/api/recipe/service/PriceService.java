package com.meokji.api.recipe.service;

import java.util.List;

import com.meokji.api.recipe.vo.PriceItemVO;

import tools.jackson.databind.JsonNode;

/**
 * 재료 가격 (정부 KAMIS 시장 조사가격). 실제 코드는 PriceServiceImpl 에 있다.
 *
 * <p>왜 쇼핑몰 최저가가 아닌가 — 쇼핑몰 가격을 쓰려면 그 쪽 제휴 API 가
 * 필요하고, 값도 파는 사람마다 달라서 "이 재료가 대략 얼마" 를 보여주는
 * 데는 맞지 않는다. KAMIS 는 정부가 시장을 돌며 조사한 가격이라 기준이
 * 하나다. 대신 조사 대상이 아닌 품목(가공식품)은 값이 없다.
 *
 * <p>RecipeService 와 따로 둔 이유 — 바깥 API 를 부르는 일이라 호출 제한 ·
 * 통신 오류 · 단위 변환 같은 사정이 따라온다. 레시피를 찾는 일과 섞으면
 * 한 클래스가 둘을 다 들고 있게 된다.
 */
public interface PriceService {

    /**
     * 레시피 재료 목록으로 재료별 가격을 가져온다
     *
     * @param ingredientsJson [{ "name": "돼지고기", "quantity": 300, "unit": "g", "grams": 300 }, ...]
     * @return 재료 하나당 한 줄. 값을 못 받은 재료도 빠지지 않고 들어간다 -
     *         빠지면 화면의 재료 목록과 가격표의 줄 수가 어긋난다.
     *         왜 값이 없는지는 PriceItemVO 의 surveyed 가 알려준다
     */
    List<PriceItemVO> forIngredients(JsonNode ingredientsJson);
}
