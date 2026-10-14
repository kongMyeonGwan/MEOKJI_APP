package com.meokji.api.free.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.meokji.api.util.Times;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

/**
 * POST_COMMENTS 표 한 행 중 자유게시판 것(BOARD_TYPE='F').
 *
 * 레시피리뷰 댓글과 표를 같이 쓰지만 BOARD_TYPE 은 이 클래스에 두지 않았다.
 * PostCommentEntity 와 그 Repository 가 모든 조회에 'F' 를 붙여서, 여기 담기는 행은 언제나
 * 자유게시판 댓글이다. 칸을 두면 누가 'R' 을 넣어 보낼 여지만 생긴다.
 *
 * 대댓글은 없다. 한 단계만 두기로 한 결정이고, 필요해지면 parent_id 를
 * 더하는 것이 아니라 화면 설계부터 다시 봐야 한다.
 *
 * 댓글 쓰기 요청도, 화면으로 나갈 모양도 이 클래스가 겸한다.
 * 나가는 모양: { "id", "nickname", "content", "created_at", "is_mine" }
 */
@Data
public class FreePostCommentVO {

    private Long id;

    /** 어떤 글의 댓글인지. 주소(/free-posts/3/comments)에서 꺼내므로 본문으로 받지 않는다 */
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

    /** 화면이 읽는 created_at. 예) "2026-10-04T12:30:00Z" */
    @JsonProperty("created_at")
    public String getCreatedAtText() {
        return Times.toText(createdAt);
    }
}
