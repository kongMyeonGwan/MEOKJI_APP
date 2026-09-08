"""레시피 후기 게시판 기능 (글 목록 · 상세 · 쓰기 · 좋아요 · 댓글).

라우터는 HTTP 껍데기만 담당하고, 실제 규칙은 전부 여기에 있다.

함수가 받는 user 를 잘 보자
---------------------------
    user: User | None    로그인 안 해도 되는 기능 (목록 · 상세)
    user: User           로그인이 꼭 필요한 기능 (글쓰기 · 좋아요 · 댓글)

"남의 기록으로 글을 쓸 수 없다" 같은 규칙은 라우터가 아니라 이 파일이 지킨다.

기억해야 할 두 가지
-------------------
1. 음식명 · 레시피 · 재료는 RECIPE_REVIEWS 테이블에 없다.
   MEAL_HISTORIES(내가 확정한 식사 기록) 와 JOIN 해서 읽어온다.
   복사본을 따로 두면 두 값이 어긋날 수 있어서 원본에서 바로 읽는다.

2. 삭제는 전부 논리 삭제다. 행이 안 지워지고 use_yn 만 'N' 이 된다.
   그래서 조회할 때마다 use_yn == 'Y' 조건을 붙여야 한다.
   좋아요 개수 · 댓글 개수를 셀 때 특히 자주 빠뜨린다.
"""

from datetime import date

from fastapi import HTTPException, status
from sqlalchemy import func, select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.core.kst import day_end_utc, day_start_utc
from app.data import find_food
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
    PopularItem,
    PostCreateRequest,
    PostDetailResponse,
    PostListItem,
    PostUpdateRequest,
)

# 목록에 내려줄 본문 미리보기 길이.
# 화면은 CSS 로 2줄까지만 보여주므로 200자면 넉넉하다.
EXCERPT_LENGTH = 200


# ==========================================================
# 도우미 함수들
# ==========================================================


def count_likes_by_post(db: Session) -> dict[int, int]:
    """게시글별 좋아요 개수를 한 번에 세서 {글 id: 개수} 로 돌려준다.

        {1: 5, 2: 0, 3: 12}

    글 하나마다 COUNT 쿼리를 날리면 글이 20개일 때 쿼리가 21번 나간다(N+1 문제).
    GROUP BY 로 한 번에 세면 쿼리 한 번으로 끝난다.

    use_yn == 'Y' 조건을 빠뜨리면 취소한 좋아요까지 세어서
    좋아요를 눌렀다 껐는데도 숫자가 안 줄어든다.
    """
    rows = db.execute(
        select(ReviewLike.review_id, func.count(ReviewLike.id))
        .where(ReviewLike.use_yn == USE_YN_YES)
        .group_by(ReviewLike.review_id)
    ).all()

    counts = {}
    for review_id, count in rows:
        counts[review_id] = count
    return counts


def count_comments_by_post(db: Session) -> dict[int, int]:
    """게시글별 댓글 개수를 한 번에 세서 {글 id: 개수} 로 돌려준다."""
    rows = db.execute(
        select(ReviewComment.review_id, func.count(ReviewComment.id))
        .where(ReviewComment.use_yn == USE_YN_YES)
        .group_by(ReviewComment.review_id)
    ).all()

    counts = {}
    for review_id, count in rows:
        counts[review_id] = count
    return counts


def count_likes_of_post(db: Session, post_id: int) -> int:
    """게시글 하나의 좋아요 개수를 센다."""
    return db.execute(
        select(func.count(ReviewLike.id)).where(
            ReviewLike.review_id == post_id,
            ReviewLike.use_yn == USE_YN_YES,
        )
    ).scalar_one()


def is_liked_by(db: Session, post_id: int, user: User | None) -> bool:
    """이 사람이 이 글에 좋아요를 눌러둔 상태인지 확인한다.

    로그인 안 했으면(user 가 None) 볼 것도 없이 False 다.
    """
    if user is None:
        return False

    row = db.execute(
        select(ReviewLike.id).where(
            ReviewLike.review_id == post_id,
            ReviewLike.user_id == user.id,
            ReviewLike.use_yn == USE_YN_YES,
        )
    ).first()

    return row is not None


