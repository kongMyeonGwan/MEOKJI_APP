package com.meokji.api.dto.ban;

import com.meokji.api.util.Times;
import com.meokji.api.vo.UserBan;

/**
 * Ban 한 건.
 *
 * <p>{@code userId} 는 내려주지 않는다. 어차피 "내 Ban" 만 조회되므로 필요 없고,
 * 내보내지 않는 값은 실수로 새어나갈 일도 없다.
 */
public record BanResponse(
        Long id,
        String ingredientName,
        String banType,
        String createdAt) {

    public static BanResponse from(UserBan ban) {
        return new BanResponse(
                ban.getId(),
                ban.getIngredientName(),
                ban.getBanType(),
                Times.toText(ban.getCreatedAt()));
    }
}
