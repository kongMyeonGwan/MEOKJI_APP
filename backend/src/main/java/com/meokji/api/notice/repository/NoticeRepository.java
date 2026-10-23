package com.meokji.api.notice.repository;
import com.meokji.api.notice.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface NoticeRepository extends JpaRepository<Notice,Long> {

    /*조회*/

    /*중요 공지먼저 보여주고, 최신 공지 순서로 조회*/
    @Query(value = """
            SELECT n FROM Notice n
             WHERE n.useYn = 'Y'
             ORDER BY
                CASE WHEN n.pinnedYn = 'Y' THEN 0 ELSE 1 END,
                n.createdAt DESC
            """,
            countQuery = """
            SELECT COUNT(n) FROM Notice n
             WHERE n.useYn = 'Y'
            """)
    Page<Notice> findActiveNotices(Pageable pageable);

    // ★ search(...) 쿼리를 삭제했다 (서비스 · 컨트롤러 어디서도 쓰지 않았다)

    /*상세*/
    Optional<Notice> findByIdAndUseYn(Long id, String useYn);

    /*조회수 +1*/
    @Modifying(clearAutomatically = true)
    @Query(value = """
            UPDATE notices 
               SET view_count = view_count + 1
             WHERE id = :id
               AND use_yn = 'Y'
            """, nativeQuery = true)
    int increaseView(@Param("id") Long id);

    /** 조회수 읽기 (올린 뒤 값을 돌려주기 위해) */
    @Query(value = "SELECT view_count FROM notices WHERE id = :id", nativeQuery = true)
    int findViewCount(@Param("id") Long id);


    /*삭제*/
    @Modifying(clearAutomatically = true)
    @Query(value = """
        UPDATE notices
           SET use_yn = 'N',
               updated_at = SYS_EXTRACT_UTC(SYSTIMESTAMP)
         WHERE use_yn = 'Y'
           AND id = :id
        """, nativeQuery = true)   // ★ updated_at 갱신 추가
    int softDelete(@Param("id") Long id);
}