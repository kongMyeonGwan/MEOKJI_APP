package com.meokji.api.dto.review;

import java.util.List;

import tools.jackson.databind.JsonNode;

/**
 * 후기 상세.
 *
 * <p>레시피와 재료를 같이 준다. 후기를 읽은 사람이 바로 따라 만들 수 있어야 해서,
 * 글쓴이의 식사 기록({@code meal_histories})에 저장된 스냅샷을 붙여 보낸다.
 *
 * <p>{@code liked} · {@code isMine} 은 지금 보는 사람 기준이다. 로그인하지 않았으면
 * 둘 다 false 다.
 */
public record ReviewDetailResponse(
        Long id,
        String title,
        String content,
        String nickname,
        int viewCount,
        int likeCount,
        String createdAt,
        String foodName,
        JsonNode recipeJson,
        JsonNode ingredientsJson,
        Long mealHistoryId,
        ReviewFileInfo file,
        boolean liked,
        boolean isMine,
        List<CommentResponse> comments) {
}
