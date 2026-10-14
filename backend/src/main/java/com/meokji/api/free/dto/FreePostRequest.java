package com.meokji.api.free.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 글쓰기 · 글 수정 요청 (화면 → 서버).
 *
 * { "title": "제목", "content": "내용" }
 *
 * 글쓴이 · 조회수 같은 칸은 일부러 없다. 글쓴이는 서버가 토큰에서 꺼내고,
 * 조회수는 POST /{id}/view 로만 오른다. 받을 칸이 없으니 화면이 무엇을
 * 보내든 그 값이 DB 에 들어갈 길이 없다.
 */
@Data
@NoArgsConstructor
public class FreePostRequest {

    @NotBlank(message = "제목을 입력해주세요")
    @Size(max = 200, message = "제목은 200자 이하여야 합니다")
    private String title;

    @NotBlank(message = "내용을 입력해주세요")
    @Size(max = 4000, message = "내용은 4000자 이하여야 합니다")
    private String content;
}
