package com.meokji.api.notice.dto;

import com.meokji.api.notice.entity.Notice;
import com.meokji.api.util.Times;

import lombok.Getter;

@Getter
public class NoticeDetailResponse {

    private final Long id;
    private final String title;
    private final String content;
    private final Integer viewCount;
    private final boolean pinned;
    private final String createdAt;    // "2026-10-04T12:30:00Z" (UTC)

    public NoticeDetailResponse(Notice notice) {
        this.id = notice.getId();
        this.title = notice.getTitle();
        this.content = notice.getContent();
        this.viewCount = notice.getViewCount();
        this.pinned = "Y".equals(notice.getPinnedYn());
        this.createdAt = Times.toText(notice.getCreatedAt());
    }
}