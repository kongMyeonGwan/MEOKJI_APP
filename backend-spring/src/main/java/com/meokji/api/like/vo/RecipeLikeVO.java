package com.meokji.api.like.vo;

import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

/**
 * RECIPE_LIKES 표 한 행. 찜한 레시피.
 *
 * 해제를 행 삭제로 처리하지 않는다. 지우고 다시 만들면 created_at 이
 * 매번 바뀌어 "언제 찜했는지" 가 사라지고, 목록 순서가 흔들린다.
 *
 * 하트를 누를 때 화면이 보내는 { "food_name": "김치찌개" } 도 이 클래스로 받는다.
 * 그래서 foodName 에 검증이 붙어 있다.
 */
@Data
public class RecipeLikeVO {

    private Long id;
    private Long userId;

    @NotBlank(message = "음식 이름을 입력해주세요")
    @Size(max = 150, message = "음식 이름이 너무 깁니다")
    private String foodName;

    private String useYn;
    private LocalDateTime createdAt;

    public boolean isActive() {
        return "Y".equals(useYn);
    }
}
