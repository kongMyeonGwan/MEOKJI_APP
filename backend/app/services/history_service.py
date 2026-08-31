"""내 식사 기록 조회 로직 (STEP 16).

"남의 기록은 보이지 않는다"는 규칙을 라우터가 아니라 여기서 지킨다.
"""

from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.meal_history import MealHistory
from app.models.post import Post
from app.models.user import User
from app.schemas.history import MealHistoryResponse


def list_histories(db: Session, user: User) -> list[MealHistoryResponse]:
    """내 기록 전체를 최신순으로 돌려준다.

    posts 를 OUTER JOIN 해서 "이미 공유한 기록인지"를 함께 계산한다.
    uq_posts_meal(UNIQUE) 덕분에 한 기록에 붙는 게시글은 최대 하나라
    JOIN 때문에 행이 늘어날 걱정이 없다.

    idx_meal_user_selected(user_id, selected_at DESC)를 그대로 타는 정렬이다.
    개수가 많아야 수백 건이라 아직 페이징하지 않는다.
    """
    rows = db.execute(
        select(MealHistory, Post.id.label("shared_post_id"))
        .outerjoin(Post, Post.meal_history_id == MealHistory.id)
        .where(MealHistory.user_id == user.id)
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
