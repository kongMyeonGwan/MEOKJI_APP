package com.meokji.api.recommend.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


/**
 * 추천 결과.
 *
 * condition 은 무슨 조건으로 골랐는지 한 줄로 적은 것이다
 * ("서울 중구 명동 · 튼구름 19°"). 화면이 목록 위에 그대로 띄운다 - 왜 이 메뉴가
 * 나왔는지 보이지 않으면 추천이 아무렇게나 고른 것처럼 읽힌다.
 *
 * weather 는 use_weather 를 켰을 때만 채워진다. 날씨만 따로
 * 부르는 엔드포인트를 두지 않은 이유는, 그러면 화면이 두 번 나뉘어 뜨고 서버와
 * 화면이 서로 다른 날씨를 볼 수 있어서다.
 *
 * <p>응답 전용이다. 요청은 RecommendRequestVO 가 맡는다. 표와도 무관하다 -
 * 추천 결과는 저장하지 않는다. 사용자가 "이걸로 먹을래요" 를 눌렀을 때만
 * history 기능이 그 한 건을 저장한다.
 *
 * <p>칸 넷 중 둘(weather · bargains)은 요청에 따라 null 이다. 요청에서
 * 켜지 않으면 부르지도 않는다 - 둘 다 바깥 API 를 거쳐 느리다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendVO {

    /** 무슨 조건으로 골랐는지 한 줄. 예) "서울 중구 명동 · 튼구름 19°" */
    private String condition;

    /**
     * 추천한 음식들. 점수가 높은 순이다.
     *
     * 조건이 까다로우면 요청한 개수보다 적게 올 수 있다. 빈 목록일 수도
     * 있는데, 그때 화면은 "조건을 넓혀보세요" 를 띄운다
     */
    private List<FoodVO> items;

    /** 지금 날씨. use_weather 를 켰을 때만 채워진다 (아니면 null) */
    private WeatherVO weather;

    /** 요즘 값이 내린 재료. use_bargains 를 켰을 때만 채워진다 (아니면 null) */
    private List<BargainVO> bargains;
}
