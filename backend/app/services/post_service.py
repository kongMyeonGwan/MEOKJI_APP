"""레시피 후기 게시판 비즈니스 로직 (STEP 20~23).

조회(list_posts / get_post)는 Guest도 가능하므로 user: User | None 을 받는다.
None이면 좋아요 여부 · 내 글 여부를 계산하지 않고 False로 둔다.

쓰기(create_post / set_like / create_comment)는 로그인이 필수라 User 를 받는다.
"남의 기록으로 글을 쓸 수 없다"는 규칙을 라우터가 아니라 여기서 지킨다.

스키마 v2 두 가지 주의
----------------------
1. 음식명 · 레시피 · 재료는 RECIPE_REVIEWS 에 없다. MEAL_HISTORIES 와 JOIN 해서
   읽는다. MEAL_HISTORIES 는 확정 당시 스냅샷이고 이후 UPDATE 하지 않으므로,
   복사본을 따로 두는 것보다 JOIN 이 안전하다(두 값이 어긋날 여지가 없다).
2. 삭제는 전부 논리 삭제다. 모든 조회에 use_yn='Y' 를 붙여야 한다.
   좋아요 개수 · 댓글 개수처럼 COUNT 하는 자리에서 특히 자주 빠뜨린다.

API 응답 필드명(food_name, meal_history_id 등)은 v1 그대로 유지한다.
프론트엔드가 그 이름으로 읽고 있어서, 테이블만 바뀐 것을 화면까지 전파하지 않는다.
"""

from fastapi import HTTPException, status
from sqlalchemy import Select, func, select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.models.meal_history import MealHistory
from app.models.recipe_review import RecipeReview
from app.models.review_comment import ReviewComment
from app.models.review_like import ReviewLike
from app.models.types import USE_YN_NO, USE_YN_YES
from app.models.user import User
from app.schemas.post import (
    CommentCreateRequest,
    CommentResponse,
    LikeResponse,
    PostCreateRequest,
    PostDetailResponse,
    PostListItem,
)

# 목록 미리보기로 내려줄 본문 길이. 화면은 CSS로 2줄까지만 보여주므로 넉넉하다.
_EXCERPT_LENGTH = 200


def _like_count_subquery():
    """후기별 좋아요 수. idx_review_likes_review(review_id, use_yn)를 탄다.

    취소한 좋아요('N')는 행이 그대로 남아 있으므로 반드시 걸러야 한다.
    이 조건을 빠뜨리면 취소한 좋아요까지 세어 숫자가 줄지 않는다.
    """
    return (
        select(func.count(ReviewLike.id))
        .where(
            ReviewLike.review_id == RecipeReview.id,
            ReviewLike.use_yn == USE_YN_YES,
        )
        .correlate(RecipeReview)
        .scalar_subquery()
    )


def _comment_count_subquery():
    """후기별 댓글 수. idx_review_comments_rev(review_id, use_yn)를 탄다."""
    return (
        select(func.count(ReviewComment.id))
        .where(
            ReviewComment.review_id == RecipeReview.id,
            ReviewComment.use_yn == USE_YN_YES,
        )
        .correlate(RecipeReview)
        .scalar_subquery()
    )


def _apply_keyword(stmt: Select, keyword: str | None) -> Select:
    """제목 · 본문 부분 일치 검색.

    Oracle에는 ILIKE가 없어 LOWER() 양쪽에 걸어 대소문자를 무시한다.
    한글에는 의미가 없지만 영문 재료명/메뉴명 검색에서 필요하다.

    사용자가 넣은 % _ 는 와일드카드가 아니라 글자로 취급해야 하므로
    ESCAPE 를 지정하고 직접 이스케이프한다.
    """
    if not keyword or not keyword.strip():
        return stmt

    escaped = keyword.strip().lower()
    for ch in ("\\", "%", "_"):
        escaped = escaped.replace(ch, f"\\{ch}")
    pattern = f"%{escaped}%"

    return stmt.where(
        func.lower(RecipeReview.title).like(pattern, escape="\\")
        | func.lower(RecipeReview.content).like(pattern, escape="\\")
    )


def _active_review(db: Session, review_id: int) -> RecipeReview:
    """살아있는 후기를 가져오거나 404를 던진다.

    삭제된 후기(use_yn='N')는 "없는 것"으로 취급한다. 404 대신 410을 주면
    "그 id는 존재했다"는 사실이 응답으로 새기 때문에 구분하지 않는다.
    """
    review = db.get(RecipeReview, review_id)
    if review is None or review.use_yn != USE_YN_YES:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "게시글을 찾을 수 없습니다.")
    return review


