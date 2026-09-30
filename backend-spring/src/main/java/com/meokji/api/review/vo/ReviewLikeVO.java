package com.meokji.api.review.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * REVIEW_LIKES 표 한 행. 후기에 누른 좋아요.
 *
 * 해제를 행 삭제로 처리하지 않는다. 지우고 다시 만들면 created_at 이
 * 매번 바뀌어 "언제 눌렀는지" 가 사라지고, 인기 레시피의 기간 집계가 틀어진다.
 * use_yn 만 뒤집는다.
 *
 * uq_review_likes 는 use_yn='Y' 일 때만 거는 함수 기반 UNIQUE 라
 * 해제한 것을 다시 누를 수 있다.
 */
@Data
public class ReviewLikeVO {

    private Long id;

    private Long userId;

    private Long reviewId;

    private String useYn;

    private LocalDateTime createdAt;
}
