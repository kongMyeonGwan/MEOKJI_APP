"""게시판 응답 스키마 (STEP 20~23).

목록과 상세를 나눈 이유
-----------------------
목록에는 CLOB(recipe_json / ingredients_json)을 싣지 않는다.
한 페이지에 20건이면 CLOB를 20번 읽어와 놓고 화면에는 제목만 쓰게 된다.
레시피는 상세에서만 필요하다.
"""

from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field, field_validator


def require_text(value: str, label: str) -> str:
    """공백만 들어온 입력을 거르고, 앞뒤 공백을 떼어낸다.

    "   " 처럼 스페이스만 넣은 값은 Field(min_length=1) 을 통과해버린다.
    그런데 Oracle 은 빈 문자열을 NULL 로 취급해서, 그대로 저장하려 하면
    NOT NULL 위반으로 500 이 난다. 그 전에 여기서 422 로 끊는다.
    """
    value = value.strip()

    if not value:
        raise ValueError(f"{label}을(를) 입력해주세요")

    return value


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
    def validate_title(cls, title: str) -> str:
        return require_text(title, "제목")

    @field_validator("content")
    @classmethod
    def validate_content(cls, content: str) -> str:
        return require_text(content, "내용")


class PostUpdateRequest(BaseModel):
    """글 수정 요청.

    meal_history_id 는 받지 않는다. 어떤 식사에 대한 글인지가 바뀌면
    본문과 레시피가 어긋나는데, 그건 수정이 아니라 다른 글이다.
    바꾸고 싶으면 지우고 새로 쓰면 된다.
    """

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
    """첨부파일 한 건. 파일 본체가 아니라 '어디서 받는지'를 알려준다.

    서버에 저장한 이름(file_name)과 폴더(file_path)는 내려주지 않는다.
    화면이 쓸 일이 없고, 디스크 구조를 밖에 알릴 이유도 없다.
    """

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
    """인기 레시피 한 줄 (기간 안에 새로 받은 좋아요 기준).

    화면의 LikedList · PopularRecipes 가 이 모양을 그대로 쓴다.
    id 는 게시글 번호다. 클릭하면 /recipe/{food_name} 으로 가고,
    좋아요 수는 "선택한 기간에 눌린 것"만 센 값이다.
    """

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
