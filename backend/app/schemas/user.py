"""내 정보 · 요리 레벨 · 알림 스키마.

요리 레벨을 왜 DB 에 저장하지 않는가
------------------------------------
레벨 · 경험치 컬럼을 만들면 "글을 쓸 때마다 +30" 같은 갱신을 모든 기능에
흩어놔야 하고, 한 곳만 빠뜨려도 값이 조용히 어긋난다.
확정한 식사 · 글 · 댓글 · 받은 좋아요는 이미 테이블에 다 있으므로,
조회할 때 세서 계산한다. 규칙을 바꿔도 과거 데이터가 자동으로 따라온다.
"""

from datetime import datetime
from typing import Literal

from pydantic import BaseModel, ConfigDict, EmailStr, Field, field_validator

from app.schemas.auth import check_password_bytes

# DDL 의 ck_users_gender CHECK 제약과 값이 일치해야 한다.
Gender = Literal["MALE", "FEMALE", "OTHER"]


class ProfileResponse(BaseModel):
    """마이페이지 · 사이드바 · 상단바가 함께 쓰는 내 정보.

    exp 를 퍼센트로 계산해서 내리지 않는다.
    사이드바는 막대로, 마이페이지는 "180 남음" 으로 보여줘야 해서
    원본 두 값(exp · exp_next)을 그대로 두는 편이 낫다.
    """

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
    level: int
    title: str
    exp: int       # 지금 레벨에서 쌓은 경험치
    exp_next: int  # 다음 레벨까지 필요한 경험치

    streak_days: int      # 오늘까지 며칠 연속으로 요리를 기록했는지
    cook_count: int       # 확정한 식사 건수
    post_count: int       # 쓴 글 수
    comment_count: int    # 쓴 댓글 수
    like_received: int    # 내 글이 받은 좋아요 수


class ProfileUpdateRequest(BaseModel):
    """내 정보 수정. 보낸 항목만 바뀐다.

    이메일은 바꿀 수 없다. 로그인 아이디이자 비밀번호 재설정 경로라서,
    바꾸려면 본인 확인이 한 번 더 필요하다. MVP 범위 밖이다.
    """

    nickname: str | None = Field(None, min_length=1, max_length=50)
    gender: Gender | None = None
    address_sido: str | None = Field(None, max_length=30)
    address_sigungu: str | None = Field(None, max_length=30)
    latitude: float | None = Field(None, ge=-90, le=90)
    longitude: float | None = Field(None, ge=-180, le=180)

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
        """빈 문자열은 "지움"으로 본다.

        Oracle 은 빈 문자열을 NULL 로 취급하므로 어차피 같은 값이 된다.
        여기서 미리 None 으로 바꿔 두면 코드에서 두 경우를 구분할 필요가 없다.
        """
        if value is None:
            return None

        value = value.strip()
        if not value:
            return None

        return value


class PasswordChangeRequest(BaseModel):
    """로그인한 상태에서 비밀번호 바꾸기.

    비밀번호 재설정(이메일 OTP)과 다른 기능이다.
    이쪽은 현재 비밀번호를 아는 사람이 쓰고, 저쪽은 잊어버린 사람이 쓴다.
    """

    current_password: str = Field(..., min_length=1)
    new_password: str = Field(..., min_length=8, max_length=72)

    @field_validator("new_password")
    @classmethod
    def validate_new_password(cls, password: str) -> str:
        """회원가입과 똑같은 규칙을 적용한다."""
        return check_password_bytes(password)


class WithdrawRequest(BaseModel):
    """회원 탈퇴.

    비밀번호를 한 번 더 받는다. 로그인한 브라우저를 잠깐 빌린 사람이
    버튼 한 번으로 계정을 없애지 못하게 하기 위함이다.
    """

    password: str = Field(..., min_length=1)


class NotificationItem(BaseModel):
    """알림 한 건.

    알림 테이블을 따로 만들지 않는다. "내 글에 달린 댓글" 과 "내 글이 받은
    좋아요" 는 이미 테이블에 있는 사실이라, 조회할 때 모아서 만들면 된다.
    별도 테이블을 두면 댓글을 지웠을 때 알림만 남는 문제를 또 관리해야 한다.

    id 는 DB 의 id 가 아니라 "COMMENT-12" 같은 문자열이다.
    댓글 12번과 좋아요 12번이 같은 번호를 쓰기 때문에 종류를 앞에 붙인다.
    """

    id: str
    kind: Literal["COMMENT", "LIKE"]
    icon: str
    title: str
    body: str
    created_at: datetime

    # 누르면 이동할 화면 주소. 예: "/community/3"
    link: str
