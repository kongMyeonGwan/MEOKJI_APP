"""내 정보 · 요리 레벨 · 알림 · 회원 탈퇴 로직.

여기 있는 함수는 전부 "로그인한 본인" 만 다룬다.
남의 정보를 조회하거나 고치는 경로는 만들지 않았다.
"""

from fastapi import HTTPException, status
from sqlalchemy import func, select, update
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.core.security import hash_password, verify_password
from app.models.meal_history import MealHistory
from app.models.recipe_review import RecipeReview
from app.models.review_comment import ReviewComment
from app.models.review_like import ReviewLike
from app.models.types import USE_YN_NO, USE_YN_YES
from app.models.user import User
from app.models.user_ban import UserBan
from app.schemas.user import (
    NotificationItem,
    PasswordChangeRequest,
    ProfileResponse,
    ProfileUpdateRequest,
)
from app.services.history_service import get_stats

# =========================================================
# 요리 레벨
#
# 무엇을 하면 몇 점인지. 실제로 요리한 것을 가장 높게 친다.
# 글만 써서 레벨이 오르면 이 앱이 요리 앱이 아니라 게시판이 된다.
# =========================================================
EXP_PER_COOK = 50       # 식사 확정 (이걸로 먹을래요)
EXP_PER_POST = 30       # 후기 글 작성
EXP_PER_COMMENT = 10    # 댓글 작성
EXP_PER_LIKE = 5        # 내 글이 좋아요를 받음

# 레벨 1 -> 2 에 필요한 경험치. 레벨이 오를수록 아래만큼 더 필요해진다.
FIRST_LEVEL_EXP = 100
LEVEL_EXP_STEP = 50

# 레벨 구간별 칭호. (이 레벨 이상이면 이 칭호) 를 위에서부터 확인한다.
TITLES = [
    (30, "요리 장인"),
    (20, "요리 고수"),
    (10, "요리 중수"),
    (5, "요리 초보"),
    (1, "요리 입문"),
]

# 알림에 한 번에 담을 최대 건수. 상단바 패널이 그보다 길어지면 어차피 안 읽는다.
NOTIFICATION_LIMIT = 20


def count_posts(db: Session, user_id: int) -> int:
    """이 사람이 쓴 글 수 (지운 글 제외)."""
    return db.execute(
        select(func.count(RecipeReview.id)).where(
            RecipeReview.user_id == user_id,
            RecipeReview.use_yn == USE_YN_YES,
        )
    ).scalar_one()


def count_comments(db: Session, user_id: int) -> int:
    """이 사람이 쓴 댓글 수 (지운 댓글 제외)."""
    return db.execute(
        select(func.count(ReviewComment.id)).where(
            ReviewComment.user_id == user_id,
            ReviewComment.use_yn == USE_YN_YES,
        )
    ).scalar_one()


def count_likes_received(db: Session, user_id: int) -> int:
    """내 글들이 받은 좋아요 수.

    review_likes 를 recipe_reviews 와 JOIN 해서 "내 글에 달린 것" 만 센다.
    양쪽 다 use_yn 조건이 필요하다. 하나만 걸면 지운 글의 좋아요나
    취소한 좋아요가 섞인다.
    """
    return db.execute(
        select(func.count(ReviewLike.id))
        .join(RecipeReview, RecipeReview.id == ReviewLike.review_id)
        .where(
            RecipeReview.user_id == user_id,
            RecipeReview.use_yn == USE_YN_YES,
            ReviewLike.use_yn == USE_YN_YES,
        )
    ).scalar_one()


def make_level(total_exp: int) -> tuple[int, int, int]:
    """총 경험치로 (레벨, 지금 레벨의 경험치, 다음 레벨까지 필요한 양) 을 구한다.

    레벨이 오를수록 필요한 양이 조금씩 늘어난다.
        1 -> 2 : 100
        2 -> 3 : 150
        3 -> 4 : 200
    """
    level = 1
    need = FIRST_LEVEL_EXP
    remain = total_exp

    while remain >= need:
        remain -= need
        level += 1
        need += LEVEL_EXP_STEP

    return level, remain, need


