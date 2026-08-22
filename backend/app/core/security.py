"""비밀번호 해싱 / 검증 · JWT 토큰 발급 / 해석.

passlib은 2020년 이후 릴리스가 없어 bcrypt 5.x와 호환되지 않는다
(AttributeError: module 'bcrypt' has no attribute '__about__').
그래서 bcrypt를 직접 사용한다.
"""

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


# =========================================================
# JWT (STEP 6)
#
# Refresh Token은 쓰지 않는다. Access Token 하나만 발급하고
# 만료되면 다시 로그인한다(JWT_EXPIRE_MINUTES, 기본 30분).
# =========================================================


def create_access_token(user_id: int) -> str:
    """사용자 id를 담은 Access Token을 발급한다.

    payload에 email/nickname을 넣지 않는다. JWT는 서명만 되어 있을 뿐
    누구나 내용을 디코딩해 읽을 수 있고, 닉네임이 바뀌어도 토큰은 그대로라
    낡은 값이 돌아다니게 된다. 식별자만 넣고 나머지는 그때그때 DB에서 읽는다.
    """
    now = datetime.now(timezone.utc)
    payload = {
        # RFC 7519상 sub는 문자열이어야 한다. PyJWT 2.10+는 int를 넣으면 거부한다.
        "sub": str(user_id),
        "iat": now,
        "exp": now + timedelta(minutes=settings.JWT_EXPIRE_MINUTES),
    }
    return jwt.encode(payload, settings.JWT_SECRET_KEY, algorithm=settings.JWT_ALGORITHM)


def decode_token(token: str) -> dict:
    """토큰을 검증하고 payload를 돌려준다.

    서명 불일치·만료·형식 오류는 모두 jwt.InvalidTokenError의 하위 예외다.
    호출하는 쪽(core/deps.py)에서 401로 변환한다.

    algorithms를 명시적으로 넘기는 것이 중요하다. 생략하면 토큰 헤더의
    alg를 그대로 신뢰해 alg=none 위조 토큰을 통과시킬 수 있다.
    """
    return jwt.decode(
        token,
        settings.JWT_SECRET_KEY,
        algorithms=[settings.JWT_ALGORITHM],
    )


# =========================================================
# 이메일 OTP (STEP 8)
# =========================================================

OTP_LENGTH = 6


def generate_otp_code() -> str:
    """6자리 숫자 인증번호를 만든다.

    random 모듈을 쓰면 안 된다. 시드를 알면 다음 값을 예측할 수 있다.
    secrets는 OS의 암호학적 난수를 쓴다.
    """
    return f"{secrets.randbelow(10**OTP_LENGTH):0{OTP_LENGTH}d}"


def hash_otp_code(code: str) -> str:
    """인증번호를 저장용 해시로 바꾼다.

    비밀번호와 같은 bcrypt를 쓴다. 6자리는 경우의 수가 100만뿐이라
    해시만으로는 금방 뚫리지만, 다음 세 가지로 막는다.
      - 유효시간 5분 (OTP_EXPIRE_MINUTES)
      - 시도 5회 제한 (OTP_MAX_ATTEMPT)
      - bcrypt 자체가 느리다
    """
    return hash_password(code)


def verify_otp_code(code: str, code_hash: str) -> bool:
    """입력된 인증번호와 저장된 해시를 대조한다."""
    return verify_password(code, code_hash)
