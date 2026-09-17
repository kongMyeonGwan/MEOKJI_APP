"""비밀번호 재설정 (이메일 OTP)."""

from datetime import datetime, timedelta, timezone

from fastapi import HTTPException, status
from sqlalchemy import delete, select
from sqlalchemy.orm import Session

from app.core.config import settings
from app.core.security import (
    generate_otp_code,
    hash_otp_code,
    hash_password,
    verify_otp_code,
)
from app.integrations.email_sender import send_verification_code
from app.models.types import USE_YN_YES
from app.models.user import User
from app.models.verification_code import PURPOSE_PASSWORD_RESET, VerificationCode

# verify / change 단계의 실패 문구는 하나로 통일한다.
INVALID_CODE = "인증번호가 올바르지 않거나 만료되었습니다."

# send 단계에서 가입 여부를 알려준다.
NOT_REGISTERED = "가입되지 않은 회원입니다."


def find_active_user(db: Session, email: str) -> User | None:
    # 탈퇴한 회원에게는 재설정 메일을 보내지 않는다.
    return db.execute(
        select(User).where(User.email == email, User.use_yn == USE_YN_YES)
    ).scalar_one_or_none()


def find_latest_code(db: Session, user_id: int) -> VerificationCode | None:
    """해당 사용자의 가장 최근 비밀번호 재설정 코드."""
    return db.execute(
        select(VerificationCode)
        .where(
            VerificationCode.user_id == user_id,
            VerificationCode.purpose == PURPOSE_PASSWORD_RESET,
        )
        .order_by(VerificationCode.created_at.desc(), VerificationCode.id.desc())
        .limit(1)
    ).scalar_one_or_none()


def raise_if_too_soon(db: Session, user_id: int) -> None:
    """방금 보냈으면 429 로 끊는다."""
    latest = find_latest_code(db, user_id)
    if latest is None:
        return

    cooldown = timedelta(seconds=settings.OTP_RESEND_COOLDOWN_SECONDS)
    ready_at = latest.created_at + cooldown
    remain = ready_at - datetime.now(timezone.utc)

    if remain <= timedelta(0):
        return

    seconds = max(1, int(remain.total_seconds()))

    raise HTTPException(
        status.HTTP_429_TOO_MANY_REQUESTS,
        f"인증번호를 방금 보냈습니다. {seconds}초 후에 다시 요청해주세요.",
        # 429 에 붙이게 되어 있는 표준 헤더다. 화면이 문구를 파싱하지 않고
        # 이 값으로 남은 시간을 셀 수 있다.
        headers={"Retry-After": str(seconds)},
    )


def send_reset_code(db: Session, email: str) -> None:
    """인증번호를 발급하고 이메일로 보낸다."""
    user = find_active_user(db, email)
    if user is None:
        raise HTTPException(status.HTTP_404_NOT_FOUND, NOT_REGISTERED)

    raise_if_too_soon(db, user.id)

    # 이전에 발급한 코드는 전부 버린다.
    # 남겨두면 옛날 코드로도 인증이 되어 유효 코드가 여러 개 살아있게 된다.
    db.execute(
        delete(VerificationCode).where(
            VerificationCode.user_id == user.id,
            VerificationCode.purpose == PURPOSE_PASSWORD_RESET,
        )
    )

    code = generate_otp_code()
    db.add(
        VerificationCode(
            user_id=user.id,
            code_hash=hash_otp_code(code),
            purpose=PURPOSE_PASSWORD_RESET,
            expires_at=datetime.now(timezone.utc)
            + timedelta(minutes=settings.OTP_EXPIRE_MINUTES),
        )
    )
    db.commit()

    # 메일 발송은 커밋 이후에 한다. 발송에 성공했는데 DB 저장이 롤백되면
    # 사용자는 코드를 받았는데 서버는 모르는 상태가 된다.
    send_verification_code(
        to=user.email, code=code, expire_minutes=settings.OTP_EXPIRE_MINUTES
    )


def verify_reset_code(db: Session, email: str, code: str) -> None:
    """인증번호를 확인하고 used_at을 찍는다."""
    user = find_active_user(db, email)
    if user is None:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, INVALID_CODE)

    record = find_latest_code(db, user.id)
    if record is None or record.used_at is not None:
        # 이미 검증에 성공한 코드는 다시 쓸 수 없다.
        raise HTTPException(status.HTTP_400_BAD_REQUEST, INVALID_CODE)

    if record.expires_at <= datetime.now(timezone.utc):
        raise HTTPException(status.HTTP_400_BAD_REQUEST, INVALID_CODE)

    if record.attempt_count >= settings.OTP_MAX_ATTEMPT:
        # 100만 분의 1을 무작위로 맞히려는 시도를 여기서 끊는다.
        raise HTTPException(
            status.HTTP_429_TOO_MANY_REQUESTS,
            "인증 시도 횟수를 초과했습니다. 인증번호를 다시 요청해주세요.",
        )

    if not verify_otp_code(code, record.code_hash):
        record.attempt_count += 1
        db.commit()

        remaining = settings.OTP_MAX_ATTEMPT - record.attempt_count
        if remaining <= 0:
            # "남은 시도 0회"라고만 하면 다음에 뭘 해야 할지 알 수 없다.
            raise HTTPException(
                status.HTTP_429_TOO_MANY_REQUESTS,
                "인증 시도 횟수를 초과했습니다. 인증번호를 다시 요청해주세요.",
            )
        raise HTTPException(
            status.HTTP_400_BAD_REQUEST,
            f"{INVALID_CODE} (남은 시도 {remaining}회)",
        )

    record.used_at = datetime.now(timezone.utc)
    db.commit()


def change_password(db: Session, email: str, code: str, new_password: str) -> None:
    """검증을 통과한 코드로 비밀번호를 교체하고 계정 잠금을 푼다."""
    user = find_active_user(db, email)
    if user is None:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, INVALID_CODE)

    record = find_latest_code(db, user.id)

    # used_at이 NULL이면 verify를 건너뛰고 바로 change를 부른 것이다.
    if record is None or record.used_at is None:
        raise HTTPException(status.HTTP_400_BAD_REQUEST, INVALID_CODE)

    # verify 이후에도 만료는 계속 적용된다.
    # 아니면 인증만 해두고 몇 시간 뒤에 비밀번호를 바꿀 수 있게 된다.
    if record.expires_at <= datetime.now(timezone.utc):
        raise HTTPException(status.HTTP_400_BAD_REQUEST, INVALID_CODE)

    if not verify_otp_code(code, record.code_hash):
        raise HTTPException(status.HTTP_400_BAD_REQUEST, INVALID_CODE)

    user.password_hash = hash_password(new_password)

    # 비밀번호 재설정이 계정 잠금(STEP 7)을 푸는 유일한 경로다.
    user.login_fail_count = 0
    user.is_locked = False
    user.locked_at = None

    # 코드를 지운다. 남겨두면 used_at이 찍힌 상태라 같은 코드로
    # 비밀번호를 계속 바꿀 수 있다.
    db.delete(record)
    db.commit()
