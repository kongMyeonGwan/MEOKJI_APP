package com.meokji.api.history.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.meokji.api.util.Json;
import com.meokji.api.util.Times;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import lombok.Data;
import tools.jackson.databind.JsonNode;

/**
 * MEAL_HISTORIES 표 한 행. 사용자가 "이걸로 먹을래요" 를 누른 기록.
 *
 * 레시피와 재료를 그 시점 스냅샷으로 저장한다. 나중에 카탈로그의 레시피가
 * 바뀌어도 내가 만들어 먹은 기록은 그대로 남아야 한다.
 *
 * 이 클래스 하나가 세 가지 일을 한다.
 *   1. DB 한 행을 담는다        (MyBatis 가 채운다)
 *   2. 화면이 보낸 값을 받는다   (검증이 그래서 붙어 있다)
 *   3. 화면으로 나갈 모양이 된다 (@JsonIgnore 가 붙은 칸은 안 나간다)
 *
 * JSON 세 칸이 까다롭다. 컬럼이 CLOB 이라 DB 에는 **글자**로 넣어야 하는데,
 * 화면은 recipe_json.steps 처럼 **객체**로 읽는다. 그래서 칸은 String 으로
 * 두고 (@JsonIgnore), 화면과 주고받을 때만 JsonNode 로 바꾼다.
 */
@Data
public class MealHistoryVO {

    private Long id;

    @JsonIgnore
    private Long userId;

    @NotBlank(message = "음식 이름을 입력해주세요")
    @Size(max = 150, message = "음식 이름이 너무 깁니다")
    private String foodName;

    @Size(max = 200)
    private String recipeName;

    /** {cook_time_minutes, servings, difficulty, steps[]} — DB 에는 글자로 */
    @JsonIgnore
    private String recipeJson;

    /** [{name, quantity, unit, grams}, ...] — DB 에는 글자로 */
    @JsonIgnore
    private String ingredientsJson;

    /** [{original, substitute, reason}, ...] 없으면 null — DB 에는 글자로 */
    @JsonIgnore
    private String substitutesJson;

    /** 확정 시점의 예상 재료비(원). 계산할 수 없으면 null. */
    @Min(value = 0, message = "예상 비용은 0원 이상이어야 합니다")
    @Max(value = 100000000, message = "예상 비용이 너무 큽니다")
    private Integer estimatedCost;

    @JsonIgnore
    private String useYn;

    /** 사용자가 이 식사를 확정한 시각(UTC). 목록 정렬 기준이다. */
    @JsonIgnore
    private LocalDateTime selectedAt;

    /**
     * 이 기록으로 쓴 커뮤니티 후기의 번호. 없으면 null.
     *
     * MEAL_HISTORIES 의 컬럼이 아니다. 목록을 조회할 때 RECIPE_REVIEWS 를
     * LEFT JOIN 해서 그 글의 id 를 같이 가져온다 (HistoryMapper.xml).
     */
    private Long recipeReviewId;

    /** 테이블 컬럼이 아니다. 썸네일 그림 종류 - 서비스가 카탈로그에서 찾아 넣는다 */
    private String art;

    // ---------- 화면과 주고받는 JSON 세 칸 ----------
    // getter 는 내보낼 때, setter 는 받을 때 쓰인다. 메서드 이름이
    // getRecipeJsonNode 라서 그대로 두면 recipeJsonNode 로 나간다.
    // 그래서 둘 다 @JsonProperty 로 recipeJson 이라고 못박는다.
    //
    // 이 JSON 안쪽 키(cook_time_minutes 등)는 아직 snake 다 - AI 가 만들어
    // CLOB 에 넣은 내용이라 Jackson 과 상관이 없다.

    @NotNull(message = "레시피가 없습니다")
    @JsonProperty("recipeJson")
    public JsonNode getRecipeJsonNode() {
        return Json.node(recipeJson);
    }

    @JsonProperty("recipeJson")
    public void setRecipeJsonNode(JsonNode node) {
        this.recipeJson = Json.text(node);
    }

    @NotNull(message = "재료 목록이 없습니다")
    @JsonProperty("ingredientsJson")
    public JsonNode getIngredientsJsonNode() {
        return Json.node(ingredientsJson);
    }

    @JsonProperty("ingredientsJson")
    public void setIngredientsJsonNode(JsonNode node) {
        this.ingredientsJson = Json.text(node);
    }

    /** 대체 재료 (없어도 된다) */
    @JsonProperty("substitutesJson")
    public JsonNode getSubstitutesJsonNode() {
        return Json.node(substitutesJson);
    }

    @JsonProperty("substitutesJson")
    public void setSubstitutesJsonNode(JsonNode node) {
        this.substitutesJson = Json.text(node);
    }

    /** 화면이 읽는 selectedAt. 예) "2026-09-11T21:29:42Z" */
    @JsonProperty("selectedAt")
    public String getSelectedAtText() {
        return Times.toText(selectedAt);
    }
}
