package com.meokji.api.recommend.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 요즘 값이 내린 재료 한 건.
 *
 * POST /api/recommendations 에 use_bargains: true 를 실어 보내면
 * 채워진다. 대시보드의 「지금 싼 재료로」 카드만 그렇게 부른다.
 *
 * percent 는 내린 것만 담으므로 음수다. 화면은 이 값의 절댓값에
 * 화살표를 붙여 "대파 12%↓" 로 그린다.
 *
 * price 와 unit 은 짝이다. 품목마다 조사 단위가 달라서
 * ("1kg" · "10개") 단위 없이 가격만 보여주면 비교가 안 된다.
 *
 * surveyedAt 은 조사일자(YYYYMMDD)다. 주말·공휴일은 조사가 없어서
 * 며칠 전 값일 수 있다 - 지난주 가격을 오늘 가격으로 오해시키면 안 된다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BargainVO {

    /** 재료 이름. 예) "대파" */
    private String name;

    /**
     * 일주일 전보다 얼마나 변했는지(%). 내린 것만 담으므로 음수다.
     *
     * double 인 이유 — 12.5% 처럼 소수점이 나온다. 정수로 반올림하면
     * 5% 기준선 근처에서 "5% 내렸다는데 목록에 없는" 일이 생긴다
     */
    private double percent;

    /** 지금 가격(원). 아래 unit 당 가격이다 */
    private int price;

    /** 가격의 기준 단위. 예) "1kg", "10개" */
    private String unit;

    /** 조사일자(YYYYMMDD). 주말 · 공휴일은 조사가 없어 며칠 전 값일 수 있다 */
    private String surveyedAt;
}
