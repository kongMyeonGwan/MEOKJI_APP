"""인증 관련 비즈니스 로직."""

from datetime import datetime, timezone

from fastapi import HTTPException, status
from sqlalchemy import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.core.config import settings
from app.core.security import hash_password, verify_password
from app.data.regions import find_coords
from app.models.types import USE_YN_YES
from app.models.user import User
from app.schemas.auth import LoginRequest, RegisterRequest


def register_user(db: Session, payload: RegisterRequest) -> User:
    """회원가입. 이메일/닉네임 중복이면 409를 던진다."""

    # 1차: 사전 조회. 어느 쪽이 중복인지 사용자에게 정확히 알려주기 위함.
    existing = db.execute(
        select(User.email, User.nickname).where(
            User.use_yn == USE_YN_YES,
            (User.email == payload.email) | (User.nickname == payload.nickname),
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
        # 아래 넷은 선택 항목이라 안 보내면 None 이 들어간다.
        # 주소는 대시보드 첫 화면의 날씨를 좌표로 조회할 때 쓴다.
        gender=payload.gender,
        address_sido=payload.address_sido,
        address_sigungu=payload.address_sigungu,
    )

    # 고른 주소를 좌표로 바꿔 함께 저장한다.
    set_coords_from_address(user)
    db.add(user)

    try:
        db.commit()
    except IntegrityError as exc:
        # 2차: 사전 조회와 INSERT 사이에 다른 요청이 끼어든 경우(경합).
        # Oracle은 ORA-00001을 던지며 메시지에 위반된 제약조건 이름이 들어있다.
        db.rollback()
        error_message = str(exc.orig).upper()

        if "UQ_USERS_EMAIL" in error_message:
            raise HTTPException(status.HTTP_409_CONFLICT, "이미 사용 중인 이메일입니다.")
        if "UQ_USERS_NICKNAME" in error_message:
            raise HTTPException(status.HTTP_409_CONFLICT, "이미 사용 중인 닉네임입니다.")

        # 그 외의 DB 에러는 그대로 올린다 (숨기면 원인을 못 찾는다)
        raise

    db.refresh(user)  # DB가 채운 id / created_at 을 읽어온다
    return user


INVALID_CREDENTIALS = "이메일 또는 비밀번호가 올바르지 않습니다."


def set_coords_from_address(user: User) -> None:
    """회원의 주소를 좌표로 바꿔 latitude / longitude 에 채운다."""
    coords = find_coords(user.address_sido, user.address_sigungu)

    if coords is None:
        user.latitude = None
        user.longitude = None
        return

    user.latitude, user.longitude = coords


def login_user(db: Session, payload: LoginRequest) -> User:
    """이메일 + 비밀번호를 검증하고 사용자를 돌려준다."""
    user = db.execute(
        select(User).where(User.email == payload.email, User.use_yn == USE_YN_YES)
    ).scalar_one_or_none()

    if user is None:
        # 가입되지 않았거나 탈퇴한 이메일. 실패 횟수를 셀 대상 자체가 없다.
        raise HTTPException(status.HTTP_401_UNAUTHORIZED, INVALID_CREDENTIALS)

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
        remaining = settings.MAX_LOGIN_FAIL_COUNT - user.login_fail_count
        raise HTTPException(
            status.HTTP_401_UNAUTHORIZED,
            f"{INVALID_CREDENTIALS} (남은 시도 {remaining}회)",
        )

    # 로그인 성공. 그동안 쌓인 실패 기록을 지운다.
    # 실패가 없었으면 UPDATE를 보내지 않는다(로그인마다 불필요한 쓰기 방지).
    if user.login_fail_count != 0:
        user.login_fail_count = 0
        db.commit()

    return user
