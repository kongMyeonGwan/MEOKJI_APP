package com.meokji.api.notice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

// 공지 작성 요청 (관리자만). 모양: { "title": "...", "content": "...", "pinned": true }
@Data
public class NoticeCreateRequest {

    @NotBlank(message = "제목을 입력해주세요")
    @Size(max = 200, message = "제목은 200자 이하여야 합니다")
    private String title;

    @NotBlank(message = "내용을 입력해주세요")
    private String content;

    private Boolean pinned; // 목록 맨 위에 고정할지. 안 보내면 null (고정 안 함)
}
