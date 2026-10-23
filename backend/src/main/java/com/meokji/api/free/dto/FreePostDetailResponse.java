package com.meokji.api.free.dto;

import java.util.List;

import com.meokji.api.free.entity.FreePostEntity;
import com.meokji.api.util.Times;

import lombok.Getter;
import lombok.ToString;

/**
 * 글 상세 (서버 → 화면). 글쓰기 · 글 수정의 응답도 이 모양이다.
 *
 * { "id", "title", "content", "nickname", "view_count", "comment_count",
 *   "has_file", "file", "is_mine", "comments", "created_at", "updated_at" }
 *
 * 표에 없는 칸(닉네임 · 댓글 · 첨부 · 내 글 여부)은 서비스가 구해서 넘긴다.
 * 글쓴이 회원 번호 · use_yn 은 내보내지 않는다.
 */
@Getter
@ToString
public class FreePostDetailResponse {

    private final Long id;
    private final String title;
    private final String content;               // 본문 전체
    private final String nickname;              // 글쓴이 닉네임
    private final Integer viewCount;
    private final Integer commentCount;         // 아래 comments 의 개수
    private final boolean hasFile;
    private final FreeFileResponse file;        // 첨부가 없으면 null — 화면이 사진 자리를 안 그린다
    /**
     * 이 글을 로그인한 사람이 썼는지. 화면이 글 수정 · 삭제 · 사진 버튼을
     * 보일지 정하는 데 쓴다.
     *
     * 아래 comments 안에도 is_mine 이 하나씩 따로 있다. 이쪽은 글쓴이 기준,
     * 그쪽은 그 댓글을 쓴 사람 기준이라 값이 다를 수 있다 — 남의 글에 내가
     * 댓글을 달면 이 칸은 false 인데 그 댓글의 칸은 true 다. 그래서 한 칸으로
     * 합치지 못한다.
     *
     * boolean 이 아니라 Boolean 인 이유는 FreeCommentResponse 참고.
     */
    private final Boolean isMine;

    private final List<FreeCommentResponse> comments; // 오래된 순
    private final String createdAt;
    private final String updatedAt;

    public FreePostDetailResponse(FreePostEntity post, String nickname, FreeFileResponse file,
                                  List<FreeCommentResponse> comments, boolean isMine) {
        this.id = post.getId();
        this.title = post.getTitle();
        this.content = post.getContent();
        this.nickname = nickname;
        this.viewCount = post.getViewCount() == null ? 0 : post.getViewCount();
        this.commentCount = comments.size();
        this.hasFile = post.hasFile();
        this.file = file;
        this.isMine = isMine;
        this.comments = comments;
        this.createdAt = Times.toText(post.getCreatedAt());
        this.updatedAt = Times.toText(post.getUpdatedAt());
    }
}
