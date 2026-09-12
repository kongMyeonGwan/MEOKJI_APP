"""내 식사 기록 로직 (STEP 15~16).

"남의 기록은 보이지도, 지워지지도 않는다"는 규칙을 라우터가 아니라 여기서 지킨다.

기록은 확정 시점의 스냅샷이다. 한 번 만들면 UPDATE 하지 않는다.
AI 가 나중에 같은 음식으로 다른 레시피를 만들어도 이 행은 변하지 않는다.
"""

from datetime import timedelta

from fastapi import HTTPException, status
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.core.kst import to_kst_date, today_kst
from app.data import find_food
from app.models.meal_history import MealHistory
from app.models.recipe_review import RecipeReview
from app.models.types import USE_YN_NO, USE_YN_YES
from app.models.user import User
from app.schemas.history import (
    MealHistoryCreateRequest,
    MealHistoryPage,
    MealHistoryResponse,
    MealHistoryStats,
)
from app.schemas.page import DEFAULT_PAGE_SIZE, count_pages


def list_histories(
    db: Session,
    user: User,
    page: int = 1,
    size: int = DEFAULT_PAGE_SIZE,
    shared: bool | None = None,
) -> MealHistoryPage:
    """내 기록을 최신순으로 한 페이지씩 돌려준다.

    recipe_reviews 를 OUTER JOIN 해서 "이미 후기를 쓴 기록인지"를 함께 계산한다.
    uq_reviews_meal 인덱스가 use_yn='Y' 인 행만 유일하게 잡으므로,
    JOIN 조건에도 같은 use_yn 조건을 넣어야 행이 늘어나지 않는다.
    (지웠다 다시 쓴 기록에는 'N' 후기가 여러 개 쌓여 있을 수 있다.)

    후기를 지우면 shared_post_id 가 다시 None 이 되어 같은 기록으로 또 쓸 수 있다.

    idx_meal_user_selected(user_id, use_yn, selected_at DESC)를 그대로 타는 정렬이다.
    id 를 같이 넣는 이유는 게시판과 같다 — selected_at 이 같은 기록 둘의 순서를
    DB 가 마음대로 정하면 같은 기록이 두 페이지에 다 나오거나 어디에도 안 나온다.

    shared 로 걸러낸다
    ------------------
    글쓰기 화면은 "아직 공유하지 않은 기록" 만 고르게 한다. 전에는 전체를
    받아다 화면에서 걸렀는데, 페이징이 붙으면 그 방법이 깨진다 — 1페이지 20건이
    전부 공유된 기록이면 고를 것이 하나도 없는 화면이 된다.

        shared=False   아직 공유 안 한 것만
        shared=True    이미 공유한 것만
        None           전부 (기록 화면)
    """
    # COUNT 와 목록이 같은 조건 · 같은 JOIN 을 봐야 한다.
    # 한쪽에만 필터를 붙이면 "3페이지가 있다는데 열면 비어 있는" 일이 생긴다.
    shared_join = (RecipeReview.meal_history_id == MealHistory.id) & (
        RecipeReview.use_yn == USE_YN_YES
    )
    mine = (MealHistory.user_id == user.id, MealHistory.use_yn == USE_YN_YES)

    def with_filter(query):
        if shared is True:
            return query.where(RecipeReview.id.is_not(None))
        if shared is False:
            return query.where(RecipeReview.id.is_(None))
        return query

    total = db.execute(
        with_filter(
            select(func.count(MealHistory.id))
            .outerjoin(RecipeReview, shared_join)
            .where(*mine)
        )
    ).scalar_one()

    rows = db.execute(
        with_filter(
            select(MealHistory, RecipeReview.id.label("shared_post_id"))
            .outerjoin(RecipeReview, shared_join)
            .where(*mine)
            .order_by(MealHistory.selected_at.desc(), MealHistory.id.desc())
        )
        .offset((page - 1) * size)
        .limit(size)
    ).all()

    items = []
    for history, shared_post_id in rows:
        items.append(to_response(history, shared_post_id))

    return MealHistoryPage(
        items=items,
        total=total,
        page=page,
        size=size,
        total_pages=count_pages(total, size),
    )


def to_response(history: MealHistory, shared_post_id: int | None) -> MealHistoryResponse:
    """모델 하나를 응답 스키마로 바꾸고, DB 에 없는 값들을 붙인다."""
    # model_validate() 는 SQLAlchemy 모델을 Pydantic 응답 객체로 바꿔준다.
    # (schemas/history.py 의 from_attributes=True 덕분에 가능하다.)
    item = MealHistoryResponse.model_validate(history)

    # shared_post_id 는 DB 컬럼이 아니라 JOIN 으로 계산한 값이라 따로 붙인다
    item.shared_post_id = shared_post_id

    # 썸네일 그림도 DB 에 없다. 카탈로그에 같은 이름이 있으면 가져다 쓴다.
    known = find_food(history.food_name)
    if known is not None:
        item.art = known.get("art")

    return item


