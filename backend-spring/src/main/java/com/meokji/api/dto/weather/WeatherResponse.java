package com.meokji.api.dto.weather;

/**
 * 지금 날씨. 대시보드 히어로 카드가 쓴다.
 *
 * <p>{@code condition} 은 화면 배경을 고르는 코드(CLEAR · CLOUDS · RAIN · SNOW)이고
 * {@code conditionText} 는 사람이 읽는 한글("튼구름")이다. 둘을 같이 주는 이유는,
 * 화면이 문구로 배경을 고르려 하면 OpenWeather 의 한글 표현이 늘 때마다 깨진다.
 *
 * <p>{@code comment} 와 {@code pickLead} 는 서버가 만든다. 화면마다 문구를 짜면
 * 같은 날씨에 다른 말이 나온다.
 */
public record WeatherResponse(
        String city,
        int temp,
        int feelsLike,
        int humidity,
        String condition,
        String conditionText,
        String comment,
        String pickLead) {
}
