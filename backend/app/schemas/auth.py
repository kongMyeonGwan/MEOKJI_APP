"""인증 관련 요청/응답 스키마."""

from datetime import datetime
from typing import Literal

from pydantic import BaseModel, ConfigDict, EmailStr, Field, field_validator

from app.core.security import BCRYPT_MAX_BYTES, OTP_LENGTH


def check_password_bytes(password: str) -> str:
    """비밀번호가 bcrypt 의 72바이트 제한을 넘지 않는지 검사한다."""
    byte_length = len(password.encode("utf-8"))

    if byte_length > BCRYPT_MAX_BYTES:
        raise ValueError(
            f"비밀번호는 UTF-8 기준 {BCRYPT_MAX_BYTES}바이트 이하여야 합니다 "
            "(한글은 1자당 3바이트)"
        )

    return password


class RegisterRequest(BaseModel):
    """회원가입 요청."""

    email: EmailStr = Field(..., max_length=255)
    # min_length=1 이 중요하다. Oracle 은 빈 문자열을 NULL 로 취급해서,
    # 빈 값을 그대로 넘기면 NOT NULL 위반으로 500 이 난다.
    password: str = Field(..., min_length=8, max_length=72)
    nickname: str = Field(..., min_length=1, max_length=50)

    # 아래 넷은 선택 항목이다. 안 보내면 NULL 로 들어간다.
    gender: Literal["MALE", "FEMALE", "OTHER"] | None = None
    address_sido: str | None = Field(None, max_length=30)
    address_sigungu: str | None = Field(None, max_length=30)

    @field_validator("password")
    @classmethod
    def validate_password(cls, password: str) -> str:
        """Field 규칙만으로 부족한 부분을 직접 검사한다."""
        return check_password_bytes(password)

    @field_validator("nickname")
    @classmethod
    def validate_nickname(cls, nickname: str) -> str:
        """공백만 입력한 닉네임을 거르고, 앞뒤 공백을 떼어낸다."""
        nickname = nickname.strip()

        if not nickname:
            raise ValueError("닉네임을 입력해주세요")

        return nickname


class LoginRequest(BaseModel):
    """로그인 요청."""

    email: EmailStr
    password: str = Field(..., min_length=1)


class UserResponse(BaseModel):
    """응답에 password_hash를 절대 포함하지 않는다."""

    model_config = ConfigDict(from_attributes=True)

    id: int
    email: EmailStr
    nickname: str
    created_at: datetime

    gender: str | None = None
    address_sido: str | None = None
    address_sigungu: str | None = None

    # 관리자인지. User 모델의 is_admin 프로퍼티(user_level == 1)에서 온다.
    is_admin: bool = False


class TokenResponse(BaseModel):
    """로그인 성공 응답."""

    access_token: str
    token_type: str = "bearer"
    expires_in: int  # 초 단위. 프론트가 만료 시각을 계산할 때 쓴다.
    user: UserResponse


# 비밀번호 재설정 (STEP 8)


class PasswordResetSendRequest(BaseModel):
    email: EmailStr


class PasswordResetVerifyRequest(BaseModel):
    email: EmailStr
    code: str = Field(..., min_length=OTP_LENGTH, max_length=OTP_LENGTH)


class PasswordResetChangeRequest(BaseModel):
    email: EmailStr
    code: str = Field(..., min_length=OTP_LENGTH, max_length=OTP_LENGTH)
    new_password: str = Field(..., min_length=8, max_length=72)

    @field_validator("new_password")
    @classmethod
    def validate_new_password(cls, password: str) -> str:
        """회원가입과 똑같은 규칙을 적용한다."""
        return check_password_bytes(password)


class MessageResponse(BaseModel):
    """본문에 담을 게 안내 문구뿐인 응답."""

    message: str
