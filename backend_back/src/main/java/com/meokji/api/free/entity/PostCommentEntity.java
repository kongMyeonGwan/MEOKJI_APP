package com.meokji.api.free.entity;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.hibernate.annotations.SQLRestriction;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * POST_COMMENTS 표 한 행 중 자유게시판 것(BOARD_TYPE='F').
 *
 * 레시피리뷰 댓글('R')과 표를 같이 쓴다. @SQLRestriction 이 JPA 가 이 엔티티를
 * 읽는 모든 SQL 에 board_type = 'F' 를 붙인다 — 하나라도 빠뜨리면 남의 게시판
 * 댓글이 섞여 나오므로 메서드마다 적지 않고 여기 한 번만 둔다.
 * 단, 네이티브 쿼리에는 붙지 않는다. PostCommentRepository 의 네이티브 쿼리는
 * 'F' 를 직접 적는다.
 *
 * id 는 오라클 IDENTITY 다 (FREE_POSTS 처럼 시퀀스가 아니다).
 * JPA 가 번호를 받으려고 save() 하는 순간 INSERT 를 바로 보낸다.
 *
 * 글쓴이는 FreePostEntity 와 같은 이유로 @ManyToOne 없이 userId 숫자로 둔다.
 * 시각도 같은 이유로 UTC 다.
 */
@Entity
@Table(name = "post_comments")
@SQLRestriction("board_type = 'F'")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PostCommentEntity {

    private static final String FREE_BOARD = "F";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 어느 게시판의 댓글인지. 이 엔티티로 만드는 행은 언제나 'F' */
    @Column(name = "board_type", nullable = false, updatable = false, length = 1)
    private String boardType;

    /** 댓글이 달린 글 */
    @Column(name = "post_id", nullable = false, updatable = false)
    private Long postId;

    /** 댓글 쓴 사람 */
    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    @Column(name = "use_yn", nullable = false, length = 1)
    private String useYn;

    /** 읽기만 한다. DB 기본값이 넣는다 */
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /** 새 자유게시판 댓글 */
    public static PostCommentEntity create(Long postId, Long userId, String content) {
        PostCommentEntity comment = new PostCommentEntity();
        comment.boardType = FREE_BOARD;
        comment.postId = postId;
        comment.userId = userId;
        comment.content = content;
        comment.useYn = "Y";
        comment.updatedAt = nowUtc();
        return comment;
    }

    /** 내용 고치기. 변경 감지로 UPDATE 가 나간다 */
    public void change(String content) {
        this.content = content;
        this.updatedAt = nowUtc();
    }

    /** 삭제. 행을 지우지 않고 use_yn='N' */
    public void deactivate() {
        this.useYn = "N";
        this.updatedAt = nowUtc();
    }

    /** DB 가 UTC 로 적으므로 여기서 넣는 값도 UTC 여야 한다 */
    private static LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
