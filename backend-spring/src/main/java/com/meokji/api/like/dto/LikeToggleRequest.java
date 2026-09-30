package com.meokji.api.like.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

// 하트 누르기 요청. 모양: { "food_name": "김치찌개" }
@Data
public class LikeToggleRequest {

    @NotBlank(message = "음식 이름을 입력해주세요")
    @Size(max = 150, message = "음식 이름이 너무 깁니다")
    private String foodName;
}
