package com.meokji.api.recipe.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

import tools.jackson.databind.JsonNode;

/**
 * 레시피 한 건 (아직 저장 전).
 *
 * 칸 이름이 recipeJson · ingredientsJson 처럼 어색한 것은,
 * 이 값이 그대로 POST /api/histories 로 되돌아가 DB 의 같은 이름 컬럼에
 * 저장되기 때문이다. 화면이 받은 것을 그대로 보낼 수 있어야 한다.
 *
 * prices 는 DB 와 무관하다. 매번 KAMIS 에서 받아온다.
 *
 * <p>표 한 행이 아니다. 레시피는 resources/data/recipes.json 에 있고,
 * 이 VO 는 그것을 읽어 화면이 쓸 모양으로 만든 결과다. 그래서 id 도
 * use_yn 도 created_at 도 없다.
 *
 * <p>칸 타입이 JsonNode 인 것들 — 안쪽 생김새를 서버가 들여다보지 않고
 * 그대로 전달하기만 한다. VO 로 풀어 적으면 recipes.json 의 모양이
 * 조금 바뀔 때마다 클래스를 같이 고쳐야 한다.
 *
 * <p>응답 전용이다. 화면이 보낼 값이 아니라서 검증도 없다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeVO {

    /** 음식 이름. 주소로 받은 값이 그대로 들어간다 */
    private String foodName;

    /** 레시피 제목. 음식 이름과 다를 수 있다 (예: "돼지고기 김치찌개") */
    private String recipeName;

    /** {cook_time_minutes, servings, difficulty, steps[]} */
    private JsonNode recipeJson;

    /**
     * [{name, quantity, unit, grams}, ...]
     *
     * 로그인했으면 못 먹는 재료가 빠지거나 다른 재료로 바뀐 뒤의 목록이다.
     */
    private JsonNode ingredientsJson;

    /**
     * [{original, substitute, reason}, ...] — 무엇을 왜 바꿨는지
     *
     * 바꾼 것이 없으면 null 이다. 화면은 이 값이 있을 때만 "오이를 빼고
     * 애호박으로 바꿨어요" 같은 안내를 띄운다. 안 알려주면 사용자는
     * 레시피가 원래 그런 줄 안다.
     */
    private JsonNode substitutesJson;

    /**
     * 재료비 합계(원). 계산할 수 없으면 null.
     *
     * 0 으로 두지 않는 이유는 "공짜" 와 "모름" 이 구별되어야 하기 때문이다.
     * 재료 가격을 하나도 못 받은 날에는 null 이 된다.
     */
    private Integer estimatedCost;

    /** 분류 (한식 · 중식 ...). catalog.json 에서 찾아 넣는다 */
    private String category;

    /** 썸네일 그림 이름. catalog.json 에서 찾아 넣는다 */
    private String art;

    /** 음식 사진 주소. 없으면 null 이고 화면은 art 로 그림을 그린다 */
    private String imageUrl;

    /**
     * 재료별 가격. 재료 하나당 한 줄이고 ingredientsJson 과 같은 순서다.
     *
     * 이 칸만 DB 에 저장되지 않는다. 시세는 날마다 바뀌므로 "지금 값" 을
     * 보여주는 용도이고, 식사 기록에 남는 것은 그때 계산한 estimatedCost 다.
     */
    private List<PriceItemVO> prices;
}
