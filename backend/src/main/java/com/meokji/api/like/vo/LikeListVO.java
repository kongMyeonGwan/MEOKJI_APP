package com.meokji.api.like.vo;

import java.util.List;

import lombok.Data;

/**
 * 내 찜 목록 응답. 모양: { "names": ["김치찌개", "제육볶음"], "count": 2 }
 *
 * 표 한 행이 아니라 여러 행을 세어서 만든 모양이라 RECIPE_LIKES 와 칸이 다르다.
 */
@Data
public class LikeListVO {

    private List<String> names; // 찜한 음식 이름들 (최근에 찜한 것이 앞)
    private int count;
}
