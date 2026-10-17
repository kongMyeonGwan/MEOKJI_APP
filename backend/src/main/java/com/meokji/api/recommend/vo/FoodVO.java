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
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FoodVO {

    private String foodName;

    private String category;

    private String reason;

    private int cookTimeMinutes;

    private String difficulty;

    private int estimatedCost;

    private List<String> tags;

    private String art;

    private String imageUrl;
}
