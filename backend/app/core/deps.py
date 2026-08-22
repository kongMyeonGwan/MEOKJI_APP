"""FastAPI 의존성 — 요청의 Authorization 헤더에서 로그인 사용자를 찾아낸다.

두 가지를 제공한다.

    get_current_user   로그인 필수. 토큰이 없거나 잘못됐으면 401
    get_optional_user  로그인 선택. 없으면 None (게시판 Guest 조회용)

라우터는 이 둘 중 하나를 Depends로 받기만 하면 되고,
토큰 파싱이나 DB 조회를 직접 하지 않는다.
"""

from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from jwt import InvalidTokenError
from sqlalchemy.orm import Session

from app.core.security import decode_token
from app.database.database import get_db
from app.models.user import User

# auto_error=False : 헤더가 없을 때 FastAPI가 곧바로 403을 뱉지 않고
# credentials=None 으로 넘겨준다. get_optional_user가 성립하려면 필수다.
# 그 대신 "없을 때 401" 판단은 아래에서 직접 한다.
bearer_scheme = HTTPBearer(auto_error=False)


def _unauthorized(detail: str) -> HTTPException:
    """401 응답을 만든다.

    WWW-Authenticate 헤더는 HTTP 표준이 401에 요구하는 값이다.
    """
    return HTTPException(
        status.HTTP_401_UNAUTHORIZED,
        detail,
        headers={"WWW-Authenticate": "Bearer"},
    )


def _resolve_user(token: str, db: Session) -> User:
    """토큰 문자열 -> User. 실패하면 401을 던진다."""
    try:
        payload = decode_token(token)
    except InvalidTokenError as exc:
        # 만료(ExpiredSignatureError) · 서명 불일치 · 형식 오류가 모두 여기로 온다.
        # 어느 쪽인지 응답에 밝히지 않는다.
        raise _unauthorized("토큰이 유효하지 않거나 만료되었습니다.") from exc

    subject = payload.get("sub")
    try:
        user_id = int(subject)
    except (TypeError, ValueError) as exc:
        raise _unauthorized("토큰이 유효하지 않거나 만료되었습니다.") from exc

    user = db.get(User, user_id)
    if user is None:
        # 토큰 발급 후 탈퇴한 경우. 서명은 멀쩡하지만 주인이 없다.
        raise _unauthorized("존재하지 않는 사용자입니다.")

    if user.is_locked:
        # 로그인 이후에 계정이 잠긴 경우, 남은 토큰으로 계속 쓰지 못하게 막는다.
        raise HTTPException(status.HTTP_423_LOCKED, "계정이 잠겼습니다.")

    return user


def get_current_user(
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer_scheme),
    db: Session = Depends(get_db),
) -> User:
    """로그인 필수 엔드포인트용."""
    if credentials is None:
        raise _unauthorized("인증이 필요합니다.")
    return _resolve_user(credentials.credentials, db)


def get_optional_user(
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer_scheme),
    db: Session = Depends(get_db),
) -> User | None:
    """로그인 선택 엔드포인트용 (게시글 목록/상세 등).

    토큰이 잘못됐거나 만료됐을 때도 401이 아니라 None(Guest)으로 처리한다.
    Guest에게 열어둔 화면인데 만료된 토큰 하나 때문에 에러 페이지가 뜨면
    안 되기 때문이다. 로그인이 반드시 필요한 동작은 어차피
    get_current_user를 쓰므로 거기서 401이 나간다.
    """
    if credentials is None:
        return None
    try:
        return _resolve_user(credentials.credentials, db)
    except HTTPException:
        return None
