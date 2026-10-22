package com.meokji.api.favorite.dto;

import com.meokji.api.util.Times;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class FavoriteListResponse {

    private final Long id;

    private final String foodName;

    private final String createdAt;

    /** JPQL 의 SELECT new 로 만든다. 시각은 여기서 응답용 문자열로 바꾼다 */
    public FavoriteListResponse(Long id, String foodName, LocalDateTime createdAt) {
        this.id = id;
        this.foodName = foodName;
        this.createdAt = Times.toText(createdAt);
    }
}
