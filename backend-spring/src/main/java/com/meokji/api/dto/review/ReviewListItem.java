package com.meokji.api.dto.review;

/**
 * 목록 한 줄. 본문은 미리보기용으로 잘라서 내려준다.
 *
 * <p>{@code hasFile} 만 준다. 목록에서 파일 정보까지 필요하지 않고, 화면은 이
 * 값으로 제목 옆에 클립 표시를 붙인다.
 *
 * <p>좋아요 · 댓글 수를 컬럼으로 두지 않고 {@code COUNT()} 로 센다. 비정규화
 * 컬럼은 동기화 버그 위험이 성능 이득보다 크다는 판단이다.
 */
public record ReviewListItem(
        Long id,
        String title,
        String content,
        String foodName,
        String nickname,
        int viewCount,
        int likeCount,
        int commentCount,
        String createdAt,
        boolean hasFile) {
}
