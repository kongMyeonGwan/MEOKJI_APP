"""공지사항 스키마."""

from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field, field_validator

from app.schemas.page import Page

# 공백만 든 제목·본문을 422 로 끊는 검증기. 게시판 스키마에 있는 것을 그대로 쓴다.
from app.schemas.post import require_text

# 본문 상한. DDL 이 VARCHAR2(4000) 이고 4000 은 바이트 기준이다.
# 글자 수가 아니라 바이트로 재야 하는 이유는 validate_content 참고.
NOTICE_CONTENT_MAX_BYTES = 4000


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
    """공지 한 페이지."""

    items: list[NoticeResponse]


class NoticeViewResponse(BaseModel):
    """조회수를 올린 결과. 화면이 직접 세지 않게 서버가 계산해 준다."""

    view_count: int


class NoticeCreateRequest(BaseModel):
    """공지 작성 요청."""

    # DDL 이 VARCHAR2(200 CHAR) 라 그 이상은 받지 않는다.
    title: str = Field(..., min_length=1, max_length=200)

    # max_length 는 바이트가 아니라 글자 수라 이것만으로는 모자란다.
    content: str = Field(..., min_length=1, max_length=NOTICE_CONTENT_MAX_BYTES)

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
        content = require_text(content, "내용")

        # 글자 수가 아니라 바이트로 잰다. DDL 의 VARCHAR2(4000) 이 바이트 기준인데
        if len(content.encode("utf-8")) > NOTICE_CONTENT_MAX_BYTES:
            raise ValueError("내용이 너무 깁니다 (한글 기준 약 1300자까지)")

        return content


class NoticeUpdateRequest(NoticeCreateRequest):
    """공지 수정 요청."""