def add_keyword_filter(query, keyword: str | None):
    """검색어가 있으면 "제목 또는 본문에 포함" 조건을 하나 더 붙인다.

    Oracle 에는 대소문자를 무시하는 ILIKE 가 없어서, 양쪽 다 소문자로 바꿔 비교한다.
    한글에는 의미가 없지만 영어 메뉴명 검색에 필요하다.

    사용자가 % 나 _ 를 입력하면 SQL 에서는 "아무 글자나" 라는 뜻이 되어버린다.
    그러면 "50%" 를 검색했을 때 엉뚱한 글이 다 나온다.
    그래서 그 앞에 역슬래시를 붙여서 그냥 글자로 취급하게 만든다.
    """
    if keyword is None or not keyword.strip():
        return query

    # 특수문자 앞에 역슬래시를 붙인다.   "50%" -> "50\\%"
    escaped = keyword.strip().lower()
    for special_char in ("\\", "%", "_"):
        escaped = escaped.replace(special_char, "\\" + special_char)

    # 앞뒤에 % 를 붙이면 "포함" 검색이 된다.   "김치" -> "%김치%"
    pattern = "%" + escaped + "%"

    return query.where(
        func.lower(RecipeReview.title).like(pattern, escape="\\")
        | func.lower(RecipeReview.content).like(pattern, escape="\\")
    )


def get_alive_post(db: Session, post_id: int) -> RecipeReview:
    """살아있는 게시글을 가져온다. 없거나 삭제됐으면 404.

    삭제된 글은 "원래 없던 글"처럼 취급한다.
    410(사라짐) 같은 걸 주면 "그 번호는 예전에 있었다"는 사실이 새어나간다.
    """
    review = db.get(RecipeReview, post_id)

    if review is None or review.use_yn != USE_YN_YES:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "게시글을 찾을 수 없습니다.")

    return review


# ==========================================================
# 화면에서 부르는 기능들
# ==========================================================


def list_posts(db: Session, keyword: str | None = None) -> list[PostListItem]:
    """게시글 목록을 최신순으로 돌려준다. 로그인 안 해도 볼 수 있다.

    음식명은 MEAL_HISTORIES 에서 JOIN 해서 가져온다.
    레시피 · 재료(CLOB)는 select 하지 않는다. 목록 화면에서는 안 쓰는데
    20건이면 큰 데이터를 20번 읽어오게 되기 때문이다.
    본문은 미리보기에 필요해서 읽되, 앞부분만 잘라서 내려준다.

    MealHistory 쪽에는 use_yn 조건을 안 건다.
    글쓴이가 나중에 자기 기록을 정리했다고 해서 남의 화면에서 음식명만
    사라진 게시글이 되면 안 되기 때문이다.
    글이 보일지 말지는 글 자신의 use_yn 이 정한다.
    """
    query = (
        select(
            RecipeReview.id,
            RecipeReview.title,
            RecipeReview.content,
            MealHistory.food_name,
            RecipeReview.view_count,
            RecipeReview.created_at,
            User.nickname,
        )
        .join(User, User.id == RecipeReview.user_id)
        .join(MealHistory, MealHistory.id == RecipeReview.meal_history_id)
        .where(RecipeReview.use_yn == USE_YN_YES)
        .order_by(RecipeReview.created_at.desc(), RecipeReview.id.desc())
    )
    query = add_keyword_filter(query, keyword)

    rows = db.execute(query).all()

    # 좋아요 · 댓글 개수는 미리 한 번에 세어둔다
    like_counts = count_likes_by_post(db)
    comment_counts = count_comments_by_post(db)

    posts = []
    for row in rows:
        posts.append(
            PostListItem(
                id=row.id,
                title=row.title,
                # 본문 전체를 목록에 실어보내지 않는다. 화면은 2줄만 쓴다.
                content=row.content[:EXCERPT_LENGTH],
                food_name=row.food_name,
                nickname=row.nickname,
                view_count=row.view_count,
                # 좋아요가 하나도 없는 글은 위 딕셔너리에 아예 없으므로 기본값 0
                like_count=like_counts.get(row.id, 0),
                comment_count=comment_counts.get(row.id, 0),
                created_at=row.created_at,
            )
        )

    return posts


