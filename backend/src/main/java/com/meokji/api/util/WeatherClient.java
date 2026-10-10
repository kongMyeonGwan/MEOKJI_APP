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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.meokji.api.recommend.service.WeatherService;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 바깥 날씨 API 를 부른다.
 *
 * WebClient 대신 JDK 의 HttpClient 를 쓴다. 부르는 곳이 한 군데고
 * 동기 호출이라, 의존성을 늘려 얻을 것이 없다.
 *
 * <p>두 곳을 부른다
 *   OpenWeatherMap : 기온 · 습도 · 하늘 상태
 *   Nominatim      : 좌표 -&gt; 한글 주소 ("서울 중구 명동")
 *
 * <p>왜 주소를 따로 부르나 — OpenWeatherMap 이 주는 지명은 로마자뿐이고
 * 구 · 동 단위도 없다. 화면 카드에 "Kwanghui-dong" 이 뜨면 어디인지 알기 어렵다.
 *
 * <p>문구는 여기서 만들지 않는다. "오늘은 맑고 더운 날씨에요" 같은 말은
 * {@link WeatherService} 가 붙인다 — 바깥 API 를 다른 곳으로 바꿀 때
 * 우리 문구까지 같이 옮기지 않게 나눠 두었다.
 *
 * <p>캐시를 들고 있다
 *
 * <p>날씨는 10분 안에 바뀌지 않고, 주소는 아예 안 바뀐다. 추천을 부를 때마다
 * 바깥 API 를 두 번씩 두드리면 느리고 호출 제한에도 걸린다.
 * 좌표를 소수 둘째 자리로 반올림해 열쇠로 쓰므로, 1km 안쪽의 사람들은
 * 같은 결과를 나눠 쓴다.
 *
 * <p>실패하면 IllegalStateException 을 던진다. ApiException 을 쓰지 않는
 * 이유 — util 은 HTTP 상태 코드를 모르는 자리다. 503 으로 바꾸는 일은
 * 부르는 쪽(WeatherServiceImpl)이 한다.
 */
@Component
public class WeatherClient {

    private static final String OPENWEATHER_URL =
            "https://api.openweathermap.org/data/2.5/weather";

    /**
     * OpenWeatherMap 이 주는 지명은 로마자뿐이고("Kwanghui-dong") 구·동 단위도 없다.
     * Nominatim(OpenStreetMap)은 키 없이 "시 구 동" 한글 주소를 준다.
     */
    // 키가 필요 없다는 점이 중요하다. 키를 받아야 하는 서비스를 쓰면
    // 팀원마다 발급받아야 하고, 설정이 하나 더 늘어난다
    private static final String NOMINATIM_URL =
            "https://nominatim.openstreetmap.org/reverse";

    /** Nominatim 은 User-Agent 를 안 보내면 요청을 거부한다. */
    // 무료로 열어둔 서비스라 "누가 쓰는지" 를 밝히라는 규칙이 있다.
    // 안 보내면 403 이 오는데, 그 이유를 알아내기까지 한참 걸린다
    private static final String NOMINATIM_USER_AGENT = "meokji-app-weather/1.0";

    /** OpenWeatherMap 의 weather[0].main 을 화면 배경 코드로 바꾸는 표. */
    //
    // 네 가지로 줄인다(CLEAR · CLOUDS · RAIN · SNOW). 화면이 고를 배경이
    // 넷뿐이라 그보다 자세할 이유가 없다.
    //
    // Drizzle(이슬비) · Thunderstorm(뇌우)을 RAIN 으로 묶는 이유 —
    // 추천에서 하는 일이 같다. 어느 쪽이든 "국물이나 튀김" 이다
    private static final Map<String, String> CONDITION_CODES = Map.of(
            "Clear", "CLEAR",
            "Clouds", "CLOUDS",
            "Rain", "RAIN",
            "Drizzle", "RAIN",
            "Thunderstorm", "RAIN",
            "Snow", "SNOW");

    /** 시·도 이름은 법정 명칭이라 길다. 카드 폭이 좁아서 흔한 접미사를 뗀다. */
    // "특별자치시" 를 "특별시" 보다 앞에 둔 이유 — 긴 것부터 떼야 한다.
    // 짧은 것부터 보면 "세종특별자치시" 에서 아무것도 안 걸린다
    private static final List<String> CITY_SUFFIXES =
            List.of("특별자치시", "특별자치도", "특별시", "광역시");

