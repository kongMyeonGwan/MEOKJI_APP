"""인증 관련 비즈니스 로직."""

from datetime import datetime, timezone

from fastapi import HTTPException, status
from sqlalchemy import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.core.config import settings
from app.core.security import hash_password, verify_password
from app.models.user import User
from app.schemas.auth import LoginRequest, RegisterRequest


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


_INVALID_CREDENTIALS = "이메일 또는 비밀번호가 올바르지 않습니다."


def login_user(db: Session, payload: LoginRequest) -> User:
    """이메일 + 비밀번호를 검증하고 사용자를 돌려준다.

    토큰 발급은 라우터가 담당한다. 이 함수는 "이 사람이 맞는가"만 판정한다.

    실패하면 login_fail_count를 올리고, MAX_LOGIN_FAIL_COUNT(기본 20)에
    도달하면 계정을 잠근다. 잠금은 비밀번호 재설정(STEP 8)으로만 풀린다.
    """
    user = db.execute(
        select(User).where(User.email == payload.email)
    ).scalar_one_or_none()

    if user is None:
        # 가입되지 않은 이메일. 실패 횟수를 셀 대상 자체가 없다.
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, _INVALID_CREDENTIALS)

    # 이미 잠긴 계정은 비밀번호 검증까지 가지 않는다.
    # 여기서 막지 않으면 잠긴 뒤에도 계속 카운트가 올라간다.
    if user.is_locked:
        raise HTTPException(
            status.HTTP_423_LOCKED,
            "계정이 잠겼습니다. 비밀번호를 재설정해주세요.",
        )

    if not verify_password(payload.password, user.password_hash):
        user.login_fail_count += 1

        if user.login_fail_count >= settings.MAX_LOGIN_FAIL_COUNT:
            user.is_locked = True
            # DDL의 DEFAULT는 INSERT 시점에만 적용되므로 UPDATE에서는 직접 넣는다.
            # UtcTimestamp가 tz-aware -> naive UTC 변환을 담당한다.
            user.locked_at = datetime.now(timezone.utc)
            db.commit()
            raise HTTPException(
                status.HTTP_423_LOCKED,
                f"로그인 {settings.MAX_LOGIN_FAIL_COUNT}회 실패로 계정이 잠겼습니다. "
                "비밀번호를 재설정해주세요.",
            )

        db.commit()

        # 남은 횟수를 알려주기로 한 결정 (2026-08-17)
        # 트레이드오프: 존재하는 계정에만 이 문구가 붙으므로, 없는 이메일과
        # 응답을 비교하면 가입 여부를 알아낼 수 있다. 사용성을 택한 것이며
        # 계정 열거를 막아야 한다면 이 부분을 _INVALID_CREDENTIALS로 되돌리면 된다.
        remaining = settings.MAX_LOGIN_FAIL_COUNT - user.login_fail_count
        raise HTTPException(
            status.HTTP_401_UNAUTHORIZED,
            f"{_INVALID_CREDENTIALS} (남은 시도 {remaining}회)",
        )

    # 로그인 성공. 그동안 쌓인 실패 기록을 지운다.
    # 실패가 없었으면 UPDATE를 보내지 않는다(로그인마다 불필요한 쓰기 방지).
    if user.login_fail_count != 0:
        user.login_fail_count = 0
        db.commit()

    return user
