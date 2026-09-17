"""게시판 응답 스키마 (STEP 20~23)."""

from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field, field_validator

from app.schemas.page import Page


def require_text(value: str, label: str) -> str:
    """공백만 들어온 입력을 거르고, 앞뒤 공백을 떼어낸다."""
    value = value.strip()

    if not value:
        raise ValueError(f"{label}을(를) 입력해주세요")

    return value


class PostCreateRequest(BaseModel):
    """글쓰기 요청."""

    meal_history_id: int = Field(..., ge=1)
    title: str = Field(..., min_length=1, max_length=200)
    content: str = Field(..., min_length=1)

    @field_validator("title")
    @classmethod
    def validate_title(cls, title: str) -> str:
        return require_text(title, "제목")

    @field_validator("content")
    @classmethod
    def validate_content(cls, content: str) -> str:
        return require_text(content, "내용")


class PostUpdateRequest(BaseModel):
    """글 수정 요청."""

    title: str = Field(..., min_length=1, max_length=200)
    content: str = Field(..., min_length=1)

    @field_validator("title")
    @classmethod
    def validate_title(cls, title: str) -> str:
        return require_text(title, "제목")

    @field_validator("content")
    @classmethod
    def validate_content(cls, content: str) -> str:
        return require_text(content, "내용")


class CommentCreateRequest(BaseModel):
    # DDL이 VARCHAR2(1000 CHAR)라 그 이상은 받지 않는다.
    content: str = Field(..., min_length=1, max_length=1000)

    @field_validator("content")
    @classmethod
    def validate_content(cls, content: str) -> str:
        return require_text(content, "댓글")


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


class PostFileInfo(BaseModel):
    """첨부파일 한 건."""

    # 사용자가 올린 원래 이름. 화면에 이 이름으로 보여준다.
    name: str

    # 점 없이 'jpg'. 아이콘을 고르거나 이미지인지 판단하는 데 쓴다.
    ext: str

    # 바이트. 크기를 못 구한 파일도 정상이라 None 일 수 있다.
    size: int | None = None

    # 사람이 읽는 크기 ('184 KB'). 화면마다 계산하지 않게 서버가 만들어 준다.
    size_text: str = ""

    # 실제로 받아갈 주소. GET /api/posts/{id}/file
    url: str


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

    # 첨부가 있는지만 알려준다. 목록은 클립 아이콘 하나만 붙이면 되는데
    # 500자짜리 경로 컬럼까지 읽어올 이유가 없다 (DDL 의 FILE_YN 주석 참고).
    has_file: bool = False


class PostPage(Page):
    """게시글 목록 한 페이지."""

    items: list[PostListItem]


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

    # 첨부파일. 없으면 None — 후기 하나당 파일 하나라 목록이 아니다.
    file: PostFileInfo | None = None

    # 로그인 사용자 기준 값. Guest면 둘 다 False.
    liked: bool = False
    is_mine: bool = False

    comments: list[CommentResponse] = []


class PopularItem(BaseModel):
    """인기 레시피 한 줄 (기간 안에 새로 받은 좋아요 기준)."""

    id: int
    food_name: str
    recipe_name: str | None
    nickname: str
    like_count: int
    estimated_cost: int | None

    # 그 기간에 마지막으로 좋아요를 받은 시각. 같은 개수일 때 정렬 기준이 된다.
    liked_at: datetime

    # 썸네일 그림. 카탈로그에 있는 음식이면 서버가 채워준다.
    art: str | None = None
    image_url: str | None = None
