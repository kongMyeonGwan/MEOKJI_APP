"""인증 라우터."""

from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session

from app.core.config import settings
from app.core.deps import get_current_user
from app.core.security import create_access_token
from app.database.database import get_db
from app.models.user import User
from app.schemas.auth import (
    LoginRequest,
    MessageResponse,
    PasswordResetChangeRequest,
    PasswordResetSendRequest,
    PasswordResetVerifyRequest,
    RegisterRequest,
    TokenResponse,
    UserResponse,
)
from app.services.auth_service import login_user, register_user
from app.services.password_reset_service import (
    change_password,
    send_reset_code,
    verify_reset_code,
)

router = APIRouter(prefix="/api/auth", tags=["auth"])


@router.post(
    "/register",
    response_model=UserResponse,
    status_code=status.HTTP_201_CREATED,
    summary="회원가입",
)
def register(payload: RegisterRequest, db: Session = Depends(get_db)) -> UserResponse:
    user = register_user(db, payload)
    return UserResponse.model_validate(user)


@router.post(
    "/login",
    response_model=TokenResponse,
    summary="로그인",
    responses={
        401: {"description": "이메일 또는 비밀번호 불일치"},
        423: {"description": "잠긴 계정"},
    },
)
def login(payload: LoginRequest, db: Session = Depends(get_db)) -> TokenResponse:
    """이메일 + 비밀번호로 Access Token을 발급한다."""
    user = login_user(db, payload)
    return TokenResponse(
        access_token=create_access_token(user.id),
        expires_in=settings.JWT_EXPIRE_MINUTES * 60,
        user=UserResponse.model_validate(user),
    )


@router.get(
    "/me",
    response_model=UserResponse,
    summary="내 정보 조회",
    responses={401: {"description": "토큰 없음 · 만료 · 위조"}},
)
def read_me(current_user: User = Depends(get_current_user)) -> UserResponse:
    """토큰이 아직 유효한지 확인하는 용도로도 쓴다."""
    return UserResponse.model_validate(current_user)


# 비밀번호 재설정 (STEP 8)


@router.post(
    "/password-reset/send",
    response_model=MessageResponse,
    summary="비밀번호 재설정 — 인증번호 발송",
    responses={
        404: {"description": "가입되지 않은 이메일 (탈퇴 회원 포함)"},
        429: {"description": "방금 보냈음 — Retry-After 초 뒤에 다시"},
    },
)
def password_reset_send(
    payload: PasswordResetSendRequest, db: Session = Depends(get_db)
) -> MessageResponse:
    """이메일로 6자리 인증번호를 보낸다."""
    send_reset_code(db, payload.email)
    return MessageResponse(
        message="입력하신 이메일로 인증번호를 보냈습니다. 5분 안에 입력해주세요."
    )


@router.post(
    "/password-reset/verify",
    response_model=MessageResponse,
    summary="비밀번호 재설정 — 인증번호 확인",
    responses={
        400: {"description": "인증번호 불일치 · 만료"},
        429: {"description": "시도 횟수 초과"},
    },
)
def password_reset_verify(
    payload: PasswordResetVerifyRequest, db: Session = Depends(get_db)
) -> MessageResponse:
    verify_reset_code(db, payload.email, payload.code)
    return MessageResponse(message="인증되었습니다. 새 비밀번호를 입력해주세요.")


@router.post(
    "/password-reset/change",
    response_model=MessageResponse,
    summary="비밀번호 재설정 — 새 비밀번호로 교체",
    responses={400: {"description": "검증되지 않은 요청 · 만료"}},
)
def password_reset_change(
    payload: PasswordResetChangeRequest, db: Session = Depends(get_db)
) -> MessageResponse:
    """비밀번호를 바꾸고 계정 잠금을 함께 해제한다."""
    change_password(db, payload.email, payload.code, payload.new_password)
    return MessageResponse(message="비밀번호가 변경되었습니다. 다시 로그인해주세요.")
