package com.meokji.api.recipe.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 한 시장의 조사가격.
 *
 * 쇼핑몰 최저가가 아니라 소매시장 조사가격이다(KAMIS). 그래서 구매 링크가
 * 없고, link 는 사용자가 값을 직접 확인할 수 있는 KAMIS 가격 페이지다.
 *
 * unit 을 같이 주는 이유는 품목마다 기준이 달라서다 (쌀 20kg / 깻잎 100g).
 * 단위 없이 숫자만 보여주면 깻잎이 쌀보다 비싸 보인다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PriceOfferVO {

    /** 조사한 시장 이름. 쇼핑몰이 아니다 */
    private String shop;

    /**
     * 그 시장의 조사가격(원). 아래 unit 당 가격이다.
     *
     * int 인 이유 — KAMIS 가 원 단위 정수로 준다. 소수점이 없다
     */
    private int price;

    /** 사용자가 값을 직접 확인할 수 있는 KAMIS 가격 페이지. 구매 링크가 아니다 */
    private String link;

    /** 가격의 기준 단위. 예) "20kg", "100g" */
    private String unit;

    /**
     * 조사한 날. 글자로 준다 (예: "2026-10-10")
     *
     * 같이 보내는 이유 — 시세는 날마다 바뀐다. 언제 조사한 값인지
     * 안 보여주면 사용자는 오늘 가격으로 읽는다. 호출 제한에 걸린 날은
     * 며칠 전 값이 올 수도 있다.
     */
    private String surveyedAt;
}
