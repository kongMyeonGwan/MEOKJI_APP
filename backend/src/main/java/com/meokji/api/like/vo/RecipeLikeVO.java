package com.meokji.api.like.vo;

import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.Data;

/**
 * RECIPE_LIKES 표 한 행. 찜한 레시피.
 *
 * 하트를 누를 때 화면이 보내는 { "food_name": "김치찌개" } 도, 누른 뒤
 * 돌려주는 { "food_name": "김치찌개", "liked": true, "count": 3 } 도
 * 이 클래스가 겸한다. 셋 다 같은 표의 같은 행 이야기다.
 *
 * 해제를 행 삭제로 처리하지 않는다. 지우고 다시 만들면 created_at 이
 * 매번 바뀌어 "언제 찜했는지" 가 사라지고, 목록 순서가 흔들린다.
 * use_yn 만 뒤집는다.
 *
 * 화면이 정해도 되는 칸은 foodName 하나뿐이다. 누가 찜했는지는 토큰에서
 * 나오고, 켜고 끄는 것은 서버가 지금 상태를 보고 뒤집는다.
 */
@Data
public class RecipeLikeVO {

    @JsonIgnore
    private Long id;

    /** 찜한 사람. 요청 본문이 아니라 토큰에서 꺼낸다 */
    @JsonIgnore
    private Long userId;

    /** 어떤 음식인지. 이 칸만 화면이 보낸다 */
    @NotBlank(message = "음식 이름을 입력해주세요")
    @Size(max = 150, message = "음식 이름이 너무 깁니다")
    private String foodName;

    /** 'Y' = 지금 찜돼 있다. 아래 liked 가 이 값이다 */
    @JsonIgnore
    private String useYn;

    @JsonIgnore
    private LocalDateTime createdAt;

    // ---------- 표에 없는 칸 ----------

    /**
     * 내 찜 전체 개수. 이 음식의 개수가 아니라 내가 찜해둔 음식 수다.
     *
     * 뒤집은 뒤 다시 센 값을 준다. 화면이 1 을 더하거나 빼서 스스로 맞추면
     * 다른 기기에서 누른 것과 어긋난다.
     */
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private int count;

    /**
     * 지금 찜돼 있는지. use_yn 에서 나온다.
     *
     * 칸을 따로 두지 않는 이유: 같은 것을 두 군데 두면 use_yn 만 바꾸고
     * liked 를 안 바꾸는 실수가 난다. 한 번도 안 누른 음식은 행이 없어
     * use_yn 이 null 인데 그때도 false 가 나온다.
     */
    @JsonProperty("liked")
    public boolean getLiked() {
        return "Y".equals(useYn);
    }
}
