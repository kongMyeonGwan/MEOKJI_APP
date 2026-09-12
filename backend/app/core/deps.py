"""로그인한 사용자 찾아내기.

요청 헤더의 토큰을 읽어서 "지금 이 요청을 보낸 사람이 누구인가"를 알아낸다.
라우터는 아래 둘 중 하나를 Depends 로 받기만 하면 되고,
토큰을 직접 뜯어보거나 DB 를 조회할 필요가 없다.

    get_current_user    로그인 필수. 토큰이 없거나 잘못됐으면 401 에러
    get_optional_user   로그인 선택. 없으면 None (게시판을 Guest 도 볼 수 있게)
    get_admin_user      관리자 전용. 로그인은 했지만 관리자가 아니면 403

Depends 가 뭔가요
-----------------
FastAPI 에게 "이 함수를 먼저 실행하고, 그 결과를 내 매개변수에 넣어줘" 라고
부탁하는 것이다. 라우터에 이렇게 쓰면

    def read_me(current_user: User = Depends(get_current_user)):

FastAPI 가 read_me 를 부르기 전에 get_current_user 를 먼저 실행해서,
그 반환값을 current_user 에 넣어준다. 401 이 나면 read_me 는 아예 실행되지 않는다.
"""

from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from jwt import InvalidTokenError
from sqlalchemy.orm import Session

from app.core.security import decode_token
from app.database.database import get_db
from app.models.types import USE_YN_YES
from app.models.user import User

# 요청 헤더에서 "Authorization: Bearer <토큰>" 을 꺼내주는 도구.
#
# auto_error=False 로 둔 이유:
# 기본값(True)이면 헤더가 없을 때 FastAPI 가 곧바로 403 을 내버린다.
# 그러면 "로그인 안 해도 되는" get_optional_user 를 만들 수가 없다.
# False 로 두면 헤더가 없을 때 None 을 넘겨주므로, 401 을 낼지 말지 우리가 정한다.
bearer_scheme = HTTPBearer(auto_error=False)


def make_401(message: str) -> HTTPException:
    """401 (인증 실패) 에러를 만든다.

    WWW-Authenticate 헤더는 HTTP 규칙상 401 응답에 넣게 되어 있는 값이다.
    """
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
        # 탈퇴는 행을 지우는 게 아니라 use_yn 을 'N' 으로 바꾸는 것이라서
        # db.get() 만으로는 안 걸러진다. 값을 직접 확인해야 한다.
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
    """관리자만 들어올 수 있는 곳에서 쓴다 (공지 작성 · 수정 · 삭제).

    get_current_user 를 Depends 로 받는다. 토큰 검사 · 탈퇴 확인 · 계정 잠금을
    거기서 이미 다 하므로, 여기는 등급 한 줄만 더 본다.
    그래서 로그인하지 않았으면 403 이 아니라 401 이 먼저 나간다 — 맞는 순서다.
    "누구인지 모르겠다(401)" 와 "누구인지는 알지만 권한이 없다(403)" 는 다르다.

    왜 404 가 아니라 403 인가
    -------------------------
    게시판의 '남의 글 수정' 은 404 로 돌려준다. 403 을 주면 "그 번호의 글은
    존재한다" 는 사실이 새어나가기 때문이다.

    공지는 그 걱정이 없다. 목록이 Guest 에게도 통째로 열려 있어서 몇 번
    공지가 있는지는 이미 공개된 정보다. 숨길 것이 없으면 사실대로 알려주는
    편이 낫다 — 404 로 받으면 화면이 "없는 공지" 라고 잘못 안내하게 된다.
    """
    if not current_user.is_admin:
        raise HTTPException(
            status.HTTP_403_FORBIDDEN, "관리자만 할 수 있는 작업입니다."
        )

    return current_user


def get_optional_user(
    credentials: HTTPAuthorizationCredentials | None = Depends(bearer_scheme),
    db: Session = Depends(get_db),
) -> User | None:
    """로그인해도 되고 안 해도 되는 곳에서 쓴다 (게시글 목록 · 상세 등).

    토큰이 잘못됐거나 만료됐을 때도 401 을 내지 않고 None(Guest) 으로 넘어간다.
    누구나 볼 수 있는 화면인데 만료된 토큰 하나 때문에 에러 페이지가 뜨면 안 되기 때문이다.
    로그인이 꼭 필요한 동작(글쓰기 · 좋아요)은 get_current_user 를 쓰므로
    거기서 제대로 401 이 나간다.
    """
    if credentials is None:
        return None

    try:
        return find_user_by_token(credentials.credentials, db)
    except HTTPException:
        return None
