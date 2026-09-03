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
