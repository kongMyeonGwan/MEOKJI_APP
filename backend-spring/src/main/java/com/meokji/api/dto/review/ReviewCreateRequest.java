package com.meokji.api.dto.review;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 후기 쓰기.
 *
 * mealHistoryId 가 필수다. 후기는 자기 식사 기록에서만 쓸 수 있다 -
 * 직접 만들어본 요리만 공유한다는 규칙을 FK 로 강제한다
 * (recipe_reviews.meal_history_id NOT NULL).
 *
 * 같은 기록으로 두 번 쓸 수 없다. uq_reviews_meal 이 막는다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewCreateRequest {

    @NotNull(message = "어떤 식사 기록을 공유할지 골라주세요")
    @Min(value = 1, message = "식사 기록 번호가 올바르지 않습니다")
    private Long mealHistoryId;

    @NotBlank(message = "제목을 입력해주세요")
    @Size(max = 200, message = "제목은 200자 이하여야 합니다")
    private String title;

    @NotBlank(message = "내용을 입력해주세요")
    private String content;
}
