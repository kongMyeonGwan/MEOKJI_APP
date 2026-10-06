package com.meokji.api.review.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

/**
 * REVIEW_LIKES 표 한 행. 후기에 누른 좋아요.
 *
 * 좋아요를 켜고 끈 결과도 이 클래스로 돌려준다 — PUT · DELETE
 * /api/reviews/{id}/like 의 응답이 { "liked": true, "like_count": 12 } 다.
 * 그 둘이 다른 것이 아니다. liked 는 내 행의 use_yn 이고, like_count 는
 * 같은 표를 센 값이다.
 *
 * 해제를 행 삭제로 처리하지 않는다. 지우고 다시 만들면 created_at 이
 * 매번 바뀌어 "언제 눌렀는지" 가 사라지고, 인기 레시피의 기간 집계가 틀어진다.
 * use_yn 만 뒤집는다.
 *
 * uq_review_likes 는 use_yn='Y' 일 때만 거는 함수 기반 UNIQUE 라
 * 해제한 것을 다시 누를 수 있다.
 *
 * 화면이 정할 수 있는 칸이 없다. 누가(토큰) 어느 글에(주소) 눌렀는지는
 * 요청 본문이 아니라 둘 다 바깥에서 오고, 켜는지 끄는지는 PUT 이냐
 * DELETE 냐로 가른다. 그래서 표 칸은 전부 @JsonIgnore 다.
 */
@Data
public class ReviewLikeVO {

    @JsonIgnore
    private Long id;

    /** 누가 눌렀는지. 토큰에서 꺼낸다 */
    @JsonIgnore
    private Long userId;

    /** 어느 글에 눌렀는지. 주소(/reviews/3/like)에서 꺼낸다 */
    @JsonIgnore
    private Long reviewId;

    /** 'Y' = 지금 눌러져 있다. 아래 liked 가 이 값이다 */
    @JsonIgnore
    private String useYn;

    @JsonIgnore
    private LocalDateTime createdAt;

    // ---------- 표에 없는 칸 ----------

    /**
     * 이 글의 좋아요 수. 바꾼 뒤 다시 센 값이다.
     *
     * 화면이 1 을 더하거나 빼서 스스로 맞추면, 다른 사람이 같은 시간에
     * 누른 것과 어긋난다. (docs/BOARD_SPEC.md 좋아요 항목)
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private int likeCount;

    /**
     * 지금 눌러져 있는지. use_yn 에서 나온다.
     *
     * 칸을 따로 두지 않는 이유: 같은 것을 두 군데 두면 use_yn 만 바꾸고
     * liked 를 안 바꾸는 실수가 난다. 아직 한 번도 안 누른 사람은 행이
     * 없어서 use_yn 이 null 이고, 그때도 false 가 나온다.
     */
    @JsonProperty("liked")
    public boolean getLiked() {
        return "Y".equals(useYn);
    }
}
