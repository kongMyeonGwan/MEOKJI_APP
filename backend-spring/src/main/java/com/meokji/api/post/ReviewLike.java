package com.meokji.api.post;

import java.time.LocalDateTime;

/**
 * REVIEW_LIKES 표 한 행. 후기에 누른 좋아요.
 *
 * <p>해제를 행 삭제로 처리하지 않는다. 지우고 다시 만들면 {@code created_at} 이
 * 매번 바뀌어 "언제 눌렀는지" 가 사라지고, 인기 레시피의 기간 집계가 틀어진다.
 * {@code use_yn} 만 뒤집는다.
 *
 * <p>{@code uq_review_likes} 는 {@code use_yn='Y'} 일 때만 거는 함수 기반 UNIQUE 라
 * 해제한 것을 다시 누를 수 있다.
 */
public class ReviewLike {

    private Long id;

    private Long userId;

    private Long reviewId;

    private String useYn;

    private LocalDateTime createdAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Long getReviewId() {
        return reviewId;
    }

    public void setReviewId(Long reviewId) {
        this.reviewId = reviewId;
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
}
