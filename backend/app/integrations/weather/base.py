"""날씨 조회 인터페이스.

서비스 계층은 이 인터페이스만 알고, 실제로 Mock인지 OpenWeatherMap인지는 모른다.
"""

from dataclasses import dataclass
from typing import Protocol


@dataclass(frozen=True)
class WeatherReport:
    """좌표 하나에 대한 실시간 날씨."""

    city: str
    temp: int
    feels_like: int
    humidity: int

    condition: str
    """CLEAR / CLOUDS / RAIN / SNOW 중 하나. 프론트 배경 분기에 쓰는 코드다."""

    condition_text: str
    """사람이 읽는 날씨 문구. 표시용으로만 쓰고 분기 조건으로 삼지 않는다."""


class WeatherProvider(Protocol):
    """날씨 조회자가 갖춰야 할 모양."""

    def fetch_current(self, lat: float, lon: float) -> WeatherReport:
        """좌표로 현재 날씨를 조회한다."""
        ...