def get_alive_history(db: Session, user: User, history_id: int) -> MealHistory:
    """내 기록 하나를 가져온다. 없거나 남의 것이면 404.

    남의 기록일 때 403 을 주면 "그 번호는 존재한다"는 사실이 새어나간다.
    이미 지운 기록도 같은 이유로 404 로 취급한다.
    """
    history = db.get(MealHistory, history_id)

    if history is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "기록을 찾을 수 없습니다.")
    if history.user_id != user.id:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "기록을 찾을 수 없습니다.")
    if history.use_yn != USE_YN_YES:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "기록을 찾을 수 없습니다.")

    return history


def create_history(
    db: Session, user: User, payload: MealHistoryCreateRequest
) -> MealHistoryResponse:
    """'이걸로 먹을래요'를 눌렀을 때 기록을 남긴다.

    같은 음식을 여러 번 먹을 수 있으므로 중복 검사를 하지 않는다.
    어제 먹은 김치찌개와 오늘 먹은 김치찌개는 다른 기록이다.
    """
    history = MealHistory(
        user_id=user.id,
        food_name=payload.food_name,
        recipe_name=payload.recipe_name,
        recipe_json=payload.recipe_json,
        ingredients_json=payload.ingredients_json,
        substitutes_json=payload.substitutes_json,
        estimated_cost=payload.estimated_cost,
    )
    db.add(history)
    db.commit()
    db.refresh(history)  # DB 가 채운 id · selected_at 을 읽어온다

    # 방금 만든 기록이라 아직 공유했을 수 없다
    return to_response(history, None)


def delete_history(db: Session, user: User, history_id: int) -> None:
    """내 기록 하나를 지운다 (use_yn='N').

    살아있는 후기가 달린 기록은 지우지 못하게 막는다.
    DDL 의 fk_reviews_meal 은 ON DELETE 절이 없어 물리 삭제를 막아주지만,
    우리는 물리 삭제를 쓰지 않는다. UPDATE use_yn='N' 은 DB 입장에서 그냥
    값 수정이라 FK 가 아무 반응도 하지 않으므로 여기서 직접 확인해야 한다.
    (db/01_schema.sql 상단 [4] 참고)
    """
    history = get_alive_history(db, user, history_id)

    shared = db.execute(
        select(RecipeReview.id).where(
            RecipeReview.meal_history_id == history.id,
            RecipeReview.use_yn == USE_YN_YES,
        )
    ).first()

    if shared is not None:
        raise HTTPException(
            status.HTTP_409_CONFLICT,
            "커뮤니티에 공유한 기록입니다. 글을 먼저 삭제해주세요.",
        )

    history.use_yn = USE_YN_NO
    db.commit()


def get_stats(db: Session, user: User) -> MealHistoryStats:
    """History 화면 위쪽 요약값."""
    rows = db.execute(
        select(MealHistory.selected_at, MealHistory.estimated_cost).where(
            MealHistory.user_id == user.id,
            MealHistory.use_yn == USE_YN_YES,
        )
    ).all()

    total_cost = 0
    days = set()

    for selected_at, cost in rows:
        # estimated_cost 가 NULL 인 기록도 있다. 0 으로 친다.
        if cost:
            total_cost += cost
        days.add(to_kst_date(selected_at))

    return MealHistoryStats(
        count=len(rows),
        total_cost=total_cost,
        streak_days=count_streak(days),
    )


def count_streak(days: set) -> int:
    """오늘(또는 어제)부터 며칠 연속으로 기록이 있는지 센다.

    어제까지만 있어도 연속으로 친다. 오늘 아직 아무것도 안 먹었다고
    7일 연속이 0일로 초기화되면, 저녁 먹기 전까지 화면이 거짓말을 한다.
    """
    if not days:
        return 0

    today = today_kst()

    if today in days:
        cursor = today
    elif (today - timedelta(days=1)) in days:
        cursor = today - timedelta(days=1)
    else:
        return 0

    streak = 0
    while cursor in days:
        streak += 1
        cursor = cursor - timedelta(days=1)

    return streak


def count_histories(db: Session, user_id: int) -> int:
    """이 사람이 확정한 식사 건수. 요리 레벨 계산에 쓴다."""
    return db.execute(
        select(func.count(MealHistory.id)).where(
            MealHistory.user_id == user_id,
            MealHistory.use_yn == USE_YN_YES,
        )
    ).scalar_one()
