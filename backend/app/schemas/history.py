"""내 식사 기록 요청/응답 스키마 (STEP 15~16)."""

from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field, field_validator

from app.schemas.page import Page


class MealHistoryCreateRequest(BaseModel):
    """'이걸로 먹을래요'를 눌렀을 때 보내는 값.

    AI 가 만들어준 레시피(GET /api/recipes/{음식이름})를 그대로 담아 보낸다.
    이 시점의 값이 스냅샷으로 저장되고, 이후 AI 가 같은 음식으로 다른 레시피를
    만들어도 저장된 행은 바뀌지 않는다.

    "클라이언트가 보낸 값을 왜 그대로 믿는가" 에 대한 답
    ----------------------------------------------------
    이건 남이 볼 데이터가 아니라 **자기 기록**이다. 사용자가 자기 기록에
    무엇을 적든 다른 사람에게 영향이 없다. 반대로 게시글(PostCreateRequest)은
    남이 보는 값이라 레시피를 받지 않고 서버가 원본 기록에서 복사한다.
    """

    # Oracle 은 빈 문자열을 NULL 로 취급한다.
    # min_length=1 을 안 걸면 ORA-01400(NOT NULL 위반)으로 500 이 난다.
    food_name: str = Field(..., min_length=1, max_length=150)
    recipe_name: str | None = Field(None, max_length=200)

    # DDL 이 CLOB + CHECK(... IS JSON) 이라 모양이 깨지면 DB 가 거부한다.
    # 여기서 dict / list 인지 먼저 확인해 422 로 끊는다.
    recipe_json: dict
    ingredients_json: list
    substitutes_json: list | None = None

    estimated_cost: int | None = Field(None, ge=0, le=100_000_000)

    @field_validator("food_name")
    @classmethod
    def validate_food_name(cls, name: str) -> str:
        """공백만 입력한 이름을 거르고, 앞뒤 공백을 떼어낸다."""
        name = name.strip()

        if not name:
            raise ValueError("음식 이름을 입력해주세요")

        return name


class MealHistoryResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    food_name: str
    recipe_name: str | None
    recipe_json: dict
    ingredients_json: list
    substitutes_json: list | None
    estimated_cost: int | None
    selected_at: datetime

    # DB 컬럼이 아니라 서버가 계산해 붙이는 값이다.
    # uq_reviews_meal(함수 기반 UNIQUE)이 걸려 있어 한 기록은 한 번만 공유할 수
    # 있다. 화면은 이 값으로 "공유 완료" 배지를 띄우고,
    # 글쓰기 화면은 이 값이 있는 기록을 선택지에서 뺀다.
    shared_post_id: int | None = None

    # 화면의 썸네일용. 카탈로그에 있는 음식이면 서버가 채워준다.
    art: str | None = None

    # user_id는 내려주지 않는다. "내 기록"만 조회되므로 불필요하다.


class MealHistoryPage(Page):
    """내 기록 한 페이지.

    나머지 네 칸과 "왜 배열이 아니라 한 겹 감쌌나" 는 schemas/page.py 참고.
    """

    items: list[MealHistoryResponse]


class MealHistoryStats(BaseModel):
    """History 화면 위쪽 요약."""

    count: int
    total_cost: int

    # 오늘까지 며칠 연속으로 기록을 남겼는지 (한국 시각 기준)
    streak_days: int
