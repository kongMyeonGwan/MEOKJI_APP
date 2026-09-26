package com.meokji.api.post.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 후기 수정.
 *
 * <p>{@code mealHistoryId} 는 받지 않는다. 어떤 식사의 후기인지는 바뀔 수 없다 -
 * 바꿀 수 있게 두면 남의 기록에 내 후기를 붙일 수 있다.
 */
public record PostUpdateRequest(

        @NotBlank(message = "제목을 입력해주세요")
        @Size(max = 200, message = "제목은 200자 이하여야 합니다")
        String title,

        @NotBlank(message = "내용을 입력해주세요")
        String content) {
}
