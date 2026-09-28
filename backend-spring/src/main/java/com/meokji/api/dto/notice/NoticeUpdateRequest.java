package com.meokji.api.dto.notice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 공지 수정. 작성과 같은 모양이다.
 *
 * <p>따로 둔 이유는 앞으로 갈라질 수 있어서다 - 수정에서는 고정 여부를 못 바꾸게
 * 하거나, 수정 사유를 받게 될 수 있다. 지금 합쳐두면 그때 두 곳을 고쳐야 한다.
 */
public record NoticeUpdateRequest(

        @NotBlank(message = "제목을 입력해주세요")
        @Size(max = 200, message = "제목은 200자 이하여야 합니다")
        String title,

        @NotBlank(message = "내용을 입력해주세요")
        String content,

        Boolean pinned) {

    public boolean pinnedOrFalse() {
        return Boolean.TRUE.equals(pinned);
    }
}