    private static final long CACHE_MILLIS = 600_000L;      // 10분
    private static final int CACHE_COORD_DIGITS = 2;         // 2자리 = 약 1.1km
    private static final int CACHE_MAX_ENTRIES = 500;
    // 600_000L 처럼 밑줄을 넣으면 자리 수를 눈으로 셀 수 있다 (자바 문법이다).
    //
    // 10분으로 둔 이유 — 기온이 10분 안에 느껴질 만큼 바뀌지 않는다.
    // 500개로 둔 이유 — 1km 격자 500개면 서울 전체를 덮는다

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    // mock 이면 늘 28도 맑음, openweather 면 진짜 날씨 (application.yml 의 app.weather.provider)
    //
    // mock 을 두는 이유 — 키 없이도 화면을 끝까지 볼 수 있어야 한다.
    // 그리고 테스트가 날씨 API 에 끌려가지 않는다
    @Value("${app.weather.provider}")
    private String weatherProvider;

    // OpenWeather 인증키
    @Value("${app.weather.api-key}")
    private String weatherApiKey;

    // HttpClient 를 하나만 만들어 재사용한다. 연결을 다시 쓸 수 있어서
    // 부를 때마다 만드는 것보다 빠르고, 만드는 비용도 가볍지 않다.
    //
    // connectTimeout 은 "연결이 될 때까지" 의 한계다. 응답을 기다리는
    // 한계는 요청마다 따로 건다(아래 timeout)
    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    /** 좌표(반올림) -> (저장 시각, 날씨). 1km 안쪽이면 같은 열쇠가 된다. */
    // ConcurrentHashMap 인 이유 — 요청이 동시에 들어온다. 보통 HashMap 을
    // 여러 곳에서 같이 고치면 내부가 망가져 엉뚱한 값이 나오거나 멈춘다
    private final Map<String, Cached> weatherCache = new ConcurrentHashMap<>();

    /** 좌표(반올림) -> 한글 주소. 동네 이름은 안 바뀌므로 기한을 두지 않는다. */
    // 날씨 쪽과 달리 저장 시각을 안 들고 있다. 기한이 없으니 쓸 데가 없다
    private final Map<String, String> addressCache = new ConcurrentHashMap<>();

    // 캐시 한 칸. 날씨와 "언제 저장했는지" 를 묶어 둔다
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Cached {

        private long savedAt;

        private Weather weather;
    }

    /** 바깥 API 가 준 값 그대로. 문구 만들기는 WeatherService 가 한다. */
    // WeatherVO 와 칸이 거의 같지만 comment · pickLead 가 없다.
    // 그 둘이 서비스가 만드는 값이라 여기 둘 자리가 아니다
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
        // 대소문자를 맞춘다. 설정에 "Mock" 이라고 적어도 통하게 한다
        String provider = weatherProvider.toLowerCase();

        if ("mock".equals(provider)) {
            return mock();
        }

        if (!"openweather".equals(provider)) {
            // 모르는 값이면 조용히 mock 으로 넘어가지 않는다. 넘어가면
            // "날씨가 계속 28도 맑음으로만 나오는데 이유를 모르겠다" 가 된다.
            //
            // 쓸 수 있는 값을 문장에 적어준다. 안 적으면 무엇으로
            // 고쳐야 할지 모른다
            throw new IllegalStateException(
                    "지원하지 않는 app.weather.provider: " + provider
                    + ". 사용 가능한 값: mock, openweather");
        }

        if (weatherApiKey.isBlank()) {
            // 부르기 전에 막는다. 빈 키로 부르면 401 이 오는데,
            // 그 메시지만 보면 키가 틀린 것인지 없는 것인지 알 수 없다
            throw new IllegalStateException(
                    "app.weather.provider=openweather 인데 api-key 가 비어 있습니다.");
        }

        String key = cacheKey(lat, lon);
        Cached cached = weatherCache.get(key);

        // 10분이 안 지났으면 저장해둔 것을 그대로 준다
        if (cached != null && System.currentTimeMillis() - cached.getSavedAt() < CACHE_MILLIS) {
            return cached.getWeather();
        }

        Weather weather = fetchOpenWeather(lat, lon);

        // 캐시가 너무 커지면 통째로 비운다. LRU 를 붙일 만큼 큰 데이터가 아니다.
        //
        // 다 비우면 다음 요청들이 한 번씩 다시 부르게 된다. 그 편이
        // "오래된 것부터 지우는" 구조를 만들어 유지하는 것보다 간단하다
        if (weatherCache.size() > CACHE_MAX_ENTRIES) {
            weatherCache.clear();
        }
        weatherCache.put(key, new Cached(System.currentTimeMillis(), weather));

