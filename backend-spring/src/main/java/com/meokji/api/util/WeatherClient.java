package com.meokji.api.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.meokji.api.recommend.service.WeatherService;
import com.meokji.api.user.vo.UserVO;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 바깥 날씨 API 를 부른다.
 *
 * WebClient 대신 JDK 의 HttpClient 를 쓴다. 부르는 곳이 한 군데고
 * 동기 호출이라, 의존성을 늘려 얻을 것이 없다.
 */
@Component
public class WeatherClient {

    private static final String OPENWEATHER_URL =
            "https://api.openweathermap.org/data/2.5/weather";

    /**
     * OpenWeatherMap 이 주는 지명은 로마자뿐이고("Kwanghui-dong") 구·동 단위도 없다.
     * Nominatim(OpenStreetMap)은 키 없이 "시 구 동" 한글 주소를 준다.
     */
    private static final String NOMINATIM_URL =
            "https://nominatim.openstreetmap.org/reverse";

    /** Nominatim 은 User-Agent 를 안 보내면 요청을 거부한다. */
    private static final String NOMINATIM_USER_AGENT = "meokji-app-weather/1.0";

    /** OpenWeatherMap 의 weather[0].main 을 화면 배경 코드로 바꾸는 표. */
    private static final Map<String, String> CONDITION_CODES = Map.of(
            "Clear", "CLEAR",
            "Clouds", "CLOUDS",
            "Rain", "RAIN",
            "Drizzle", "RAIN",
            "Thunderstorm", "RAIN",
            "Snow", "SNOW");

    /** 시·도 이름은 법정 명칭이라 길다. 카드 폭이 좁아서 흔한 접미사를 뗀다. */
    private static final List<String> CITY_SUFFIXES =
            List.of("특별자치시", "특별자치도", "특별시", "광역시");

    private static final long CACHE_MILLIS = 600_000L;      // 10분
    private static final int CACHE_COORD_DIGITS = 2;         // 2자리 = 약 1.1km
    private static final int CACHE_MAX_ENTRIES = 500;

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    // mock 이면 늘 28도 맑음, openweather 면 진짜 날씨 (application.yml 의 app.weather.provider)
    @Value("${app.weather.provider}")
    private String weatherProvider;

    // OpenWeather 인증키
    @Value("${app.weather.api-key}")
    private String weatherApiKey;

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /** 좌표(반올림) -> (저장 시각, 날씨). 1km 안쪽이면 같은 열쇠가 된다. */
    private final Map<String, Cached> weatherCache = new ConcurrentHashMap<>();

    /** 좌표(반올림) -> 한글 주소. 동네 이름은 안 바뀌므로 기한을 두지 않는다. */
    private final Map<String, String> addressCache = new ConcurrentHashMap<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Cached {

        private long savedAt;

        private Weather weather;
    }

    /** 바깥 API 가 준 값 그대로. 문구 만들기는 WeatherService 가 한다. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Weather {

        private String city;

        private int temp;

        private int feelsLike;

        private int humidity;

        private String condition;

        private String conditionText;
    }

    /**
     * 좌표로 현재 날씨를 가져온다.
     *
     * @throws IllegalStateException 설정이 잘못됐거나 바깥 API 가 응답하지 않을 때.
     *         부르는 쪽이 503 으로 바꿔 내보낸다.
     */
    public Weather get(double lat, double lon) {
        String provider = weatherProvider.toLowerCase();

        if ("mock".equals(provider)) {
            return mock();
        }

        if (!"openweather".equals(provider)) {
            // 모르는 값이면 조용히 mock 으로 넘어가지 않는다. 넘어가면
            // "날씨가 계속 28도 맑음으로만 나오는데 이유를 모르겠다" 가 된다.
            throw new IllegalStateException(
                    "지원하지 않는 app.weather.provider: " + provider
                    + ". 사용 가능한 값: mock, openweather");
        }

        if (weatherApiKey.isBlank()) {
            throw new IllegalStateException(
                    "app.weather.provider=openweather 인데 api-key 가 비어 있습니다.");
        }

        String key = cacheKey(lat, lon);
        Cached cached = weatherCache.get(key);

        if (cached != null && System.currentTimeMillis() - cached.getSavedAt() < CACHE_MILLIS) {
            return cached.getWeather();
        }

        Weather weather = fetchOpenWeather(lat, lon);

        // 캐시가 너무 커지면 통째로 비운다. LRU 를 붙일 만큼 큰 데이터가 아니다.
        if (weatherCache.size() > CACHE_MAX_ENTRIES) {
            weatherCache.clear();
        }
        weatherCache.put(key, new Cached(System.currentTimeMillis(), weather));

        return weather;
    }

    /** 개발용 가짜 날씨. 언제 불러도 같은 값이 나온다. */
    private static Weather mock() {
        return new Weather("서울특별시", 28, 30, 52, "CLEAR", "맑음");
    }

