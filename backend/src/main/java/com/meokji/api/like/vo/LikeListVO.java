package com.meokji.api.like.vo;

import java.util.List;

import com.meokji.api.recommend.vo.FoodVO;

import lombok.Data;

/**
 * 내 찜 목록 응답. 모양:
 * { "names": [...], "count": 2, "items": [{ "food_name": "김치찌개", "category": "한식", ... }] }
 *
 * 표 한 행이 아니라 여러 행을 세어서 만든 모양이라 RECIPE_LIKES 와 칸이 다르다.
 *
 * names 와 items 가 같은 것을 두 번 담는 것처럼 보이지만 둘 다 쓰인다 - names 는
 * FoodCard 가 "이 음식을 찜했나" 를 확인할 때, items 는 찜 목록 화면이 카드를
 * 그릴 때 쓴다. names 를 빼면 추천 화면의 하트가 전부 빈 채로 뜬다.
 */
@Data
public class LikeListVO {

    private List<String> names; // 찜한 음식 이름들 (최근에 찜한 것이 앞)
    private int count;

    /** 카탈로그에서 찾아 붙인 음식 정보. names 와 같은 순서다. */
    private List<FoodVO> items;
}
