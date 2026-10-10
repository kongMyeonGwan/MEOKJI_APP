package com.meokji.api.recommend.service;

import com.meokji.api.recommend.vo.WeatherVO;

/**
 * 날씨. 실제 코드는 WeatherServiceImpl 에 있다.
 *
 * <p>추천 기능 안에 있는 이유 — 날씨를 보여주려고 부르는 것이 아니라
 * "더운 날엔 냉면" 처럼 추천 점수에 쓰려고 부른다. 화면의 날씨 카드는
 * 그 값을 같이 보여주는 것이다.
 *
 * <p>날씨만 주는 주소를 따로 두지 않았다. 그러면 화면이 두 번 나뉘어 뜨고,
 * 서버가 점수에 쓴 날씨와 화면이 보여주는 날씨가 다를 수 있다.
 */
public interface WeatherService {

    /**
     * 좌표의 지금 날씨 + 안내 문구. 좌표가 null 이면 기본 위치(서울)
     *
     * 좌표가 없어도 에러를 내지 않는 이유 — 주소를 안 고른 사람도 있고,
     * 로그인 안 한 사람은 좌표를 알 길이 없다. 날씨 때문에 추천이
     * 통째로 막히면 안 된다.
     */
    WeatherVO current(Double lat, Double lon);
}
