package com.meokji.api.dto.recipe;

import java.util.List;

import tools.jackson.databind.JsonNode;

/**
 * 재료 한 줄의 가격.
 *
 * <p>{@code name} 은 "돼지고기 300g" 처럼 <b>필요한 분량이 붙은</b> 이름이다.
 * 화면이 재료 목록과 가격표를 따로 읽지 않게 서버가 만들어 준다.
 *
 * <p>{@code surveyed} 가 값이 없는 이유를 가려준다.
 *
 * <pre>
 *   surveyed = true,  offers 비었음   이번에 못 받았다 (호출 제한 · 통신 오류)
 *   surveyed = false                  조사 대상이 아니다 (가공식품). 앞으로도 없다
 * </pre>
 *
 * <p>이 구분이 필요했던 이유가 있다. 전에는 합계가 모자랄 때마다 "가공식품은 조사
 * 대상이 아니라 가격이 없습니다" 를 띄웠는데, 호출 제한으로 값이 빈 날에도 그렇게
 * 말하니 사실이 아니었다(STEP 39).
 *
 * <p>{@code minUnit} 은 쪼개 팔지 않는 재료를 최소단위로 올렸다는 표시다
 * ("무 0.2개 필요 -> 1개 구입"). 합계가 레시피 분량보다 커지므로 왜 큰지 밝혀야
 * 한다 - 안 그러면 계산이 틀린 것처럼 읽힌다(STEP 42).
 */
public record PriceItem(
        String name,
        List<PriceOffer> offers,
        JsonNode trend,
        String needText,
        Integer lowCost,
        Integer highCost,
        boolean minUnit,
        String buyText,
        boolean surveyed) {
}
