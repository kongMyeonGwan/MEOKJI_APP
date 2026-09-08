"""날씨 조회.

.env 의 WEATHER_PROVIDER 값에 따라 두 가지로 동작한다.

    mock          API 키 없이도 화면을 볼 수 있게 고정값을 돌려준다 (개발용)
    openweather   OpenWeatherMap 에서 진짜 날씨를 받아온다

바깥에서는 get_weather(lat, lon) 하나만 쓰면 된다.
어느 쪽으로 갈지는 이 파일 안에서 정하므로, 서비스 코드는 몰라도 된다.

돌려주는 값은 dict 이고 키는 항상 아래 여섯 개다.

    city            "서울특별시 중구 광희동"
    temp            기온 (섭씨, 정수)
    feels_like      체감온도
    humidity        습도 (%)
    condition       CLEAR / CLOUDS / RAIN / SNOW  <- 화면 배경을 고르는 코드
    condition_text  "맑음" 처럼 사람이 읽는 문구   <- 화면에 그대로 쓰는 값
"""

import time

import httpx

from app.core.config import settings

# OpenWeatherMap 현재 날씨 API
# https://openweathermap.org/current
OPENWEATHER_URL = "https://api.openweathermap.org/data/2.5/weather"

# OpenWeatherMap 이 주는 지명은 로마자뿐이고("Kwanghui-dong") 구·동 단위도 없다.
# Nominatim(OpenStreetMap)은 API 키 없이 "시 구 동" 한글 주소를 준다.
NOMINATIM_URL = "https://nominatim.openstreetmap.org/reverse"

# Nominatim 은 User-Agent 를 안 보내면 요청을 거부한다.
NOMINATIM_USER_AGENT = "meokji-app-weather/1.0"

# OpenWeatherMap 의 weather[0].main 값을 화면 배경 코드로 바꾸는 표.
# 표에 없는 값(Mist, Haze, Fog, Dust ...)은 전부 CLOUDS 로 묶는다.
# 배경 이미지가 4종류뿐이라 더 나눠봐야 화면에서 구분되지 않는다.
CONDITION_CODES = {
    "Clear": "CLEAR",
    "Clouds": "CLOUDS",
    "Rain": "RAIN",
    "Drizzle": "RAIN",
    "Thunderstorm": "RAIN",
    "Snow": "SNOW",
}

# 외부 API 하나를 기다리는 최대 시간(초).
#
# 둘을 다르게 잡았다. OpenWeather 는 이 값이 없으면 보여줄 게 없어서 넉넉히 주고,
# Nominatim(주소)은 없어도 화면이 도니까 짧게 끊는다.
# 주소 하나 때문에 대시보드 첫 화면이 10초씩 멈추면 안 된다.
OPENWEATHER_TIMEOUT_SECONDS = 10.0
NOMINATIM_TIMEOUT_SECONDS = 4.0

# 같은 자리를 다시 물어보지 않으려고 잠깐 저장해둔다 (아래 캐시 설명 참고).
WEATHER_CACHE_SECONDS = 600  # 10분

# 좌표를 소수점 몇 자리까지 보고 "같은 자리"로 칠지.
# 2자리면 약 1.1km 다. 그 안에서는 날씨도 동네 이름도 같다.
CACHE_COORD_DIGITS = 2

# 캐시가 무한정 커지지 않게 하는 상한. 넘으면 통째로 비운다.
# LRU 같은 걸 붙일 만큼 큰 데이터가 아니다.
CACHE_MAX_ENTRIES = 500

# 한글 주소의 시·도 이름은 법정 명칭이라 길다.
# 카드 폭이 좁아서 다 붙이면 구·동이 밀려나므로 흔한 접미사를 뗀다.
CITY_SUFFIXES = ("특별자치시", "특별자치도", "특별시", "광역시")


