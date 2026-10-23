package com.meokji.api.notice.dto;

import com.meokji.api.notice.entity.Notice;
import com.meokji.api.util.Times;

import lombok.Getter;


@Getter
public class NoticeListResponse {

    private final Long id;
    private final String title;
    private final String content;      // 목록에서 내용을 안 보여주면 이 칸을 뺀다
    private final Integer viewCount;
    private final boolean pinned;      // 상단 고정 여부 (DB 는 'Y'/'N')
    private final String createdAt;    // "2026-10-04T12:30:00Z" (UTC)

    public NoticeListResponse(Notice notice) {
        this.id = notice.getId();
        this.title = notice.getTitle();
        this.content = notice.getContent();
        this.viewCount = notice.getViewCount();
        this.pinned = notice.isPinned();
        this.createdAt = Times.toText(notice.getCreatedAt());
    }
}