def list_posts(db: Session, keyword: str | None = None) -> list[PostListItem]:
    """후기 목록을 최신순으로 돌려준다. Guest도 볼 수 있다.

    음식명은 MEAL_HISTORIES 에서 JOIN 해 온다. CLOB인 recipe_json /
    ingredients_json 은 select 하지 않는다. 목록 화면은 쓰지 않는데
    20건이면 CLOB을 20번 읽어오게 된다.
    본문(content)도 CLOB이지만 미리보기에 필요해 읽되, 잘라서 내려준다.

    MealHistory 쪽에는 use_yn 조건을 걸지 않는다. 합성 FK가 행의 존재를
    보장하고, 후기가 보일지 말지는 후기 자신의 use_yn 이 정한다.
    여기서 걸러버리면 글쓴이가 기록을 정리한 순간 남의 화면에서 음식명만
    사라진 게시글이 남는다.
    """
    stmt = (
        select(
            RecipeReview.id,
            RecipeReview.title,
            RecipeReview.content,
            MealHistory.food_name,
            RecipeReview.view_count,
            RecipeReview.created_at,
            User.nickname,
            _like_count_subquery().label("like_count"),
            _comment_count_subquery().label("comment_count"),
        )
        .join(User, User.id == RecipeReview.user_id)
        .join(MealHistory, MealHistory.id == RecipeReview.meal_history_id)
        .where(RecipeReview.use_yn == USE_YN_YES)
        .order_by(RecipeReview.created_at.desc(), RecipeReview.id.desc())
    )
    stmt = _apply_keyword(stmt, keyword)

    return [
        PostListItem(
            id=row.id,
            title=row.title,
            # 목록에 본문 전체를 실어보내지 않는다. 화면은 2줄만 쓴다.
            content=row.content[:_EXCERPT_LENGTH],
            food_name=row.food_name,
            nickname=row.nickname,
            view_count=row.view_count,
            like_count=row.like_count,
            comment_count=row.comment_count,
            created_at=row.created_at,
        )
        for row in db.execute(stmt).all()
    ]


def get_post(db: Session, post_id: int, user: User | None) -> PostDetailResponse:
    """후기 하나 + 댓글 목록. Guest도 볼 수 있다.

    조회수는 여기서 올리지 않는다. GET 요청은 멱등해야 하고,
    새로고침 한 번에 1씩 오르는 카운터는 STEP 21에서 따로 다룬다.
    """
    row = db.execute(
        select(
            RecipeReview,
            MealHistory,
            User.nickname,
            _like_count_subquery().label("like_count"),
        )
        .join(User, User.id == RecipeReview.user_id)
        .join(MealHistory, MealHistory.id == RecipeReview.meal_history_id)
        .where(RecipeReview.id == post_id, RecipeReview.use_yn == USE_YN_YES)
    ).first()

    if row is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "게시글을 찾을 수 없습니다.")

    review, history, nickname, like_count = row

    liked = False
    if user is not None:
        liked = db.execute(
            select(ReviewLike.id).where(
                ReviewLike.review_id == review.id,
                ReviewLike.user_id == user.id,
                ReviewLike.use_yn == USE_YN_YES,
            )
        ).first() is not None

    comments = db.execute(
        select(ReviewComment, User.nickname)
        .join(User, User.id == ReviewComment.user_id)
        .where(
            ReviewComment.review_id == review.id,
            ReviewComment.use_yn == USE_YN_YES,
        )
        # idx_review_comments_rev(review_id, use_yn, created_at)를 그대로 탄다
        .order_by(ReviewComment.created_at, ReviewComment.id)
    ).all()

    return PostDetailResponse(
        id=review.id,
        title=review.title,
        content=review.content,
        nickname=nickname,
        view_count=review.view_count,
        like_count=like_count,
        created_at=review.created_at,
        # 복사본이 아니라 원본 히스토리에서 읽는다.
        # 히스토리는 확정 후 UPDATE 하지 않으므로 값이 흔들리지 않는다.
        food_name=history.food_name,
        recipe_json=history.recipe_json,
        ingredients_json=history.ingredients_json,
        meal_history_id=review.meal_history_id,
        liked=liked,
        is_mine=user is not None and review.user_id == user.id,
        comments=[
            CommentResponse(
                id=comment.id,
                nickname=writer_nickname,
                content=comment.content,
                created_at=comment.created_at,
                is_mine=user is not None and comment.user_id == user.id,
            )
            for comment, writer_nickname in comments
        ],
    )