    private Weather fetchOpenWeather(double lat, double lon) {
        String url = OPENWEATHER_URL
                + "?lat=" + lat
                + "&lon=" + lon
                + "&appid=" + weatherApiKey
                // 섭씨로 받는다. 안 주면 켈빈으로 온다.
                + "&units=metric"
                // 날씨 설명을 한글로 받는다.
                + "&lang=kr";

        JsonNode data;

        try {
            HttpResponse<String> response = http.send(
                    HttpRequest.newBuilder(URI.create(url))
                            .timeout(Duration.ofSeconds(10))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 401) {
                // 키가 틀렸거나 발급 직후라 아직 활성화되지 않은 경우다.
                // 새 키는 활성화까지 시간이 걸릴 수 있어서 안내를 따로 붙인다.
                throw new IllegalStateException(
                        "날씨 API 키가 유효하지 않습니다. app.weather.api-key 를 "
                        + "확인해주세요. 방금 발급한 키라면 활성화까지 시간이 걸릴 수 "
                        + "있습니다.");
            }

            if (response.statusCode() >= 400) {
                throw new IllegalStateException(
                        "날씨 API 가 오류를 돌려줬습니다 (HTTP " + response.statusCode() + ").");
            }

            data = MAPPER.readTree(response.body());
        } catch (IllegalStateException exc) {
            throw exc;
        } catch (InterruptedException exc) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("날씨 API 호출이 중단되었습니다.");
        } catch (Exception exc) {
            // 연결 실패 · 시간 초과 · DNS 오류 · 200 인데 JSON 이 아닌 경우
            throw new IllegalStateException(
                    "날씨 API 에 연결하지 못했습니다 (" + exc.getClass().getSimpleName() + ").");
        }

        JsonNode first = data.path("weather").path(0);
        String sky = first.path("main").asString("");
        // 표에 없는 값이면 CLOUDS 로 본다. 배경이 없는 것보다 낫다.
        String condition = CONDITION_CODES.getOrDefault(sky, "CLOUDS");

        // 한글 주소를 먼저 시도하고, 실패하면 OpenWeather 가 준 이름을 쓴다.
        // 바다나 격오지는 그 이름마저 빈 문자열로 와서 마지막 방어선을 둔다.
        String city = koreanAddress(lat, lon);
        if (city == null || city.isBlank()) {
            city = data.path("name").asString("");
        }
        if (city.isBlank()) {
            city = "알 수 없음";
        }

        JsonNode main = data.path("main");

        return new Weather(
                city,
                (int) Math.round(main.path("temp").asDouble()),
                (int) Math.round(main.path("feels_like").asDouble()),
                main.path("humidity").asInt(),
                condition,
                first.path("description").asString(""));
    }

    /** 좌표를 "시 구 동" 한글 주소로. 못 구하면 null. */
    private String koreanAddress(double lat, double lon) {
        String key = cacheKey(lat, lon);

        if (addressCache.containsKey(key)) {
            return addressCache.get(key);
        }

        String address = fetchKoreanAddress(lat, lon);

        if (addressCache.size() > CACHE_MAX_ENTRIES) {
            addressCache.clear();
        }
        // null 도 넣어둔다. 못 구하는 자리를 매번 다시 물어볼 이유가 없다.
        addressCache.put(key, address);

        return address;
    }

    private String fetchKoreanAddress(double lat, double lon) {
        String url = NOMINATIM_URL
                + "?lat=" + lat
                + "&lon=" + lon
                + "&format=jsonv2"
                + "&accept-language=ko"
                // 18 이면 동 단위까지 나온다.
                + "&zoom=18";

        JsonNode address;

        try {
            HttpResponse<String> response = http.send(
                    HttpRequest.newBuilder(URI.create(url))
                            .timeout(Duration.ofSeconds(4))
                            .header("User-Agent", NOMINATIM_USER_AGENT)
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 400) {
                return null;
            }

            address = MAPPER.readTree(response.body()).path("address");
        } catch (InterruptedException exc) {
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception exc) {
            // 주소는 있으면 좋은 값이다. 못 구해도 날씨는 보여준다.
            return null;
        }

        // 지역마다 채워주는 키가 달라서 있는 것 중 앞에 있는 값을 쓴다.
        String city = firstOf(address, "city", "county", "state");
        String district = firstOf(address, "borough", "city_district");
        String dong = firstOf(address, "suburb", "quarter", "neighbourhood");

        // "서울특별시" -> "서울"
        if (city != null) {
            for (String suffix : CITY_SUFFIXES) {
                if (city.endsWith(suffix)) {
                    city = city.substring(0, city.length() - suffix.length());
                    break;
                }
            }
        }

        return joinAddress(city, district, dong);
    }

    private static String firstOf(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && !value.isNull() && !value.asString("").isBlank()) {
                return value.asString();
            }
        }

        return null;
    }

    /**
     * 시·구·동을 하나로 잇는다.
     *
     * 단순히 붙이지 않는다. Nominatim 이 "서귀포시" 와 "서귀포시 서홍동" 을 같이
     * 줄 때가 있어서, 그대로 이으면 "서귀포시 서귀포시 서홍동" 이 된다. 낱말이
     * 포함 관계면 더 자세한 쪽만 남긴다.
     */
    static String joinAddress(String city, String district, String dong) {
        List<String> parts = new ArrayList<>();

        for (String part : new String[] {city, district, dong}) {
            if (part != null && !part.isBlank()) {
                addPart(parts, part);
            }
        }

        return parts.isEmpty() ? null : String.join(" ", parts);
    }

    private static void addPart(List<String> parts, String part) {
        Set<String> words = new LinkedHashSet<>(List.of(part.split(" ")));

        for (int index = 0; index < parts.size(); index++) {
            Set<String> kept = new LinkedHashSet<>(List.of(parts.get(index).split(" ")));

            // 이미 담은 쪽이 더 자세하다. 새 조각은 버린다.
            if (kept.containsAll(words)) {
                return;
            }

            // 새 조각이 더 자세하다. 있던 자리에 갈아 끼운다.
            // 뒤에 붙이면 순서가 흐트러진다.
            if (words.containsAll(kept)) {
                parts.set(index, part);
                return;
            }
        }

        parts.add(part);
    }

    private static String cacheKey(double lat, double lon) {
        double factor = Math.pow(10, CACHE_COORD_DIGITS);

        return Math.round(lat * factor) / factor + "," + Math.round(lon * factor) / factor;
    }
}
