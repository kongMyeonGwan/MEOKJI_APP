package com.meokji.api.dto.review;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 후기 쓰기.
 *
 * <p>{@code mealHistoryId} 가 필수다. <b>후기는 자기 식사 기록에서만 쓸 수 있다</b> -
 * 직접 만들어본 요리만 공유한다는 규칙을 FK 로 강제한다
 * ({@code recipe_reviews.meal_history_id} NOT NULL).
 *
 * <p>같은 기록으로 두 번 쓸 수 없다. {@code uq_reviews_meal} 이 막는다.
 */
public record ReviewCreateRequest(

        @NotNull(message = "어떤 식사 기록을 공유할지 골라주세요")
        @Min(value = 1, message = "식사 기록 번호가 올바르지 않습니다")
        Long mealHistoryId,

        @NotBlank(message = "제목을 입력해주세요")
        @Size(max = 200, message = "제목은 200자 이하여야 합니다")
        String title,

        @NotBlank(message = "내용을 입력해주세요")
        String content) {
}
