package com.meokji.api.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.meokji.api.common.ApiException;
import com.meokji.api.dto.weather.WeatherResponse;
import com.meokji.api.util.WeatherClient;

/**
 * 날씨.
 * 날씨 API 는 WeatherClient 가 부르고, 여기서는 "오늘은 맑고 더운 날씨에요!" 같은 문구를 붙인다.
 */
@Service
public class WeatherServiceImpl implements WeatherService {

    @Autowired
    private WeatherClient weatherClient;

    // 좌표가 없을 때 쓸 기본 위치 (서울시청 근처)
    @Value("${app.weather.default-lat}")
    private double defaultLat;

    @Value("${app.weather.default-lon}")
    private double defaultLon;

    @Override
    public WeatherResponse current(Double lat, Double lon) {
        if (lat == null) {
            lat = defaultLat;
        }
        if (lon == null) {
            lon = defaultLon;
        }

        // 날씨 API 부르기. 실패하면 503 에러
        WeatherClient.Weather weather;
        try {
            weather = weatherClient.get(lat, lon);
        } catch (IllegalStateException e) {
            throw ApiException.serviceUnavailable(e.getMessage());
        }

        WeatherResponse response = new WeatherResponse();
        response.setCity(weather.getCity());
        response.setTemp(weather.getTemp());
        response.setFeelsLike(weather.getFeelsLike());
        response.setHumidity(weather.getHumidity());
        response.setCondition(weather.getCondition());
        response.setConditionText(weather.getConditionText());
        response.setComment(makeComment(weather.getCondition(), weather.getTemp()));
        response.setPickLead(makePickLead(weather.getCondition(), weather.getTemp()));
        return response;
    }

    // 기온 → "추운" / "쌀쌀한" / "선선한" / "더운"
    private String tempFeel(int temp) {
        if (temp < 5) {
            return "추운";
        }
        if (temp < 15) {
            return "쌀쌀한";
        }
        if (temp < 25) {
            return "선선한";
        }
        return "더운";
    }

    // 날씨 카드 왼쪽의 두 줄 문구
    //   예) "오늘은 맑고 더운 날씨에요!\n시원하고 담백한 메뉴가 잘 어울려요"
    private String makeComment(String condition, int temp) {
        String feel = tempFeel(temp);

        // 첫째 줄: 하늘 + 기온
        String sky = null;
        if ("CLEAR".equals(condition)) {
            sky = "맑고";
        } else if ("CLOUDS".equals(condition)) {
            sky = "흐리고";
        } else if ("RAIN".equals(condition)) {
            sky = "비 오고";
        } else if ("SNOW".equals(condition)) {
            sky = "눈 오고";
        }

        String firstLine = "오늘은 " + feel + " 날씨에요!";
        if (sky != null) {
            firstLine = "오늘은 " + sky + " " + feel + " 날씨에요!";
        }

        // 둘째 줄: 어울리는 음식. 비 · 눈이 오면 기온보다 그쪽을 먼저 말한다
        String secondLine;
        if ("RAIN".equals(condition)) {
            secondLine = "따끈한 국물이나 바삭한 튀김이 당기는 날이에요";
        } else if ("SNOW".equals(condition)) {
            secondLine = "뜨끈한 국물 요리로 몸을 녹여보세요";
        } else if (feel.equals("추운")) {
            secondLine = "따뜻하고 얼큰한 메뉴가 잘 어울려요";
        } else if (feel.equals("쌀쌀한")) {
            secondLine = "따끈한 국물 요리가 잘 어울려요";
        } else if (feel.equals("선선한")) {
            secondLine = "가볍고 든든한 메뉴가 잘 어울려요";
        } else {
            secondLine = "시원하고 담백한 메뉴가 잘 어울려요";
        }

        return firstLine + "\n" + secondLine;
    }

    // 날씨 카드 오른쪽(추천 메뉴) 위의 한 줄
    private String makePickLead(String condition, int temp) {
        if ("RAIN".equals(condition)) {
            return "비 오는 날엔 뜨끈한 국물 어떠세요?";
        }
        if ("SNOW".equals(condition)) {
            return "눈 오는 날엔 뜨끈한 한 그릇 어떠세요?";
        }

        String feel = tempFeel(temp);
        if (feel.equals("추운")) {
            return "추운 날엔 뜨끈한 한 그릇 어떠세요?";
        }
        if (feel.equals("쌀쌀한")) {
            return "쌀쌀한 날엔 국물 있는 메뉴 어떠세요?";
        }
        if (feel.equals("선선한")) {
            return "선선한 날엔 든든한 한 끼 어떠세요?";
        }
        return "더운 날씨엔 시원한 한 그릇 어떠세요?";
    }
}
