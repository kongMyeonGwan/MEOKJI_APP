"""Ban 재료 비즈니스 로직 (STEP 9)."""

from fastapi import HTTPException, status
from sqlalchemy import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.models.types import USE_YN_NO, USE_YN_YES
from app.models.user import User
from app.models.user_ban import UserBan
from app.schemas.ban import BanCreateRequest


def list_bans(db: Session, user: User) -> list[UserBan]:
    """내 Ban 중 해제하지 않은 것을 등록 순으로 돌려준다."""
    rows = db.execute(
        select(UserBan)
        .where(UserBan.user_id == user.id, UserBan.use_yn == USE_YN_YES)
        .order_by(UserBan.created_at, UserBan.id)
    )

    # .scalars() 는 "각 행의 첫 번째 값만 꺼내줘" 라는 뜻이다.
    # 이걸 안 쓰면 UserBan 이 아니라 (UserBan,) 처럼 튜플로 나온다.
    return list(rows.scalars().all())


def create_ban(db: Session, user: User, payload: BanCreateRequest) -> UserBan:
    """Ban을 추가한다. 같은 재료를 또 넣으면 409."""

    # 회원가입(register_user)과 달리 사전 조회를 하지 않는다.
    ban = UserBan(
        user_id=user.id,
        ingredient_name=payload.ingredient_name,
        ban_type=payload.ban_type,
    )
    db.add(ban)

    try:
        db.commit()
    except IntegrityError as exc:
        db.rollback()
        # 중복이면 Oracle 이 ORA-00001 을 던지는데,
        # 그 에러 메시지 안에 위반된 제약조건 이름이 들어있다.
        if "UQ_USER_BANS" in str(exc.orig).upper():
            raise HTTPException(status.HTTP_409_CONFLICT, "이미 등록한 재료입니다.")

        # 그 외의 DB 에러는 그대로 올린다 (숨기면 원인을 못 찾는다)
        raise

    db.refresh(ban)  # DB가 채운 id / created_at 을 읽어온다
    return ban


def delete_ban(db: Session, user: User, ban_id: int) -> None:
    """내 Ban 하나를 해제한다."""
    ban = db.get(UserBan, ban_id)

    if ban is None or ban.user_id != user.id or ban.use_yn != USE_YN_YES:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "등록되지 않은 재료입니다.")

    ban.use_yn = USE_YN_NO
    db.commit()
