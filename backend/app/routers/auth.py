"""인증 라우터.

STEP 5: 회원가입
STEP 6~8에서 로그인 / 비밀번호 재설정이 여기에 추가된다.
"""

from fastapi import APIRouter, Depends, status
from sqlalchemy.orm import Session

from app.database.database import get_db
from app.schemas.auth import RegisterRequest, UserResponse
from app.services.auth_service import register_user

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
