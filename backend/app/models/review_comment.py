"""REVIEW_COMMENTS 테이블 모델 (댓글)."""

from datetime import datetime

from sqlalchemy import ForeignKey, Identity, Integer, String, text
from sqlalchemy.orm import Mapped, mapped_column

from app.database.database import Base
from app.models.types import USE_YN_YES, UtcTimestamp


class ReviewComment(Base):
    __tablename__ = "review_comments"

    id: Mapped[int] = mapped_column(Integer, Identity(always=False), primary_key=True)

    review_id: Mapped[int] = mapped_column(
        Integer, ForeignKey("recipe_reviews.id", ondelete="CASCADE"), nullable=False
    )
    user_id: Mapped[int] = mapped_column(
        Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False
    )

    # 후기 본문과 달리 CLOB이 아니다. 댓글은 1000자로 제한한다.
    content: Mapped[str] = mapped_column(String(1000), nullable=False)

    # 'N' = 삭제된 댓글. 목록과 개수 양쪽에서 걸러야 한다.
    use_yn: Mapped[str] = mapped_column(
        String(1), nullable=False, server_default=text(f"'{USE_YN_YES}'")
    )

    created_at: Mapped[datetime] = mapped_column(
        UtcTimestamp(),
        nullable=False,
        server_default=text("SYS_EXTRACT_UTC(SYSTIMESTAMP)"),
    )
    updated_at: Mapped[datetime] = mapped_column(
        UtcTimestamp(),
        nullable=False,
        server_default=text("SYS_EXTRACT_UTC(SYSTIMESTAMP)"),
        onupdate=text("SYS_EXTRACT_UTC(SYSTIMESTAMP)"),
    )

    def __repr__(self) -> str:
        return f"<ReviewComment id={self.id} review_id={self.review_id}>"
