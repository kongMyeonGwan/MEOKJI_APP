package com.meokji.api.dto.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 댓글 쓰기 · 수정. */
public record CommentCreateRequest(

        @NotBlank(message = "댓글을 입력해주세요")
        @Size(max = 1000, message = "댓글은 1000자 이하여야 합니다")
        String content) {
}
