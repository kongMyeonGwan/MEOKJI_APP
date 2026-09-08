"""날씨 라우터 (STEP 12).

로그인 여부와 무관하게 누구나 조회할 수 있다.

대시보드는 이 API 를 직접 부르지 않는다
----------------------------------------
히어로 카드(WeatherPick)는 POST /api/recommendations 에 use_weather 를 켜서
날씨와 추천 메뉴를 **한 응답으로** 받는다. 따로 부르면 화면이 두 번 나뉘어
뜨고, 서버와 화면이 서로 다른 날씨를 보게 될 수도 있다.

이 엔드포인트는 "날씨만" 필요할 때를 위해 남겨둔다.
서비스가 지금 어떤 날씨를 보고 있는지 확인하는 용도로도 쓴다.
"""

from fastapi import APIRouter, Query

from app.schemas.weather import WeatherResponse
from app.services.weather_service import get_current_weather

router = APIRouter(prefix="/api/weather", tags=["weather"])


@router.get("", response_model=WeatherResponse, summary="현재 날씨")
def read_weather(
    lat: float | None = Query(None, ge=-90, le=90),
    lon: float | None = Query(None, ge=-180, le=180),
) -> WeatherResponse:
    """좌표를 안 주면 종로2가 날씨로 대답한다."""
    return get_current_weather(lat, lon)
