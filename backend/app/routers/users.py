"""내 정보 · 알림 · 회원 탈퇴 라우터."""

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
    summary="내 정보",
    responses={401: AUTH_401, 423: AUTH_423},
)
def read_profile(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> ProfileResponse:
    """연속 방문일 · 요리 횟수는 DB 컬럼이 아니라 조회할 때 세서 계산한다."""
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
    """PUT 이 아니라 PATCH 다."""
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
    """알림 테이블은 없다."""
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
    """행을 지우지 않고 use_yn='N' 으로 바꾼다."""
    withdraw(db, current_user, payload.password)
