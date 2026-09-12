"""공지사항 스키마.

목록과 상세를 나누지 않았다. 공지는 본문이 짧고 개수도 적어서, 화면이
목록에서 그 자리에 펼친다(NoticeList 는 아코디언이다). 펼칠 때마다 상세를
다시 부르면 왕복만 늘어난다.

쓰기 요청은 만들기(Create)와 고치기(Update)가 같은 모양이다. 그래도 한
클래스로 합치지 않고 둘로 둔 이유는 게시판(post.py)과 같다 — 지금은 같아도
한쪽만 바뀌는 일이 생기고, 그때 합쳐둔 것을 다시 가르는 편이 더 번거롭다.
"""

from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field, field_validator

from app.schemas.page import Page

# 공백만 든 제목·본문을 422 로 끊는 검증기. 게시판 스키마에 있는 것을 그대로 쓴다.
# Oracle 은 빈 문자열을 NULL 로 보기 때문에, 걸러내지 않으면 NOT NULL 위반으로
# 500 이 난다. 같은 이유로 막는 일이라 규칙도 같아야 한다.
from app.schemas.post import require_text

class NoticeResponse(BaseModel):
    """공지 한 건."""

    model_config = ConfigDict(from_attributes=True)

    id: int
    title: str
    content: str

    view_count: int

    # 상단 고정 여부. DB 의 pinned_yn('Y'/'N')을 화면이 쓰기 쉬운 bool 로 바꿔서 준다.
    # 화면이 'Y' 문자열을 비교하게 두면 조건문마다 오타가 날 자리가 생긴다.
    pinned: bool

    created_at: datetime


class NoticePage(Page):
    """공지 한 페이지.

    공지는 지금 몇 건뿐이라 페이징이 없어도 돌아간다. 그래도 붙여둔 이유는
    공지가 사용자 수가 아니라 **운영 기간에 비례해 늘어나는** 값이기 때문이다.
    점검 · 이벤트 안내가 한 달에 몇 건씩 쌓이면 몇 년 뒤에는 수백 건이 된다.
    그때 본문(CLOB)까지 통째로 내려주게 된다 — 목록에서 그 자리에 펼치느라
    본문을 같이 싣고 있어서 게시판보다 더 무겁다.

    나머지 네 칸은 schemas/page.py 참고.
    """

    items: list[NoticeResponse]


class NoticeViewResponse(BaseModel):
    """조회수를 올린 결과. 화면이 직접 세지 않게 서버가 계산해 준다."""

    view_count: int


class NoticeCreateRequest(BaseModel):
    """공지 작성 요청. 관리자만 보낼 수 있다.

    view_count 를 받지 않는다. 조회수는 사람들이 읽어서 쌓이는 값이지
    작성자가 정하는 값이 아니다. 받아주면 아무 숫자나 써넣을 수 있다.

    작성자(user_id)도 받지 않는다. 토큰에서 나온 사람으로 서버가 넣는다.
    본문으로 받으면 남의 이름으로 공지를 쓸 수 있게 된다.
    """

    # DDL 이 VARCHAR2(200 CHAR) 라 그 이상은 받지 않는다.
    title: str = Field(..., min_length=1, max_length=200)
    content: str = Field(..., min_length=1)

    # 상단 고정. DB 는 'Y'/'N' 이지만 바깥에서는 bool 로 주고받는다.
    # 화면이 'Y' 문자열을 비교하게 두면 조건문마다 오타가 날 자리가 생긴다.
    pinned: bool = False

    @field_validator("title")
    @classmethod
    def validate_title(cls, title: str) -> str:
        return require_text(title, "제목")

    @field_validator("content")
    @classmethod
    def validate_content(cls, content: str) -> str:
        return require_text(content, "내용")


class NoticeUpdateRequest(NoticeCreateRequest):
    """공지 수정 요청.

    지금은 작성과 받는 항목이 같아서 그대로 상속했다. 상속으로 둔 덕에
    검증기 네 개를 다시 적지 않는다.

    고칠 때도 전체를 다 보낸다(PATCH 가 아니라 PUT). 공지는 항목이 넷뿐이고
    화면이 폼에 기존 값을 채워놓고 고치게 하므로, 어차피 넷을 다 들고 있다.
    """

