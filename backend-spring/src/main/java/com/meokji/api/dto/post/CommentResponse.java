package com.meokji.api.dto.post;

/**
 * 댓글 한 건.
 *
 * <p>{@code isMine} 은 지금 보는 사람이 쓴 댓글인지다. 화면이 이 값으로 수정 ·
 * 삭제 버튼을 보여준다 - 다만 <b>버튼을 숨기는 것은 잠그는 것이 아니다.</b>
 * 서버가 다시 확인해야 한다.
 */
public record CommentResponse(
        Long id,
        String nickname,
        String content,
        String createdAt,
        boolean isMine) {
}
