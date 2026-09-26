package com.meokji.api.post.dto;

/**
 * 좋아요를 켜고 끈 결과.
 *
 * <p>{@code likeCount} 를 같이 준다. 화면이 1 을 더하거나 빼서 스스로 맞추면,
 * 다른 사람이 같은 시간에 누른 것과 어긋난다.
 */
public record LikeResponse(
        boolean liked,
        int likeCount) {
}
