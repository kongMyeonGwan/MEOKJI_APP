package com.meokji.api.review.dto;

import lombok.Getter;
import com.meokji.api.review.repository.ReviewCommentView;
import com.meokji.api.util.Times;

@Getter
public class ReviewCommentResponse {
    private final Long id;
    private final String nickname;
    private final String content;
    private final String createdAt;
    private final Boolean isMine;

    public ReviewCommentResponse(ReviewCommentView row, boolean isMine) {
        this.id = row.getId();
        this.nickname = row.getNickname();
        this.content = row.getContent();
        this.createdAt = Times.toText(row.getCreatedAt());
        this.isMine = isMine;
    }
}
