package com.meokji.api.dto.review;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import tools.jackson.databind.JsonNode;

/**
 * 후기 상세.
 *
 * 레시피와 재료를 같이 준다. 후기를 읽은 사람이 바로 따라 만들 수 있어야 해서,
 * 글쓴이의 식사 기록(meal_histories)에 저장된 스냅샷을 붙여 보낸다.
 *
 * liked · isMine 은 지금 보는 사람 기준이다. 로그인하지 않았으면
 * 둘 다 false 다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewDetailResponse {

    private Long id;

    private String title;

    private String content;

    private String nickname;

    private int viewCount;

    private int likeCount;

    private String createdAt;

    private String foodName;

    private JsonNode recipeJson;

    private JsonNode ingredientsJson;

    private Long mealHistoryId;

    private ReviewFileInfo file;

    private boolean liked;

    private Boolean isMine;

    private List<CommentResponse> comments;
}
