package com.meokji.api.dto.ban;

import com.meokji.api.util.Times;
import com.meokji.api.vo.UserBanVO;

import lombok.Data;

// 못 먹는 재료 한 건 응답
@Data
public class BanResponse {

    private Long id;
    private String ingredientName;
    private String banType;
    private String createdAt;

    // UserBanVO(DB 한 행) → 응답 모양으로 옮겨 담는다
    public static BanResponse from(UserBanVO ban) {
        BanResponse response = new BanResponse();
        response.setId(ban.getId());
        response.setIngredientName(ban.getIngredientName());
        response.setBanType(ban.getBanType());
        response.setCreatedAt(Times.toText(ban.getCreatedAt()));
        return response;
    }
}
