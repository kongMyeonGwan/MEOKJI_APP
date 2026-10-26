package com.meokji.api.review.repository;

import com.meokji.api.history.entity.MealHistoryEntity;
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
                   m.food_name                        AS "foodName",
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
              JOIN meal_histories m ON m.id = r.meal_history_id
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

    // 후기 한 건에 딸린 식사 기록. 레시피 내용은 RECIPE_REVIEWS 가 아니라 MEAL_HISTORIES 에 있다.
    //
    // 인터페이스 프로젝션이 아니라 엔티티로 받는다. RECIPE_JSON 같은 CLOB 칸을
    // 프로젝션은 String 으로 못 바꾼다 (Cannot project ... Clob to java.lang.String).
    @Query(value = """
            SELECT m.*
              FROM recipe_reviews r
              JOIN meal_histories m ON m.id = r.meal_history_id
             WHERE r.id = :reviewId
            """, nativeQuery = true)
    MealHistoryEntity findMeal(@Param("reviewId") Long reviewId);

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

    // [인기 레시피] 선택한 기간에 좋아요를 많이 받은 게시글 조회
    @Query(value = """
    SELECT R.ID AS "id",
           R.TITLE AS "title",
        MAX(DBMS_LOB.SUBSTR(R.CONTENT, 200, 1)) AS "summary",
        COUNT(*) AS "likeCount",
        M.FOOD_NAME AS "foodName",
        M.RECIPE_NAME AS "recipeName",
        U.NICKNAME AS "nickname",
        M.ESTIMATED_COST AS "estimatedCost",
        JSON_VALUE(M.RECIPE_JSON, '$.cook_time_minutes' RETURNING NUMBER) AS "cookTimeMinutes",
            (SELECT COUNT(c.id)
               FROM post_comments c
              WHERE c.board_type = 'R'
                AND c.post_id = R.ID
                AND c.use_yn = 'Y') AS "commentCount"
    FROM REVIEW_LIKES L
    JOIN RECIPE_REVIEWS R ON L.REVIEW_ID = R.ID
        JOIN MEAL_HISTORIES M ON R.MEAL_HISTORY_ID = M.ID
        JOIN USERS U ON R.USER_ID = U.ID
    WHERE L.USE_YN = 'Y'
      AND R.USE_YN = 'Y'
      AND L.CREATED_AT >= TO_DATE(:start, 'YYYY-MM-DD')
      AND L.CREATED_AT < TO_DATE(:end, 'YYYY-MM-DD') + 1
    GROUP BY R.ID,
             R.TITLE,
             M.FOOD_NAME,
             M.RECIPE_NAME,
             U.NICKNAME,
             M.ESTIMATED_COST,
             JSON_VALUE(M.RECIPE_JSON,'$.cook_time_minutes' RETURNING NUMBER)
    ORDER BY COUNT(*) DESC, R.ID ASC
    FETCH FIRST 6 ROWS ONLY
    """, nativeQuery = true)
    List<ReviewPopularView> findPopular(@Param("start") String start,
                                        @Param("end") String end);

}
