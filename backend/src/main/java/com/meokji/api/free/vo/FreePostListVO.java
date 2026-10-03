package com.meokji.api.free.vo;

import lombok.Data;

/**
 * 자유게시판 목록 한 줄.
 *
 * 본문(content)은 미리보기용으로 앞 200자만 잘라서 내려준다. 글이 길면
 * 목록 응답이 통째로 무거워지는데, 화면은 두 줄만 보여주기 때문이다.
 *
 * hasFile 만 준다. 목록에서 파일 이름 · 크기까지 필요하지 않고, 화면은 이
 * 값으로 제목 옆에 클립 표시를 붙인다.
 *
 * 댓글 수는 컬럼으로 두지 않고 COUNT() 로 센다. 숫자를 따로 들고 있으면
 * 댓글을 지웠을 때 같이 줄이는 것을 잊어 조용히 어긋난다.
 */
@Data
public class FreePostListVO {

    private Long id;

    private String title;

    /** 앞 200자만 */
    private String content;

    private String nickname;

    private int viewCount;

    private int commentCount;

    private String createdAt;

    private boolean hasFile;
}
