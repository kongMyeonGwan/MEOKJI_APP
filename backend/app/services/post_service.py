"""레시피 후기 게시판 기능 (글 목록 · 상세 · 쓰기 · 좋아요 · 댓글)."""

from datetime import date
from pathlib import Path

from fastapi import HTTPException, UploadFile, status
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
from app.schemas.page import DEFAULT_PAGE_SIZE, count_pages
from app.schemas.post import (
    CommentCreateRequest,
    CommentResponse,
    LikeResponse,
    PopularItem,
    PostCreateRequest,
    PostDetailResponse,
    PostFileInfo,
    PostListItem,
    PostPage,
    PostUpdateRequest,
)
from app.services import file_service

# 목록에 내려줄 본문 미리보기 길이.
# 화면은 CSS 로 2줄까지만 보여주므로 200자면 넉넉하다.
EXCERPT_LENGTH = 200

# 도우미 함수들


def count_likes_by_post(db: Session, post_ids: list[int]) -> dict[int, int]:
    """주어진 글들의 좋아요 개수를 한 번에 세서 {글 id: 개수} 로 돌려준다."""
    if not post_ids:
        return {}

    rows = db.execute(
        select(ReviewLike.review_id, func.count(ReviewLike.id))
        .where(
            ReviewLike.review_id.in_(post_ids),
            ReviewLike.use_yn == USE_YN_YES,
        )
        .group_by(ReviewLike.review_id)
    ).all()

    counts = {}
    for review_id, count in rows:
        counts[review_id] = count
    return counts


def count_comments_by_post(db: Session, post_ids: list[int]) -> dict[int, int]:
    """주어진 글들의 댓글 개수를 한 번에 세서 {글 id: 개수} 로 돌려준다."""
    if not post_ids:
        return {}

    rows = db.execute(
        select(ReviewComment.review_id, func.count(ReviewComment.id))
        .where(
            ReviewComment.review_id.in_(post_ids),
            ReviewComment.use_yn == USE_YN_YES,
        )
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
    """이 사람이 이 글에 좋아요를 눌러둔 상태인지 확인한다."""
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
    """검색어가 있으면 "제목 또는 본문에 포함" 조건을 하나 더 붙인다."""
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
    """살아있는 게시글을 가져온다."""
    review = db.get(RecipeReview, post_id)

    if review is None or review.use_yn != USE_YN_YES:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "게시글을 찾을 수 없습니다.")

    return review


def make_file_info(review: RecipeReview) -> PostFileInfo | None:
    """첨부파일 정보를 화면이 쓸 모양으로 만든다."""
    if review.file_yn != USE_YN_YES:
        return None

    return PostFileInfo(
        name=review.file_org_name or "첨부파일",
        ext=review.file_ext or "",
        size=review.file_size,
        size_text=file_service.human_size(review.file_size),
        url=f"/api/posts/{review.id}/file",
    )


# 화면에서 부르는 기능들


def list_posts(
    db: Session,
    keyword: str | None = None,
    page: int = 1,
    size: int = DEFAULT_PAGE_SIZE,
) -> PostPage:
    """게시글 목록을 최신순으로 한 페이지씩 돌려준다."""
    # COUNT 와 목록이 **같은 조건 · 같은 JOIN** 을 봐야 한다.
    # 한쪽에만 검색어를 붙이면 "3페이지가 있다는데 열면 비어 있는" 일이 생긴다.
    total = db.execute(
        add_keyword_filter(
            select(func.count(RecipeReview.id))
            .join(User, User.id == RecipeReview.user_id)
            .join(MealHistory, MealHistory.id == RecipeReview.meal_history_id)
            .where(RecipeReview.use_yn == USE_YN_YES),
            keyword,
        )
    ).scalar_one()

    query = add_keyword_filter(
        select(
            RecipeReview.id,
            RecipeReview.title,
            RecipeReview.content,
            MealHistory.food_name,
            RecipeReview.view_count,
            RecipeReview.created_at,
            User.nickname,
            # 첨부 여부만 읽는다. 경로(500자)와 원본 파일명은 상세에서만 쓴다.
            RecipeReview.file_yn,
        )
        .join(User, User.id == RecipeReview.user_id)
        .join(MealHistory, MealHistory.id == RecipeReview.meal_history_id)
        .where(RecipeReview.use_yn == USE_YN_YES)
        .order_by(RecipeReview.created_at.desc(), RecipeReview.id.desc()),
        keyword,
    )

    # SQLAlchemy 가 Oracle 용 OFFSET ... FETCH NEXT ... ROWS ONLY 로 바꿔준다.
    # (Oracle 12c 부터 되는 문법이다. XE 18c 인 우리 DB 는 문제없다)
    rows = db.execute(query.offset((page - 1) * size).limit(size)).all()

    # 좋아요 · 댓글 개수는 이 페이지에 나갈 글들만 한 번에 세어둔다
    post_ids = [row.id for row in rows]
    like_counts = count_likes_by_post(db, post_ids)
    comment_counts = count_comments_by_post(db, post_ids)

    items = []
    for row in rows:
        items.append(
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
                has_file=row.file_yn == USE_YN_YES,
            )
        )

    return PostPage(
        items=items,
        total=total,
        page=page,
        size=size,
        total_pages=count_pages(total, size),
    )


