package com.meokji.api.review.repository;

import com.meokji.api.review.entity.ReviewEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ReviewRepository extends JpaRepository<ReviewEntity, Long> {

    Optional<ReviewEntity> findByIdAndUseYn(Long id, String useYn);

    @Query(value = """
    SELECT COUNT(r.id)
    FROM recipe_reviews r
    WHERE r.use_yn = 'Y'
      AND (:keyword IS NULL
          OR LOWER(r.title)   LIKE '%' || LOWER(:keyword) || '%'
          OR LOWER(r.content) LIKE '%' || LOWER(:keyword) || '%')
        """, nativeQuery = true)

    int countAlive(@Param("keyword") String keyword);

    @Query(value = """
            SELECT r.id                               AS "id",
                   r.title                            AS "title",
                   DBMS_LOB.SUBSTR(r.content, 200, 1) AS "content",
                   u.nickname                         AS "nickname",
                   r.view_count                       AS "viewCount",
                   r.created_at                       AS "createdAt",
                   r.file_yn                          AS "fileYn",
                   (SELECT COUNT(c.id)
                      FROM post_comments c
                     WHERE c.board_type = 'R'
                       AND c.post_id = r.id
                       AND c.use_yn = 'Y')            AS "commentCount"
              FROM recipe_reviews r
              JOIN users u ON u.id = r.user_id
             WHERE r.use_yn = 'Y'
               AND (:keyword IS NULL
                    OR LOWER(r.title)   LIKE '%' || LOWER(:keyword) || '%'
                    OR LOWER(r.content) LIKE '%' || LOWER(:keyword) || '%')
             ORDER BY r.created_at DESC, r.id DESC
             OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY
            """, nativeQuery = true)
    List<ReviewListView> findPage(
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("size") int size);

    @Query(value = "SELECT nickname FROM users WHERE id = :userId", nativeQuery = true)
    String findNickname(@Param("userId") Long userId);

    @Modifying
    @Query(value = """
            UPDATE recipe_reviews
               SET view_count = view_count + 1
             WHERE id = :id
               AND use_yn = 'Y'
            """, nativeQuery = true)
    int increaseView(@Param("id") Long id);

    @Query(value = "SELECT view_count FROM recipe_reviews WHERE id = :id", nativeQuery = true)
    int findViewCount(@Param("id") Long id);

}
