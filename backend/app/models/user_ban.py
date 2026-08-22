"""USER_BANS 테이블 모델 (못 먹거나 먹기 싫은 재료).

스키마의 단일 소스는 db/01_schema.sql 이다.
"""

from datetime import datetime

from sqlalchemy import ForeignKey, Identity, Integer, String, text
from sqlalchemy.orm import Mapped, mapped_column

from app.database.database import Base
from app.models.types import UtcTimestamp

# DDL의 ck_user_bans_type CHECK 제약과 값이 일치해야 한다.
# 스키마(schemas/ban.py)의 Literal도 이 둘만 허용한다.
BAN_TYPE_DISLIKE = "DISLIKE"
BAN_TYPE_ALLERGY = "ALLERGY"


class UserBan(Base):
    __tablename__ = "user_bans"

    id: Mapped[int] = mapped_column(Integer, Identity(always=False), primary_key=True)

    user_id: Mapped[int] = mapped_column(
        Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False
    )

    # DDL은 VARCHAR2(100 CHAR). 바이트가 아니라 문자 100개다.
    ingredient_name: Mapped[str] = mapped_column(String(100), nullable=False)

    ban_type: Mapped[str] = mapped_column(String(20), nullable=False)

    created_at: Mapped[datetime] = mapped_column(
        UtcTimestamp(),
        nullable=False,
        server_default=text("SYS_EXTRACT_UTC(SYSTIMESTAMP)"),
    )

    def __repr__(self) -> str:
        return (
            f"<UserBan id={self.id} user_id={self.user_id} "
            f"name={self.ingredient_name!r} type={self.ban_type!r}>"
        )
