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

# 공백만 든 제목·본문을 422 로 끊는 검증기. 게시판 스키마에 있는 것을 그대로 쓴다.
# Oracle 은 빈 문자열을 NULL 로 보기 때문에, 걸러내지 않으면 NOT NULL 위반으로
# 500 이 난다. 같은 이유로 막는 일이라 규칙도 같아야 한다.
from app.schemas.post import require_text

# 카테고리를 안 적었을 때. DDL 의 DEFAULT '안내' 와 같은 값이어야 한다.
DEFAULT_CATEGORY = "안내"

# 화면(NoticeList)이 배지로 그리는 분류들. 폼의 선택지로 내려준다.
#
# Literal 로 묶어서 이 넷만 받게 하지 않는다. DDL 에 CHECK 를 걸지 않은 것과
# 같은 이유다 — 분류는 운영하면서 늘어나는 값이라, 늘 때마다 코드를 고쳐야
# 하는 제약은 득보다 실이 크다. 화면도 '점검' 만 경고색이고 나머지는 기본색이라
# 모르는 값이 와도 깨지지 않는다.
KNOWN_CATEGORIES = ["안내", "점검", "업데이트", "이벤트"]


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

    # 안 보내면 '안내'. 대부분의 공지가 '안내' 라서 매번 적게 하지 않는다.
    category: str = Field(DEFAULT_CATEGORY, min_length=1, max_length=20)

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

    @field_validator("category")
    @classmethod
    def validate_category(cls, category: str) -> str:
        return require_text(category, "분류")


class NoticeUpdateRequest(NoticeCreateRequest):
    """공지 수정 요청.

    지금은 작성과 받는 항목이 같아서 그대로 상속했다. 상속으로 둔 덕에
    검증기 네 개를 다시 적지 않는다.

    고칠 때도 전체를 다 보낸다(PATCH 가 아니라 PUT). 공지는 항목이 넷뿐이고
    화면이 폼에 기존 값을 채워놓고 고치게 하므로, 어차피 넷을 다 들고 있다.
    """

