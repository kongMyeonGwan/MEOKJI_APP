"""인증 관련 요청/응답 스키마."""

from datetime import datetime

from pydantic import BaseModel, ConfigDict, EmailStr, Field, field_validator

from app.core.security import BCRYPT_MAX_BYTES


class RegisterRequest(BaseModel):
    email: EmailStr = Field(..., max_length=255)
    # min_length=1 이 중요하다. Oracle은 빈 문자열을 NULL로 취급하므로
    # ''를 그대로 넘기면 ORA-01400(NOT NULL 위반)으로 500이 난다.
    password: str = Field(..., min_length=8, max_length=72)
    nickname: str = Field(..., min_length=1, max_length=50)

    @field_validator("password")
    @classmethod
    def check_password_bytes(cls, v: str) -> str:
        """bcrypt의 72바이트 제한을 문자 수가 아닌 바이트로 검사한다.

        max_length=72는 '문자 72개'라서 한글 비밀번호는 통과해도
        UTF-8 인코딩 후 216바이트가 되어 bcrypt에서 터진다.
        """
        if len(v.encode("utf-8")) > BCRYPT_MAX_BYTES:
            raise ValueError(
                f"비밀번호는 UTF-8 기준 {BCRYPT_MAX_BYTES}바이트 이하여야 합니다 "
                "(한글은 1자당 3바이트)"
            )
        return v

    @field_validator("nickname")
    @classmethod
    def strip_nickname(cls, v: str) -> str:
        """공백만 입력한 닉네임을 거른다."""
        stripped = v.strip()
        if not stripped:
            raise ValueError("닉네임을 입력해주세요")
        return stripped


class UserResponse(BaseModel):
    """응답에 password_hash를 절대 포함하지 않는다."""

    model_config = ConfigDict(from_attributes=True)

    id: int
    email: EmailStr
    nickname: str
    created_at: datetime
