package com.meokji.api.free.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.meokji.api.free.entity.PostCommentEntity;

/**
 * POST_COMMENTS 중 자유게시판 댓글('F').
 *
 * 메서드 이름으로 만드는 조회는 엔티티의 @SQLRestriction 이 'F' 를 붙여준다.
 * 네이티브 쿼리는 그렇지 않으므로 'F' 를 직접 적는다 — 빠뜨리면 레시피리뷰
 * 댓글이 섞여 나온다.
 */
public interface PostCommentRepository extends JpaRepository<PostCommentEntity, Long> {

    /**
     * 살아있는 댓글 하나.
     *
     * postId 를 같이 보는 이유: /free-posts/1/comments/7 처럼 남의 글 주소에
     * 내 댓글 번호를 끼워 넣는 요청을 막는다.
     */
    Optional<PostCommentEntity> findByIdAndPostIdAndUseYn(Long id, Long postId, String useYn);

    /** 글 하나의 댓글 전부. 오래된 순 (대화 순서로 읽힌다) */
    @Query(value = """
            /* findViewsByPost */
            SELECT c.id         AS "id",
                   c.user_id    AS "userId",
                   c.content    AS "content",
                   u.nickname   AS "nickname",
                   c.created_at AS "createdAt"
              FROM post_comments c
              JOIN users u ON u.id = c.user_id
             WHERE c.board_type = 'F'
               AND c.post_id = :postId
               AND c.use_yn = 'Y'
             ORDER BY c.created_at, c.id
            """, nativeQuery = true)
    List<FreeCommentView> findViewsByPost(@Param("postId") Long postId);

    /** 댓글 한 건 (쓰기 · 수정 직후 화면에 돌려줄 모양) */
    @Query(value = """
            SELECT c.id         AS "id",
                   c.user_id    AS "userId",
                   c.content    AS "content",
                   u.nickname   AS "nickname",
                   c.created_at AS "createdAt"
              FROM post_comments c
              JOIN users u ON u.id = c.user_id
             WHERE c.board_type = 'F'
               AND c.id = :id
            """, nativeQuery = true)
    FreeCommentView findView(@Param("id") Long id);
}
