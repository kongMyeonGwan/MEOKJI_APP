
package com.meokji.api.review.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "review_likes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewLikeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 좋아요를 누른 회원 번호
    @Column(name = "user_id", nullable = false)
    private Long userId;

    // 좋아요가 달린 게시글 번호
    @Column(name = "review_id", nullable = false)
    private Long reviewId;

    // 좋아요 활성화 여부 (Y/N)
    @Column(name = "use_yn", nullable = false, length = 1)
    private String useYn;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public static ReviewLikeEntity create(Long userId, Long reviewId) {
        ReviewLikeEntity like = new ReviewLikeEntity();

        like.userId = userId;
        like.reviewId = reviewId;
        like.useYn = "Y";
        like.createdAt = LocalDateTime.now(ZoneId.of("Asia/Seoul"));

        return like;
    }

    public void activate() {
        this.useYn = "Y";
    }

    public void deactivate() {
        this.useYn = "N";
    }
}
