package com.meokji.api.review.repository;

import java.time.LocalDateTime;

public interface ReviewCommentView {

    Long getId();

    Long getUserId();

    String getContent();

    String getNickname();

    LocalDateTime getCreatedAt();
}
