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
 *
 * <p>왜 레시피를 통째로 복사해 넣나 (정규화하지 않는 이유)
 *
 * <p>레시피 번호만 저장하면 표가 작아지지만, recipes.json 의 레시피가
 * 바뀌거나 사라지면 "내가 그때 만든 것" 이 달라진다. 재료비도 그때 시세로
 * 계산한 값이라 지금 다시 계산하면 다른 숫자가 나온다. 기록은 과거의
 * 사실이라 그때 모습 그대로 남아야 한다.
 *
 * <p>그래서 이 표는 한 행이 크다. 대신 읽을 때 JOIN 이 없다.
 */
@Data
public class MealHistoryVO {

    /** 시퀀스로 받는 번호. 삭제할 때 /api/histories/{이 값} 으로 가리킨다 */
    private Long id;

    /** 누구 것인지. 토큰에서 정하므로 화면에 보내지도, 받지도 않는다 */
    @JsonIgnore
    private Long userId;

    // art(썸네일)를 찾는 열쇠이기도 하다. catalog.json 의 음식 이름과
    // 글자가 똑같아야 그림을 찾을 수 있어서, 서비스가 trim 해서 넣는다
    @NotBlank(message = "음식 이름을 입력해주세요")
    @Size(max = 150, message = "음식 이름이 너무 깁니다")
    private String foodName;

    // 레시피 제목. 없어도 된다 (@NotBlank 가 아니다) - 음식 이름만 고르고
    // 레시피를 안 거친 경로도 있다
    @Size(max = 200)
    private String recipeName;

    /** {cook_time_minutes, servings, difficulty, steps[]} — DB 에는 글자로 */
    // @JsonIgnore 가 붙은 이유 — 이 칸을 그대로 내보내면 JSON 안에
    // JSON 글자가 들어간 모양("{\"steps\":...}")이 되어 화면이 또 파싱해야 한다.
    // 아래 getRecipeJsonNode 가 객체로 바꿔 내보낸다
    @JsonIgnore
    private String recipeJson;

    /** [{name, quantity, unit, grams}, ...] — DB 에는 글자로 */
    @JsonIgnore
    private String ingredientsJson;

    /** [{original, substitute, reason}, ...] 없으면 null — DB 에는 글자로 */
    @JsonIgnore
    private String substitutesJson;

    /** 확정 시점의 예상 재료비(원). 계산할 수 없으면 null. */
    // 시세를 못 가져온 재료가 있으면 null 이다. 0 으로 두지 않는 이유는
    // "공짜" 와 "모름" 이 구별되어야 하기 때문이다
    //
    // @Max 를 1억으로 둔 이유 — 한 끼 재료비가 그 이상이 될 수는 없다.
    // 화면 쪽 계산이 잘못돼 엉뚱한 숫자가 와도 누적 재료비가 망가지지 않게 막는다
    @Min(value = 0, message = "예상 비용은 0원 이상이어야 합니다")
    @Max(value = 100000000, message = "예상 비용이 너무 큽니다")
    private Integer estimatedCost;

    /** 'N' = 사용자가 지운 기록. 목록 SQL 이 'Y' 만 가려 오므로 화면에 알릴 필요가 없다 */
    @JsonIgnore
    private String useYn;

    /** 사용자가 이 식사를 확정한 시각(UTC). 목록 정렬 기준이다. */
    // 칸 자체는 감추고 아래 getSelectedAtText 로 글자를 내보낸다 -
    // LocalDateTime 을 Jackson 에 그냥 넘기면 모양이 고정되지 않는다
    @JsonIgnore
    private LocalDateTime selectedAt;

    /**
     * 이 기록으로 쓴 커뮤니티 후기의 번호. 없으면 null.
     *
     * MEAL_HISTORIES 의 컬럼이 아니다. 목록을 조회할 때 RECIPE_REVIEWS 를
     * LEFT JOIN 해서 그 글의 id 를 같이 가져온다 (HistoryMapper.xml).
     *
     * 화면이 이 값으로 버튼을 가린다 — null 이면 "후기 쓰기", 값이 있으면
     * "내 후기 보기" 다. 없으면 화면이 기록마다 후기 유무를 따로 물어봐야 한다.
     */
    private Long recipeReviewId;

    /** 테이블 컬럼이 아니다. 썸네일 그림 종류 - 서비스가 카탈로그에서 찾아 넣는다 */
    // 표에 같이 저장하지 않는 이유 — 그림을 바꾸면 과거 기록까지 전부
    // UPDATE 해야 한다. 음식 이름으로 그때그때 찾으면 그럴 일이 없다
    private String art;

    // ---------- 화면과 주고받는 JSON 세 칸 ----------
    // getter 는 내보낼 때, setter 는 받을 때 쓰인다. 이름을 둘 다
    // @JsonProperty 로 못박아서 recipe_json 으로 오가게 한다.
    //
    // 칸(String)과 주고받는 모양(JsonNode)이 다르기 때문에 이렇게 한다.
    // 칸 이름을 손으로 적는 이유 — snake_case 설정은 getRecipeJsonNode 에서
    // recipe_json_node 를 만든다. 뒤에 붙은 Node 까지 이름에 들어간다.
    //
    // 검증(@NotNull)을 칸이 아니라 getter 에 붙인 이유 — 칸은 String 이라
    // 받을 때 null 인지 보려면 setter 를 지나고 나서여야 한다. getter 에
    // 붙이면 @Valid 가 그 결과를 보고 판단한다.

    @NotNull(message = "레시피가 없습니다")
    @JsonProperty("recipe_json")
    public JsonNode getRecipeJsonNode() {
        // 저장된 글자를 객체로 되돌린다. 글자가 null 이면 null 을 준다
        return Json.node(recipeJson);
    }

    @JsonProperty("recipe_json")
    public void setRecipeJsonNode(JsonNode node) {
        // 받은 객체를 글자로 바꿔 칸에 넣는다. DB 컬럼이 CLOB 이라서다
        this.recipeJson = Json.text(node);
    }

    @NotNull(message = "재료 목록이 없습니다")
    @JsonProperty("ingredients_json")
    public JsonNode getIngredientsJsonNode() {
        return Json.node(ingredientsJson);
    }

    @JsonProperty("ingredients_json")
    public void setIngredientsJsonNode(JsonNode node) {
        this.ingredientsJson = Json.text(node);
    }

    /** 대체 재료 (없어도 된다) */
    // 위 둘과 달리 @NotNull 이 없다. 못 먹는 재료를 등록하지 않은 사람은
    // 바꿀 것이 없어서 이 칸이 비어 있다
    @JsonProperty("substitutes_json")
    public JsonNode getSubstitutesJsonNode() {
        return Json.node(substitutesJson);
    }

    @JsonProperty("substitutes_json")
    public void setSubstitutesJsonNode(JsonNode node) {
        this.substitutesJson = Json.text(node);
    }

    /** 화면이 읽는 selected_at. 예) "2026-09-11T21:29:42Z" */
    // setter 짝이 없다. 확정 시각은 DB 가 찍는 값이라 화면이 보낼 일이 없다
    @JsonProperty("selected_at")
    public String getSelectedAtText() {
        return Times.toText(selectedAt);
    }
}
