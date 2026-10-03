package com.meokji.api.review.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.meokji.api.util.Times;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

/**
 * POST_COMMENTS 표 한 행 중 레시피리뷰 것(BOARD_TYPE='R').
 *
 * 자유게시판 댓글과 표를 같이 쓴다. 매퍼를 만들 때 모든 문장에
 * BOARD_TYPE='R' 을 넣어야 한다 — 빠뜨리면 자유게시판 댓글이 섞여 나온다.
 * (본보기: resources/mapper/FreeCommentMapper.xml)
 *
 * 대댓글은 없다. 한 단계만 두기로 한 결정이고, 필요해지면
 * parent_id 를 추가하는 것이 아니라 화면 설계부터 다시 봐야 한다.
 *
 * 댓글 쓰기 요청도, 화면으로 나갈 모양도 이 클래스가 겸한다.
 * 나가는 모양: { "id", "nickname", "content", "created_at", "is_mine" }
 */
@Data
public class ReviewCommentVO {

    private Long id;

    /** 어떤 후기의 댓글인지. 표에서는 POST_ID 이고 BOARD_TYPE='R' 과 짝으로 본다 */
    @JsonIgnore
    private Long postId;

    @JsonIgnore
    private Long userId;

    @NotBlank(message = "댓글을 입력해주세요")
    @Size(max = 1000, message = "댓글은 1000자 이하여야 합니다")
    private String content;

    @JsonIgnore
    private String useYn;

    @JsonIgnore
    private LocalDateTime createdAt;

    @JsonIgnore
    private LocalDateTime updatedAt;

    // ---------- 표에 없는 칸. 조회할 때 JOIN 하거나 서비스가 채운다 ----------

    /** 댓글을 쓴 사람 */
    private String nickname;

    /** 로그인한 사람이 쓴 댓글인지. 비로그인이면 false */
    private Boolean isMine;

    /** 화면이 읽는 created_at. 예) "2026-09-11T21:29:42Z" */
    @JsonProperty("created_at")
    public String getCreatedAtText() {
        return Times.toText(createdAt);
    }
}
