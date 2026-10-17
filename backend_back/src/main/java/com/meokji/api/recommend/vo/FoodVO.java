package com.meokji.api.recommend.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 추천 후보 한 건.
 *
 * 카탈로그의 한 줄을 화면용으로 바꾼 것이다. imageUrl 은 요리 사진 주소로
 * catalog.json 에 적어둔 "/images/food/김치찌개.png" 같은 값이 그대로 올라온다.
 * art 는 사진이 없는 요리에 대신 그려줄 썸네일 그림 이름이다.
 *
 * <p>추천 말고 찜 목록(LikeListVO.items)에서도 쓴다. 둘 다 "카탈로그의
 * 음식 한 줄" 을 화면 카드로 보여주는 일이라 모양이 같다. 바꾸는 코드도
 * 한 곳(FoodScorer.toFoodVO)에 모아 두었다.
 *
 * <p>응답 전용이고 표와 무관하다. catalog.json 의 한 줄에서 왔다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoodVO {

    /** 음식 이름. 레시피 · 찜 · 기록이 모두 이 이름으로 음식을 가리킨다 */
    private String foodName;

    /** 분류. 한식 · 중식 ... */
    private String category;

    /**
     * 왜 이 메뉴가 나왔는지. 예) "더운 날엔 시원한 메뉴"
     *
     * 점수를 계산한 FoodScorer 가 적어 넣는다. 숫자(점수)를 그대로 보내면
     * 화면이 "87점" 을 띄우게 되는데, 그 숫자가 무슨 뜻인지는 사용자가
     * 알 수 없다. 이유를 말로 적어주는 쪽이 읽힌다
     */
    private String reason;

    /** 조리 시간(분). 화면이 "20분" 으로 띄운다 */
    private int cookTimeMinutes;

    /** 난이도. "쉬움" · "보통" · "어려움" */
    private String difficulty;

    /**
     * 재료비 어림값(원). catalog.json 에 적어둔 값이다.
     *
     * 레시피 화면의 재료비(RecipeVO.estimatedCost)와 다를 수 있다 -
     * 그쪽은 오늘 시세로 계산하고, 이쪽은 미리 적어둔 값이다.
     * 추천은 후보 100개의 값을 다 알아야 예산으로 걸러낼 수 있는데,
     * 그때마다 시세 API 를 100번 부를 수는 없다
     */
    private int estimatedCost;

    /** 태그. 국물 · 매콤 · 혼밥 ... 점수 계산과 검색어 맞추기에 쓴다 */
    private List<String> tags;

    /** 썸네일 그림 이름. 사진이 없는 음식에 대신 그린다 */
    private String art;

    /** 요리 사진 주소. 없으면 null 이고 화면은 art 로 떨어진다 */
    private String imageUrl;
}
