"""OpenWeatherMap Current Weather Data 연동.

https://openweathermap.org/current
"""

import httpx

from app.integrations.weather.base import WeatherReport

_ENDPOINT = "https://api.openweathermap.org/data/2.5/weather"

# OpenWeatherMap의 지명은 로마자 표기("Kwanghŭi-dong")뿐이고 구·동 단위도 안 준다.
# Nominatim(OpenStreetMap)은 API 키 없이 "시/구/동" 단위 한글 행정구역명을 준다.
_GEO_ENDPOINT = "https://nominatim.openstreetmap.org/reverse"
_GEO_USER_AGENT = "meokji-app-weather/1.0"

# 리버스 지오코딩 결과의 "시/도" 이름은 법정 명칭이라 길다.
# 카드 폭이 좁아 시/도까지 다 붙이면 구·동이 밀려나므로 흔한 접미사를 뗀다.
_CITY_SUFFIXES = ("특별자치시", "특별자치도", "특별시", "광역시")

# OpenWeatherMap의 weather[0].main 값 -> 화면 배경 분기 코드.
# 목록에 없는 값(Mist, Haze, Fog, Dust ...)은 전부 CLOUDS로 묶는다.
# 배경 이미지가 4종류뿐이라 더 세분해봐야 화면에서 구분이 안 된다.
_CONDITION_MAP = {
    "Clear": "CLEAR",
    "Clouds": "CLOUDS",
    "Rain": "RAIN",
    "Drizzle": "RAIN",
    "Thunderstorm": "RAIN",
    "Snow": "SNOW",
}


class OpenWeatherProvider:
    def __init__(self, api_key: str) -> None:
        self._api_key = api_key

    def fetch_current(self, lat: float, lon: float) -> WeatherReport:
        response = httpx.get(
            _ENDPOINT,
            params={
                "lat": lat,
                "lon": lon,
                "appid": self._api_key,
                "units": "metric",  # 섭씨로 받는다
                "lang": "kr",  # condition_text를 한글로 받는다
            },
            timeout=10.0,
        )
        response.raise_for_status()
        data = response.json()

        condition_main = data["weather"][0]["main"]

        return WeatherReport(
            # 바다·격오지 좌표는 name이 빈 문자열로 올 수 있다.
            city=self._fetch_dong_address(lat, lon) or data.get("name") or "알 수 없음",
            temp=round(data["main"]["temp"]),
            feels_like=round(data["main"]["feels_like"]),
            humidity=data["main"]["humidity"],
            condition=_CONDITION_MAP.get(condition_main, "CLOUDS"),
            condition_text=data["weather"][0]["description"],
        )

    def _fetch_dong_address(self, lat: float, lon: float) -> str | None:
        """"시 구 동"까지 나오는 한글 주소를 받는다.

        OpenWeatherMap은 "Kwanghŭi-dong" 같은 로마자 지명만 주고 구·동 단위도 없다.
        Nominatim의 reverse 결과는 city/borough/suburb(또는 quarter)로 시/구/동에
        대응돼서, 별도 API 키 없이 이 조합으로 만들 수 있다.
        """
        try:
            response = httpx.get(
                _GEO_ENDPOINT,
                params={"lat": lat, "lon": lon, "format": "jsonv2", "accept-language": "ko", "zoom": 18},
                headers={"User-Agent": _GEO_USER_AGENT},
                timeout=10.0,
            )
            response.raise_for_status()
            address = response.json().get("address", {})
        except httpx.HTTPError:
            return None

        city = address.get("city") or address.get("county") or address.get("state")
        for suffix in _CITY_SUFFIXES:
            if city and city.endswith(suffix):
                city = city[: -len(suffix)]
                break

        district = address.get("borough") or address.get("city_district")
        dong = address.get("suburb") or address.get("quarter") or address.get("neighbourhood")

        parts = [p for p in (city, district, dong) if p]
        return " ".join(parts) or None
