package com.meokji.api.dto.like;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 찜하기 · 해제. 같은 이름을 다시 보내면 뒤집힌다. */
public record LikeToggleRequest(

        @NotBlank(message = "음식 이름을 입력해주세요")
        @Size(max = 150, message = "음식 이름이 너무 깁니다")
        String foodName) {

    public String trimmedFoodName() {
        return foodName == null ? null : foodName.trim();
    }
}
