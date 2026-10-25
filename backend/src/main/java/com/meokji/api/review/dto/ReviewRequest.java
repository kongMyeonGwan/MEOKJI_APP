package com.meokji.api.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ReviewRequest {

    @NotBlank(message = "제목을 입력해 주세요")
    @Size(max = 200, message = "제목은 200자 이하여야 합니다.")
    private String title;

    @NotBlank(message = "내용을 입력해 주세요.")
    @Size(max = 4000, message = "내용은 4000자 이하여야 합니다.")
    private String content;

    @NotNull(message = "레시피를 선택해 주세요.")
    private Long meal_history_id;
}
