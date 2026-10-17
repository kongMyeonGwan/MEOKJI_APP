package com.meokji.api.recipe.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import tools.jackson.databind.JsonNode;

/**
 * 재료 한 줄의 가격.
 *
 * name 은 "돼지고기 300g" 처럼 필요한 분량이 붙은 이름이다.
 * 화면이 재료 목록과 가격표를 따로 읽지 않게 서버가 만들어 준다.
 *
 * surveyed 가 값이 없는 이유를 가려준다.
 *
 *   surveyed = true,  offers 비었음   이번에 못 받았다 (호출 제한 · 통신 오류)
 *   surveyed = false                  조사 대상이 아니다 (가공식품). 앞으로도 없다
 *
 * 이 구분이 필요했던 이유가 있다. 전에는 합계가 모자랄 때마다 "가공식품은 조사
 * 대상이 아니라 가격이 없습니다" 를 띄웠는데, 호출 제한으로 값이 빈 날에도 그렇게
 * 말하니 사실이 아니었다(STEP 39).
 *
 * minUnit 은 쪼개 팔지 않는 재료를 최소단위로 올렸다는 표시다
 * ("무 0.2개 필요 -> 1개 구입"). 합계가 레시피 분량보다 커지므로 왜 큰지 밝혀야
 * 한다 - 안 그러면 계산이 틀린 것처럼 읽힌다(STEP 42).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceItemVO {

    /** "돼지고기 300g" 처럼 분량이 붙은 이름. 화면에 그대로 띄운다 */
    private String name;

    /**
     * 시장별 조사가격. 값을 못 받았으면 빈 목록이다.
     *
     * null 이 아니라 빈 목록인 이유 — 화면이 offers.map 을 바로 쓸 수 있다
     */
    private List<PriceOfferVO> offers;

    /**
     * 최근 시세 흐름. 화면이 작은 그래프를 그리는 데 쓴다.
     *
     * JsonNode 인 이유 — KAMIS 가 준 모양을 그대로 전달한다.
     * 서버가 안쪽을 들여다보지 않으므로 VO 로 풀어 적을 이유가 없다
     */
    private JsonNode trend;

    /** 레시피가 요구하는 분량. 예) "300g 필요" */
    private String needText;

    /**
     * 가장 싼 시장 기준 금액(원), 가장 비싼 시장 기준 금액(원).
     *
     * 하나의 값이 아니라 범위로 주는 이유 — 시장마다 값이 꽤 다르다.
     * 평균 하나만 보여주면 "이 가격에 살 수 있다" 로 읽히는데 그렇지 않다.
     *
     * Integer 인 이유는 값을 못 받았을 때 null 이어야 하기 때문이다.
     * 0 으로 두면 공짜 재료로 보인다
     */
    private Integer lowCost;

    private Integer highCost;

    /**
     * 최소단위로 올렸는지. true 면 레시피 분량보다 많이 사는 것이다.
     *
     * 화면이 이 표시를 보고 사유를 덧붙인다. 안 밝히면 합계가 커진 것이
     * 계산 실수처럼 읽힌다
     */
    private boolean minUnit;

    /** 실제로 사야 하는 양. 예) "무 1개 구입" */
    private String buyText;

    /**
     * KAMIS 조사 대상 품목인지.
     *
     * false 면 앞으로도 값이 안 나온다(가공식품 등). true 인데 offers 가
     * 비어 있으면 이번에만 못 받은 것이다 - 클래스 주석의 표를 참고
     */
    private boolean surveyed;
}
