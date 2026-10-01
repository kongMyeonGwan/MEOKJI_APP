package com.meokji.api.ban.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.meokji.api.util.Times;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Data;

/**
 * USER_BANS 표 한 행. 못 먹거나 싫은 재료.
 *
 * 재료 마스터 표를 두지 않고 문자열로 저장한다. 사용자가 "고수" 라고 적으면
 * 그대로 남기고, 추천과 레시피에서 이름으로 걸러낸다.
 *
 * 이 클래스 하나가 세 가지 일을 한다.
 *   1. DB 한 행을 담는다           (MyBatis 가 채운다)
 *   2. 화면이 보낸 값을 받는다      (@NotBlank 같은 검증이 그래서 붙어 있다)
 *   3. 화면으로 나갈 모양이 된다    (@JsonIgnore 가 붙은 칸은 안 나간다)
 *
 * 나가는 모양: { "id": 3, "ingredient_name": "오이", "ban_type": "ALLERGY",
 *               "created_at": "2026-09-11T21:29:42Z" }
 */
@Data
public class UserBanVO {

    private Long id;

    /** 누구 것인지. 토큰에서 정하므로 화면에 보내지도, 받지도 않는다. */
    @JsonIgnore
    private Long userId;

    @NotBlank(message = "재료 이름을 입력해주세요")
    @Size(max = 100, message = "재료 이름이 너무 깁니다")
    private String ingredientName;

    /** DISLIKE(싫어함) / ALLERGY(알레르기). 알레르기를 더 강하게 취급한다. */
    @Pattern(regexp = "DISLIKE|ALLERGY", message = "Ban 종류가 올바르지 않습니다")
    private String banType;

    /** 'N' = 사용자가 뺀 재료. 화면에는 알릴 필요가 없다. */
    @JsonIgnore
    private String useYn;

    /** DB 에는 시각으로, 화면에는 글자로 나간다. 그래서 이 칸 자체는 감춘다. */
    @JsonIgnore
    private LocalDateTime createdAt;

    /** 화면이 읽는 created_at. 예) "2026-09-11T21:29:42Z" */
    @JsonProperty("created_at")
    public String getCreatedAtText() {
        return Times.toText(createdAt);
    }
}
