package com.meokji.api.dto.notice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

// 공지 수정 요청 (관리자만). 세 칸을 전부 보낸다
@Data
public class NoticeUpdateRequest {

    @NotBlank(message = "제목을 입력해주세요")
    @Size(max = 200, message = "제목은 200자 이하여야 합니다")
    private String title;

    @NotBlank(message = "내용을 입력해주세요")
    private String content;

    private Boolean pinned;
}
