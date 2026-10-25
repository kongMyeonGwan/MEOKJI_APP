package com.meokji.api.review.repository;

import com.meokji.api.review.entity.ReviewLikeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReviewLikeRepository extends JpaRepository<ReviewLikeEntity, Long> {

    // userId, reviewId가 모두 일치하는 좋아요 기록 찾기
    Optional<ReviewLikeEntity> findByUserIdAndReviewId(Long userId, Long reviewId);

    // 좋아요 개수를 반환하는 메서드
    long countByReviewIdAndUseYn(
            Long reviewId, String useYn
    );
    // existsBy... boolean(으)로 반환, 좋아요 존재 여부 확인
    boolean existsByUserIdAndReviewIdAndUseYn(
            Long userId, Long reviewId, String useYn
    );






}