        return weather;
    }

    /** 개발용 가짜 날씨. 언제 불러도 같은 값이 나온다. */
    // 같은 값이 나오는 것이 중요하다. 무작위로 주면 테스트가 돌릴 때마다
    // 다른 결과를 내고, 화면도 새로고침마다 바뀐다
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
        // 좌표를 그대로 이어 붙인다. 숫자라서 주소에 넣을 때 따로
        // 처리할 글자가 없다

        JsonNode data;

        try {
            HttpResponse<String> response = http.send(
                    HttpRequest.newBuilder(URI.create(url))
                            // 응답을 기다릴 한계. 연결 한계(5초)와 별도다
                            .timeout(Duration.ofSeconds(10))
                            .GET()
                            .build(),
                    HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 401) {
                // 키가 틀렸거나 발급 직후라 아직 활성화되지 않은 경우다.
                // 새 키는 활성화까지 시간이 걸릴 수 있어서 안내를 따로 붙인다.
                //
                // 이 안내가 없으면 "키를 넣었는데 안 된다" 로 한참 헤맨다
                throw new IllegalStateException(
                        "날씨 API 키가 유효하지 않습니다. app.weather.api-key 를 "
                        + "확인해주세요. 방금 발급한 키라면 활성화까지 시간이 걸릴 수 "
                        + "있습니다.");
            }

            if (response.statusCode() >= 400) {
                // 그 밖의 오류. 상태 코드를 문장에 적어준다 -
                // 429(호출 제한)와 500(그쪽 장애)은 할 일이 다르다
                throw new IllegalStateException(
                        "날씨 API 가 오류를 돌려줬습니다 (HTTP " + response.statusCode() + ").");
            }

            data = MAPPER.readTree(response.body());
        } catch (IllegalStateException exc) {
            throw exc;
            // 위에서 우리가 던진 것을 그대로 올려보낸다. 이 catch 가
            // 없으면 아래 Exception 에 잡혀 문장이 "연결하지 못했습니다" 로
            // 뭉개진다 - 키 문제인지 통신 문제인지 알 수 없게 된다
        } catch (InterruptedException exc) {
            // 멈추라는 신호가 온 경우(서버를 끌 때). 그 사실을 지우지
            // 않도록 interrupt 를 다시 켜 둔다
            Thread.currentThread().interrupt();
            throw new IllegalStateException("날씨 API 호출이 중단되었습니다.");
        } catch (Exception exc) {
            // 연결 실패 · 시간 초과 · DNS 오류 · 200 인데 JSON 이 아닌 경우
            //
            // 클래스 이름만 남긴다. 메시지에는 주소와 키가 섞여 있을 수 있고,
            // 이 문장은 사용자 화면까지 나간다
            throw new IllegalStateException(
                    "날씨 API 에 연결하지 못했습니다 (" + exc.getClass().getSimpleName() + ").");
        }

        // weather 는 배열이다. 지금 날씨는 늘 한 칸뿐이라 0번을 본다
        JsonNode first = data.path("weather").path(0);
        String sky = first.path("main").asString("");
        // 표에 없는 값이면 CLOUDS 로 본다. 배경이 없는 것보다 낫다.
        //
        // OpenWeather 는 안개 · 황사 · 모래폭풍 같은 값도 준다. 그 전부에
        // 배경을 만들어 두지 않았고, 흐린 하늘이 가장 무난하다
        String condition = CONDITION_CODES.getOrDefault(sky, "CLOUDS");

        // 한글 주소를 먼저 시도하고, 실패하면 OpenWeather 가 줄 이름을 쓴다.
        // 바다나 격오지는 그 이름마저 빈 문자열로 와서 마지막 방어선을 둔다.
        String city = koreanAddress(lat, lon);
        if (city == null || city.isBlank()) {
            city = data.path("name").asString("");
        }
        if (city.isBlank()) {
            // 세 단계가 다 실패한 경우. 빈 글자를 내보내면 카드에
            // "· 맑음 28°" 처럼 앞이 비어 보인다
            city = "알 수 없음";
        }

        JsonNode main = data.path("main");

        return new Weather(
                city,
                // 소수점을 반올림해 정수로. 19.34도를 그대로 보여줄 이유가 없고,
                // 화면마다 반올림하면 한 화면에 19도와 19.3도가 같이 나온다
                (int) Math.round(main.path("temp").asDouble()),
                (int) Math.round(main.path("feels_like").asDouble()),
                main.path("humidity").asInt(),
                condition,
                // 한글 설명("튼구름"). lang=kr 로 받았다
                first.path("description").asString(""));
    }

    /** 좌표를 "시 구 동" 한글 주소로. 못 구하면 null. */
    private String koreanAddress(double lat, double lon) {
        String key = cacheKey(lat, lon);

        // get 이 아니라 containsKey 로 본다. 값이 null 인 칸도 "이미
        // 물어봤다" 는 뜻이라, get 으로는 둘을 구별할 수 없다
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
                            // 날씨(10초)보다 짧게 둔다. 주소는 있으면
                            // 좋은 값이라, 오래 기다려서 전체를 늦출 이유가 없다
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
            //
            // 위 fetchOpenWeather 와 반대다. 저쪽은 예외를 던져 올려보내고,
            // 이쪽은 null 로 삼킨다 - 날씨가 없으면 카드를 못 그리지만
            // 주소가 없으면 지명만 비면 된다
            return null;
        }

        // 지역마다 채워주는 키가 달라서 있는 것 중 앞에 있는 값을 쓴다.
        //
        // 서울은 city + borough + suburb 로 오고, 군 지역은 county + quarter 로
        // 온다. 어느 키가 올지 미리 알 수 없어서 후보를 늘어놓고 찾는다
        String city = firstOf(address, "city", "county", "state");
        String district = firstOf(address, "borough", "city_district");
        String dong = firstOf(address, "suburb", "quarter", "neighbourhood");

        // "서울특별시" -> "서울"
        // 카드 폭이 좁아서, 법정 명칭을 그대로 쓰면 "서울특별시 중구 명동" 이
        // 한 줄을 넘긴다
        if (city != null) {
            for (String suffix : CITY_SUFFIXES) {
                if (city.endsWith(suffix)) {
                    city = city.substring(0, city.length() - suffix.length());
                    break; // 하나만 떼고 끝낸다. 겹쳐 붙는 경우는 없다
                }
            }
        }

        return joinAddress(city, district, dong);
    }

    // 여러 후보 칸 중 값이 있는 첫 번째를 돌려준다. 다 없으면 null.
    //
    // 가변 인자(String... fields)로 받아서 부르는 쪽이 후보를 늘어놓을 수 있다
    private static String firstOf(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.get(field);
            // 셋을 다 본다 — 칸이 없는 경우 · null 인 경우 · 빈 글자인 경우.
            // 빈 글자가 들어 있는 칸이 실제로 온다
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
    // package-private 이다. 같은 패키지의 테스트가 이 규칙을 직접
    // 확인할 수 있게 열어둔 것이다 - 바깥 API 를 부르지 않고 셋을 넣어볼 수 있다
    static String joinAddress(String city, String district, String dong) {
        List<String> parts = new ArrayList<>();

        // 배열로 묶어 돌리는 이유 — 셋에 같은 처리를 한다.
        // addPart 를 세 번 부르면 null 확인도 세 번 적게 된다
        for (String part : new String[] {city, district, dong}) {
            if (part != null && !part.isBlank()) {
                addPart(parts, part);
            }
        }

        // 하나도 없으면 null. 빈 글자를 주면 부르는 쪽이 isBlank 로
        // 또 확인해야 한다
        return parts.isEmpty() ? null : String.join(" ", parts);
    }

    // 조각 하나를 목록에 넣는다. 이미 담은 것과 포함 관계면 자세한 쪽만 남긴다.
    //
    // 낱말 단위로 견주는 이유 — 글자로만 보면 "중구" 와 "중구동" 이 겹치는
    // 것으로 읽힌다. 띄어쓰기로 나눠 낱말 집합을 만들면 그런 일이 없다
    private static void addPart(List<String> parts, String part) {
        // LinkedHashSet 이라 같은 낱말이 겹쳐도 한 번만 들어간다
        Set<String> words = new LinkedHashSet<>(List.of(part.split(" ")));

        for (int index = 0; index < parts.size(); index++) {
            Set<String> kept = new LinkedHashSet<>(List.of(parts.get(index).split(" ")));

            // 이미 담은 쪽이 더 자세하다. 새 조각은 버린다.
            if (kept.containsAll(words)) {
                return;
            }

            // 새 조각이 더 자세하다. 있던 자리에 갈아 끼운다.
            // 뒤에 붙이면 순서가 흐트러진다.
            //
            // 순서가 중요한 이유 — "서울 중구 명동" 처럼 큰 단위부터
            // 적혀야 주소로 읽힌다
            if (words.containsAll(kept)) {
                parts.set(index, part);
                return;
            }
        }

        // 겹치는 것이 없으면 뒤에 붙인다
        parts.add(part);
    }

    // 좌표를 캐시 열쇠로. 소수 둘째 자리로 반올림한다.
    //
    // 반올림하는 이유 — 좌표가 조금만 달라도 다른 열쇠가 되면 캐시가
    // 거의 안 맞는다. 둘째 자리면 약 1.1km 이고, 그 안의 날씨는 같다.
    //
    // 글자로 이어 붙이는 이유 — Map 의 열쇠로 쓰려면 하나의 값이어야 한다.
    // 전용 클래스를 만들면 equals · hashCode 를 또 챙겨야 한다
    private static String cacheKey(double lat, double lon) {
        double factor = Math.pow(10, CACHE_COORD_DIGITS);

        return Math.round(lat * factor) / factor + "," + Math.round(lon * factor) / factor;
    }
}
