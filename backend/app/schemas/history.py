"""내 식사 기록 응답 스키마 (STEP 16)."""

from datetime import datetime

from pydantic import BaseModel, ConfigDict


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
    # posts.meal_history_id 에 uq_posts_meal(UNIQUE)이 걸려 있어 한 기록은
    # 한 번만 공유할 수 있다. 화면은 이 값으로 "공유 완료" 배지를 띄우고,
    # 글쓰기 화면은 이 값이 있는 기록을 선택지에서 뺀다.
    shared_post_id: int | None = None

    # user_id는 내려주지 않는다. "내 기록"만 조회되므로 불필요하다.
