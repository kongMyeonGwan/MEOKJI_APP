package com.meokji.api.service;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.meokji.api.common.ApiException;
import com.meokji.api.config.AppProperties;
import com.meokji.api.dto.weather.WeatherResponse;
import com.meokji.api.util.WeatherClient;

/**
 * 날씨에 안내 문구를 붙여 화면용으로 만든다.
 *
 * <p>파이썬 쪽 {@code app/services/weather_service.py} 자리다. 문구를 서버가 만드는
 * 이유는, 화면마다 짜면 같은 날씨에 다른 말이 나오기 때문이다.
 */
@Service
public class WeatherService {

    // 온도 구간을 나누는 경계값(섭씨)
    private static final int COLD_TEMP = 5;    // 이 아래면 "추운"
    private static final int COOL_TEMP = 15;   // 이 아래면 "쌀쌀한"
    private static final int WARM_TEMP = 25;   // 이 아래면 "선선한", 이상이면 "더운"

    private static final Map<String, String> SKY_TEXTS = Map.of(
            "CLEAR", "맑고",
            "CLOUDS", "흐리고",
            "RAIN", "비 오고",
            "SNOW", "눈 오고");

    /** 온도 느낌별로 어울리는 음식 한 줄. */
    private static final Map<String, String> FOOD_HINTS = Map.of(
            "추운", "따뜻하고 얼큰한 메뉴가 잘 어울려요",
            "쌀쌀한", "따끈한 국물 요리가 잘 어울려요",
            "선선한", "가볍고 든든한 메뉴가 잘 어울려요",
            "더운", "시원하고 담백한 메뉴가 잘 어울려요");

    /** 왼쪽이 "오늘은 이런 날" 이면 이쪽은 "그래서 이걸 드세요" 다. */
    private static final Map<String, String> PICK_LEADS = Map.of(
            "추운", "추운 날엔 뜨끈한 한 그릇 어떠세요?",
            "쌀쌀한", "쌀쌀한 날엔 국물 있는 메뉴 어떠세요?",
            "선선한", "선선한 날엔 든든한 한 끼 어떠세요?",
            "더운", "더운 날씨엔 시원한 한 그릇 어떠세요?");

    /** 비 · 눈이 오면 기온 문구 대신 이쪽을 쓴다. */
    private static final Map<String, String> SKY_FOOD_HINTS = Map.of(
            "RAIN", "따끈한 국물이나 바삭한 튀김이 당기는 날이에요",
            "SNOW", "뜨끈한 국물 요리로 몸을 녹여보세요");

    private static final Map<String, String> SKY_PICK_LEADS = Map.of(
            "RAIN", "비 오는 날엔 뜨끈한 국물 어떠세요?",
            "SNOW", "눈 오는 날엔 뜨끈한 한 그릇 어떠세요?");

    @Autowired
    private WeatherClient client;
    @Autowired
    private AppProperties app;


    /**
     * 좌표로 현재 날씨를 조회한다. 좌표가 없으면 기본 자리(서울시청 근처)를 쓴다.
     *
     * <p>바깥 API 가 죽었거나 키가 잘못된 경우를 503 으로 바꿔 내보낸다. 500 으로
     * 새어나가면 화면에 "요청을 처리하지 못했습니다" 만 뜬다.
     */
    public WeatherResponse current(Double lat, Double lon) {
        double useLat = lat != null ? lat : app.getWeather().getDefaultLat();
        double useLon = lon != null ? lon : app.getWeather().getDefaultLon();

        WeatherClient.Weather weather;
        try {
            weather = client.get(useLat, useLon);
        } catch (IllegalStateException exc) {
            throw ApiException.serviceUnavailable(exc.getMessage());
        }

        return new WeatherResponse(
                weather.city(),
                weather.temp(),
                weather.feelsLike(),
                weather.humidity(),
                weather.condition(),
                weather.conditionText(),
                makeComment(weather.condition(), weather.temp()),
                makePickLead(weather.condition(), weather.temp()));
    }

    /** 기온을 "추운 / 쌀쌀한 / 선선한 / 더운" 넷 중 하나로. */
    static String tempFeel(int temp) {
        if (temp < COLD_TEMP) {
            return "추운";
        }
        if (temp < COOL_TEMP) {
            return "쌀쌀한";
        }
        if (temp < WARM_TEMP) {
            return "선선한";
        }

        return "더운";
    }

    /** 화면 왼쪽에 넣을 두 줄짜리 문구. */
    static String makeComment(String condition, int temp) {
        String feel = tempFeel(temp);
        String sky = SKY_TEXTS.get(condition);

        // 표에 없는 날씨 코드면 하늘 상태는 빼고 온도만 말한다.
        String firstLine = sky != null
                ? "오늘은 " + sky + " " + feel + " 날씨에요!"
                : "오늘은 " + feel + " 날씨에요!";

        String secondLine = SKY_FOOD_HINTS.getOrDefault(condition, FOOD_HINTS.get(feel));

        return firstLine + "\n" + secondLine;
    }

    /** 오른쪽 추천 칸의 머리말. */
    static String makePickLead(String condition, int temp) {
        String bySky = SKY_PICK_LEADS.get(condition);

        return bySky != null ? bySky : PICK_LEADS.get(tempFeel(temp));
    }
}
