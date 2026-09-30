package com.meokji.api.ban.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Data;

// 못 먹는 재료 추가 요청. 모양: { "ingredient_name": "오이", "ban_type": "ALLERGY" }
@Data
public class BanCreateRequest {

    @NotBlank(message = "재료 이름을 입력해주세요")
    @Size(max = 100, message = "재료 이름이 너무 깁니다")
    private String ingredientName;

    // DISLIKE(싫어함) 또는 ALLERGY(알레르기). 안 보내면 DISLIKE
    @Pattern(regexp = "DISLIKE|ALLERGY", message = "Ban 종류가 올바르지 않습니다")
    private String banType;
}
