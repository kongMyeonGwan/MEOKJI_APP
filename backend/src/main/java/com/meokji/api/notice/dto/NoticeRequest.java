package com.meokji.api.notice.dto;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class NoticeRequest {

    private Boolean pinned;

    @NotBlank(message = "제목을 입력해주세요")
    @Size(max = 200, message = "제목은 200자 이하여야 합니다")
    private String title;

    @NotBlank(message = "내용을 입력해주세요")
    @Size(max = 600, message = "내용은 600자 이하여야 합니다")
    private String content;
}