# =========================================================
# 캐시
#
# 왜 필요한가
# -----------
# 대시보드를 열 때마다 바깥 API 를 두 번 부르는데, 주소 변환(Nominatim)이
# 매번 2~3초씩 걸렸다. 첫 화면이 그만큼 멈춘다.
#
# 날씨는 10분마다 바뀌지 않고, 동네 이름은 아예 안 바뀐다. 그래서
#   - 날씨는 10분간
#   - 주소는 서버가 살아있는 동안
# 같은 좌표면 저장해둔 값을 그대로 쓴다.
#
# 덤으로 두 가지가 더 해결된다.
#   - OpenWeather 무료 플랜의 분당 60회 제한에 걸리지 않는다
#   - Nominatim 은 "초당 1회 이상 부르지 말 것"이 이용 정책이다
#
# 프로세스 메모리에만 둔다. 서버를 재시작하면 사라지지만, 그래도 상관없는
# 데이터라 Redis 같은 걸 붙일 이유가 없다.
# =========================================================

# {(반올림한 위도, 경도): (저장한 시각, 날씨 dict)}
weather_cache: dict[tuple, tuple[float, dict]] = {}

# {(반올림한 위도, 경도): 주소 문자열 또는 None}
address_cache: dict[tuple, str | None] = {}


def cache_key(lat: float, lon: float) -> tuple:
    """좌표를 캐시 열쇠로 바꾼다. 1km 안쪽이면 같은 열쇠가 된다."""
    return (round(lat, CACHE_COORD_DIGITS), round(lon, CACHE_COORD_DIGITS))


def trim_cache(cache: dict) -> None:
    """캐시가 너무 커지면 비운다.

    오래된 것만 골라 지우지 않고 통째로 비운다.
    한 번 더 조회하면 다시 채워지는 값이라, 정교하게 관리할 이유가 없다.
    """
    if len(cache) > CACHE_MAX_ENTRIES:
        cache.clear()


def get_weather(lat: float, lon: float) -> dict:
    """좌표로 현재 날씨를 가져온다.

    .env 의 WEATHER_PROVIDER 를 보고 가짜 날씨와 진짜 날씨 중에 고른다.
    """
    provider = settings.WEATHER_PROVIDER.lower()

    if provider == "mock":
        return get_mock_weather()

    if provider == "openweather":
        if not settings.OPENWEATHER_API_KEY:
            raise RuntimeError(
                "WEATHER_PROVIDER=openweather 인데 OPENWEATHER_API_KEY 가 비어 있습니다."
            )
        return get_openweather(lat, lon)

    # 모르는 값이면 조용히 mock 으로 넘어가지 않고 에러를 낸다.
    # 조용히 넘어가면 "날씨가 계속 28도 맑음으로만 나오는데 이유를 모르겠다"가 된다.
    raise RuntimeError(
        f"지원하지 않는 WEATHER_PROVIDER: {settings.WEATHER_PROVIDER}. "
        "사용 가능한 값: mock, openweather"
    )


def get_mock_weather() -> dict:
    """개발용 가짜 날씨. 언제 불러도 같은 값이 나온다."""
    return {
        "city": "서울특별시",
        "temp": 28,
        "feels_like": 30,
        "humidity": 52,
        "condition": "CLEAR",
        "condition_text": "맑음",
    }


def get_openweather(lat: float, lon: float) -> dict:
    """OpenWeatherMap 에서 현재 날씨를 받아온다. 10분간은 저장해둔 값을 쓴다."""
    key = cache_key(lat, lon)
    cached = weather_cache.get(key)

    if cached is not None:
        saved_at, weather = cached
        if time.time() - saved_at < WEATHER_CACHE_SECONDS:
            return weather

    weather = fetch_openweather(lat, lon)

    trim_cache(weather_cache)
    weather_cache[key] = (time.time(), weather)

    return weather


