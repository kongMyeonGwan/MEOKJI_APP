package com.meokji.api.notice.dto;

/**
 * 공지 한 건.
 *
 * <p>{@code pinned} 는 DB 의 {@code pinned_yn}('Y'/'N')을 화면이 쓰기 쉬운
 * boolean 으로 바꿔서 준다. 목록은 고정 공지가 먼저, 그 안에서는 최신순이다.
 */
public record NoticeResponse(
        Long id,
        String title,
        String content,
        int viewCount,
        boolean pinned,
        String createdAt) {
}