def make_title(level: int) -> str:
    """레벨에 맞는 칭호를 고른다."""
    for threshold, title in TITLES:
        if level >= threshold:
            return title

    return TITLES[-1][1]


def get_profile(db: Session, user: User) -> ProfileResponse:
    """마이페이지 · 사이드바가 함께 쓰는 내 정보를 만든다."""
    stats = get_stats(db, user)
    post_count = count_posts(db, user.id)
    comment_count = count_comments(db, user.id)
    like_received = count_likes_received(db, user.id)

    total_exp = (
        stats.count * EXP_PER_COOK
        + post_count * EXP_PER_POST
        + comment_count * EXP_PER_COMMENT
        + like_received * EXP_PER_LIKE
    )

    level, exp, exp_next = make_level(total_exp)

    return ProfileResponse(
        id=user.id,
        email=user.email,
        nickname=user.nickname,
        created_at=user.created_at,
        gender=user.gender,
        address_sido=user.address_sido,
        address_sigungu=user.address_sigungu,
        # DB 는 NUMBER(9,6) 이라 파이썬으로 Decimal 이 온다.
        # 그대로 두면 JSON 으로 나갈 때 문자열이 되어 화면에서 계산이 안 된다.
        latitude=to_float(user.latitude),
        longitude=to_float(user.longitude),
        level=level,
        title=make_title(level),
        exp=exp,
        exp_next=exp_next,
        streak_days=stats.streak_days,
        cook_count=stats.count,
        post_count=post_count,
        comment_count=comment_count,
        like_received=like_received,
    )


def to_float(value) -> float | None:
    """Decimal 또는 None 을 float 또는 None 으로."""
    if value is None:
        return None

    return float(value)


def update_profile(
    db: Session, user: User, payload: ProfileUpdateRequest
) -> ProfileResponse:
    """내 정보를 고친다. 요청에 담긴 항목만 바꾼다.

    exclude_unset=True 가 핵심이다. 이게 없으면 닉네임만 보냈을 때
    주소가 None 으로 덮여 지워진다.
    """
    changes = payload.model_dump(exclude_unset=True)

    for field, value in changes.items():
        setattr(user, field, value)

    try:
        db.commit()
    except IntegrityError as exc:
        db.rollback()
        # uq_users_nickname 은 use_yn='Y' 인 행만 보는 함수 기반 인덱스다.
        # 탈퇴한 회원이 쓰던 닉네임은 여기 걸리지 않는다.
        if "UQ_USERS_NICKNAME" in str(exc.orig).upper():
            raise HTTPException(status.HTTP_409_CONFLICT, "이미 사용 중인 닉네임입니다.")
        raise

    db.refresh(user)
    return get_profile(db, user)


def change_my_password(db: Session, user: User, payload: PasswordChangeRequest) -> None:
    """로그인한 상태에서 비밀번호를 바꾼다.

    현재 비밀번호를 먼저 확인한다. 이게 없으면 로그인한 브라우저를 잠깐
    빌린 사람이 비밀번호를 바꿔 계정을 통째로 가져갈 수 있다.
    """
    if not verify_password(payload.current_password, user.password_hash):
        raise HTTPException(
            status.HTTP_400_BAD_REQUEST, "현재 비밀번호가 올바르지 않습니다."
        )

    if payload.current_password == payload.new_password:
        raise HTTPException(
            status.HTTP_400_BAD_REQUEST, "지금 쓰는 비밀번호와 다른 값을 입력해주세요."
        )

    user.password_hash = hash_password(payload.new_password)
    db.commit()


