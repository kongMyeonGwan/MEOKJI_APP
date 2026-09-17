"""내 정보 · 알림 · 회원 탈퇴 로직."""

from fastapi import HTTPException, status
from sqlalchemy import select, update
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.core.security import hash_password, verify_password
from app.services.auth_service import set_coords_from_address
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

# 알림에 한 번에 담을 최대 건수. 상단바 패널이 그보다 길어지면 어차피 안 읽는다.
NOTIFICATION_LIMIT = 20


def get_profile(db: Session, user: User) -> ProfileResponse:
    """마이페이지 · 사이드바가 함께 쓰는 내 정보를 만든다."""
    stats = get_stats(db, user)

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
        streak_days=stats.streak_days,
        cook_count=stats.count,
    )


def to_float(value) -> float | None:
    """Decimal 또는 None 을 float 또는 None 으로."""
    if value is None:
        return None

    return float(value)


def update_profile(
    db: Session, user: User, payload: ProfileUpdateRequest
) -> ProfileResponse:
    """내 정보를 고친다."""
    changes = payload.model_dump(exclude_unset=True)

    for field, value in changes.items():
        setattr(user, field, value)

    # 주소를 건드렸으면 좌표도 다시 맞춘다.
    # 안 하면 이사한 뒤에도 예전 동네 날씨로 추천이 나간다.
    if "address_sido" in changes or "address_sigungu" in changes:
        set_coords_from_address(user)

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
    """로그인한 상태에서 비밀번호를 바꾼다."""
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
    """회원 탈퇴."""
    if not verify_password(password, user.password_hash):
        raise HTTPException(
            status.HTTP_400_BAD_REQUEST, "비밀번호가 올바르지 않습니다."
        )

    # 자식부터 지운다. 순서가 중요한 건 아니지만, 중간에 실패했을 때
    for model in (ReviewLike, ReviewComment, RecipeReview, MealHistory, UserBan):
        db.execute(
            update(model)
            .where(model.user_id == user.id, model.use_yn == USE_YN_YES)
            .values(use_yn=USE_YN_NO)
        )

    user.use_yn = USE_YN_NO
    db.commit()

    # 토큰은 따로 무효화하지 않는다. JWT 는 서버가 발급 목록을 들고 있지


def list_notifications(db: Session, user: User) -> list[NotificationItem]:
    """내 글에 달린 댓글과 좋아요를 알림으로 모아준다."""
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
