"""날씨 조회."""

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
CONDITION_CODES = {
    "Clear": "CLEAR",
    "Clouds": "CLOUDS",
    "Rain": "RAIN",
    "Drizzle": "RAIN",
    "Thunderstorm": "RAIN",
    "Snow": "SNOW",
}

# 외부 API 하나를 기다리는 최대 시간(초).
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


# 캐시

# {(반올림한 위도, 경도): (저장한 시각, 날씨 dict)}
weather_cache: dict[tuple, tuple[float, dict]] = {}

# {(반올림한 위도, 경도): 주소 문자열 또는 None}
address_cache: dict[tuple, str | None] = {}


def cache_key(lat: float, lon: float) -> tuple:
    """좌표를 캐시 열쇠로 바꾼다. 1km 안쪽이면 같은 열쇠가 된다."""
    return (round(lat, CACHE_COORD_DIGITS), round(lon, CACHE_COORD_DIGITS))


def trim_cache(cache: dict) -> None:
    """캐시가 너무 커지면 비운다."""
    if len(cache) > CACHE_MAX_ENTRIES:
        cache.clear()


def get_weather(lat: float, lon: float) -> dict:
    """좌표로 현재 날씨를 가져온다."""
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
    """캐시를 거치지 않고 실제로 OpenWeatherMap 을 부른다."""
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
    """좌표를 "시 구 동" 한글 주소로 바꾼다."""
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
    """캐시를 거치지 않고 실제로 Nominatim 을 부른다."""
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
    """시·구·동을 하나로 잇는다."""
    parts = []

    for part in (city, district, dong):
        if part:
            add_part(parts, part)

    if not parts:
        return None

    return " ".join(parts)


def add_part(parts: list[str], part: str) -> None:
    """조각 하나를 목록에 넣는다."""
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
