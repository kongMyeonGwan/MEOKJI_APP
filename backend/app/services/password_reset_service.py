"""비밀번호 재설정 (이메일 OTP).

흐름
----
    send    -> 6자리 코드 발급 · 해시 저장 · 이메일 발송
    verify  -> 코드 대조. 맞으면 used_at 기록
    change  -> used_at이 찍힌 코드에 한해 비밀번호 교체 후 코드 폐기

verify와 change를 나눈 이유
---------------------------
화면이 "인증번호 입력" -> "새 비밀번호 입력" 두 단계이기 때문이다.
나누면 change 단계에서 "이 요청이 인증을 통과한 요청인가"를 판별할 근거가
필요한데, 그게 used_at 컬럼이다. 없으면 같은 코드로 비밀번호를 몇 번이든
바꿀 수 있어 OTP의 일회성이 깨진다.
"""

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
# "코드가 틀렸다" / "만료됐다" / "그런 요청이 없다"를 구분해서 알려주면
# 공격자가 유효한 코드를 좁혀나가는 데 쓸 수 있다.
INVALID_CODE = "인증번호가 올바르지 않거나 만료되었습니다."


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


def send_reset_code(db: Session, email: str) -> None:
    """인증번호를 발급하고 이메일로 보낸다.

    가입되지 않은 이메일이어도 아무 일 없었다는 듯 조용히 끝낸다.
    404를 주면 로그인 폼을 거치지 않고도 가입 여부를 확인할 수 있게 된다.
    """
    user = find_active_user(db, email)
    if user is None:
        return

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
