package com.meokji.api.like.dto;

import java.util.List;

import lombok.Data;

// 내 찜 목록 응답. 모양: { "names": ["김치찌개", "제육볶음"], "count": 2 }
@Data
public class LikeListResponse {

    private List<String> names; // 찜한 음식 이름들 (최근에 찜한 것이 앞)
    private int count;
}
