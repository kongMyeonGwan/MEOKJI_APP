package com.meokji.api.like.dto;

import java.util.List;

/**
 * 내 찜 목록.
 *
 * <p>음식 이름만 준다. 레시피 전체는 화면이 필요할 때 {@code GET /api/recipes/{이름}}
 * 으로 따로 받는다 - 찜 목록에 레시피를 다 담으면 응답이 수백 KB 가 된다.
 */
public record LikeListResponse(
        List<String> names,
        int count) {
}
