package com.meokji.api.ban.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Ban 추가 요청.
 *
 * <p>{@code @NotBlank} 가 중요하다 - 오라클은 빈 문자열을 NULL 로 취급해서
 * 그대로 넘기면 NOT NULL 위반(ORA-01400)으로 500 이 난다.
 */
public record BanCreateRequest(

        @NotBlank(message = "재료 이름을 입력해주세요")
        @Size(max = 100, message = "재료 이름이 너무 깁니다")
        String ingredientName,

        @Pattern(regexp = "DISLIKE|ALLERGY", message = "Ban 종류가 올바르지 않습니다")
        String banType) {

    /** 앞뒤 공백을 떼어낸 재료명. */
    public String trimmedName() {
        return ingredientName == null ? null : ingredientName.trim();
    }

    /** 안 보내면 DISLIKE. 파이썬 쪽 기본값과 같다. */
    public String banTypeOrDefault() {
        return banType == null || banType.isBlank() ? "DISLIKE" : banType;
    }
}
