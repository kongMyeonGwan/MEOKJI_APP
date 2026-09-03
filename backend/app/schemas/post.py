"""게시판 응답 스키마 (STEP 20~23).

목록과 상세를 나눈 이유
-----------------------
목록에는 CLOB(recipe_json / ingredients_json)을 싣지 않는다.
한 페이지에 20건이면 CLOB를 20번 읽어와 놓고 화면에는 제목만 쓰게 된다.
레시피는 상세에서만 필요하다.
"""

from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field, field_validator


def _require_text(value: str, label: str) -> str:
    """공백만 들어온 입력을 거른다.

    Oracle은 빈 문자열을 NULL로 취급한다. ''를 그대로 넘기면
    ORA-01400(NOT NULL 위반)으로 500이 나므로 여기서 422로 끊는다.
    """
    stripped = value.strip()
    if not stripped:
        raise ValueError(f"{label}을(를) 입력해주세요")
    return stripped


class PostCreateRequest(BaseModel):
    """글쓰기 요청.

    food_name / recipe_json / ingredients_json 은 받지 않는다.
    클라이언트가 보낸 값을 믿는 대신, 서버가 meal_history_id 로 원본을 찾아
    직접 복사한다. 그래야 "게시글은 확정한 식사의 스냅샷"이 실제로 보장된다.
    """

    meal_history_id: int = Field(..., ge=1)
    title: str = Field(..., min_length=1, max_length=200)
    content: str = Field(..., min_length=1)

    @field_validator("title")
    @classmethod
    def strip_title(cls, v: str) -> str:
        return _require_text(v, "제목")

    @field_validator("content")
    @classmethod
    def strip_content(cls, v: str) -> str:
        return _require_text(v, "내용")


class CommentCreateRequest(BaseModel):
    # DDL이 VARCHAR2(1000 CHAR)라 그 이상은 받지 않는다.
    content: str = Field(..., min_length=1, max_length=1000)

    @field_validator("content")
    @classmethod
    def strip_content(cls, v: str) -> str:
        return _require_text(v, "댓글")


class LikeResponse(BaseModel):
    """좋아요 토글 결과. 화면이 카운트를 직접 세지 않게 서버가 계산해 준다."""

    liked: bool
    like_count: int


class CommentResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    nickname: str
    content: str
    created_at: datetime

    # 내가 쓴 댓글인지. 화면의 수정/삭제 버튼 노출 판단에 쓴다.
    # Guest 조회일 때는 항상 False.
    is_mine: bool = False


class PostListItem(BaseModel):
    """목록 한 줄. 본문은 미리보기용으로 잘라서 내려준다."""

    model_config = ConfigDict(from_attributes=True)

    id: int
    title: str
    content: str
    food_name: str | None
    nickname: str
    view_count: int
    like_count: int
    comment_count: int
    created_at: datetime


class PostDetailResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    title: str
    content: str
    nickname: str
    view_count: int
    like_count: int
    created_at: datetime

    # 확정 당시 MealHistory에서 복사해 온 스냅샷 (참조가 아니다)
    food_name: str | None
    recipe_json: dict | None
    ingredients_json: list | None

    # 출처 추적용. 화면의 "원본 기록 보기" 링크에만 쓴다.
    meal_history_id: int

    # 로그인 사용자 기준 값. Guest면 둘 다 False.
    liked: bool = False
    is_mine: bool = False

    comments: list[CommentResponse] = []
