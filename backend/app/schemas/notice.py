"""공지사항 응답 스키마.

목록과 상세를 나누지 않았다. 공지는 본문이 짧고 개수도 적어서, 화면이
목록에서 그 자리에 펼친다(NoticeList 는 아코디언이다). 펼칠 때마다 상세를
다시 부르면 왕복만 늘어난다.
"""

from datetime import datetime

from pydantic import BaseModel, ConfigDict


class NoticeResponse(BaseModel):
    """공지 한 건."""

    model_config = ConfigDict(from_attributes=True)

    id: int
    title: str
    content: str

    # 목록의 분류 배지. '점검' · '안내' · '업데이트' 등
    category: str

    view_count: int

    # 상단 고정 여부. DB 의 pinned_yn('Y'/'N')을 화면이 쓰기 쉬운 bool 로 바꿔서 준다.
    # 화면이 'Y' 문자열을 비교하게 두면 조건문마다 오타가 날 자리가 생긴다.
    pinned: bool

    created_at: datetime


class NoticeViewResponse(BaseModel):
    """조회수를 올린 결과. 화면이 직접 세지 않게 서버가 계산해 준다."""

    view_count: int
