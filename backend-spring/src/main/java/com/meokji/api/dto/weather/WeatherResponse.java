package com.meokji.api.dto.weather;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 지금 날씨. 대시보드 히어로 카드가 쓴다.
 *
 * condition 은 화면 배경을 고르는 코드(CLEAR · CLOUDS · RAIN · SNOW)이고
 * conditionText 는 사람이 읽는 한글("튼구름")이다. 둘을 같이 주는 이유는,
 * 화면이 문구로 배경을 고르려 하면 OpenWeather 의 한글 표현이 늘 때마다 깨진다.
 *
 * comment 와 pickLead 는 서버가 만든다. 화면마다 문구를 짜면
 * 같은 날씨에 다른 말이 나온다.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WeatherResponse {

    private String city;

    private int temp;

    private int feelsLike;

    private int humidity;

    private String condition;

    private String conditionText;

    private String comment;

    private String pickLead;
}
