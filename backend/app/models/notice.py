"""NOTICES : 공지사항."""

from datetime import datetime

from sqlalchemy import ForeignKey, Identity, Integer, String, text
from sqlalchemy.orm import Mapped, mapped_column

from app.database.database import Base
from app.models.types import USE_YN_NO, USE_YN_YES, UtcTimestamp


class Notice(Base):
    __tablename__ = "notices"

    id: Mapped[int] = mapped_column(Identity(), primary_key=True)

    user_id: Mapped[int] = mapped_column(
        ForeignKey("users.id"), nullable=False
    )

    title: Mapped[str] = mapped_column(String(200), nullable=False)
    # DDL 은 VARCHAR2(4000) 이다. 후기 본문(recipe_reviews.content)과 달리 CLOB 이
    content: Mapped[str] = mapped_column(String(4000), nullable=False)

    view_count: Mapped[int] = mapped_column(Integer, nullable=False, server_default="0")

    # 'Y' = 목록 상단 고정. 삭제 플래그(use_yn)와 헷갈리기 쉬우니 주의한다.
    pinned_yn: Mapped[str] = mapped_column(
        String(1), nullable=False, server_default=text(f"'{USE_YN_NO}'")
    )

    # 'N' = 삭제된 공지. 조회에 반드시 use_yn == USE_YN_YES 조건을 붙인다.
    use_yn: Mapped[str] = mapped_column(
        String(1), nullable=False, server_default=text(f"'{USE_YN_YES}'")
    )

    created_at: Mapped[datetime] = mapped_column(
        UtcTimestamp,
        nullable=False,
        server_default=text("SYS_EXTRACT_UTC(SYSTIMESTAMP)"),
    )

    updated_at: Mapped[datetime] = mapped_column(
        UtcTimestamp,
        nullable=False,
        server_default=text("SYS_EXTRACT_UTC(SYSTIMESTAMP)"),
        onupdate=text("SYS_EXTRACT_UTC(SYSTIMESTAMP)"),
    )

    def __repr__(self) -> str:
        return f"<Notice id={self.id} title={self.title!r}>"
