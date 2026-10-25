package com.meokji.api.review.repository;

import org.springframework.data.jpa.repository.Query;

public interface ReviewPopularView {
    Long getId();
    String getTitle();
    Long getLikeCount();
    String getFoodName();
    String getRecipeName();
    String getNickname();
    Integer getCookTimeMinutes();
    Integer getCommentCount();
    String getSummary();
    Integer getEstimatedCost();
}