def fetch_openweather(lat: float, lon: float) -> dict:
    """캐시를 거치지 않고 실제로 OpenWeatherMap 을 부른다.

    실패하면 httpx 예외를 그대로 올려보내지 않고 RuntimeError 로 바꾼다.
    그냥 두면 서비스 계층까지 httpx 예외가 새어나가 500 이 되는데,
    화면에는 "요청을 처리하지 못했습니다" 만 뜨고 원인을 알 수 없다.
    서비스 계층(weather_service)이 이 메시지를 503 으로 바꿔 내보낸다.
    """
    try:
        response = httpx.get(
            OPENWEATHER_URL,
            params={
                "lat": lat,
                "lon": lon,
                "appid": settings.OPENWEATHER_API_KEY,
                "units": "metric",  # 섭씨로 받는다 (안 주면 켈빈으로 온다)
                "lang": "kr",       # 날씨 설명을 한글로 받는다
            },
            timeout=OPENWEATHER_TIMEOUT_SECONDS,
        )
        response.raise_for_status()  # 4xx / 5xx 면 여기서 예외가 난다
        data = response.json()
    except httpx.HTTPStatusError as exc:
        # 401 은 키가 틀렸거나 발급 직후라 아직 활성화되지 않은 경우다.
        # 새 키는 활성화까지 시간이 걸릴 수 있어서 이 안내를 따로 붙인다.
        status_code = exc.response.status_code
        if status_code == 401:
            raise RuntimeError(
                "날씨 API 키가 유효하지 않습니다. backend/.env 의 "
                "OPENWEATHER_API_KEY 를 확인해주세요. "
                "방금 발급한 키라면 활성화까지 시간이 걸릴 수 있습니다."
            )
        raise RuntimeError(f"날씨 API 가 오류를 돌려줬습니다 (HTTP {status_code}).")
    except httpx.HTTPError as exc:
        # 연결 실패 · 시간 초과 · DNS 오류
        raise RuntimeError(
            f"날씨 API 에 연결하지 못했습니다 ({type(exc).__name__})."
        )
    except ValueError:
        # 200 인데 본문이 JSON 이 아닌 경우 (점검 안내 페이지 등)
        raise RuntimeError("날씨 API 응답을 해석하지 못했습니다.")

    # 예: "Clear" -> "CLEAR". 표에 없으면 CLOUDS 로 처리한다.
    sky = data["weather"][0]["main"]
    condition = CONDITION_CODES.get(sky, "CLOUDS")

    # 한글 주소를 먼저 시도하고, 실패하면 OpenWeatherMap 이 준 이름을 쓴다.
    # 바다나 격오지는 그 이름마저 빈 문자열로 올 수 있어서 마지막 방어선을 둔다.
    city = get_korean_address(lat, lon) or data.get("name") or "알 수 없음"

    return {
        "city": city,
        "temp": round(data["main"]["temp"]),
        "feels_like": round(data["main"]["feels_like"]),
        "humidity": data["main"]["humidity"],
        "condition": condition,
        "condition_text": data["weather"][0]["description"],
    }


def get_korean_address(lat: float, lon: float) -> str | None:
    """좌표를 "시 구 동" 한글 주소로 바꾼다. 실패하면 None.

    한 번 구한 주소는 계속 저장해둔다. 동네 이름은 바뀌지 않는데
    이 조회가 2~3초씩 걸려서, 매번 부르면 대시보드 첫 화면이 그만큼 멈춘다.

    실패(None)도 저장한다. 안 그러면 바다 한가운데 좌표처럼 영영 못 구하는
    자리에서 매번 4초씩 기다리게 된다.
    """
    key = cache_key(lat, lon)

    # None 도 정상적인 결과라서 `if address_cache.get(key)` 로 확인하면 안 된다.
    # 그렇게 쓰면 "실패했다"는 기록이 캐시 미스로 읽혀 매번 다시 부른다.
    if key in address_cache:
        return address_cache[key]

    address = fetch_korean_address(lat, lon)

    trim_cache(address_cache)
    address_cache[key] = address

    return address


