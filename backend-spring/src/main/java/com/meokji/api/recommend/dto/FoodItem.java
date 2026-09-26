package com.meokji.api.recommend.dto;

import java.util.List;

/**
 * 추천 후보 한 건.
 *
 * <p>카탈로그의 한 줄을 화면용으로 바꾼 것이다. {@code art} 는 썸네일 그림 이름,
 * {@code imageUrl} 은 실제 사진 주소인데 지금은 사진이 없어서 늘 null 이다 -
 * 칸을 남겨둔 이유는 사진이 붙어도 화면을 고치지 않게 하려는 것이다.
 */
public record FoodItem(
        String foodName,
        String category,
        String reason,
        int cookTimeMinutes,
        String difficulty,
        int estimatedCost,
        List<String> tags,
        String art,
        String imageUrl) {
}
