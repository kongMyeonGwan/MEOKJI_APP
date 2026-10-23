package com.meokji.api.favorite.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class FavoriteRequest {
    @NotBlank(message = "음식이 선택되지 않았습니다")
    private String foodName;
}
