package com.meokji.api.free.vo;

import java.util.List;

import lombok.Data;

/**
 * 자유게시판 글 상세.
 *
 * isMine 은 지금 보는 사람 기준이다. 로그인하지 않았으면 false 이고,
 * 화면은 이 값으로 수정 · 삭제 버튼을 보일지 정한다. 다만 화면이 숨기는
 * 것은 눈에 안 보이게 하는 것일 뿐이라, 서버도 수정 · 삭제 요청마다
 * 글쓴이인지 다시 확인한다.
 *
 * 댓글을 같이 담아 보낸다. 글을 열면 댓글도 늘 같이 보이기 때문에
 * 따로 받으면 요청만 두 번이 된다.
 */
@Data
public class FreePostDetailVO {

    private Long id;

    private String title;

    private String content;

    private String nickname;

    private int viewCount;

    private String createdAt;

    private String updatedAt;

    /** 첨부 사진. 없으면 null */
    private FreeFileVO file;

    /**
     * 로그인한 사람이 쓴 글인지.
     *
     * boolean 이 아니라 Boolean 인 이유: boolean isMine 이면 롬복이 isMine() 을
     * 만들고, Jackson 이 그 is 를 접두어로 보고 떼어내 "mine" 으로 내보낸다.
     * 화면은 is_mine 을 읽으므로 값이 통째로 사라진다.
     */
    private Boolean isMine;

    private List<FreePostCommentVO> comments;
}
