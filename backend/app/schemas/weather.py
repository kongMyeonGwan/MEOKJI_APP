"""날씨 응답 스키마 (STEP 12)."""

from pydantic import BaseModel


class WeatherResponse(BaseModel):
    city: str
    temp: int
    feels_like: int
    humidity: int
    condition: str
    condition_text: str

    comment: str
    """날씨를 한 문장으로 풀어주는 문구. 화면 왼쪽 칸(wpick__comment)에 그대로 쓴다."""

    pick_lead: str
    """추천 메뉴로 넘어가는 문구. 화면 오른쪽 칸(wpick__lead)에 그대로 쓴다.

    comment 와 나눠 둔 이유는 히어로가 왼쪽(근거) -> 오른쪽(답)으로 읽히기
    때문이다. 왼쪽은 "오늘은 이런 날", 오른쪽은 "그래서 이걸 드세요" 다.
    한 문장으로 합치면 둘 중 한쪽이 어색해진다.
    """
