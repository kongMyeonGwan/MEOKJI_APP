package com.meokji.api.free.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 댓글 쓰기 · 수정 요청 (화면 → 서버).
 *
 * { "content": "댓글 내용" }
 *
 * 어느 글의 댓글인지는 주소(/free-posts/3/comments)에서 꺼내므로 본문으로 받지 않는다.
 */
@Data
@NoArgsConstructor
public class FreeCommentRequest {

    @NotBlank(message = "댓글을 입력해주세요")
    @Size(max = 1000, message = "댓글은 1000자 이하여야 합니다")
    private String content;
}
