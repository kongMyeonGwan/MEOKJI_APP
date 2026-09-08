"""내 정보 · 요리 레벨 · 알림 · 회원 탈퇴 라우터.

전부 로그인이 필요하다. 남의 정보를 보는 경로는 만들지 않았다.

/api/auth/me 와 무엇이 다른가
-----------------------------
    /api/auth/me    토큰이 살아있는지 확인하는 용도. 가볍고 빠르다
    /api/users/me   요리 레벨 · 연속 방문일까지 계산해서 준다 (쿼리 4~5개)

새로고침할 때마다 부르는 것은 앞쪽이고, 마이페이지와 사이드바가 부르는 것은
뒤쪽이다. 하나로 합치면 화면을 열 때마다 필요 없는 COUNT 가 다섯 번 돈다.
"""

from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session

from app.core.deps import get_current_user
from app.database.database import get_db
from app.models.user import User
from app.schemas.auth import MessageResponse
from app.schemas.user import (
    NotificationItem,
    PasswordChangeRequest,
    ProfileResponse,
    ProfileUpdateRequest,
    WithdrawRequest,
)
from app.services.user_service import (
    change_my_password,
    get_profile,
    list_notifications,
    update_profile,
    withdraw,
)

router = APIRouter(prefix="/api/users", tags=["users"])

AUTH_401 = {"description": "토큰 없음 · 만료 · 위조"}
AUTH_423 = {"description": "잠긴 계정"}


@router.get(
    "/me",
    response_model=ProfileResponse,
    summary="내 정보 + 요리 레벨",
    responses={401: AUTH_401, 423: AUTH_423},
)
def read_profile(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> ProfileResponse:
    """레벨 · 경험치 · 연속 방문일은 DB 컬럼이 아니라 조회할 때 세서 계산한다."""
    return get_profile(db, current_user)


@router.patch(
    "/me",
    response_model=ProfileResponse,
    summary="내 정보 수정",
    responses={
        401: AUTH_401,
        423: AUTH_423,
        409: {"description": "이미 사용 중인 닉네임"},
        422: {"description": "빈 닉네임 · 좌표 범위 벗어남"},
    },
)
def edit_profile(
    payload: ProfileUpdateRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> ProfileResponse:
    """PUT 이 아니라 PATCH 다. 보낸 항목만 바꾸고 나머지는 그대로 둔다.

    PUT 으로 만들면 닉네임만 고치려 해도 주소를 전부 같이 보내야 하고,
    빠뜨리면 조용히 지워진다.
    """
    return update_profile(db, current_user, payload)


@router.post(
    "/me/password",
    response_model=MessageResponse,
    summary="비밀번호 변경 (로그인 상태)",
    responses={
        401: AUTH_401,
        423: AUTH_423,
        400: {"description": "현재 비밀번호 불일치 · 새 비밀번호가 기존과 같음"},
    },
)
def change_password(
    payload: PasswordChangeRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> MessageResponse:
    """비밀번호를 잊어버린 경우는 /api/auth/password-reset 쪽을 쓴다."""
    change_my_password(db, current_user, payload)
    return MessageResponse(message="비밀번호가 변경되었습니다.")


@router.get(
    "/me/notifications",
    response_model=list[NotificationItem],
    summary="내 알림 (내 글의 댓글 · 좋아요)",
    responses={401: AUTH_401, 423: AUTH_423},
)
def read_notifications(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> list[NotificationItem]:
    """알림 테이블은 없다. 조회 시점에 댓글 · 좋아요에서 만들어 낸다.

    "읽음" 여부는 서버가 모른다. 브라우저에 마지막으로 확인한 시각을
    저장해 두고 화면에서 판단한다 (frontend/src/contexts/NotifyContext.jsx).
    """
    return list_notifications(db, current_user)


@router.delete(
    "/me",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="회원 탈퇴",
    responses={
        401: AUTH_401,
        423: AUTH_423,
        400: {"description": "비밀번호 불일치"},
    },
)
def delete_me(
    payload: WithdrawRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> None:
    """행을 지우지 않고 use_yn='N' 으로 바꾼다.

    내가 쓴 글 · 댓글 · 좋아요 · 기록 · Ban 도 함께 'N' 이 된다.
    같은 이메일로 다시 가입할 수 있다 (uq_users_email 이 'Y' 인 행만 보므로).
    """
    withdraw(db, current_user, payload.password)
