package com.meokji.api.dto.review;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 인기 레시피 한 줄. 기간 안에 좋아요를 많이 받은 순.
 *
 * 기간은 한국 날짜로 받아 UTC 로 바꿔 조회한다. 이 9시간을 안 맞추면 경계에서
 * 결과가 어긋난다 - 새벽에 눌린 좋아요가 전날 것으로 세어진다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PopularItem {

    private Long id;

    private String foodName;

    private String recipeName;

    private String nickname;

    private int likeCount;

    private Integer estimatedCost;
}
