package com.meokji.api.vo;

import java.time.LocalDateTime;

/**
 * REVIEW_COMMENTS 표 한 행. 후기에 달린 댓글.
 *
 * <p>대댓글은 없다. 한 단계만 두기로 한 결정이고, 필요해지면
 * {@code parent_id} 를 추가하는 것이 아니라 화면 설계부터 다시 봐야 한다.
 */
public class ReviewComment {

    private Long id;

    /** 어떤 후기의 댓글인지 */
    private Long reviewId;

    private Long userId;

    private String content;

    private String useYn;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getReviewId() {
        return reviewId;
    }

    public void setReviewId(Long reviewId) {
        this.reviewId = reviewId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getUseYn() {
        return useYn;
    }

    public void setUseYn(String useYn) {
        this.useYn = useYn;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
