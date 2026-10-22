package com.meokji.api.notice.repository;

import java.time.LocalDateTime;

public interface NoticeListView {
    Long getId();

    String getTitle();

    String getContent();

    Integer getViewCount();

    String getPinnedYn();

    LocalDateTime getCreatedAt();

}
