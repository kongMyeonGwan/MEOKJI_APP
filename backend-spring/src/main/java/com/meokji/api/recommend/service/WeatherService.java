package com.meokji.api.recommend.service;

import com.meokji.api.recommend.dto.WeatherResponse;

/** 날씨. 실제 코드는 WeatherServiceImpl 에 있다. */
public interface WeatherService {

    // 좌표의 지금 날씨 + 안내 문구. 좌표가 null 이면 기본 위치(서울)
    WeatherResponse current(Double lat, Double lon);
}
