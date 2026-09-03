"""날씨 라우터 (STEP 12).

로그인 여부와 무관하게 누구나 조회할 수 있다. Guest도 대시보드에서 날씨를 본다.
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
