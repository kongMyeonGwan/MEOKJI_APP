"""USERS 테이블 모델.

스키마의 단일 소스는 db/01_schema.sql 이다.
이 파일은 그 테이블을 파이썬에서 다루기 위한 매핑일 뿐이며,
create_all()로 테이블을 만들지 않는다.
"""

from datetime import datetime
from decimal import Decimal

from sqlalchemy import Boolean, Identity, Integer, Numeric, String, text
from sqlalchemy.orm import Mapped, mapped_column

from app.database.database import Base
from app.models.types import USE_YN_YES, UtcTimestamp

# DDL의 ck_users_gender CHECK 제약과 값이 일치해야 한다.
GENDER_MALE = "MALE"
GENDER_FEMALE = "FEMALE"
GENDER_OTHER = "OTHER"


class User(Base):
    # 소문자로 쓴다. SQLAlchemy가 따옴표 없이 보내면 Oracle이 대문자 USERS로 해석한다.
    __tablename__ = "users"

    id: Mapped[int] = mapped_column(Integer, Identity(always=False), primary_key=True)

    # unique=True를 주지 않는다. DDL의 유일성은 UNIQUE 제약이 아니라
    # uq_users_email / uq_users_nickname 함수 기반 인덱스가 담당하며,
    # 그 인덱스는 use_yn='Y' 인 행만 대상으로 한다(탈퇴 후 재가입 허용).
    # 여기에 unique=True를 적으면 실제 제약과 다른 내용을 문서화하게 된다.
    email: Mapped[str] = mapped_column(String(255), nullable=False)
    password_hash: Mapped[str] = mapped_column(String(255), nullable=False)
    nickname: Mapped[str] = mapped_column(String(50), nullable=False)

    # NULL = "선택 안 함". GENDER_* 상수만 넣는다.
    gender: Mapped[str | None] = mapped_column(String(10), nullable=True)

    # 대시보드 첫 화면의 날씨 조회에 쓴다.
    #   address_* : 화면에 "서울특별시 강남구"로 보여주는 표시용
    #   위경도    : 날씨 API를 실제로 호출하는 값
    # OpenWeather가 한국 시·군·구를 도시명 문자열로 못 찾기 때문에 둘로 나눠 둔다.
    address_sido: Mapped[str | None] = mapped_column(String(30), nullable=True)
    address_sigungu: Mapped[str | None] = mapped_column(String(30), nullable=True)
    latitude: Mapped[Decimal | None] = mapped_column(Numeric(9, 6), nullable=True)
    longitude: Mapped[Decimal | None] = mapped_column(Numeric(9, 6), nullable=True)

    # DB는 NUMBER(1) + CHECK(0,1). SQLAlchemy Boolean이 True<->1 변환을 담당한다.
    login_fail_count: Mapped[int] = mapped_column(Integer, nullable=False, server_default="0")
    is_locked: Mapped[bool] = mapped_column(Boolean, nullable=False, server_default="0")
    locked_at: Mapped[datetime | None] = mapped_column(UtcTimestamp(), nullable=True)

    # 'N' = 탈퇴. 회원 탈퇴는 DELETE가 아니라 이 값을 바꾸는 것이다.
    # 모든 조회에 use_yn == USE_YN_YES 조건을 붙여야 한다.
    use_yn: Mapped[str] = mapped_column(
        String(1), nullable=False, server_default=text(f"'{USE_YN_YES}'")
    )

    created_at: Mapped[datetime] = mapped_column(
        UtcTimestamp(), nullable=False, server_default=text('SYS_EXTRACT_UTC(SYSTIMESTAMP)')
    )
    # DDL에 갱신 트리거가 없으므로 애플리케이션이 채운다.
    updated_at: Mapped[datetime] = mapped_column(
        UtcTimestamp(),
        nullable=False,
        server_default=text('SYS_EXTRACT_UTC(SYSTIMESTAMP)'),
        onupdate=text('SYS_EXTRACT_UTC(SYSTIMESTAMP)'),
    )

    def __repr__(self) -> str:
        return (
            f"<User id={self.id} email={self.email!r} "
            f"nickname={self.nickname!r} use_yn={self.use_yn!r}>"
        )
