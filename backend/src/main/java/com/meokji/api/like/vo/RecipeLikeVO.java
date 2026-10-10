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
 *
 * <p>칸 대부분이 @JsonIgnore 인 점에 주의 — 이 VO 의 응답 모양은 사실
 * 세 칸뿐이다(food_name · liked · count). 나머지는 DB 를 다루는 데만 쓰는
 * 칸이라 바깥으로 내보낼 이유가 없다.
 */
@Data
public class RecipeLikeVO {

    // id 도 감춘다. 다른 기능과 달리 화면이 찜을 번호로 가리키는 일이 없다 -
    // /api/likes/{id} 같은 주소가 아예 없고, 음식 이름으로만 다룬다
    @JsonIgnore
    private Long id;

    /** 찜한 사람. 요청 본문이 아니라 토큰에서 꺼낸다 */
    // 받지도 않는다. @JsonIgnore 는 두 방향을 다 막는다 -
    // 화면이 보낸 값을 받으면 번호만 바꿔 남의 찜을 만들 수 있다
    @JsonIgnore
    private Long userId;

    /** 어떤 음식인지. 이 칸만 화면이 보낸다 */
    // max = 150 은 RECIPE_LIKES.FOOD_NAME 컬럼 길이와 맞춘 값이다
    @NotBlank(message = "음식 이름을 입력해주세요")
    @Size(max = 150, message = "음식 이름이 너무 깁니다")
    private String foodName;

    /** 'Y' = 지금 찜돼 있다. 아래 liked 가 이 값이다 */
    // 'Y'/'N' 글자를 그대로 내보내지 않는 이유 — 화면은 참/거짓으로 쓰는
    // 값이다. 글자를 보내면 화면마다 "Y" 와 비교하는 코드를 적게 된다
    @JsonIgnore
    private String useYn;

    // 찜한 시각. 목록 정렬(최근 순)의 기준이다.
    //
    // 화면에 보여주지 않으므로 다른 VO 처럼 글자로 바꿔 내보내는
    // 메서드(getCreatedAtText)도 두지 않았다
    @JsonIgnore
    private LocalDateTime createdAt;

    // ---------- 표에 없는 칸 ----------

    /**
     * 내 찜 전체 개수. 이 음식의 개수가 아니라 내가 찜해둔 음식 수다.
     *
     * 뒤집은 뒤 다시 센 값을 준다. 화면이 1 을 더하거나 빼서 스스로 맞추면
     * 다른 기기에서 누른 것과 어긋난다.
     */
    // READ_ONLY 는 "내보내기만 하고 받지는 않는다" 는 뜻이다.
    // 화면이 count 를 보내도 무시된다 - 서버가 세는 값이다
    @JsonProperty(access = JsonProperty.Access.READ_ONLY)
    private int count;

    /**
     * 지금 찜돼 있는지. use_yn 에서 나온다.
     *
     * 칸을 따로 두지 않는 이유: 같은 것을 두 군데 두면 use_yn 만 바꾸고
     * liked 를 안 바꾸는 실수가 난다. 한 번도 안 누른 음식은 행이 없어
     * use_yn 이 null 인데 그때도 false 가 나온다.
     */
    // 칸이 아니라 메서드다. 그래서 늘 use_yn 에서 계산되고,
    // 둘이 어긋날 자리가 없다.
    //
    // "Y".equals(useYn) 순서로 쓰는 이유 — useYn.equals("Y") 는
    // useYn 이 null 일 때 NullPointerException 이 난다
    @JsonProperty("liked")
    public boolean getLiked() {
        return "Y".equals(useYn);
    }
}
