"""Ban 재료 비즈니스 로직 (STEP 9).

세 함수 모두 첫 인자로 로그인 사용자를 받는다.
"남의 Ban을 건드릴 수 없다"는 규칙을 라우터가 아니라 여기서 지킨다.
"""

from fastapi import HTTPException, status
from sqlalchemy import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.models.types import USE_YN_NO, USE_YN_YES
from app.models.user import User
from app.models.user_ban import UserBan
from app.schemas.ban import BanCreateRequest


def list_bans(db: Session, user: User) -> list[UserBan]:
    """내 Ban 중 해제하지 않은 것을 등록 순으로 돌려준다.

    uq_user_bans는 CASE WHEN use_yn='Y' 로 감싼 함수 기반 인덱스라
    이 조회에 쓰이지 않는다. 대신 idx_user_bans_user(user_id, use_yn)를 탄다.

    해제한 재료(use_yn='N')는 행이 남아 있으므로 반드시 걸러야 한다.
    개수가 많아야 수십 개라 페이징하지 않는다.
    """
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
        # 중복이면 Oracle 이 ORA-00001 을 던지는데,
        # 그 에러 메시지 안에 위반된 제약조건 이름이 들어있다.
        if "UQ_USER_BANS" in str(exc.orig).upper():
            raise HTTPException(status.HTTP_409_CONFLICT, "이미 등록한 재료입니다.")

        # 그 외의 DB 에러는 그대로 올린다 (숨기면 원인을 못 찾는다)
        raise

    db.refresh(ban)  # DB가 채운 id / created_at 을 읽어온다
    return ban


def delete_ban(db: Session, user: User, ban_id: int) -> None:
    """내 Ban 하나를 해제한다.

    행을 지우지 않고 use_yn='N'으로 바꾸는 논리 삭제다.
    uq_user_bans 인덱스가 use_yn='Y'인 행만 보기 때문에,
    해제한 재료를 나중에 다시 등록해도 ORA-00001이 나지 않는다.

    남의 Ban이면 403이 아니라 404를 준다. 403으로 구분해주면
    "그 id는 존재한다"는 사실이 응답으로 새기 때문이다.
    이미 해제한 Ban도 같은 이유로 404로 취급한다.
    """
    ban = db.get(UserBan, ban_id)

    if ban is None or ban.user_id != user.id or ban.use_yn != USE_YN_YES:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "등록되지 않은 재료입니다.")

    ban.use_yn = USE_YN_NO
    db.commit()
