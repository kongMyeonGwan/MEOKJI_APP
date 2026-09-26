package com.meokji.api.notice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 공지 작성 · 수정. 관리자({@code users.user_level = 1})만 쓸 수 있다.
 *
 * <p>작성자를 본문으로 받지 않는다. 토큰의 주인이 작성자다 - 받게 두면 남의
 * 이름으로 공지를 쓸 수 있다.
 *
 * <p>본문 상한이 글자 수가 아니라 <b>4000바이트</b>다. 컬럼이
 * {@code VARCHAR2(4000)} 이고 이 DB 의 길이 단위가 BYTE 라서, 한글로는 약
 * 1333자다. 글자 수로 검사하면 통과했는데 INSERT 에서 잘린다.
 */
public record NoticeCreateRequest(

        @NotBlank(message = "제목을 입력해주세요")
        @Size(max = 200, message = "제목은 200자 이하여야 합니다")
        String title,

        @NotBlank(message = "내용을 입력해주세요")
        String content,

        /** 상단 고정. 안 보내면 false. */
        Boolean pinned) {

    /** 본문 상한(바이트). 컬럼이 VARCHAR2(4000)이고 단위가 BYTE 다. */
    public static final int CONTENT_MAX_BYTES = 4000;

    /** 안 보내면 고정하지 않는다. */
    public boolean pinnedOrFalse() {
        return Boolean.TRUE.equals(pinned);
    }
}