def withdraw(db: Session, user: User, password: str) -> None:
    """회원 탈퇴. DELETE 를 실행하지 않고 use_yn 을 'N' 으로 바꾼다.

    하위 데이터도 함께 'N' 으로 바꿔야 한다.
    FK 의 ON DELETE CASCADE 는 진짜 DELETE 에만 작동하고,
    UPDATE use_yn='N' 은 DB 입장에서 그냥 값 수정이라 아무 반응도 없다.
    (db/01_schema.sql 상단 [4] 참고)

    안 지우면 탈퇴한 사람의 글과 댓글이 게시판에 계속 남는다.
    """
    if not verify_password(password, user.password_hash):
        raise HTTPException(
            status.HTTP_400_BAD_REQUEST, "비밀번호가 올바르지 않습니다."
        )

    # 자식부터 지운다. 순서가 중요한 건 아니지만, 중간에 실패했을 때
    # "회원은 살아있는데 글만 사라진" 상태가 낫기 때문이다.
    for model in (ReviewLike, ReviewComment, RecipeReview, MealHistory, UserBan):
        db.execute(
            update(model)
            .where(model.user_id == user.id, model.use_yn == USE_YN_YES)
            .values(use_yn=USE_YN_NO)
        )

    user.use_yn = USE_YN_NO
    db.commit()

    # 토큰은 따로 무효화하지 않는다. JWT 는 서버가 발급 목록을 들고 있지
    # 않아서 취소할 방법이 없다. 대신 core/deps.py 가 요청마다
    # use_yn 을 확인하므로, 남은 토큰으로는 아무것도 할 수 없다.


def list_notifications(db: Session, user: User) -> list[NotificationItem]:
    """내 글에 달린 댓글과 좋아요를 알림으로 모아준다.

    내가 내 글에 단 댓글과 내가 누른 좋아요는 뺀다.
    자기 행동이 자기 알림으로 돌아오면 그냥 시끄럽기만 하다.
    """
    items = []

    comment_rows = db.execute(
        select(
            ReviewComment.id,
            ReviewComment.content,
            ReviewComment.created_at,
            ReviewComment.review_id,
            RecipeReview.title,
            User.nickname,
        )
        .join(RecipeReview, RecipeReview.id == ReviewComment.review_id)
        .join(User, User.id == ReviewComment.user_id)
        .where(
            RecipeReview.user_id == user.id,
            RecipeReview.use_yn == USE_YN_YES,
            ReviewComment.use_yn == USE_YN_YES,
            ReviewComment.user_id != user.id,
        )
        .order_by(ReviewComment.created_at.desc())
        .limit(NOTIFICATION_LIMIT)
    ).all()

    for row in comment_rows:
        items.append(
            NotificationItem(
                id=f"COMMENT-{row.id}",
                kind="COMMENT",
                icon="💬",
                title=f'"{row.title}"에 새 댓글이 달렸어요',
                body=f"{row.nickname}: {shorten(row.content)}",
                created_at=row.created_at,
                link=f"/community/{row.review_id}",
            )
        )

    like_rows = db.execute(
        select(
            ReviewLike.id,
            ReviewLike.created_at,
            ReviewLike.review_id,
            RecipeReview.title,
            User.nickname,
        )
        .join(RecipeReview, RecipeReview.id == ReviewLike.review_id)
        .join(User, User.id == ReviewLike.user_id)
        .where(
            RecipeReview.user_id == user.id,
            RecipeReview.use_yn == USE_YN_YES,
            ReviewLike.use_yn == USE_YN_YES,
            ReviewLike.user_id != user.id,
        )
        .order_by(ReviewLike.created_at.desc())
        .limit(NOTIFICATION_LIMIT)
    ).all()

    for row in like_rows:
        items.append(
            NotificationItem(
                id=f"LIKE-{row.id}",
                kind="LIKE",
                icon="❤️",
                title="내 글이 좋아요를 받았어요",
                body=f'{row.nickname}님이 "{row.title}"을(를) 좋아합니다',
                created_at=row.created_at,
                link=f"/community/{row.review_id}",
            )
        )

    # 두 목록을 합쳤으니 다시 최신순으로 세운다
    items.sort(key=notification_time, reverse=True)

    return items[:NOTIFICATION_LIMIT]


def notification_time(item: NotificationItem):
    """정렬용. 알림을 최신순으로 세운다."""
    return item.created_at


def shorten(text: str, limit: int = 40) -> str:
    """알림 본문에 넣을 만큼만 자른다."""
    text = text.replace("\n", " ").strip()

    if len(text) <= limit:
        return text

    return text[:limit] + "…"
