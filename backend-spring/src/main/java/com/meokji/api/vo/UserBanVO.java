package com.meokji.api.vo;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * USER_BANS 표 한 행. 못 먹거나 싫은 재료.
 *
 * 재료 마스터 표를 두지 않고 문자열로 저장한다. 사용자가 "고수" 라고 적으면
 * 그대로 남기고, 추천과 레시피에서 이름으로 걸러낸다.
 */
@Data
public class UserBanVO {

    private Long id;
    private Long userId;
    private String ingredientName;

    /** DISLIKE(싫어함) / ALLERGY(알레르기). 알레르기를 더 강하게 취급한다. */
    private String banType;

    private String useYn;
    private LocalDateTime createdAt;
}
