"""인증 관련 요청/응답 스키마."""

from datetime import datetime

from pydantic import BaseModel, ConfigDict, EmailStr, Field, field_validator

from app.core.security import BCRYPT_MAX_BYTES, OTP_LENGTH


def check_password_bytes(v: str) -> str:
    """bcrypt의 72바이트 제한을 문자 수가 아닌 바이트로 검사한다.

    max_length=72는 '문자 72개'라서 한글 비밀번호는 통과해도
    UTF-8 인코딩 후 216바이트가 되어 bcrypt에서 터진다.

    회원가입과 비밀번호 재설정이 같은 규칙을 써야 하므로 함수로 빼뒀다.
    """
    if len(v.encode("utf-8")) > BCRYPT_MAX_BYTES:
        raise ValueError(
            f"비밀번호는 UTF-8 기준 {BCRYPT_MAX_BYTES}바이트 이하여야 합니다 "
            "(한글은 1자당 3바이트)"
        )
    return v


class RegisterRequest(BaseModel):
    email: EmailStr = Field(..., max_length=255)
    # min_length=1 이 중요하다. Oracle은 빈 문자열을 NULL로 취급하므로
    # ''를 그대로 넘기면 ORA-01400(NOT NULL 위반)으로 500이 난다.
    password: str = Field(..., min_length=8, max_length=72)
    nickname: str = Field(..., min_length=1, max_length=50)

    _check_password = field_validator("password")(check_password_bytes)

    @field_validator("nickname")
    @classmethod
    def strip_nickname(cls, v: str) -> str:
        """공백만 입력한 닉네임을 거른다."""
        stripped = v.strip()
        if not stripped:
            raise ValueError("닉네임을 입력해주세요")
        return stripped


class LoginRequest(BaseModel):
    """로그인 요청.

    회원가입과 달리 password에 min_length=8 같은 규칙을 걸지 않는다.
    규칙을 걸면 "8자 미만"이라는 422가 먼저 나가면서, 공격자에게
    저장된 비밀번호의 형식을 알려주는 꼴이 된다. 형식 검증은 가입 때만 한다.
    """

    email: EmailStr
    password: str = Field(..., min_length=1)


class UserResponse(BaseModel):
    """응답에 password_hash를 절대 포함하지 않는다."""

    model_config = ConfigDict(from_attributes=True)

    id: int
    email: EmailStr
    nickname: str
    created_at: datetime


class TokenResponse(BaseModel):
    """로그인 성공 응답.

    user를 함께 내려준다. 프론트가 로그인 직후 닉네임을 표시하려고
    /api/auth/me를 한 번 더 호출하지 않아도 되게 하기 위함이다.
    """

    access_token: str
    token_type: str = "bearer"
    expires_in: int  # 초 단위. 프론트가 만료 시각을 계산할 때 쓴다.
    user: UserResponse


# =========================================================
# 비밀번호 재설정 (STEP 8)
#
# 세 단계로 나눈다.
#   1) send    이메일로 인증번호 발송
#   2) verify  인증번호 확인 (used_at 기록)
#   3) change  새 비밀번호로 교체
# =========================================================


class PasswordResetSendRequest(BaseModel):
    email: EmailStr


class PasswordResetVerifyRequest(BaseModel):
    email: EmailStr
    code: str = Field(..., min_length=OTP_LENGTH, max_length=OTP_LENGTH)


class PasswordResetChangeRequest(BaseModel):
    email: EmailStr
    code: str = Field(..., min_length=OTP_LENGTH, max_length=OTP_LENGTH)
    new_password: str = Field(..., min_length=8, max_length=72)

    _check_password = field_validator("new_password")(check_password_bytes)


class MessageResponse(BaseModel):
    """본문에 담을 게 안내 문구뿐인 응답."""

    message: str
