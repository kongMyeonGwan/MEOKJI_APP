"""내 정보 · 알림 스키마."""

from datetime import datetime
from typing import Literal

from pydantic import BaseModel, ConfigDict, EmailStr, Field, field_validator

from app.schemas.auth import check_password_bytes

# DDL 의 ck_users_gender CHECK 제약과 값이 일치해야 한다.
Gender = Literal["MALE", "FEMALE", "OTHER"]


class ProfileResponse(BaseModel):
    """마이페이지 · 사이드바 · 상단바가 함께 쓰는 내 정보."""

    model_config = ConfigDict(from_attributes=True)

    id: int
    email: EmailStr
    nickname: str
    created_at: datetime

    gender: Gender | None = None
    address_sido: str | None = None
    address_sigungu: str | None = None
    latitude: float | None = None
    longitude: float | None = None

    # --- 아래는 DB 컬럼이 아니라 서버가 세서 계산한 값이다 ---
    streak_days: int      # 오늘까지 며칠 연속으로 요리를 기록했는지
    cook_count: int       # 확정한 식사 건수


class ProfileUpdateRequest(BaseModel):
    """내 정보 수정."""

    #
    # 위경도는 받지 않는다. 주소를 바꾸면 서버가 좌표를 다시 계산한다.
    nickname: str | None = Field(None, min_length=1, max_length=50)
    gender: Gender | None = None
    address_sido: str | None = Field(None, max_length=30)
    address_sigungu: str | None = Field(None, max_length=30)

    @field_validator("nickname")
    @classmethod
    def validate_nickname(cls, nickname: str | None) -> str | None:
        """공백만 입력한 닉네임을 거르고, 앞뒤 공백을 떼어낸다."""
        if nickname is None:
            return None

        nickname = nickname.strip()
        if not nickname:
            raise ValueError("닉네임을 입력해주세요")

        return nickname

    @field_validator("address_sido", "address_sigungu")
    @classmethod
    def blank_to_none(cls, value: str | None) -> str | None:
        """빈 문자열은 "지움"으로 본다."""
        if value is None:
            return None

        value = value.strip()
        if not value:
            return None

        return value


class PasswordChangeRequest(BaseModel):
    """로그인한 상태에서 비밀번호 바꾸기."""

    current_password: str = Field(..., min_length=1)
    new_password: str = Field(..., min_length=8, max_length=72)

    @field_validator("new_password")
    @classmethod
    def validate_new_password(cls, password: str) -> str:
        """회원가입과 똑같은 규칙을 적용한다."""
        return check_password_bytes(password)


class WithdrawRequest(BaseModel):
    """회원 탈퇴."""

    password: str = Field(..., min_length=1)


class NotificationItem(BaseModel):
    """알림 한 건."""

    id: str
    kind: Literal["COMMENT", "LIKE"]
    icon: str
    title: str
    body: str
    created_at: datetime

    # 누르면 이동할 화면 주소. 예: "/community/3"
    link: str
