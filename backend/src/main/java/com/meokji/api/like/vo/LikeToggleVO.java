package com.meokji.api.like.vo;

import lombok.Data;

/** 하트 누르기 응답. 모양: { "food_name": "김치찌개", "liked": true, "count": 3 } */
@Data
public class LikeToggleVO {

    private String foodName;
    private boolean liked; // 지금 찜된 상태인지
    private int count; // 내 찜 전체 개수
}
