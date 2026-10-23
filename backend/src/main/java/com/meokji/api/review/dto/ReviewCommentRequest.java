package com.meokji.api.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ReviewCommentRequest {

    @NotBlank(message = "댓글을 입력해 주세요")
    @Size(max = 1000, message = "댓글은 1000자 이하여야 합니다")
    private String content;
}
