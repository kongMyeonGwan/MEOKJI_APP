package com.meokji.api.review.dto;

import com.meokji.api.review.repository.ReviewListView;
import com.meokji.api.util.Times;
import lombok.Getter;

@Getter
public class ReviewListResponse {

    private final Long id;
    private final String title;
    private final String content;
    private final String nickname;
    private final Integer viewCount;
    private final Integer commentCount;
    private final boolean hasFile;
    private final String createdAt;

    public ReviewListResponse(ReviewListView row){
        this.id = row.getId();
        this.title = row.getTitle();
        this.content = row.getContent();
        this.nickname = row.getNickname();
        this.viewCount = row.getViewCount();
        this.commentCount = row.getCommentCount();
        this.hasFile = "Y".equals(row.getFileYn());
        this.createdAt = Times.toText(row.getCreatedAt());
    }
}
