package com.meokji.api.recommend.dto;

/**
 * 요즘 값이 내린 재료 한 건.
 *
 * <p><b>아직 채우지 않는다.</b> KAMIS 품목 116개를 훑어 일주일 전과 비교하는 일이라
 * 시세 조회를 옮긴 뒤에 붙인다. 지금은 {@code use_bargains} 를 켜도 빈 목록이 온다.
 *
 * <p>{@code percent} 는 내린 것만 담으므로 음수다.
 */
public record BargainItem(
        String name,
        double percent,
        int price,
        String unit,
        String surveyedAt) {
}
