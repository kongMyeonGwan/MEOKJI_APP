"""인증 관련 요청/응답 스키마."""

from datetime import datetime
from typing import Literal

from pydantic import BaseModel, ConfigDict, EmailStr, Field, field_validator

from app.core.security import BCRYPT_MAX_BYTES, OTP_LENGTH


def check_password_bytes(password: str) -> str:
    """비밀번호가 bcrypt 의 72바이트 제한을 넘지 않는지 검사한다.

    Field(max_length=72) 로는 부족하다. 그건 "글자 72개" 라서
    한글 비밀번호 30자는 통과하는데, UTF-8 로 바꾸면 90바이트가 되어
    bcrypt 에서 에러가 난다 (한글 1자 = 3바이트).

    회원가입과 비밀번호 재설정이 같은 규칙을 써야 해서 함수로 빼뒀다.
    """
    byte_length = len(password.encode("utf-8"))

    if byte_length > BCRYPT_MAX_BYTES:
        raise ValueError(
            f"비밀번호는 UTF-8 기준 {BCRYPT_MAX_BYTES}바이트 이하여야 합니다 "
            "(한글은 1자당 3바이트)"
        )

    return password


class RegisterRequest(BaseModel):
    """회원가입 요청.

    Field(...) 의 점 세 개는 "필수 항목" 이라는 뜻이다.
    여기 적은 규칙에 안 맞는 요청은 우리 코드가 실행되기도 전에 422 로 막힌다.
    """

    email: EmailStr = Field(..., max_length=255)
    # min_length=1 이 중요하다. Oracle 은 빈 문자열을 NULL 로 취급해서,
    # 빈 값을 그대로 넘기면 NOT NULL 위반으로 500 이 난다.
    password: str = Field(..., min_length=8, max_length=72)
    nickname: str = Field(..., min_length=1, max_length=50)

    # 아래 넷은 선택 항목이다. 안 보내면 NULL 로 들어간다.
    #
    # 성별을 필수로 만들지 않는다. 추천 품질에 큰 영향이 없는데
    # 가입 첫 화면에서 답하기 껄끄러운 질문을 하면 이탈이 생긴다.
    #
    # 주소는 대시보드 첫 화면의 날씨 조회에 쓴다.
    # 화면에는 "서울특별시 강남구"로 보여주고, 날씨 API 는 좌표로 부른다.
    # OpenWeather 가 한국 시·군·구를 도시명 문자열로 못 찾기 때문에 둘로 나눠 둔다.
    #
    # 위경도는 받지 않는다. 고른 주소를 서버가 좌표로 바꿔 저장한다
    # (services/auth_service.set_coords_from_address).
    # 클라이언트가 좌표를 직접 보내게 하면 주소와 좌표가 어긋난 회원이 생긴다.
    gender: Literal["MALE", "FEMALE", "OTHER"] | None = None
    address_sido: str | None = Field(None, max_length=30)
    address_sigungu: str | None = Field(None, max_length=30)

    @field_validator("password")
    @classmethod
    def validate_password(cls, password: str) -> str:
        """Field 규칙만으로 부족한 부분을 직접 검사한다.

        @field_validator("password") 는 "password 값이 들어오면 이 함수를 통과시켜라"
        라는 뜻이다. 여기서 ValueError 를 던지면 FastAPI 가 422 로 바꿔서 내보낸다.
        """
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
    """로그인 요청.

    회원가입과 달리 password에 min_length=8 같은 규칙을 걸지 않는다.
    규칙을 걸면 "8자 미만"이라는 422가 먼저 나가면서, 공격자에게
    저장된 비밀번호의 형식을 알려주는 꼴이 된다. 형식 검증은 가입 때만 한다.
    """

    email: EmailStr
    password: str = Field(..., min_length=1)


class UserResponse(BaseModel):
    """응답에 password_hash를 절대 포함하지 않는다.

    요리 레벨 · 연속 방문일은 여기 없다. 그건 COUNT 쿼리가 몇 개 필요해서
    /api/users/me 로 따로 뺐다. 이 응답은 새로고침할 때마다 나가는 값이라
    가벼워야 한다.
    """

    model_config = ConfigDict(from_attributes=True)

    id: int
    email: EmailStr
    nickname: str
    created_at: datetime

    gender: str | None = None
    address_sido: str | None = None
    address_sigungu: str | None = None

    # 관리자인지. User 모델의 is_admin 프로퍼티(user_level == 1)에서 온다.
    #
    # 등급 숫자(user_level)를 그대로 내리지 않는다. 화면이 필요한 건 "공지 쓰기
    # 버튼을 그릴까" 하나뿐이라, 1 이 관리자라는 규칙까지 프론트에 퍼뜨릴 이유가 없다.
    #
    # 이 값은 화면을 그릴지 말지에만 쓴다. 진짜 차단은 서버의 get_admin_user 가 한다.
    # 버튼을 숨기는 것은 잠그는 것이 아니다 — 주소를 직접 부르면 그만이다.
    is_admin: bool = False


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

    @field_validator("new_password")
    @classmethod
    def validate_new_password(cls, password: str) -> str:
        """회원가입과 똑같은 규칙을 적용한다."""
        return check_password_bytes(password)


class MessageResponse(BaseModel):
    """본문에 담을 게 안내 문구뿐인 응답."""

    message: str
