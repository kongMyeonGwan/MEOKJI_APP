"""인증 관련 비즈니스 로직."""

from fastapi import HTTPException, status
from sqlalchemy import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.core.security import hash_password
from app.models.user import User
from app.schemas.auth import RegisterRequest


def register_user(db: Session, payload: RegisterRequest) -> User:
    """회원가입. 이메일/닉네임 중복이면 409를 던진다."""

    # 1차: 사전 조회. 어느 쪽이 중복인지 사용자에게 정확히 알려주기 위함.
    existing = db.execute(
        select(User.email, User.nickname).where(
            (User.email == payload.email) | (User.nickname == payload.nickname)
        )
    ).first()

    if existing is not None:
        if existing.email == payload.email:
            raise HTTPException(status.HTTP_409_CONFLICT, "이미 사용 중인 이메일입니다.")
        raise HTTPException(status.HTTP_409_CONFLICT, "이미 사용 중인 닉네임입니다.")

    user = User(
        email=payload.email,
        password_hash=hash_password(payload.password),
        nickname=payload.nickname,
    )
    db.add(user)

    try:
        db.commit()
    except IntegrityError as exc:
        # 2차: 사전 조회와 INSERT 사이에 다른 요청이 끼어든 경우(경합).
        # Oracle은 ORA-00001을 던지며 메시지에 위반된 제약조건 이름이 들어있다.
        db.rollback()
        detail = str(exc.orig).upper()
        if "UQ_USERS_EMAIL" in detail:
            raise HTTPException(status.HTTP_409_CONFLICT, "이미 사용 중인 이메일입니다.") from exc
        if "UQ_USERS_NICKNAME" in detail:
            raise HTTPException(status.HTTP_409_CONFLICT, "이미 사용 중인 닉네임입니다.") from exc
        raise

    db.refresh(user)  # DB가 채운 id / created_at 을 읽어온다
    return user
