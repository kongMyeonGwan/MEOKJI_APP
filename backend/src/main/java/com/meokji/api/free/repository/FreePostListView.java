package com.meokji.api.free.repository;

import java.time.LocalDateTime;

/**
 * 목록 한 줄. FreePostRepository.findPage 의 네이티브 쿼리 결과를 받는 모양이다.
 *
 * 메서드 이름과 SQL 의 별칭(AS "viewCount")이 짝이다. 오라클은 따옴표 없는
 * 별칭을 대문자로 돌려주므로 SQL 쪽 별칭은 따옴표로 감싸 대소문자를 맞춘다.
 *
 * 숫자는 오라클이 BigDecimal 로, 시각은 Timestamp 로 주지만 스프링이
 * 여기 적힌 타입(Long · LocalDateTime)으로 바꿔서 돌려준다.
 */
public interface FreePostListView {

    Long getId();

    String getTitle();

    /** 본문 앞 200자 (DBMS_LOB.SUBSTR 로 자른 값) */
    String getContent();

    /** 글쓴이 닉네임 (USERS 와 JOIN) */
    String getNickname();

    Integer getViewCount();

    LocalDateTime getCreatedAt();

    /** 'Y' = 첨부 있음 */
    String getFileYn();

    /** 살아있는 댓글 수 */
    Integer getCommentCount();
}
