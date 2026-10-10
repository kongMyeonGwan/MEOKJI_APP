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
 *
 * <p>하나로 겸하는 대신 칸마다 "어느 방향으로 쓰이는지" 를 표시해 둬야 한다.
 * 요청 전용 VO · 응답 전용 VO · DB 전용 VO 를 따로 만들면 칸을 하나 늘릴 때
 * 세 군데를 같이 고쳐야 하고, 한 군데를 빠뜨려도 컴파일은 된다.
 *
 * <p>재료 마스터 표를 안 둔 이유를 더 적으면 — 마스터를 두면 "오이" 가
 * 목록에 없을 때 사용자가 등록할 방법이 없다. 세상의 모든 재료를 미리
 * 넣어둘 수는 없고, 빠진 재료가 하필 그 사람의 알레르기일 수 있다.
 */
@Data
public class UserBanVO {

    /** 시퀀스로 받는 번호. 삭제할 때 /api/bans/{이 값} 으로 가리킨다 */
    private Long id;

    /** 누구 것인지. 토큰에서 정하므로 화면에 보내지도, 받지도 않는다. */
    // @JsonIgnore 가 두 방향을 다 막는다 - 응답에 안 나가고, 화면이 보내도
    // 무시된다. 화면이 보낸 값을 받으면 번호만 바꿔 남의 것으로 저장할 수 있다
    @JsonIgnore
    private Long userId;

    // max = 100 은 USER_BANS.INGREDIENT_NAME 컬럼 길이와 맞춘 값이다.
    // 안 막으면 DB 에서 걸려 500 이 나가고, 사용자는 뭘 고칠지 알 수 없다
    @NotBlank(message = "재료 이름을 입력해주세요")
    @Size(max = 100, message = "재료 이름이 너무 깁니다")
    private String ingredientName;

    /** DISLIKE(싫어함) / ALLERGY(알레르기). 알레르기를 더 강하게 취급한다. */
    // @Pattern 은 값이 null 이면 그냥 통과시킨다. 그래서 종류를 안 보내도
    // 422 가 아니라, 서비스가 DISLIKE 를 기본값으로 넣는다
    //
    // 두 종류를 나눈 이유 — 알레르기는 대체재료로도 넣으면 안 되고,
    // 싫어함은 다른 것으로 바꿔주면 된다. 추천 쪽에서 취급이 다르다
    @Pattern(regexp = "DISLIKE|ALLERGY", message = "Ban 종류가 올바르지 않습니다")
    private String banType;

    /** 'N' = 사용자가 뺀 재료. 화면에는 알릴 필요가 없다. */
    // 목록 SQL 이 'Y' 만 가려 오므로, 화면이 받는 목록에는 늘 'Y' 뿐이다.
    // 보내도 쓸 데가 없는 값이라 감춘다
    @JsonIgnore
    private String useYn;

    /** DB 에는 시각으로, 화면에는 글자로 나간다. 그래서 이 칸 자체는 감춘다. */
    // LocalDateTime 을 Jackson 에 그냥 넘기면 [2026,9,11,21,29] 같은 배열이나
    // 시간대 없는 글자로 나간다. 모양을 고정하려고 아래 메서드를 거친다
    @JsonIgnore
    private LocalDateTime createdAt;

    /** 화면이 읽는 created_at. 예) "2026-09-11T21:29:42Z" */
    // 칸이 아니라 메서드인데 @JsonProperty 가 붙어 있으면, Jackson 이
    // 이 메서드를 불러 그 결과를 created_at 칸으로 내보낸다.
    //
    // 이름을 손으로 적은 이유 — snake_case 설정은 칸 이름을 바꿔주지만,
    // 메서드 이름(getCreatedAtText)에서는 created_at_text 가 나온다
    @JsonProperty("created_at")
    public String getCreatedAtText() {
        return Times.toText(createdAt);
    }
}
