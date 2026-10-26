package com.meokji.api.review.dto;

import com.meokji.api.review.repository.ReviewListView;
import com.meokji.api.util.Times;
import lombok.Getter;

@Getter
public class ReviewListResponse {
    private final Long id;
    private final String title;
    private final String content;
    private final String nickname;        // 글쓴이 닉네임 (USERS 와 JOIN)
    private final Integer viewCount;
    private final Long likeCount;
    private final Integer commentCount;   // 살아있는 댓글 수
    private final boolean hasFile;        // 제목 옆 클립 표시용
    private final String createdAt;       // "2026-10-04T12:30:00Z" (UTC)


    // 생성자
    public ReviewListResponse(ReviewListView row) {
        this.id = row.getId();
        this.title = row.getTitle();
        this.content = row.getContent();
        this.nickname = row.getNickname();
        this.viewCount = row.getViewCount();
        this.likeCount = row.getLikeCount();
        this.commentCount = row.getCommentCount();
        this.hasFile = "Y".equals(row.getFileYn()); // 두 값이 같은지 비교하는 메서드
        this.createdAt = Times.toText(row.getCreatedAt()); // 날짜를 문자열로 변환
    }


}
