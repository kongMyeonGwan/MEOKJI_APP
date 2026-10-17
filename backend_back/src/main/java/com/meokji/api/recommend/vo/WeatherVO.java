package com.meokji.api.recommend.vo;

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
public class WeatherVO {

    /** 지역 이름. 좌표로 찾은 값이라 "중구" 처럼 세부 지역이 올 수 있다 */
    private String city;

    /**
     * 기온, 체감온도 (섭씨).
     *
     * int 로 반올림해 보낸다. 소수점까지(19.3도) 보여줄 이유가 없고,
     * 화면마다 반올림하면 한 화면에 19도와 19.3도가 같이 나올 수 있다
     */
    private int temp;

    private int feelsLike;

    /** 습도(%) */
    private int humidity;

    /** 화면 배경을 고르는 코드. CLEAR · CLOUDS · RAIN · SNOW */
    private String condition;

    /** 사람이 읽는 날씨. "튼구름" · "맑음" */
    private String conditionText;

    /**
     * 날씨에 맞춘 한마디. 예) "더운 날엔 시원한 게 좋죠"
     *
     * 아래 pickLead 와 다른 점 — comment 는 날씨 카드에 띄우는 말이고,
     * pickLead 는 추천 목록 위에 붙이는 말이다
     */
    private String comment;

    /** 추천 목록 앞에 붙일 말. 예) "이런 날엔" */
    private String pickLead;
}
