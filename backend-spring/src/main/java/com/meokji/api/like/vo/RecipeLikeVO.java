package com.meokji.api.like.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * RECIPE_LIKES 표 한 행. 찜한 레시피.
 *
 * 해제를 행 삭제로 처리하지 않는다. 지우고 다시 만들면 created_at 이
 * 매번 바뀌어 "언제 찜했는지" 가 사라지고, 목록 순서가 흔들린다.
 */
@Data
public class RecipeLikeVO {

    private Long id;
    private Long userId;
    private String foodName;
    private String useYn;
    private LocalDateTime createdAt;

    public boolean isActive() {
        return "Y".equals(useYn);
    }
}