def get_post(db: Session, post_id: int, user: User | None) -> PostDetailResponse:
    """게시글 하나 + 댓글 목록. 로그인 안 해도 볼 수 있다.

    조회수는 여기서 안 올린다.
    GET 요청은 몇 번을 보내도 결과가 같아야 하는데, 새로고침할 때마다
    1씩 오르는 카운터는 그 원칙에 어긋난다.
    """
    row = db.execute(
        select(RecipeReview, MealHistory, User.nickname)
        .join(User, User.id == RecipeReview.user_id)
        .join(MealHistory, MealHistory.id == RecipeReview.meal_history_id)
        .where(RecipeReview.id == post_id, RecipeReview.use_yn == USE_YN_YES)
    ).first()

    if row is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "게시글을 찾을 수 없습니다.")

    # 위에서 select 한 순서대로 세 개가 한 줄로 묶여서 온다
    review, history, nickname = row

    comment_rows = db.execute(
        select(ReviewComment, User.nickname)
        .join(User, User.id == ReviewComment.user_id)
        .where(
            ReviewComment.review_id == review.id,
            ReviewComment.use_yn == USE_YN_YES,
        )
        .order_by(ReviewComment.created_at, ReviewComment.id)
    ).all()

    comments = []
    for comment, writer_nickname in comment_rows:
        comments.append(
            CommentResponse(
                id=comment.id,
                nickname=writer_nickname,
                content=comment.content,
                created_at=comment.created_at,
                # 로그인했고 그 댓글을 내가 썼으면 True (수정/삭제 버튼 표시용)
                is_mine=user is not None and comment.user_id == user.id,
            )
        )

    return PostDetailResponse(
        id=review.id,
        title=review.title,
        content=review.content,
        nickname=nickname,
        view_count=review.view_count,
        like_count=count_likes_of_post(db, review.id),
        created_at=review.created_at,
        # 복사본이 아니라 원본 기록에서 읽어온다.
        # 기록은 확정한 뒤로 수정하지 않으므로 값이 흔들리지 않는다.
        food_name=history.food_name,
        recipe_json=history.recipe_json,
        ingredients_json=history.ingredients_json,
        meal_history_id=review.meal_history_id,
        liked=is_liked_by(db, review.id, user),
        is_mine=user is not None and review.user_id == user.id,
        comments=comments,
    )


def create_post(
    db: Session, user: User, payload: PostCreateRequest
) -> PostDetailResponse:
    """내가 확정한 식사 하나에 대해 후기를 올린다.

    레시피 · 재료를 요청 본문으로 받지 않는다.
    화면에 보여줄 값은 조회할 때 원본 기록에서 JOIN 해 오므로,
    여기서는 "어떤 기록에 대한 글인지"만 정하면 된다.

    남의 기록 번호를 넣어도 DB 의 FK 가 막아주긴 하지만,
    거기까지 가면 Oracle 에러가 500 으로 새어나간다. 여기서 먼저 404 로 끊는다.
    """
    history = db.get(MealHistory, payload.meal_history_id)

    # 없는 기록 · 남의 기록 · 삭제한 기록을 전부 똑같이 404 로 처리한다.
    # 남의 기록일 때만 403 을 주면 "그 번호는 존재한다"는 사실이 새어나간다.
    if history is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "기록을 찾을 수 없습니다.")
    if history.user_id != user.id:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "기록을 찾을 수 없습니다.")
    if history.use_yn != USE_YN_YES:
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

        # 이미 후기를 쓴 기록. 화면에서도 목록에서 빼두지만,
        # 탭 두 개에서 동시에 등록하면 여기까지 온다.
        # 최종 판단은 DB 의 uq_reviews_meal 인덱스가 한다.
        if "UQ_REVIEWS_MEAL" in str(exc.orig).upper():
            raise HTTPException(status.HTTP_409_CONFLICT, "이미 공유한 기록입니다.")

        # 그 외의 DB 에러는 그대로 올린다 (숨기면 원인을 못 찾는다)
        raise

    db.refresh(review)  # DB 가 채워준 id · created_at 을 읽어온다

    return get_post(db, review.id, user)


