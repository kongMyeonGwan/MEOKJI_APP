"""USER_BANS 테이블 모델 (못 먹거나 먹기 싫은 재료)."""

from datetime import datetime

from sqlalchemy import ForeignKey, Identity, Integer, String, text
from sqlalchemy.orm import Mapped, mapped_column

from app.database.database import Base
from app.models.types import USE_YN_YES, UtcTimestamp

# DDL의 ck_user_bans_type CHECK 제약과 값이 일치해야 한다.
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

    # 'N' = 해제한 재료. Ban 해제는 DELETE가 아니라 이 값을 바꾸는 것이다.
    # uq_user_bans 인덱스가 use_yn='Y' 인 행만 보므로 해제 후 재등록이 가능하다.
    use_yn: Mapped[str] = mapped_column(
        String(1), nullable=False, server_default=text(f"'{USE_YN_YES}'")
    )

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