def get_post(db: Session, post_id: int, user: User | None) -> PostDetailResponse:
    """게시글 하나 + 댓글 목록."""
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
        file=make_file_info(review),
        liked=is_liked_by(db, review.id, user),
        is_mine=user is not None and review.user_id == user.id,
        comments=comments,
    )


def create_post(
    db: Session, user: User, payload: PostCreateRequest
) -> PostDetailResponse:
    """내가 확정한 식사 하나에 대해 후기를 올린다."""
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
        if "UQ_REVIEWS_MEAL" in str(exc.orig).upper():
            raise HTTPException(status.HTTP_409_CONFLICT, "이미 공유한 기록입니다.")

        # 그 외의 DB 에러는 그대로 올린다 (숨기면 원인을 못 찾는다)
        raise

    db.refresh(review)  # DB 가 채워준 id · created_at 을 읽어온다

    return get_post(db, review.id, user)


def set_like(db: Session, user: User, post_id: int, liked: bool) -> LikeResponse:
    """좋아요를 켜거나 끈다."""
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


# 수정 · 삭제 (STEP 21)


def get_my_post(db: Session, user: User, post_id: int) -> RecipeReview:
    """내 글 하나를 가져온다."""
    review = get_alive_post(db, post_id)

    if review.user_id != user.id:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "게시글을 찾을 수 없습니다.")

    return review


def update_post(
    db: Session, user: User, post_id: int, payload: PostUpdateRequest
) -> PostDetailResponse:
    """내 글의 제목과 본문을 고친다."""
    review = get_my_post(db, user, post_id)

    review.title = payload.title
    review.content = payload.content
    db.commit()

    return get_post(db, review.id, user)


def delete_post(db: Session, user: User, post_id: int) -> None:
    """내 글을 지운다 (use_yn='N')."""
    review = get_my_post(db, user, post_id)

    review.use_yn = USE_YN_NO
    db.commit()

    # 첨부파일은 디스크에서 지우지 않는다.


# 첨부파일 (후기 하나당 파일 하나)


def attach_file(
    db: Session, user: User, post_id: int, upload: UploadFile
) -> PostDetailResponse:
    """내 글에 첨부파일을 붙인다."""
    review = get_my_post(db, user, post_id)

    # 갈아 끼우기 전에 옛 파일 위치를 기억해둔다. set_file() 이 덮어쓴다.
    old_path, old_name = review.file_path, review.file_name

    saved = file_service.save_upload(upload)

    # 컬럼 여섯 개를 손으로 만지지 않는다. 하나만 빠뜨려도
    # ck_reviews_file_set 이 ORA-02290 으로 막는다.
    review.set_file(**saved)

    try:
        db.commit()
    except Exception:
        db.rollback()
        # DB 에 못 적었으면 방금 쓴 파일은 아무도 못 찾는 쓰레기다.
        file_service.remove_file(saved["path"], saved["name"])
        raise

    file_service.remove_file(old_path, old_name)

    return get_post(db, review.id, user)


def detach_file(db: Session, user: User, post_id: int) -> None:
    """내 글의 첨부파일을 뗀다."""
    review = get_my_post(db, user, post_id)

    if review.file_yn != USE_YN_YES:
        return

    old_path, old_name = review.file_path, review.file_name

    # file_yn 만 'N' 으로 바꾸고 나머지를 남겨두면 ck_reviews_file_set 이 막는다.
    review.clear_file()
    db.commit()

    # DB 를 먼저 정리하고 파일을 지운다. 반대로 하면 지우다 실패했을 때
    # DB 는 첨부가 있다는데 파일이 없는 상태가 된다.
    file_service.remove_file(old_path, old_name)


def open_post_file(db: Session, post_id: int) -> tuple[Path, str, str]:
    """내려줄 파일의 (실제 경로, 원본 파일명, 확장자)."""
    review = get_alive_post(db, post_id)

    if review.file_yn != USE_YN_YES:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "첨부파일이 없습니다.")

    target = file_service.absolute_path(review.file_path, review.file_name)

    if not target.is_file():
        raise HTTPException(status.HTTP_404_NOT_FOUND, "첨부파일을 찾을 수 없습니다.")

    return target, review.file_org_name or review.file_name, review.file_ext or ""


def increase_view(db: Session, post_id: int) -> int:
    """조회수를 1 올리고 올라간 값을 돌려준다."""
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
    """내 댓글을 지운다 (use_yn='N')."""
    comment = get_alive_comment(db, post_id, comment_id)

    if comment.user_id != user.id:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "댓글을 찾을 수 없습니다.")

    comment.use_yn = USE_YN_NO
    db.commit()


# 인기 레시피 (기간 안에 새로 받은 좋아요 기준)


def list_popular(
    db: Session, start: date, end: date, limit: int
) -> list[PopularItem]:
    """고른 기간에 좋아요를 많이 받은 글을 순서대로 돌려준다."""
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
