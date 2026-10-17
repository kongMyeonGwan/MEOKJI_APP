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
 *
 * <p>names 만으로 하트를 가릴 수 있는데 items 가 또 필요한 이유 — 추천 화면은
 * 음식 정보를 이미 들고 있고(추천 결과로 받았다) 하트 여부만 모른다.
 * 찜 목록 화면은 반대로 아무것도 없어서 음식 정보까지 받아야 한다.
 * 한 응답으로 두 화면을 다 쓰려고 둘을 같이 담았다.
 *
 * <p>응답 전용이다. 화면이 보낼 값이 아니라 서버가 만들어 주는 값이라
 * 검증도, @JsonIgnore 로 감출 칸도 없다.
 */
@Data
public class LikeListVO {

    private List<String> names; // 찜한 음식 이름들 (최근에 찜한 것이 앞)

    // names.size() 와 같은 값이다. 따로 보내는 이유 — 화면이 "찜 N개" 를
    // 띄울 때 names 를 세지 않고 바로 쓸 수 있고, 찜을 비운 응답처럼
    // items 가 없는 경우에도 개수는 확실히 전달된다
    private int count;

    /** 카탈로그에서 찾아 붙인 음식 정보. names 와 같은 순서다. */
    // 순서가 같아야 화면이 names[i] 와 items[i] 를 짝지을 수 있다.
    // 그래서 카탈로그에 없는 이름도 건너뛰지 않고 이름만 담아 넣는다
    // (LikeServiceImpl 참고)
    //
    // 찜을 비우는 응답(clear)에서는 이 칸이 null 이다. 그때는 카드를
    // 그릴 일이 없기 때문이다
    private List<FoodVO> items;
}
