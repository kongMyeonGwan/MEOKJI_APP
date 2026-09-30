package com.meokji.api.dto.review;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 댓글 한 건.
 *
 * isMine 은 지금 보는 사람이 쓴 댓글인지다. 화면이 이 값으로 수정 ·
 * 삭제 버튼을 보여준다 - 다만 버튼을 숨기는 것은 잠그는 것이 아니다.
 * 서버가 다시 확인해야 한다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {

    private Long id;

    private String nickname;

    private String content;

    private String createdAt;

    private Boolean isMine;
}
