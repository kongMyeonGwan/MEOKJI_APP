"""찜한 레시피 로직.

두 함수 모두 첫 인자로 로그인 사용자를 받는다. "남의 찜을 건드릴 수 없다"는
규칙을 라우터가 아니라 여기서 지킨다 (ban_service 와 같은 방식).

껐다 켰다 하는 것을 행 삭제로 처리하지 않는다
---------------------------------------------
하트는 눌렀다 뗐다 하는 버튼이라, 지우고 다시 만들면 created_at 이 매번
바뀌어 "언제 찜했는지" 가 사라진다. 그래서 use_yn 만 뒤집는다.
게시글 좋아요(post_service.set_like)와 같은 판단이다.
"""

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
    """내 찜 목록을 최근 찜한 순으로 돌려준다.

    페이징하지 않는다. 화면(LikesContext)이 "이 음식이 찜돼 있나" 를 판단하려면
    목록 전체를 들고 있어야 하기 때문이다. 한 페이지만 받으면 2페이지에 있는
    음식의 하트가 빈 채로 그려진다.

    개수가 많아야 카탈로그 크기(100종)를 넘지 않는다.

    idx_recipe_likes_user(user_id, use_yn) 를 그대로 타는 조회다.
    uq_recipe_likes 는 선두가 표현식이라 여기 쓰이지 않는다.
    """
    rows = db.execute(
        select(RecipeLike.food_name)
        .where(RecipeLike.user_id == user.id, RecipeLike.use_yn == USE_YN_YES)
        .order_by(RecipeLike.created_at.desc(), RecipeLike.id.desc())
    ).scalars()

    names = list(rows)

    return LikeListResponse(names=names, count=len(names))


def toggle_like(db: Session, user: User, food_name: str) -> LikeToggleResponse:
    """찜을 켜거나 끈다.

    같은 음식을 두 번 찜해도 행이 늘지 않는다. 이미 있는 행의 use_yn 만
    뒤집기 때문이다.

    행이 없을 때만 새로 만든다
    --------------------------
    IntegrityError 를 잡는 이유는 경합 때문이다. 탭 두 개에서 같은 하트를
    동시에 누르면 둘 다 "행이 없다" 를 보고 INSERT 로 간다. 그때 뒤엣것이
    uq_recipe_likes 에 걸리는데, 이미 앞엣것이 찜을 만들어 놨으므로
    에러를 낼 일이 아니다 — 다시 읽어서 그 행을 쓴다.
    """
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
    """찜을 전부 해제한다 (설정 화면의 '찜 비우기').

    행을 지우지 않고 use_yn 만 바꾼다. 실수로 눌렀을 때 DB 에서 되돌릴 수
    있고, 되살리면 찜한 시각도 그대로 남는다.
    """
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
