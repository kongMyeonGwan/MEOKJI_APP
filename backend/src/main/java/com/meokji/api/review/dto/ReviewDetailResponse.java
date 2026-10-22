package com.meokji.api.review.dto;

import java.util.List;

import com.meokji.api.review.entity.ReviewEntity;
import com.meokji.api.util.Times;
import lombok.Getter;

@Getter
public class ReviewDetailResponse {

    private final Long id;
    private final String title;
    private final Long meal_history_id;         // 먹은 레시피
    private final String content;               // 본문 전체
    private final String nickname;              // 글쓴이 닉네임
    private final Integer viewCount;
    private final Integer commentCount;         // 아래 comments 의 개수
    private final boolean hasFile;
    private final ReviewFileResponse file;        // 첨부가 없으면 null — 화면이 사진 자리를 안 그린다

    private final Boolean isMine;

    private final List<ReviewCommentResponse> comments; // 오래된 순
    private final String createdAt;
    private final String updatedAt;

    public ReviewDetailResponse(ReviewEntity review, String nickname, ReviewFileResponse file,
                                List<ReviewCommentResponse> comments, boolean isMine){
        this.id = review.getId();
        this.title = review.getTitle();
        this.meal_history_id = review.getMeal_history_Id();
        this.content = review.getContent();
        this.nickname = nickname;
        this.viewCount = review.getViewCount() == null ? 0 :review.getViewCount();
        this.commentCount = comments.size();
        this.hasFile = review.hasFile();
        this.file = file;
        this.isMine = isMine;
        this.comments = comments;
        this.createdAt = Times.toText(review.getCreatedAt());
        this.updatedAt = Times.toText(review.getUpdatedAt());
        }
}