def set_like(db: Session, user: User, post_id: int, liked: bool) -> LikeResponse:
    """좋아요를 켜거나 끈다.

    같은 상태를 두 번 요청해도 에러를 내지 않는다.
    따닥 눌러서 요청이 두 번 나가는 건 사용자 잘못이 아니다.

    좋아요 취소는 행을 지우는 게 아니라 use_yn='N' 이다.
    다시 누르면 새 행을 만들지 않고 잠들어 있던 행을 되살린다.
    새로 INSERT 해도 동작은 하지만, 껐다 켤 때마다 행이 계속 쌓인다.
    """
    get_alive_post(db, post_id)  # 없는 글이면 여기서 404

    # 지금 켜져 있는 좋아요가 있는지 찾는다
    active_like = db.execute(
        select(ReviewLike).where(
            ReviewLike.review_id == post_id,
            ReviewLike.user_id == user.id,
            ReviewLike.use_yn == USE_YN_YES,
        )
    ).scalar_one_or_none()

    if liked and active_like is None:
        # 켜달라고 했는데 켜진 게 없다 -> 예전에 껐던 행이 있는지 찾아본다
        dormant_like = db.execute(
            select(ReviewLike)
            .where(
                ReviewLike.review_id == post_id,
                ReviewLike.user_id == user.id,
                ReviewLike.use_yn == USE_YN_NO,
            )
            .order_by(ReviewLike.id.desc())
            .limit(1)
        ).scalar_one_or_none()

        if dormant_like is not None:
            dormant_like.use_yn = USE_YN_YES  # 껐던 걸 되살린다
        else:
            db.add(ReviewLike(user_id=user.id, review_id=post_id))  # 처음 누른 것

    elif not liked and active_like is not None:
        # 꺼달라고 했고 켜진 게 있다 -> 끈다
        active_like.use_yn = USE_YN_NO

    try:
        db.commit()
    except IntegrityError:
        # 동시에 두 번 눌러서 INSERT 가 둘 다 들어온 경우.
        # 결과는 "좋아요 켜짐"으로 어차피 같으므로 그냥 넘어간다.
        db.rollback()

    return LikeResponse(liked=liked, like_count=count_likes_of_post(db, post_id))


def create_comment(
    db: Session, user: User, post_id: int, payload: CommentCreateRequest
) -> CommentResponse:
    """댓글을 단다. 자기 글에도 달 수 있다."""
    get_alive_post(db, post_id)  # 없는 글이면 여기서 404

    comment = ReviewComment(
        review_id=post_id,
        user_id=user.id,
        content=payload.content,
    )
    db.add(comment)
    db.commit()
    db.refresh(comment)  # DB 가 채워준 id · created_at 을 읽어온다

    return CommentResponse(
        id=comment.id,
        nickname=user.nickname,
        content=comment.content,
        created_at=comment.created_at,
        is_mine=True,  # 방금 내가 쓴 댓글이므로 항상 True
    )


# ==========================================================
# 수정 · 삭제 (STEP 21)
#
# 남의 글은 못 고치고 못 지운다. 그 판정을 여기서 한다.
# ==========================================================


def get_my_post(db: Session, user: User, post_id: int) -> RecipeReview:
    """내 글 하나를 가져온다. 없거나 남의 글이면 404.

    남의 글일 때만 403 을 주면 "그 번호는 존재한다"는 사실이 새어나간다.
    그래서 둘 다 404 로 똑같이 응답한다.
    """
    review = get_alive_post(db, post_id)

    if review.user_id != user.id:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "게시글을 찾을 수 없습니다.")

    return review


def update_post(
    db: Session, user: User, post_id: int, payload: PostUpdateRequest
) -> PostDetailResponse:
    """내 글의 제목과 본문을 고친다.

    레시피는 안 바뀐다. 원본 기록(MealHistory)에서 JOIN 해 오는 값이고,
    그 기록은 확정한 뒤로 UPDATE 하지 않기 때문이다.
    """
    review = get_my_post(db, user, post_id)

    review.title = payload.title
    review.content = payload.content
    db.commit()

    return get_post(db, review.id, user)


def delete_post(db: Session, user: User, post_id: int) -> None:
    """내 글을 지운다 (use_yn='N').

    댓글과 좋아요는 함께 지우지 않는다. 글이 안 보이면 거기 달린 것도 같이
    안 보이므로 화면상 차이가 없고, 나중에 글을 되살릴 때 그대로 돌아온다.

    uq_reviews_meal 인덱스가 use_yn='Y' 인 행만 보므로, 글을 지우면
    그 식사 기록으로 다시 글을 쓸 수 있다.
    """
    review = get_my_post(db, user, post_id)

    review.use_yn = USE_YN_NO
    db.commit()


def increase_view(db: Session, post_id: int) -> int:
    """조회수를 1 올리고 올라간 값을 돌려준다.

    GET 요청에서 올리지 않고 별도 엔드포인트로 뺐다.
    GET 은 몇 번을 보내도 결과가 같아야 하는데, 새로고침할 때마다 1씩 오르는
    카운터는 그 원칙에 어긋난다. 화면이 상세를 열 때 한 번만 이 API 를 부른다.
    """
    review = get_alive_post(db, post_id)

    review.view_count = review.view_count + 1
    db.commit()

    return review.view_count


