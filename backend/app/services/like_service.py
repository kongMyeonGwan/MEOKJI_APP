"""찜한 레시피 로직."""

from sqlalchemy import func, select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.models.recipe_like import RecipeLike
from app.models.types import USE_YN_NO, USE_YN_YES
from app.models.user import User
from app.schemas.like import LikeListResponse, LikeToggleResponse


def count_likes(db: Session, user: User) -> int:
    """내 찜 개수. use_yn='N' 은 빼야 한다 — 해제한 것도 행은 남아 있다."""
    return db.execute(
        select(func.count(RecipeLike.id)).where(
            RecipeLike.user_id == user.id,
            RecipeLike.use_yn == USE_YN_YES,
        )
    ).scalar_one()


def list_likes(db: Session, user: User) -> LikeListResponse:
    """내 찜 목록을 최근 찜한 순으로 돌려준다."""
    rows = db.execute(
        select(RecipeLike.food_name)
        .where(RecipeLike.user_id == user.id, RecipeLike.use_yn == USE_YN_YES)
        .order_by(RecipeLike.created_at.desc(), RecipeLike.id.desc())
    ).scalars()

    names = list(rows)

    return LikeListResponse(names=names, count=len(names))


def toggle_like(db: Session, user: User, food_name: str) -> LikeToggleResponse:
    """찜을 켜거나 끈다."""
    row = db.execute(
        select(RecipeLike).where(
            RecipeLike.user_id == user.id,
            RecipeLike.food_name == food_name,
        )
    ).scalars().first()

    if row is None:
        row = RecipeLike(user_id=user.id, food_name=food_name)
        db.add(row)
        try:
            db.commit()
        except IntegrityError:
            # 다른 요청이 먼저 만들었다. 그 행을 찾아 쓴다.
            db.rollback()
            row = db.execute(
                select(RecipeLike).where(
                    RecipeLike.user_id == user.id,
                    RecipeLike.food_name == food_name,
                )
            ).scalars().one()
            row.use_yn = USE_YN_YES
            db.commit()
    else:
        row.use_yn = USE_YN_NO if row.use_yn == USE_YN_YES else USE_YN_YES
        db.commit()

    return LikeToggleResponse(
        food_name=food_name,
        liked=row.use_yn == USE_YN_YES,
        count=count_likes(db, user),
    )


def clear_likes(db: Session, user: User) -> LikeListResponse:
    """찜을 전부 해제한다 (설정 화면의 '찜 비우기')."""
    rows = db.execute(
        select(RecipeLike).where(
            RecipeLike.user_id == user.id,
            RecipeLike.use_yn == USE_YN_YES,
        )
    ).scalars()

    for row in rows:
        row.use_yn = USE_YN_NO

    db.commit()

    return LikeListResponse(names=[], count=0)
