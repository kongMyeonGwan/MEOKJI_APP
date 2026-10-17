package com.meokji.api.free.dto;

import com.meokji.api.free.repository.FreeCommentView;
import com.meokji.api.util.Times;

import lombok.Getter;

/**
 * 댓글 한 건 (서버 → 화면).
 *
 * { "id", "nickname", "content", "created_at", "is_mine" }
 *
 * 쓴 사람의 회원 번호는 내보내지 않는다. 화면은 is_mine 만 보고
 * 수정 · 삭제 버튼을 보일지 정한다.
 */
@Getter
public class FreeCommentResponse {

    private final Long id;
    private final String nickname;
    private final String content;
    private final String createdAt;

    /**
     * 이 댓글을 로그인한 사람이 썼는지. 비로그인이면 false.
     *
     * 글의 is_mine(FreePostDetailResponse)과 다른 값이다. 이쪽은 이 댓글을
     * 쓴 사람 기준이라, 남의 글에 달린 내 댓글은 글 쪽이 false 여도 여기는
     * true 다. 화면은 이 칸만 보고 그 댓글의 수정 · 삭제 버튼을 정한다.
     *
     * boolean 이 아니라 Boolean 인 이유: boolean isMine 이면 롬복이 isMine() 을
     * 만들고, Jackson 이 그 is 를 접두어로 보고 떼어내 "mine" 으로 내보낸다.
     * 화면은 is_mine 을 읽으므로 값이 통째로 사라진다.
     */
    private final Boolean isMine;

    public FreeCommentResponse(FreeCommentView row, boolean isMine) {
        this.id = row.getId();
        this.nickname = row.getNickname();
        this.content = row.getContent();
        this.createdAt = Times.toText(row.getCreatedAt());
        this.isMine = isMine;
    }
}
