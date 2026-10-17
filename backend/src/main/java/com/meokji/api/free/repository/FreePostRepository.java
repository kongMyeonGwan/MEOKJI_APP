package com.meokji.api.free.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.meokji.api.free.entity.FreePostEntity;

/**
 * FREE_POSTS 에 JPA 로 보내는 것.
 *
 * 글쓰기 · 수정 · 삭제 · 첨부는 JpaRepository 가 이미 가진 save() 와
 * 엔티티 변경 감지로 끝난다. 여기 적은 것은 그것만으로 안 되는 것들이다.
 *
 * 목록 · 개수 · 닉네임은 네이티브 쿼리다
 *
 *   글쓴이를 @ManyToOne 으로 걸지 않았으므로(FreePostEntity 참고) JPQL 로는
 *   USERS 를 JOIN 할 수 없다. 본문 앞 200자 자르기(DBMS_LOB.SUBSTR)와
 *   댓글 수 스칼라 서브쿼리도 오라클 SQL 그대로가 가장 단순하다.
 *
 *   검색 조건은 개수와 목록이 똑같아야 한다. 한쪽만 고치면
 *   "3페이지가 있다는데 열면 비어 있는" 일이 생긴다.
 *
 *   keyword 가 없으면 서비스가 null 로 넘긴다. content 는 CLOB 이라 LIKE 가
 *   되지만 느리다. 글이 많아지면 제목만 찾도록 줄이는 것이 맞다.
 */
public interface FreePostRepository extends JpaRepository<FreePostEntity, Long> {

    /**
     * 살아있는 글 하나. 지운 글(use_yn='N')은 안 나온다
     *
     * 이름이 곧 조건이다 — findBy + Id + And + UseYn 을 보고 JPA 가
     * WHERE id = ? AND use_yn = ? 를 만든다. 그래서 이름을 잘못 적으면
     * 서버가 뜰 때 "그런 칸이 없다" 로 막힌다.
     *
     * Optional 로 받는 이유 — JPA 가 정한 모양이다. 못 찾았을 때 null 이
     * 아니라 "빈 Optional" 이 와서, 받는 쪽이 null 확인을 빠뜨릴 수 없다.
     * (프로젝트의 다른 자리에서는 Optional 을 쓰지 않는다 — MyBatis 는
     *  못 찾으면 그냥 null 을 준다)
     *
     * useYn 을 인자로 받는 모양인 이유 — 메서드 이름으로 값을 고정할 수
     * 없다. 부르는 쪽이 늘 "Y" 를 넘긴다.
     */
    Optional<FreePostEntity> findByIdAndUseYn(Long id, String useYn);

    /** 조건에 맞는 살아있는 글 수 */
    @Query(value = """
            SELECT COUNT(p.id)
              FROM free_posts p
             WHERE p.use_yn = 'Y'
               AND (:keyword IS NULL
                    OR LOWER(p.title)   LIKE '%' || LOWER(:keyword) || '%'
                    OR LOWER(p.content) LIKE '%' || LOWER(:keyword) || '%')
            """, nativeQuery = true)
    int countAlive(@Param("keyword") String keyword);

    /**
     * 목록 한 페이지. 최신순.
     *
     * OFFSET ... FETCH NEXT 는 오라클 12c 부터 된다. ROWNUM 으로 자르면
     * 정렬 전에 매겨져서 아무 행이나 나온다.
     *
     * 댓글 수는 스칼라 서브쿼리로 센다. 컬럼으로 들고 있으면 댓글을 지웠을 때
     * 같이 줄이는 것을 잊어 조용히 어긋난다.
     */
    @Query(value = """
            SELECT p.id                               AS "id",
                   p.title                            AS "title",
                   DBMS_LOB.SUBSTR(p.content, 200, 1) AS "content",
                   u.nickname                         AS "nickname",
                   p.view_count                       AS "viewCount",
                   p.created_at                       AS "createdAt",
                   p.file_yn                          AS "fileYn",
                   (SELECT COUNT(c.id)
                      FROM post_comments c
                     WHERE c.board_type = 'F'
                       AND c.post_id = p.id
                       AND c.use_yn = 'Y')            AS "commentCount"
              FROM free_posts p
              JOIN users u ON u.id = p.user_id
             WHERE p.use_yn = 'Y'
               AND (:keyword IS NULL
                    OR LOWER(p.title)   LIKE '%' || LOWER(:keyword) || '%'
                    OR LOWER(p.content) LIKE '%' || LOWER(:keyword) || '%')
             ORDER BY p.created_at DESC, p.id DESC
             OFFSET :offset ROWS FETCH NEXT :size ROWS ONLY
            """, nativeQuery = true)
    List<FreePostListView> findPage(
            @Param("keyword") String keyword,
            @Param("offset") int offset,
            @Param("size") int size);

    /** 글쓴이 닉네임. 상세 화면용 */
    @Query(value = "SELECT nickname FROM users WHERE id = :userId", nativeQuery = true)
    String findNickname(@Param("userId") Long userId);

    /**
     * 조회수 +1. 바뀐 행 수를 돌려준다 (지운 글이면 0).
     *
     * 엔티티의 viewCount 를 +1 해서 변경 감지로 보내지 않는다. 그러면
     * 동시에 연 두 사람이 같은 옛 값을 읽고 둘 다 +1 을 써서 하나가 사라진다.
     */
    @Modifying
    @Query(value = """
            UPDATE free_posts
               SET view_count = view_count + 1
             WHERE id = :id
               AND use_yn = 'Y'
            """, nativeQuery = true)
    int increaseView(@Param("id") Long id);

    /** 조회수 읽기 (올린 뒤 값을 돌려주기 위해) */
    @Query(value = "SELECT view_count FROM free_posts WHERE id = :id", nativeQuery = true)
    int findViewCount(@Param("id") Long id);
}
