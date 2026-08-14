"""USERS 테이블 모델.

스키마의 단일 소스는 db/01_schema.sql 이다.
이 파일은 그 테이블을 파이썬에서 다루기 위한 매핑일 뿐이며,
create_all()로 테이블을 만들지 않는다.
"""

from datetime import datetime

from sqlalchemy import Boolean, Identity, Integer, String, text
from sqlalchemy.orm import Mapped, mapped_column

from app.database.database import Base
from app.models.types import UtcTimestamp


class User(Base):
    # 소문자로 쓴다. SQLAlchemy가 따옴표 없이 보내면 Oracle이 대문자 USERS로 해석한다.
    __tablename__ = "users"

    id: Mapped[int] = mapped_column(Integer, Identity(always=False), primary_key=True)

    email: Mapped[str] = mapped_column(String(255), nullable=False, unique=True)
    password_hash: Mapped[str] = mapped_column(String(255), nullable=False)
    nickname: Mapped[str] = mapped_column(String(50), nullable=False, unique=True)

    # DB는 NUMBER(1) + CHECK(0,1). SQLAlchemy Boolean이 True<->1 변환을 담당한다.
    login_fail_count: Mapped[int] = mapped_column(Integer, nullable=False, server_default="0")
    is_locked: Mapped[bool] = mapped_column(Boolean, nullable=False, server_default="0")
    locked_at: Mapped[datetime | None] = mapped_column(UtcTimestamp(), nullable=True)

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
        return f"<User id={self.id} email={self.email!r} nickname={self.nickname!r}>"
