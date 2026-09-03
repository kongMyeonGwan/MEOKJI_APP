"""내 식사 기록 조회 로직 (STEP 16).

"남의 기록은 보이지 않는다"는 규칙을 라우터가 아니라 여기서 지킨다.
"""

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.meal_history import MealHistory
from app.models.recipe_review import RecipeReview
from app.models.types import USE_YN_YES
from app.models.user import User
from app.schemas.history import MealHistoryResponse


def list_histories(db: Session, user: User) -> list[MealHistoryResponse]:
    """내 기록 전체를 최신순으로 돌려준다.

    recipe_reviews 를 OUTER JOIN 해서 "이미 후기를 쓴 기록인지"를 함께 계산한다.
    uq_reviews_meal 인덱스가 use_yn='Y' 인 행만 유일하게 잡으므로,
    JOIN 조건에도 같은 use_yn 조건을 넣어야 행이 늘어나지 않는다.
    (지웠다 다시 쓴 기록에는 'N' 후기가 여러 개 쌓여 있을 수 있다.)

    후기를 지우면 shared_post_id 가 다시 None 이 되어 같은 기록으로 또 쓸 수 있다.

    idx_meal_user_selected(user_id, use_yn, selected_at DESC)를 그대로 타는 정렬이다.
    개수가 많아야 수백 건이라 아직 페이징하지 않는다.
    """
    rows = db.execute(
        select(MealHistory, RecipeReview.id.label("shared_post_id"))
        .outerjoin(
            RecipeReview,
            (RecipeReview.meal_history_id == MealHistory.id)
            & (RecipeReview.use_yn == USE_YN_YES),
        )
        .where(MealHistory.user_id == user.id, MealHistory.use_yn == USE_YN_YES)
        .order_by(MealHistory.selected_at.desc(), MealHistory.id.desc())
    ).all()

    items: list[MealHistoryResponse] = []
    for history, shared_post_id in rows:
        # model_validate() 에는 update= 인자가 없다(model_copy 쪽에만 있다).
        # 계산 값은 검증을 마친 뒤 붙인다.
        item = MealHistoryResponse.model_validate(history)
        item.shared_post_id = shared_post_id
        items.append(item)

    return items
