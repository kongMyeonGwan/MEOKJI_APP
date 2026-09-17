"""로그인한 사용자 찾아내기."""

from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from jwt import InvalidTokenError
from sqlalchemy.orm import Session

from app.core.security import decode_token
from app.database.database import get_db
from app.models.types import USE_YN_YES
from app.models.user import User

# 요청 헤더에서 "Authorization: Bearer <토큰>" 을 꺼내주는 도구.
bearer_scheme = HTTPBearer(auto_error=False)


def make_401(message: str) -> HTTPException:
    """401 (인증 실패) 에러를 만든다."""
    return HTTPException(
        status_code=status.HTTP_401_UNAUTHORIZED,
        detail=message,
        headers={"WWW-Authenticate": "Bearer"},
    )


def find_user_by_token(token: str, db: Session) -> User:
    """토큰 문자열을 받아서 User 를 찾아준다. 실패하면 401 을 던진다."""

    # 1) 토큰이 진짜 우리가 발급한 것인지 확인하고 내용물을 꺼낸다
    try:
        payload = decode_token(token)
    except InvalidTokenError:
        # 만료됐든, 서명이 안 맞든, 형식이 깨졌든 전부 여기로 온다.
        # 어느 쪽인지는 응답에 밝히지 않는다. 공격자에게 힌트가 되기 때문이다.
        raise make_401("토큰이 유효하지 않거나 만료되었습니다.")

    # 2) 토큰 안에 들어있는 사용자 id 를 꺼낸다
    #    JWT 규칙상 sub 는 문자열이라서 정수로 바꿔야 한다
    user_id_text = payload.get("sub")
    if user_id_text is None or not str(user_id_text).isdigit():
        raise make_401("토큰이 유효하지 않거나 만료되었습니다.")

    user_id = int(user_id_text)

    # 3) 그 id 의 사용자를 DB 에서 찾는다
    user = db.get(User, user_id)

    if user is None or user.use_yn != USE_YN_YES:
        # 토큰을 받은 뒤에 탈퇴한 경우. 서명은 멀쩡한데 주인이 없다.
        raise make_401("존재하지 않는 사용자입니다.")

    if user.is_locked:
        # 로그인한 뒤에 계정이 잠긴 경우.
        # 이미 받아둔 토큰으로 계속 쓰지 못하게 막는다.
        raise HTTPException(status.HTTP_423_LOCKED, "계정이 잠겼습니다.")

    return user


def get_current_user(
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer_scheme),
    db: Session = Depends(get_db),
) -> User:
    """로그인이 반드시 필요한 곳에서 쓴다. 토큰이 없으면 401."""
    if credentials is None:
        raise make_401("인증이 필요합니다.")

    # credentials.credentials 안에 "Bearer " 를 뗀 토큰 문자열만 들어있다
    return find_user_by_token(credentials.credentials, db)


def get_admin_user(current_user: User = Depends(get_current_user)) -> User:
    """관리자만 들어올 수 있는 곳에서 쓴다 (공지 작성 · 수정 · 삭제)."""
    if not current_user.is_admin:
        raise HTTPException(
            status.HTTP_403_FORBIDDEN, "관리자만 할 수 있는 작업입니다."
        )

    return current_user


def get_optional_user(
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer_scheme),
    db: Session = Depends(get_db),
) -> User | None:
    """로그인해도 되고 안 해도 되는 곳에서 쓴다 (게시글 목록 · 상세 등)."""
    if credentials is None:
        return None

    try:
        return find_user_by_token(credentials.credentials, db)
    except HTTPException:
        return None
