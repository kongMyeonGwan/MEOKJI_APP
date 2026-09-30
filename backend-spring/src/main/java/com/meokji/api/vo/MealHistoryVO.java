package com.meokji.api.vo;

import java.time.LocalDateTime;

import com.meokji.api.service.HistoryService;

/**
 * MEAL_HISTORIES 표 한 행. 사용자가 "이걸로 먹을래요" 를 누른 기록.
 *
 * <p>레시피와 재료를 <b>그 시점 스냅샷</b>으로 저장한다. 나중에 카탈로그의 레시피가
 * 바뀌어도 내가 만들어 먹은 기록은 그대로 남아야 한다.
 *
 * <p>JSON 세 칸을 {@code String} 으로 들고 있다. 컬럼이 CLOB + {@code CHECK(... IS JSON)}
 * 이라 DB 에는 문자열로 넣고, 응답에서는 JSON 그대로 내보낸다 - 변환은
 * {@code HistoryService} 가 경계에서 한 번만 한다.
 */
public class MealHistory {

    private Long id;
    private Long userId;
    private String foodName;
    private String recipeName;

    /** {cook_time_minutes, servings, difficulty, steps[]} */
    private String recipeJson;

    /** [{name, quantity, unit, grams}, ...] */
    private String ingredientsJson;

    /** [{original, substitute, reason}, ...] 없으면 null */
    private String substitutesJson;

    /** 확정 시점의 예상 재료비(원). 계산할 수 없으면 null. */
    private Integer estimatedCost;

    private String useYn;

    /** 사용자가 이 식사를 확정한 시각(UTC). 목록 정렬 기준이다. */
    private LocalDateTime selectedAt;

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

    public String getRecipeName() {
        return recipeName;
    }

    public void setRecipeName(String recipeName) {
        this.recipeName = recipeName;
    }

    public String getRecipeJson() {
        return recipeJson;
    }

    public void setRecipeJson(String recipeJson) {
        this.recipeJson = recipeJson;
    }

    public String getIngredientsJson() {
        return ingredientsJson;
    }

    public void setIngredientsJson(String ingredientsJson) {
        this.ingredientsJson = ingredientsJson;
    }

    public String getSubstitutesJson() {
        return substitutesJson;
    }

    public void setSubstitutesJson(String substitutesJson) {
        this.substitutesJson = substitutesJson;
    }

    public Integer getEstimatedCost() {
        return estimatedCost;
    }

    public void setEstimatedCost(Integer estimatedCost) {
        this.estimatedCost = estimatedCost;
    }

    public String getUseYn() {
        return useYn;
    }

    public void setUseYn(String useYn) {
        this.useYn = useYn;
    }

    public LocalDateTime getSelectedAt() {
        return selectedAt;
    }

    public void setSelectedAt(LocalDateTime selectedAt) {
        this.selectedAt = selectedAt;
    }
}
