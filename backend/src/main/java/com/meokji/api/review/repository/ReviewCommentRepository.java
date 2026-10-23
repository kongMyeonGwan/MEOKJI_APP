package com.meokji.api.review.repository;

import com.meokji.api.review.entity.ReviewCommentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

/** 후기 게시판 댓글(board_type = 'R')의 저장과 조회. */
public interface ReviewCommentRepository extends JpaRepository<ReviewCommentEntity, Long> {

    Optional<ReviewCommentEntity> findByIdAndPostIdAndUseYn(Long id, Long postId, String useYn);

    @Query(value = """
            SELECT c.id         AS "id",
                   c.user_id    AS "userId",
                   c.content    AS "content",
                   u.nickname   AS "nickname",
                   c.created_at AS "createdAt"
              FROM post_comments c
              JOIN users u ON u.id = c.user_id
             WHERE c.board_type = 'R'
               AND c.post_id = :postId
               AND c.use_yn = 'Y'
             ORDER BY c.created_at, c.id
            """, nativeQuery = true)
    List<ReviewCommentView> findViewsByPost(@Param("postId") Long postId);

    @Query(value = """
            SELECT c.id         AS "id",
                   c.user_id    AS "userId",
                   c.content    AS "content",
                   u.nickname   AS "nickname",
                   c.created_at AS "createdAt"
              FROM post_comments c
              JOIN users u ON u.id = c.user_id
             WHERE c.board_type = 'R'
               AND c.id = :id
               AND c.use_yn = 'Y'
            """, nativeQuery = true)
    Optional<ReviewCommentView> findView(@Param("id") Long id);
}
