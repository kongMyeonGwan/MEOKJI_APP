"""Ban 재료 비즈니스 로직 (STEP 9).

세 함수 모두 첫 인자로 로그인 사용자를 받는다.
"남의 Ban을 건드릴 수 없다"는 규칙을 라우터가 아니라 여기서 지킨다.
"""

from fastapi import HTTPException, status
from sqlalchemy import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.models.user import User
from app.models.user_ban import UserBan
from app.schemas.ban import BanCreateRequest


def list_bans(db: Session, user: User) -> list[UserBan]:
    """내 Ban 전체를 등록 순으로 돌려준다.

    uq_user_bans(user_id, ingredient_name) 인덱스의 선두 컬럼이 user_id라
    이 조회는 그 인덱스를 탄다. 별도 인덱스가 필요 없다.

    개수가 많아야 수십 개라 페이징하지 않는다.
    """
    return list(
        db.execute(
            select(UserBan)
            .where(UserBan.user_id == user.id)
            .order_by(UserBan.created_at, UserBan.id)
        )
        .scalars()
        .all()
    )


def create_ban(db: Session, user: User, payload: BanCreateRequest) -> UserBan:
    """Ban을 추가한다. 같은 재료를 또 넣으면 409."""

    # 회원가입(register_user)과 달리 사전 조회를 하지 않는다.
    # UNIQUE 제약이 하나뿐이라 IntegrityError가 곧 "재료명 중복"이고,
    # 사전 조회는 SELECT-INSERT 사이의 경합을 못 막으면서 쿼리만 늘린다.
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
        # Oracle은 ORA-00001을 던지며 메시지에 위반된 제약조건 이름이 들어있다.
        if "UQ_USER_BANS" in str(exc.orig).upper():
            raise HTTPException(
                status.HTTP_409_CONFLICT, "이미 등록한 재료입니다."
            ) from exc
        raise

    db.refresh(ban)  # DB가 채운 id / created_at 을 읽어온다
    return ban


def delete_ban(db: Session, user: User, ban_id: int) -> None:
    """내 Ban 하나를 지운다.

    남의 Ban이면 403이 아니라 404를 준다. 403으로 구분해주면
    "그 id는 존재한다"는 사실이 응답으로 새기 때문이다.
    """
    ban = db.get(UserBan, ban_id)

    if ban is None or ban.user_id != user.id:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "등록되지 않은 재료입니다.")

    db.delete(ban)
    db.commit()
