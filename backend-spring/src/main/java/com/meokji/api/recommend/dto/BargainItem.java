package com.meokji.api.recommend.dto;

/**
 * 요즘 값이 내린 재료 한 건.
 *
 * <p>{@code POST /api/recommendations} 에 {@code use_bargains: true} 를 실어 보내면
 * 채워진다. 대시보드의 「지금 싼 재료로」 카드만 그렇게 부른다.
 *
 * <p>{@code percent} 는 내린 것만 담으므로 <b>음수</b>다. 화면은 이 값의 절댓값에
 * 화살표를 붙여 "대파 12%↓" 로 그린다.
 *
 * <p>{@code price} 와 {@code unit} 은 짝이다. 품목마다 조사 단위가 달라서
 * ("1kg" · "10개") 단위 없이 가격만 보여주면 비교가 안 된다.
 *
 * <p>{@code surveyedAt} 은 조사일자(YYYYMMDD)다. 주말·공휴일은 조사가 없어서
 * 며칠 전 값일 수 있다 - 지난주 가격을 오늘 가격으로 오해시키면 안 된다.
 */
public record BargainItem(
        String name,
        double percent,
        int price,
        String unit,
        String surveyedAt) {
}
