package com.meokji.api.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * REVIEW_COMMENTS 표 한 행. 후기에 달린 댓글.
 *
 * 대댓글은 없다. 한 단계만 두기로 한 결정이고, 필요해지면
 * parent_id 를 추가하는 것이 아니라 화면 설계부터 다시 봐야 한다.
 */
@Data
public class ReviewCommentVO {

    private Long id;

    /** 어떤 후기의 댓글인지 */
    private Long reviewId;

    private Long userId;

    private String content;

    private String useYn;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;
}
