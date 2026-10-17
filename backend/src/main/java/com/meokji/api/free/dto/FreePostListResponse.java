package com.meokji.api.free.dto;

import com.meokji.api.free.repository.FreePostListView;
import com.meokji.api.util.Times;

import lombok.Getter;

/**
 * 글 목록 한 줄 (서버 → 화면).
 *
 * { "id", "title", "content", "nickname", "view_count", "comment_count",
 *   "has_file", "created_at" }
 *
 * 상세에만 있는 칸(댓글 · 첨부 · is_mine)은 없다. 목록 화면이 쓰지 않는다 —
 * 목록에는 수정 · 삭제 버튼이 없어서 내 글인지 알 필요가 없다. 고치고 지우는
 * 것은 글을 열고 나서 한다. 한 페이지 스무 줄마다 로그인한 사람과 견주는
 * 일을 아끼는 셈이기도 하다.
 * content 는 본문 앞 200자다 (FreePostRepository.findPage 가 잘라서 가져온다).
 *
 * 칸 이름은 자바에서 카멜(viewCount)이고, 응답으로 나갈 때 application.yml 의
 * SNAKE_CASE 설정이 view_count 로 바꾼다.
 */
@Getter
public class FreePostListResponse {

    private final Long id;
    private final String title;
    private final String content;
    private final String nickname;        // 글쓴이 닉네임 (USERS 와 JOIN)
    private final Integer viewCount;
    private final Integer commentCount;   // 살아있는 댓글 수
    private final boolean hasFile;        // 제목 옆 클립 표시용
    private final String createdAt;       // "2026-10-04T12:30:00Z" (UTC)

    public FreePostListResponse(FreePostListView row) {
        this.id = row.getId();
        this.title = row.getTitle();
        this.content = row.getContent();
        this.nickname = row.getNickname();
        this.viewCount = row.getViewCount();
        this.commentCount = row.getCommentCount();
        this.hasFile = "Y".equals(row.getFileYn());
        this.createdAt = Times.toText(row.getCreatedAt());
    }
}
