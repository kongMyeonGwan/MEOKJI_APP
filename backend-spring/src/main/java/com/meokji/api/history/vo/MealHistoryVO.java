package com.meokji.api.history.vo;

import lombok.Data;
import java.time.LocalDateTime;

import com.meokji.api.history.service.HistoryService;

/**
 * MEAL_HISTORIES 표 한 행. 사용자가 "이걸로 먹을래요" 를 누른 기록.
 *
 * 레시피와 재료를 그 시점 스냅샷으로 저장한다. 나중에 카탈로그의 레시피가
 * 바뀌어도 내가 만들어 먹은 기록은 그대로 남아야 한다.
 *
 * JSON 세 칸을 String 으로 들고 있다. 컬럼이 CLOB + CHECK(... IS JSON)
 * 이라 DB 에는 문자열로 넣고, 응답에서는 JSON 그대로 내보낸다 - 변환은
 * HistoryService 가 경계에서 한 번만 한다.
 */
@Data
public class MealHistoryVO {

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

    // 테이블 컬럼이 아니다. 목록을 조회할 때 JOIN 으로 같이 가져오는 "이 기록으로 쓴 글 번호" (없으면 null)
    private Long sharedPostId;
}
