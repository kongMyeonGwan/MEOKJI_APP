"""비밀번호 해싱 / 검증.

passlib은 2020년 이후 릴리스가 없어 bcrypt 5.x와 호환되지 않는다
(AttributeError: module 'bcrypt' has no attribute '__about__').
그래서 bcrypt를 직접 사용한다.
"""

import bcrypt

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
