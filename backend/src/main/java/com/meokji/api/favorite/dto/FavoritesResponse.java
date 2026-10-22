package com.meokji.api.favorite.dto;

import java.util.List;

import com.meokji.api.recommend.vo.FoodVO;

import lombok.Getter;

/**
 * 내 찜 목록 (서버 → 화면). GET /api/favorites 의 응답.
 *
 * { "names": ["김치찌개", ...], "items": [음식 정보, ...], "count": 20 }
 *
 * 화면마다 쓰는 칸이 다르다.
 *   names : 하트를 채울지 정한다 (FoodCard · RecipePage · WorldCupPage)
 *   items : 찜 목록 화면이 그린다 (LikedPage)
 *   count : 찜 개수 뱃지 (TopBar · SettingsPage)
 *
 * items 는 표에 없다. 찜 표에는 음식 이름만 있으므로, 서비스가 그 이름으로
 * 카탈로그(catalog.json)에서 찾아 채운다. 카탈로그에 없는 이름이면 foodName
 * 하나만 담겨 온다 — 화면은 나머지 칸을 있을 때만 그린다(LikedCard).
 */
@Getter
public class FavoritesResponse {

    private final List<String> names;

    private final List<FoodVO> items;

    private final int count;

    public FavoritesResponse(List<String> names, List<FoodVO> items, int count) {
        this.names = names;
        this.items = items;
        this.count = count;
    }
}
