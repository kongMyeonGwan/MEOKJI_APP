package com.meokji.api.recipe.dto;

/**
 * 한 시장의 조사가격.
 *
 * <p>쇼핑몰 최저가가 아니라 <b>소매시장 조사가격</b>이다(KAMIS). 그래서 구매 링크가
 * 없고, {@code link} 는 사용자가 값을 직접 확인할 수 있는 KAMIS 가격 페이지다.
 *
 * <p>{@code unit} 을 같이 주는 이유는 품목마다 기준이 달라서다 (쌀 20kg / 깻잎 100g).
 * 단위 없이 숫자만 보여주면 깻잎이 쌀보다 비싸 보인다.
 */
public record PriceOffer(
        String shop,
        int price,
        String link,
        String unit,
        String surveyedAt) {
}
