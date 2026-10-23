package com.meokji.api.favorite.dto;

import lombok.Getter;

/**
 * 찜 토글 결과 (서버 → 화면). PUT /api/favorites 의 응답.
 *
 * { "foodName": "김치찌개", "favorited": true, "count": 3 }
 *
 * 화면은 favorited 로 하트를 칠할지 정하고, count 로 뱃지를 고친다.
 * 목록을 다시 부르지 않아도 되도록 개수를 같이 준다.
 */
@Getter
public class FavoriteToggleResponse {

    private final String foodName;

    /** true = 방금 찜했다, false = 방금 풀었다 */
    private final boolean favorited;

    /** 이 회원의 찜 개수 (바뀐 뒤) */
    private final int count;

    public FavoriteToggleResponse(String foodName, boolean favorited, int count) {
        this.foodName = foodName;
        this.favorited = favorited;
        this.count = count;
    }
}