def create_post(db: Session, user: User, payload: PostCreateRequest) -> PostDetailResponse:
    """확정한 식사 하나에 대한 후기를 커뮤니티에 올린다.

    레시피/재료를 요청 본문에서 받지 않는다. 화면에 뿌릴 값은 조회 시점에
    원본 MealHistory 에서 JOIN 해 오므로, 여기서는 어떤 기록인지만 정하면 된다.

    남의 기록을 지정해도 DB의 합성 FK(fk_reviews_meal)가 막지만,
    거기까지 가면 ORA-02291이 500으로 새어나간다. 여기서 먼저 404로 끊는다.
    """
    history = db.get(MealHistory, payload.meal_history_id)

    # 남의 기록이면 403이 아니라 404다. 403으로 구분해주면
    # "그 id는 존재한다"는 사실이 응답으로 새기 때문이다.
    # 삭제한 기록('N')도 같은 이유로 없는 것으로 취급한다.
    if history is None or history.user_id != user.id or history.use_yn != USE_YN_YES:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "기록을 찾을 수 없습니다.")

    review = RecipeReview(
        user_id=user.id,
        meal_history_id=history.id,
        title=payload.title,
        content=payload.content,
    )
    db.add(review)

    try:
        db.commit()
    except IntegrityError as exc:
        db.rollback()
        # 이미 후기를 쓴 기록. 화면에서도 목록에서 빼두지만, 두 탭에서 동시에
        # 등록하면 여기까지 온다. 최종 판단은 uq_reviews_meal 이 한다.
        if "UQ_REVIEWS_MEAL" in str(exc.orig).upper():
            raise HTTPException(
                status.HTTP_409_CONFLICT, "이미 공유한 기록입니다."
            ) from exc
        raise

    db.refresh(review)  # DB가 채운 id / created_at 을 읽어온다
    return get_post(db, review.id, user)


def set_like(db: Session, user: User, post_id: int, liked: bool) -> LikeResponse:
    """좋아요를 켜거나 끈다.

    같은 상태를 두 번 요청해도 에러를 내지 않는다(멱등).
    따닥 눌러 요청이 두 번 나가는 건 사용자 잘못이 아니고,
    중복은 어차피 uq_review_likes 가 막는다.

    취소는 행 삭제가 아니라 use_yn='N' 이다. 다시 누를 때 새 행을 만들지 않고
    잠들어 있던 행을 되살린다. 새로 INSERT 해도 함수 기반 인덱스가
    'N' 행을 무시하므로 동작은 하지만, 껐다 켤 때마다 행이 쌓인다.
    """
    _active_review(db, post_id)

    active = db.execute(
        select(ReviewLike).where(
            ReviewLike.review_id == post_id,
            ReviewLike.user_id == user.id,
            ReviewLike.use_yn == USE_YN_YES,
        )
    ).scalar_one_or_none()

    if liked and active is None:
        dormant = db.execute(
            select(ReviewLike)
            .where(
                ReviewLike.review_id == post_id,
                ReviewLike.user_id == user.id,
                ReviewLike.use_yn == USE_YN_NO,
            )
            .order_by(ReviewLike.id.desc())
        ).scalars().first()

        if dormant is not None:
            dormant.use_yn = USE_YN_YES
        else:
            db.add(ReviewLike(user_id=user.id, review_id=post_id))
    elif not liked and active is not None:
        active.use_yn = USE_YN_NO

    try:
        db.commit()
    except IntegrityError:
        # 동시에 두 번 눌러 둘 다 INSERT로 들어온 경우. 결과는 "좋아요 켜짐"으로 같다.
        db.rollback()

    count = db.execute(
        select(func.count(ReviewLike.id)).where(
            ReviewLike.review_id == post_id,
            ReviewLike.use_yn == USE_YN_YES,
        )
    ).scalar_one()

    return LikeResponse(liked=liked, like_count=count)


def create_comment(
    db: Session, user: User, post_id: int, payload: CommentCreateRequest
) -> CommentResponse:
    """댓글을 단다. 자기 글에도 달 수 있다."""
    _active_review(db, post_id)

    comment = ReviewComment(review_id=post_id, user_id=user.id, content=payload.content)
    db.add(comment)
    db.commit()
    db.refresh(comment)

    return CommentResponse(
        id=comment.id,
        nickname=user.nickname,
        content=comment.content,
        created_at=comment.created_at,
        is_mine=True,
    )
