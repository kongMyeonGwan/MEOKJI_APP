package com.meokji.api.vo;

import java.time.LocalDateTime;

/**
 * RECIPE_LIKES 표 한 행. 찜한 레시피.
 *
 * <p>해제를 행 삭제로 처리하지 않는다. 지우고 다시 만들면 {@code created_at} 이
 * 매번 바뀌어 "언제 찜했는지" 가 사라지고, 목록 순서가 흔들린다.
 */
public class RecipeLike {

    private Long id;
    private Long userId;
    private String foodName;
    private String useYn;
    private LocalDateTime createdAt;

    public boolean isActive() {
        return "Y".equals(useYn);
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getFoodName() {
        return foodName;
    }

    public void setFoodName(String foodName) {
        this.foodName = foodName;
    }

    public String getUseYn() {
        return useYn;
    }

    public void setUseYn(String useYn) {
        this.useYn = useYn;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