def get_alive_comment(db: Session, post_id: int, comment_id: int) -> ReviewComment:
    """살아있는 댓글 하나를 가져온다. 없으면 404."""
    comment = db.get(ReviewComment, comment_id)

    if comment is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "댓글을 찾을 수 없습니다.")
    if comment.review_id != post_id:
        # 다른 글의 댓글 번호를 넣은 경우
        raise HTTPException(status.HTTP_404_NOT_FOUND, "댓글을 찾을 수 없습니다.")
    if comment.use_yn != USE_YN_YES:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "댓글을 찾을 수 없습니다.")

    return comment


def update_comment(
    db: Session,
    user: User,
    post_id: int,
    comment_id: int,
    payload: CommentCreateRequest,
) -> CommentResponse:
    """내 댓글을 고친다."""
    comment = get_alive_comment(db, post_id, comment_id)

    if comment.user_id != user.id:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "댓글을 찾을 수 없습니다.")

    comment.content = payload.content
    db.commit()
    db.refresh(comment)

    return CommentResponse(
        id=comment.id,
        nickname=user.nickname,
        content=comment.content,
        created_at=comment.created_at,
        is_mine=True,
    )


def delete_comment(db: Session, user: User, post_id: int, comment_id: int) -> None:
    """내 댓글을 지운다 (use_yn='N').

    글쓴이가 남의 댓글을 지우는 기능은 넣지 않았다.
    신고·차단 같은 운영 기능이 없는 상태에서 삭제 권한만 주면
    마음에 안 드는 댓글을 지우는 도구가 된다.
    """
    comment = get_alive_comment(db, post_id, comment_id)

    if comment.user_id != user.id:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "댓글을 찾을 수 없습니다.")

    comment.use_yn = USE_YN_NO
    db.commit()


# ==========================================================
# 인기 레시피 (기간 안에 새로 받은 좋아요 기준)
# ==========================================================


def list_popular(
    db: Session, start: date, end: date, limit: int
) -> list[PopularItem]:
    """고른 기간에 좋아요를 많이 받은 글을 순서대로 돌려준다.

    누적 좋아요가 아니라 **그 기간에 눌린 좋아요만** 센다.
    누적으로 세면 오래된 글이 영원히 1위라서 순위가 움직이지 않는다.

    화면에서 고른 날짜는 한국 날짜이고 review_likes.created_at 은 UTC 다.
    그대로 비교하면 경계에서 9시간이 어긋나므로 app/core/kst.py 로 변환한다.
    끝 날짜는 '다음 날 0시 미만' 으로 잡아야 그날 마지막 1초가 안 빠진다.

    use_yn 조건이 세 군데 필요하다. 하나라도 빠지면
    취소한 좋아요나 지운 글이 순위에 섞인다.
    """
    like_count = func.count(ReviewLike.id)
    last_liked = func.max(ReviewLike.created_at)

    rows = db.execute(
        select(
            RecipeReview.id,
            MealHistory.food_name,
            MealHistory.recipe_name,
            MealHistory.estimated_cost,
            User.nickname,
            like_count.label("like_count"),
            last_liked.label("liked_at"),
        )
        .join(ReviewLike, ReviewLike.review_id == RecipeReview.id)
        .join(User, User.id == RecipeReview.user_id)
        .join(MealHistory, MealHistory.id == RecipeReview.meal_history_id)
        .where(
            RecipeReview.use_yn == USE_YN_YES,
            ReviewLike.use_yn == USE_YN_YES,
            ReviewLike.created_at >= day_start_utc(start),
            ReviewLike.created_at < day_end_utc(end),
        )
        .group_by(
            RecipeReview.id,
            MealHistory.food_name,
            MealHistory.recipe_name,
            MealHistory.estimated_cost,
            User.nickname,
        )
        # 좋아요가 많은 순, 같으면 최근에 받은 순.
        # 두 번째 기준이 없으면 같은 데이터인데도 순서가 들쭉날쭉해 보인다.
        .order_by(like_count.desc(), last_liked.desc())
        .limit(limit)
    ).all()

    items = []
    for row in rows:
        item = PopularItem(
            id=row.id,
            food_name=row.food_name,
            recipe_name=row.recipe_name,
            nickname=row.nickname,
            like_count=row.like_count,
            estimated_cost=row.estimated_cost,
            liked_at=row.liked_at,
        )

        # 썸네일 그림은 DB 에 없다. 카탈로그에 같은 이름이 있으면 가져다 쓴다.
        known = find_food(row.food_name)
        if known is not None:
            item.art = known.get("art")
            item.image_url = known.get("image_url")

        items.append(item)

    return items
