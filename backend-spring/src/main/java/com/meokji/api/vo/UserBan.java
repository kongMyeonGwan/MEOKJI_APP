package com.meokji.api.vo;

import java.time.LocalDateTime;

/**
 * USER_BANS 표 한 행. 못 먹거나 싫은 재료.
 *
 * <p>재료 마스터 표를 두지 않고 문자열로 저장한다. 사용자가 "고수" 라고 적으면
 * 그대로 남기고, 추천과 레시피에서 이름으로 걸러낸다.
 */
public class UserBan {

    private Long id;
    private Long userId;
    private String ingredientName;

    /** DISLIKE(싫어함) / ALLERGY(알레르기). 알레르기를 더 강하게 취급한다. */
    private String banType;

    private String useYn;
    private LocalDateTime createdAt;

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

    public String getIngredientName() {
        return ingredientName;
    }

    public void setIngredientName(String ingredientName) {
        this.ingredientName = ingredientName;
    }

    public String getBanType() {
        return banType;
    }

    public void setBanType(String banType) {
        this.banType = banType;
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
