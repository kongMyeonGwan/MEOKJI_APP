"""게시판 비즈니스 로직 (STEP 20~23).

조회(list_posts / get_post)는 Guest도 가능하므로 user: User | None 을 받는다.
None이면 좋아요 여부 · 내 글 여부를 계산하지 않고 False로 둔다.

쓰기(create_post / set_like / create_comment)는 로그인이 필수라 User 를 받는다.
"남의 기록으로 글을 쓸 수 없다"는 규칙을 라우터가 아니라 여기서 지킨다.
"""

from fastapi import HTTPException, status
from sqlalchemy import Select, func, select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.models.comment import Comment
from app.models.meal_history import MealHistory
from app.models.post import Post
from app.models.post_like import PostLike
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
    """게시글별 좋아요 수. idx_post_likes_post 를 탄다."""
    return (
        select(func.count(PostLike.id))
        .where(PostLike.post_id == Post.id)
        .correlate(Post)
        .scalar_subquery()
    )


def _comment_count_subquery():
    """게시글별 댓글 수. idx_comments_post 를 탄다."""
    return (
        select(func.count(Comment.id))
        .where(Comment.post_id == Post.id)
        .correlate(Post)
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
        func.lower(Post.title).like(pattern, escape="\\")
        | func.lower(Post.content).like(pattern, escape="\\")
    )


def list_posts(db: Session, keyword: str | None = None) -> list[PostListItem]:
    """게시글 목록을 최신순으로 돌려준다. Guest도 볼 수 있다.

    CLOB인 recipe_json / ingredients_json 은 select 하지 않는다.
    목록 화면은 쓰지 않는데 20건이면 CLOB을 20번 읽어오게 된다.
    본문(content)도 CLOB이지만 미리보기에 필요해 읽되, 잘라서 내려준다.
    """
    stmt = (
        select(
            Post.id,
            Post.title,
            Post.content,
            Post.food_name,
            Post.view_count,
            Post.created_at,
            User.nickname,
            _like_count_subquery().label("like_count"),
            _comment_count_subquery().label("comment_count"),
        )
        .join(User, User.id == Post.user_id)
        .order_by(Post.created_at.desc(), Post.id.desc())
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
    """게시글 하나 + 댓글 목록. Guest도 볼 수 있다.

    조회수는 여기서 올리지 않는다. GET 요청은 멱등해야 하고,
    새로고침 한 번에 1씩 오르는 카운터는 STEP 21에서 따로 다룬다.
    """
    row = db.execute(
        select(Post, User.nickname, _like_count_subquery().label("like_count"))
        .join(User, User.id == Post.user_id)
        .where(Post.id == post_id)
    ).first()

    if row is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "게시글을 찾을 수 없습니다.")

    post, nickname, like_count = row

    liked = False
    if user is not None:
        liked = db.execute(
            select(PostLike.id).where(
                PostLike.post_id == post.id, PostLike.user_id == user.id
            )
        ).first() is not None

    comments = db.execute(
        select(Comment, User.nickname)
        .join(User, User.id == Comment.user_id)
        .where(Comment.post_id == post.id)
        .order_by(Comment.created_at, Comment.id)  # idx_comments_post 를 탄다
    ).all()

    return PostDetailResponse(
        id=post.id,
        title=post.title,
        content=post.content,
        nickname=nickname,
        view_count=post.view_count,
        like_count=like_count,
        created_at=post.created_at,
        # 참조가 아니라 확정 당시의 복사본을 그대로 내려준다
        food_name=post.food_name,
        recipe_json=post.recipe_json,
        ingredients_json=post.ingredients_json,
        meal_history_id=post.meal_history_id,
        liked=liked,
        is_mine=user is not None and post.user_id == user.id,
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
    """확정한 식사 하나를 커뮤니티에 공유한다.

    레시피/재료는 요청 본문이 아니라 원본 MealHistory에서 서버가 직접 복사한다.
    클라이언트가 보낸 값을 그대로 저장하면 "게시글은 확정한 식사의 스냅샷"이라는
    전제가 언제든 깨질 수 있다.
    """
    history = db.get(MealHistory, payload.meal_history_id)

    # 남의 기록이면 403이 아니라 404다. 403으로 구분해주면
    # "그 id는 존재한다"는 사실이 응답으로 새기 때문이다.
    if history is None or history.user_id != user.id:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "기록을 찾을 수 없습니다.")

    post = Post(
        user_id=user.id,
        meal_history_id=history.id,
        title=payload.title,
        content=payload.content,
        # --- 여기부터 스냅샷 복사 ---
        food_name=history.food_name,
        recipe_json=history.recipe_json,
        ingredients_json=history.ingredients_json,
    )
    db.add(post)

    try:
        db.commit()
    except IntegrityError as exc:
        db.rollback()
        # 이미 공유한 기록. 화면에서도 목록에서 빼두지만, 두 탭에서 동시에
        # 등록하면 여기까지 온다. 최종 판단은 uq_posts_meal 이 한다.
        if "UQ_POSTS_MEAL" in str(exc.orig).upper():
            raise HTTPException(
                status.HTTP_409_CONFLICT, "이미 공유한 기록입니다."
            ) from exc
        raise

    db.refresh(post)  # DB가 채운 id / created_at 을 읽어온다
    return get_post(db, post.id, user)


def set_like(db: Session, user: User, post_id: int, liked: bool) -> LikeResponse:
    """좋아요를 켜거나 끈다.

    같은 상태를 두 번 요청해도 에러를 내지 않는다(멱등).
    따닥 눌러 요청이 두 번 나가는 건 사용자 잘못이 아니고,
    중복은 어차피 uq_post_likes 가 막는다.
    """
    if db.get(Post, post_id) is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "게시글을 찾을 수 없습니다.")

    existing = db.execute(
        select(PostLike).where(
            PostLike.post_id == post_id, PostLike.user_id == user.id
        )
    ).scalar_one_or_none()

    if liked and existing is None:
        db.add(PostLike(user_id=user.id, post_id=post_id))
    elif not liked and existing is not None:
        db.delete(existing)

    try:
        db.commit()
    except IntegrityError:
        # 동시에 두 번 눌러 둘 다 INSERT로 들어온 경우. 결과는 "좋아요 켜짐"으로 같다.
        db.rollback()

    count = db.execute(
        select(func.count(PostLike.id)).where(PostLike.post_id == post_id)
    ).scalar_one()

    return LikeResponse(liked=liked, like_count=count)


def create_comment(
    db: Session, user: User, post_id: int, payload: CommentCreateRequest
) -> CommentResponse:
    """댓글을 단다. 자기 글에도 달 수 있다."""
    if db.get(Post, post_id) is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "게시글을 찾을 수 없습니다.")

    comment = Comment(post_id=post_id, user_id=user.id, content=payload.content)
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
