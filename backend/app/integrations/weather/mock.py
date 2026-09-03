"""개발용 가짜 날씨 Provider.

API 키 없이도, 또는 OpenWeatherMap 호출이 실패했을 때도 화면이 안 깨지게 고정값을 준다.
"""

from app.integrations.weather.base import WeatherReport


class MockWeatherProvider:
    def fetch_current(self, lat: float, lon: float) -> WeatherReport:
        return WeatherReport(
            city="서울특별시",
            temp=28,
            feels_like=30,
            humidity=52,
            condition="CLEAR",
            condition_text="맑음",
        )
