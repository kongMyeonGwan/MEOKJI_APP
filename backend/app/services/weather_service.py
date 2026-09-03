"""날씨 조회 + 문구 생성.

Provider는 온도·상태 코드만 돌려준다. "오늘은 맑고 더운 날씨에요" 같은 문구는
날씨 API가 주지 않으므로 여기서 규칙 기반으로 만든다. AI가 필요한 일이 아니다.
"""

from app.core.config import settings
from app.integrations.weather import get_weather_provider
from app.schemas.weather import WeatherResponse

# 온도 구간 경계값(°C).
_COLD, _COOL, _WARM = 5, 15, 25

_SKY_TEXT = {
    "CLEAR": "맑고",
    "CLOUDS": "흐리고",
    "RAIN": "비 오고",
    "SNOW": "눈 오고",
}

# 온도 구간별로 어떤 음식이 어울리는지 한 줄. 오른쪽 카드의 구체적인 메뉴(예: 콩국수)로
# 넘어가기 전에, 왼쪽 날씨 문구 안에서 먼저 "이런 스타일이 어울려요"까지 말해 둔다.
# 이걸 빼면 왼쪽(날씨)과 오른쪽(메뉴)이 서로 상관없어 보인다.
_FOOD_HINT = {
    "추운": "따뜻하고 얼큰한 메뉴가 잘 어울려요",
    "쌀쌀한": "따끈한 국물 요리가 잘 어울려요",
    "선선한": "가볍고 든든한 메뉴가 잘 어울려요",
    "더운": "시원하고 담백한 메뉴가 잘 어울려요",
}


def _describe(condition: str, temp: int) -> str:
    if temp < _COLD:
        feel = "추운"
    elif temp < _COOL:
        feel = "쌀쌀한"
    elif temp < _WARM:
        feel = "선선한"
    else:
        feel = "더운"

    sky = _SKY_TEXT.get(condition, "")
    headline = f"오늘은 {sky} {feel} 날씨에요!" if sky else f"오늘은 {feel} 날씨에요!"
    return f"{headline}\n{_FOOD_HINT[feel]}"


def get_current_weather(lat: float | None, lon: float | None) -> WeatherResponse:
    """좌표로 현재 날씨를 조회한다.

    좌표가 없으면 종로2가(기본값)로 조회한다. 프론트가 위치 권한을 못 받았거나
    브라우저가 Geolocation을 지원하지 않을 때 이 경로를 탄다.
    """
    target_lat = lat if lat is not None else settings.DEFAULT_WEATHER_LAT
    target_lon = lon if lon is not None else settings.DEFAULT_WEATHER_LON

    report = get_weather_provider().fetch_current(target_lat, target_lon)

    return WeatherResponse(
        city=report.city,
        temp=report.temp,
        feels_like=report.feels_like,
        humidity=report.humidity,
        condition=report.condition,
        condition_text=report.condition_text,
        comment=_describe(report.condition, report.temp),
    )
