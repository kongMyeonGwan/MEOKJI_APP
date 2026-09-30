package com.meokji.api.recommend.dto;

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
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecommendResponse {

    private String condition;

    private List<FoodItem> items;

    private WeatherResponse weather;

    private List<BargainItem> bargains;
}
