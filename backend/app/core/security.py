"""비밀번호 해싱 / 검증 · JWT 토큰 발급 / 해석."""

import secrets
from datetime import datetime, timedelta, timezone

import bcrypt
import jwt

from app.core.config import settings

# bcrypt 알고리즘 자체의 제약. 72바이트를 넘는 입력은 라이브러리가 거부한다.
# UTF-8에서 한글 1자 = 3바이트이므로 한글 비밀번호는 24자가 상한이다.
BCRYPT_MAX_BYTES = 72


def hash_password(plain_password: str) -> str:
    """평문 비밀번호를 bcrypt 해시 문자열로 변환한다."""
    pw_bytes = plain_password.encode("utf-8")
    if len(pw_bytes) > BCRYPT_MAX_BYTES:
        # 스키마 계층에서 이미 걸러지지만, 서비스가 직접 호출할 경우를 대비한 방어선.
        # 조용히 자르면 서로 다른 비밀번호가 같은 해시를 갖게 되므로 예외를 던진다.
        raise ValueError(
            f"비밀번호가 너무 깁니다. UTF-8 기준 {BCRYPT_MAX_BYTES}바이트 이하여야 합니다."
        )
    return bcrypt.hashpw(pw_bytes, bcrypt.gensalt()).decode("utf-8")


def verify_password(plain_password: str, password_hash: str) -> bool:
    """평문과 저장된 해시를 대조한다. 해시를 복호화하지 않는다."""
    pw_bytes = plain_password.encode("utf-8")
    if len(pw_bytes) > BCRYPT_MAX_BYTES:
        return False
    try:
        return bcrypt.checkpw(pw_bytes, password_hash.encode("utf-8"))
    except ValueError:
        # DB의 해시가 손상된 경우 등. 인증 실패로 처리한다.
        return False


# JWT (STEP 6)


def create_access_token(user_id: int) -> str:
    """사용자 id를 담은 Access Token을 발급한다."""
    now = datetime.now(timezone.utc)
    payload = {
        # RFC 7519상 sub는 문자열이어야 한다. PyJWT 2.10+는 int를 넣으면 거부한다.
        "sub": str(user_id),
        "iat": now,
        "exp": now + timedelta(minutes=settings.JWT_EXPIRE_MINUTES),
    }
    return jwt.encode(payload, settings.JWT_SECRET_KEY, algorithm=settings.JWT_ALGORITHM)


def decode_token(token: str) -> dict:
    """토큰을 검증하고 payload를 돌려준다."""
    return jwt.decode(
        token,
        settings.JWT_SECRET_KEY,
        algorithms=[settings.JWT_ALGORITHM],
    )


# 이메일 OTP (STEP 8)

OTP_LENGTH = 6


def generate_otp_code() -> str:
    """6자리 숫자 인증번호를 만든다."""
    return f"{secrets.randbelow(10**OTP_LENGTH):0{OTP_LENGTH}d}"


def hash_otp_code(code: str) -> str:
    """인증번호를 저장용 해시로 바꾼다."""
    return hash_password(code)


def verify_otp_code(code: str, code_hash: str) -> bool:
    """입력된 인증번호와 저장된 해시를 대조한다."""
    return verify_password(code, code_hash)
