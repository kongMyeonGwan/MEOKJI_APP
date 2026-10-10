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

    /** 시퀀스로 받는 번호. 수정 · 삭제 주소가 이 값을 쓴다 */
    private Long id;

    /** 어떤 후기의 댓글인지. 표에서는 POST_ID 이고 BOARD_TYPE='R' 과 짝으로 본다 */
    @JsonIgnore
    private Long postId;

    /** 쓴 사람. 토큰에서 정하므로 받지도, 내보내지도 않는다 */
    // 번호를 내보내지 않는 대신 아래 isMine 으로 "내 댓글인지" 만 알려준다
    @JsonIgnore
    private Long userId;

    @NotBlank(message = "댓글을 입력해주세요")
    @Size(max = 1000, message = "댓글은 1000자 이하여야 합니다")
    private String content;

    /** 'N' = 지운 댓글. 목록 SQL 이 Y 만 가려 오므로 화면에 알릴 필요가 없다 */
    @JsonIgnore
    private String useYn;

    /** 쓴 시각(UTC). 아래 getCreatedAtText 로 글자가 되어 나간다 */
    @JsonIgnore
    private LocalDateTime createdAt;

    /** 고친 시각. 화면에 내보내지 않는다 - 댓글은 "쓴 시각" 만 보여준다 */
    @JsonIgnore
    private LocalDateTime updatedAt;

    // ---------- 표에 없는 칸. 조회할 때 JOIN 하거나 서비스가 채운다 ----------

    /** 댓글을 쓴 사람 */
    // POST_COMMENTS 에 없는 칸이다. USERS 를 JOIN 해서 가져온다.
    // 댓글마다 닉네임을 같이 저장하지 않는 이유 — 닉네임을 바꾸면
    // 과거 댓글의 이름까지 다 UPDATE 해야 한다
    private String nickname;

    /** 로그인한 사람이 쓴 댓글인지. 비로그인이면 false */
    // 서비스가 채운다. SQL 로는 알 수 없다 - 누가 보고 있는지는
    // 토큰에서 나오는 값이라 DB 가 모른다.
    //
    // Boolean 인 이유는 MyBatis 가 채우지 않는 칸이라 null 로 남을 수
    // 있기 때문이다
    private Boolean isMine;

    /** 화면이 읽는 created_at. 예) "2026-09-11T21:29:42Z" */
    // 이름을 손으로 적는 이유 — snake_case 설정은 메서드 이름에서
    // created_at_text 를 만든다. 뒤에 붙은 Text 까지 이름에 들어간다
    @JsonProperty("created_at")
    public String getCreatedAtText() {
        return Times.toText(createdAt);
    }
}
