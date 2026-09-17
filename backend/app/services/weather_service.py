"""날씨 조회 + 안내 문구 만들기."""

from fastapi import HTTPException, status

from app.core.config import settings
from app.integrations.weather_api import get_weather
from app.schemas.weather import WeatherResponse

# 온도 구간을 나누는 경계값(섭씨)
COLD_TEMP = 5    # 이 아래면 "추운"
COOL_TEMP = 15   # 이 아래면 "쌀쌀한"
WARM_TEMP = 25   # 이 아래면 "선선한", 이상이면 "더운"

# 날씨 코드를 한글 조각으로 바꾸는 표
SKY_TEXTS = {
    "CLEAR": "맑고",
    "CLOUDS": "흐리고",
    "RAIN": "비 오고",
    "SNOW": "눈 오고",
}

# 온도 느낌별로 어울리는 음식 한 줄.
FOOD_HINTS = {
    "추운": "따뜻하고 얼큰한 메뉴가 잘 어울려요",
    "쌀쌀한": "따끈한 국물 요리가 잘 어울려요",
    "선선한": "가볍고 든든한 메뉴가 잘 어울려요",
    "더운": "시원하고 담백한 메뉴가 잘 어울려요",
}

# 오른쪽 추천 칸으로 넘어가는 한 줄.
# 왼쪽이 "오늘은 이런 날"이면 이쪽은 "그래서 이걸 드세요" 다.
PICK_LEADS = {
    "추운": "추운 날엔 뜨끈한 한 그릇 어떠세요?",
    "쌀쌀한": "쌀쌀한 날엔 국물 있는 메뉴 어떠세요?",
    "선선한": "선선한 날엔 든든한 한 끼 어떠세요?",
    "더운": "더운 날씨엔 시원한 한 그릇 어떠세요?",
}

# 비 · 눈이 오면 기온 문구 대신 이쪽을 쓴다.
SKY_FOOD_HINTS = {
    "RAIN": "따끈한 국물이나 바삭한 튀김이 당기는 날이에요",
    "SNOW": "뜨끈한 국물 요리로 몸을 녹여보세요",
}

SKY_PICK_LEADS = {
    "RAIN": "비 오는 날엔 뜨끈한 국물 어떠세요?",
    "SNOW": "눈 오는 날엔 뜨끈한 한 그릇 어떠세요?",
}


def get_temp_feel(temp: int) -> str:
    """기온을 "추운 / 쌀쌀한 / 선선한 / 더운" 넷 중 하나로 바꾼다."""
    if temp < COLD_TEMP:
        return "추운"
    if temp < COOL_TEMP:
        return "쌀쌀한"
    if temp < WARM_TEMP:
        return "선선한"
    return "더운"


def make_pick_lead(condition: str, temp: int) -> str:
    """오른쪽 추천 칸의 머리말을 만든다."""
    if condition in SKY_PICK_LEADS:
        return SKY_PICK_LEADS[condition]

    return PICK_LEADS[get_temp_feel(temp)]


def make_comment(condition: str, temp: int) -> str:
    """화면 왼쪽에 넣을 두 줄짜리 문구를 만든다."""
    feel = get_temp_feel(temp)
    sky = SKY_TEXTS.get(condition, "")

    if sky:
        first_line = f"오늘은 {sky} {feel} 날씨에요!"
    else:
        # 표에 없는 날씨 코드면 하늘 상태는 빼고 온도만 말한다
        first_line = f"오늘은 {feel} 날씨에요!"

    second_line = SKY_FOOD_HINTS.get(condition) or FOOD_HINTS[feel]

    return f"{first_line}\n{second_line}"


def get_current_weather(lat: float | None, lon: float | None) -> WeatherResponse:
    """좌표로 현재 날씨를 조회한다."""
    if lat is None:
        lat = settings.DEFAULT_WEATHER_LAT
    if lon is None:
        lon = settings.DEFAULT_WEATHER_LON

    # 바깥 API 가 죽었거나 키가 잘못된 경우.
    try:
        weather = get_weather(lat, lon)
    except RuntimeError as exc:
        raise HTTPException(status.HTTP_503_SERVICE_UNAVAILABLE, str(exc))

    return WeatherResponse(
        city=weather["city"],
        temp=weather["temp"],
        feels_like=weather["feels_like"],
        humidity=weather["humidity"],
        condition=weather["condition"],
        condition_text=weather["condition_text"],
        comment=make_comment(weather["condition"], weather["temp"]),
        pick_lead=make_pick_lead(weather["condition"], weather["temp"]),
    )
