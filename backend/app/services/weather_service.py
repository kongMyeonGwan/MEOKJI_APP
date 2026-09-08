"""날씨 조회 + 안내 문구 만들기.

날씨 API 는 온도와 상태 코드만 준다.
"오늘은 맑고 더운 날씨에요" 같은 문구는 우리가 직접 만든다.
규칙이 단순해서 AI 를 부를 일이 아니다.
"""

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
# 오른쪽 카드에 구체적인 메뉴(예: 콩국수)가 나오기 전에,
# 왼쪽 날씨 문구에서 먼저 "이런 스타일이 어울려요"까지 말해준다.
# 이게 없으면 왼쪽(날씨)과 오른쪽(메뉴)이 서로 상관없어 보인다.
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


def get_temp_feel(temp: int) -> str:
    """기온을 "추운 / 쌀쌀한 / 선선한 / 더운" 넷 중 하나로 바꾼다."""
    if temp < COLD_TEMP:
        return "추운"
    if temp < COOL_TEMP:
        return "쌀쌀한"
    if temp < WARM_TEMP:
        return "선선한"
    return "더운"


def make_pick_lead(temp: int) -> str:
    """오른쪽 추천 칸의 머리말을 만든다."""
    return PICK_LEADS[get_temp_feel(temp)]


def make_comment(condition: str, temp: int) -> str:
    """화면 왼쪽에 넣을 두 줄짜리 문구를 만든다.

    예)  오늘은 맑고 더운 날씨에요!
         시원하고 담백한 메뉴가 잘 어울려요
    """
    feel = get_temp_feel(temp)
    sky = SKY_TEXTS.get(condition, "")

    if sky:
        first_line = f"오늘은 {sky} {feel} 날씨에요!"
    else:
        # 표에 없는 날씨 코드면 하늘 상태는 빼고 온도만 말한다
        first_line = f"오늘은 {feel} 날씨에요!"

    second_line = FOOD_HINTS[feel]

    return f"{first_line}\n{second_line}"


def get_current_weather(lat: float | None, lon: float | None) -> WeatherResponse:
    """좌표로 현재 날씨를 조회한다.

    좌표가 없으면 .env 에 적어둔 기본 좌표(종로2가)로 조회한다.
    프론트가 위치 권한을 못 받았거나 브라우저가 위치를 지원하지 않으면
    좌표 없이 요청이 들어온다.
    """
    if lat is None:
        lat = settings.DEFAULT_WEATHER_LAT
    if lon is None:
        lon = settings.DEFAULT_WEATHER_LON

    # 바깥 API 가 죽었거나 키가 잘못된 경우.
    # 500 으로 새어나가면 화면에 원인이 안 보이므로 503 으로 바꿔서 내보낸다.
    # 가짜 날씨로 슬쩍 대체하지 않는다. 28도 맑음이 계속 뜨는데 왜 그런지
    # 아무도 모르는 상태가 제일 나쁘다.
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
        pick_lead=make_pick_lead(weather["temp"]),
    )
