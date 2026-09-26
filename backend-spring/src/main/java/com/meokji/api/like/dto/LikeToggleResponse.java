package com.meokji.api.like.dto;

/**
 * 찜을 켜거나 끈 결과.
 *
 * <p>{@code count} 를 같이 준다. 화면이 ±1 로 스스로 맞추면 다른 기기에서 누른
 * 것과 어긋난다 - 찜을 DB 로 옮긴 이유가 기기 사이에 맞추려는 것이었다(STEP 36).
 */
public record LikeToggleResponse(
        String foodName,
        boolean liked,
        int count) {
}
