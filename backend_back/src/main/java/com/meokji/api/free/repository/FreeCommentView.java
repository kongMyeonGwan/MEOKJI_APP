package com.meokji.api.free.repository;

import java.time.LocalDateTime;

/**
 * 화면에 나갈 댓글 한 건. PostCommentRepository 의 네이티브 쿼리 결과를 받는 모양이다.
 * 별칭과 메서드 이름의 짝은 FreePostListView 와 같은 규칙이다.
 */
public interface FreeCommentView {

    Long getId();

    Long getUserId();

    String getContent();

    /** 댓글 쓴 사람 닉네임 (USERS 와 JOIN) */
    String getNickname();

    LocalDateTime getCreatedAt();
}
