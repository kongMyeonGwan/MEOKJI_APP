package com.meokji.api.review.repository;

import java.time.LocalDateTime;

public interface ReviewListView {

    Long getId();

    String getTitle();

    String getContent();

    String getNickname();

    Integer getViewCount();

    LocalDateTime getCreatedAt();

    String getFileYn();

    Integer getCommentCount();
}
