package com.meokji.api.recommend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.meokji.api.common.ApiException;
import com.meokji.api.recommend.vo.WeatherVO;
import com.meokji.api.util.WeatherClient;

/**
 * 날씨.
 * 날씨 API 는 WeatherClient 가 부르고, 여기서는 "오늘은 맑고 더운 날씨에요!" 같은 문구를 붙인다.
 *
 * <p>왜 둘로 나눴나 — WeatherClient 는 "바깥 API 를 부르고 응답을 읽는" 일만
 * 한다. 문구를 거기서 만들면, API 를 다른 곳으로 바꿀 때 우리 문구까지
 * 같이 옮겨야 한다.
 *
 * <p>문구를 서버가 만드는 이유 — 화면마다 짜면 같은 날씨에 다른 말이 나온다.
 * 날씨 카드와 추천 목록 위의 말이 서로 어긋나면 한 화면 안에서 티가 난다.
 *
 * <p>문구를 if 로 늘어놓은 모양인 이유 — 경우가 날씨(4) x 기온대(4) 라
 * 표로 만들어도 열여섯 줄이다. 그 중 실제로 다르게 말하는 것은 여섯
 * 가지뿐이라(비 · 눈 · 기온 네 단계) if 로 그만큼만 적었다.
 *
 * <p>&#64;Transactional 이 없다. DB 를 쓰지 않는다.
 */
@Service
public class WeatherServiceImpl implements WeatherService {

    @Autowired
    private WeatherClient weatherClient;

    // 좌표가 없을 때 쓸 기본 위치 (서울시청 근처)
    //
    // 코드에 박지 않은 이유 — 서비스를 다른 지역 중심으로 쓸 수도 있고,
    // 개발 중에 다른 곳 날씨로 바꿔보기도 한다
    @Value("${app.weather.default-lat}")
    private double defaultLat;

    @Value("${app.weather.default-lon}")
    private double defaultLon;

    /**
     * 좌표의 지금 날씨 + 문구.
     *
     * 좌표가 null 이면 기본 위치로 떨어진다. 에러를 내지 않는 이유 —
     * 주소를 안 고른 사람도 있고, 로그인 안 한 사람은 좌표를 알 길이 없다.
     */
    @Override
    public WeatherVO current(Double lat, Double lon) {
        // 둘을 따로 본다. 하나만 온 경우는 없어야 하지만, 하나라도
        // null 이면 API 가 에러를 내므로 각각 채운다
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
            // WeatherClient 는 접속 · 키 문제를 IllegalStateException 으로
            // 알린다. 그대로 올려보내면 500 이 되는데, 우리 잘못이 아니라
            // "잠시 뒤에 다시 하면 될 수도 있다" 는 뜻이라 503 으로 바꾼다
            throw ApiException.serviceUnavailable(e.getMessage());
        }

        // Weather(API 응답을 읽은 모양) -> WeatherVO(화면에 나갈 모양).
        // 앞의 여섯 칸은 그대로 옮기고, 뒤의 두 칸은 여기서 만든다
        WeatherVO response = new WeatherVO();
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
    //
    // 네 단계로 나눈 기준은 5 · 15 · 25도다. 사람이 느끼는 선을 어림잡은 값이고,
    // 아래 두 메서드가 같이 쓴다 - 각자 나누면 한쪽만 고치는 일이 생긴다.
    //
    // 체감온도(feelsLike)가 아니라 기온을 쓴다. 체감온도는 바람 · 습도까지
    // 섞은 값이라 맑은 날에도 "추운" 으로 떨어질 수 있는데, 화면에는
    // 기온이 함께 보이므로 말과 숫자가 어긋나 보인다
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
    //
    // 줄바꿈(\n)을 글자 안에 넣어 보낸다. 칸을 둘로 나누지 않은 이유는
    // 화면이 그냥 한 덩어리로 받아 white-space 로 줄을 살리기 때문이다
    private String makeComment(String condition, int temp) {
        String feel = tempFeel(temp);

        // 첫째 줄: 하늘 + 기온
        //
        // sky 가 null 로 남을 수 있다. OpenWeather 는 위 넷 말고도
        // 안개 · 황사 같은 상태를 주는데, 거기까지 우리 말로 옮기지 않았다.
        // 그때는 기온만 말한다
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

        // 하늘을 모르면 기온만, 알면 둘 다 말한다
        String firstLine = "오늘은 " + feel + " 날씨에요!";
        if (sky != null) {
            firstLine = "오늘은 " + sky + " " + feel + " 날씨에요!";
        }

        // 둘째 줄: 어울리는 음식. 비 · 눈이 오면 기온보다 그쪽을 먼저 말한다
        //
        // 비 오는 날은 기온이 20도여도 "가볍고 든든한" 보다 "따끈한 국물" 이
        // 맞는다. 그래서 if 순서가 비 · 눈 먼저다
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
            // "더운" 이거나, tempFeel 이 넷 중 무엇을 줘도 여기로 떨어진다.
            // else 로 두면 단계를 늘릴 때 문구가 빠지는 일이 없다
            secondLine = "시원하고 담백한 메뉴가 잘 어울려요";
        }

        return firstLine + "\n" + secondLine;
    }

    // 날씨 카드 오른쪽(추천 메뉴) 위의 한 줄
    //
    // 위 makeComment 와 가리는 순서가 같다(비 · 눈 먼저, 그 다음 기온).
    // 두 문구가 서로 다른 날씨를 말하면 한 화면에서 티가 난다.
    //
    // 문장 모양을 물음표로 맞춘 이유 — 추천 목록 바로 위에 붙는 말이라,
    // "어떠세요?" 로 끝나면 아래 카드로 눈이 이어진다
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
        // "더운" 이거나 그 밖의 경우
        return "더운 날씨엔 시원한 한 그릇 어떠세요?";
    }
}
