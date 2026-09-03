"""날씨 Provider 선택.

.env의 WEATHER_PROVIDER 값에 따라 구현체를 고른다.
"""

from functools import lru_cache

from app.core.config import settings
from app.integrations.weather.base import WeatherProvider, WeatherReport
from app.integrations.weather.mock import MockWeatherProvider
from app.integrations.weather.openweather import OpenWeatherProvider

__all__ = ["WeatherProvider", "WeatherReport", "get_weather_provider"]


@lru_cache
def get_weather_provider() -> WeatherProvider:
    """설정에 맞는 Provider를 돌려준다 (한 번만 만들어 재사용)."""
    provider = settings.WEATHER_PROVIDER.lower()

    if provider == "mock":
        return MockWeatherProvider()

    if provider == "openweather":
        if not settings.OPENWEATHER_API_KEY:
            raise RuntimeError(
                "WEATHER_PROVIDER=openweather 인데 OPENWEATHER_API_KEY가 비어 있습니다."
            )
        return OpenWeatherProvider(api_key=settings.OPENWEATHER_API_KEY)

    # 조용히 mock으로 떨어뜨리면 운영에서 날씨가 고정값으로 나가는데
    # 아무도 원인을 모르는 상황이 되므로 에러를 낸다.
    raise RuntimeError(
        f"지원하지 않는 WEATHER_PROVIDER: {settings.WEATHER_PROVIDER!r}. "
        "사용 가능한 값: mock, openweather"
    )
