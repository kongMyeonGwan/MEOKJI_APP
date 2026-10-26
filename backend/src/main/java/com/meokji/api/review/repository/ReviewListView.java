package com.meokji.api.review.repository;

import java.time.LocalDateTime;

public interface ReviewListView {

    Long getId();

    String getTitle();

    String getContent();

    String getNickname();

    /** 무슨 음식에 대한 후기인가. MEAL_HISTORIES 와 JOIN 해서 읽는다 */
    String getFoodName();

    Integer getViewCount();

    LocalDateTime getCreatedAt();

    String getFileYn();

    Integer getCommentCount();
}
