package com.meokji.api.review.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.meokji.api.history.entity.MealHistoryEntity;
import com.meokji.api.review.entity.ReviewEntity;
import com.meokji.api.util.Json;
import com.meokji.api.util.Times;
import lombok.Getter;
import tools.jackson.databind.JsonNode;

@Getter
public class ReviewDetailResponse {

    private final Long id;
    private final String title;
    private final Long meal_history_id;         // 먹은 레시피

    // ---------- 먹은 레시피 (MEAL_HISTORIES 에서 JOIN 으로 읽어온다) ----------
    private final String foodName;
    private final String recipeName;
    private final Integer estimatedCost;

    // 컬럼이 CLOB 이라 DB 에는 글자로 들어 있다. 화면은 recipeJson.steps 처럼
    // 객체로 읽으므로, 칸은 String 으로 두고 내보낼 때만 JSON 으로 바꾼다 (아래 getter)
    @JsonIgnore
    private final String recipeJson;

    @JsonIgnore
    private final String ingredientsJson;

    @JsonIgnore
    private final String substitutesJson;

    private final String content;               // 본문 전체
    private final String nickname;              // 글쓴이 닉네임
    private final Integer viewCount;
    private final Integer commentCount;         // 아래 comments 의 개수
    private final boolean hasFile;
    private final ReviewFileResponse file;        // 첨부가 없으면 null — 화면이 사진 자리를 안 그린다

    private final Boolean isMine;
    // (추가) 좋아요
    private final boolean liked;
    private final long likeCount;

    private final List<ReviewCommentResponse> comments; // 오래된 순
    private final String createdAt;
    private final String updatedAt;



    public ReviewDetailResponse(
            ReviewEntity review,
            MealHistoryEntity meal,
            String nickname,
            ReviewFileResponse file,
            List<ReviewCommentResponse> comments,
            boolean isMine,
            boolean liked,
            Long likeCount
            ) //
    {
        this.id = review.getId();
        this.title = review.getTitle();
        this.meal_history_id = review.getMeal_history_Id();

        // meal 은 JOIN 결과다. FK 가 NOT NULL 이라 정상적으로는 null 이 아니지만,
        // 한 건 때문에 상세가 통째로 500 이 되지 않게 비는 쪽으로 받는다
        this.foodName = meal == null ? null : meal.getFoodName();
        this.recipeName = meal == null ? null : meal.getRecipeName();
        this.estimatedCost = meal == null ? null : meal.getEstimatedCost();
        this.recipeJson = meal == null ? null : meal.getRecipeJson();
        this.ingredientsJson = meal == null ? null : meal.getIngredientsJson();
        this.substitutesJson = meal == null ? null : meal.getSubstitutesJson();
        this.content = review.getContent();
        this.nickname = nickname;
        this.viewCount = review.getViewCount() == null ? 0 :review.getViewCount();
        this.commentCount = comments.size();
        this.hasFile = review.hasFile();
        this.file = file;
        this.isMine = isMine;
        this.comments = comments;
        this.createdAt = Times.toText(review.getCreatedAt());
        this.updatedAt = Times.toText(review.getUpdatedAt());
        this.liked = liked;
        this.likeCount = likeCount;
        }

    // ---------- 레시피 JSON 세 칸 ----------
    // 칸은 String 인데(@JsonIgnore) 내보낼 때는 JSON 객체여야 한다.
    // 화면이 recipeJson.steps 처럼 읽기 때문이다 (MealHistoryVO 와 같은 방식).
    // 깨진 값이면 Json.node 가 null 을 주고, 화면은 그 칸만 안 그린다.

    @JsonProperty("recipeJson")
    public JsonNode getRecipeJsonNode() {
        return Json.node(recipeJson);
    }

    @JsonProperty("ingredientsJson")
    public JsonNode getIngredientsJsonNode() {
        return Json.node(ingredientsJson);
    }

    @JsonProperty("substitutesJson")
    public JsonNode getSubstitutesJsonNode() {
        return Json.node(substitutesJson);
    }
}