def fetch_korean_address(lat: float, lon: float) -> str | None:
    """캐시를 거치지 않고 실제로 Nominatim 을 부른다.

    주소를 못 구했다고 날씨 전체가 실패하면 안 되므로,
    예외를 여기서 잡아서 None 으로 돌려준다.
    """
    try:
        response = httpx.get(
            NOMINATIM_URL,
            params={
                "lat": lat,
                "lon": lon,
                "format": "jsonv2",
                "accept-language": "ko",
                "zoom": 18,  # 18 이면 동 단위까지 나온다
            },
            headers={"User-Agent": NOMINATIM_USER_AGENT},
            timeout=NOMINATIM_TIMEOUT_SECONDS,
        )
        response.raise_for_status()
        address = response.json().get("address", {})
    except httpx.HTTPError:
        return None

    # 지역마다 채워주는 키가 달라서 있는 것 중에 앞에 있는 값을 쓴다.
    city = address.get("city") or address.get("county") or address.get("state")
    district = address.get("borough") or address.get("city_district")
    dong = (
        address.get("suburb")
        or address.get("quarter")
        or address.get("neighbourhood")
    )

    # "서울특별시" -> "서울" 처럼 긴 접미사를 뗀다.
    if city:
        for suffix in CITY_SUFFIXES:
            if city.endswith(suffix):
                city = city[: -len(suffix)]
                break

    return join_address(city, district, dong)


def join_address(city, district, dong) -> str | None:
    """시·구·동을 하나로 잇는다. 비어 있거나 겹치는 칸은 뺀다.

    겹침을 걸러야 하는 이유
    -----------------------
    Nominatim 이 채워주는 키가 지역마다 다르다. 서귀포시는 city_district 와
    quarter 에 똑같이 "서홍동" 을 넣어줘서, 그대로 이으면 이렇게 된다.

        서귀포시 서홍동 서홍동

    글자가 완전히 같을 때만 보지 않고 한쪽이 다른 쪽을 품는 경우도 뺀다.
    다만 "품는다"를 글자 단위로 보면 안 된다. 그러면 "부산" 과 "부산진구" 가
    같은 곳으로 묶여서 구 이름이 사라진다. 낱말(띄어쓰기) 단위로 봐야
    아래 둘이 갈린다.

        서귀포시 + "서귀포시 서홍동"  ->  낱말이 겹친다. 하나로 합친다
        부산     + 부산진구           ->  낱말이 다르다. 둘 다 남긴다
    """
    parts = []

    for part in (city, district, dong):
        if part:
            add_part(parts, part)

    if not parts:
        return None

    return " ".join(parts)


def add_part(parts: list[str], part: str) -> None:
    """조각 하나를 목록에 넣는다. 같은 곳이 이미 있으면 더 자세한 쪽만 남긴다.

    낱말 집합이 다른 쪽에 통째로 들어가면 같은 곳으로 본다.

        {서귀포시} ⊂ {서귀포시, 서홍동}   -> 같은 곳. 자세한 쪽을 남긴다
        {부산}     vs {부산진구}          -> 다른 곳. 낱말이 다르므로 둘 다 남긴다

    글자 단위가 아니라 낱말 단위로 보는 이유가 저 두 번째 줄이다.
    글자로 보면 "부산" 이 "부산진구" 를 삼켜서 구 이름이 사라진다.
    """
    words = set(part.split())

    for index, kept in enumerate(parts):
        kept_words = set(kept.split())

        # 이미 담은 쪽이 더 자세하다. 새 조각은 버린다.
        if words <= kept_words:
            return

        # 새 조각이 더 자세하다. 있던 자리에 갈아 끼운다.
        # 뒤에 붙이면 "서귀포시 서귀포시 서홍동" 처럼 순서가 흐트러진다.
        if kept_words <= words:
            parts[index] = part
            return

    parts.append(part)
