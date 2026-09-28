package com.meokji.api.dto.review;

/**
 * 인기 레시피 한 줄. 기간 안에 좋아요를 많이 받은 순.
 *
 * <p>기간은 한국 날짜로 받아 UTC 로 바꿔 조회한다. 이 9시간을 안 맞추면 경계에서
 * 결과가 어긋난다 - 새벽에 눌린 좋아요가 전날 것으로 세어진다.
 */
public record PopularItem(
        Long id,
        String foodName,
        String recipeName,
        String nickname,
        int likeCount,
        Integer estimatedCost) {
}
