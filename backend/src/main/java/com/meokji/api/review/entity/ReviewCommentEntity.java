package com.meokji.api.review.entity;

import com.meokji.api.free.entity.PostCommentEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLRestriction;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(name = "post_comments")
@SQLRestriction("board_type = 'R'")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ReviewCommentEntity {

    private static final String Review_Board = "R";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "board_type", nullable = false, updatable = false, length = 1)
    private String boardType;

    @Column(name = "post_id", nullable = false, updatable = false)
    private Long postId;

    @Column(name = "user_id", nullable = false, updatable = false)
    private Long userId;

    @Column(name = "content", nullable = false, length = 1000)
    private String content;

    @Column(name = "use_yn", nullable = false, length = 1)
    private String useYn;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public static ReviewCommentEntity create(Long postId, Long userId, String content) {
        ReviewCommentEntity comment = new ReviewCommentEntity();
        comment.boardType = Review_Board;
        comment.postId = postId;
        comment.userId = userId;
        comment.content = content;
        comment.useYn = "Y";
        comment.updatedAt = nowUtc();
        return comment;
    }

    public void change(String content) {
        this.content = content;
        this.updatedAt = nowUtc();
    }

    public void deactivate() {
        this.useYn = "N";
        this.updatedAt = nowUtc();
    }

    private static LocalDateTime nowUtc() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }
}
