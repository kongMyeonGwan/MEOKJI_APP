package com.meokji.api.favorite.dto;

import java.util.ArrayList;
import java.util.List;

import lombok.Getter;

/**
 * 내 찜 목록 (서버 → 화면). GET /api/favorites 의 응답.
 *
 * { "names": ["김치찌개", ...], "items": [{id, food_name, created_at}, ...], "count": 20 }
 *
 * 화면마다 쓰는 칸이 다르다.
 *   names : 하트를 채울지 정한다 (FoodCard · RecipePage · WorldCupPage)
 *   items : 찜 목록 화면이 그린다 (LikedPage)
 *   count : 찜 개수 뱃지 (TopBar · SettingsPage)
 */
@Getter
public class FavoritesResponse {

    private final List<String> names;

    private final List<FavoriteListResponse> items;

    private final int count;

    public FavoritesResponse(List<String> names, List<FavoriteListResponse> items, int count) {
        this.names = names;
        this.items = items;
        this.count = count;
    }
}
