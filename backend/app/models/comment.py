"""COMMENTS 테이블 모델 (댓글).

스키마의 단일 소스는 db/01_schema.sql 이다.
MVP에서는 대댓글이 없어 parent_comment_id 컬럼도 없다.
"""

from datetime import datetime

from sqlalchemy import ForeignKey, Identity, Integer, String, text
from sqlalchemy.orm import Mapped, mapped_column

from app.database.database import Base
from app.models.types import UtcTimestamp


class Comment(Base):
    __tablename__ = "comments"

    id: Mapped[int] = mapped_column(Integer, Identity(always=False), primary_key=True)

    post_id: Mapped[int] = mapped_column(
        Integer, ForeignKey("posts.id", ondelete="CASCADE"), nullable=False
    )
    user_id: Mapped[int] = mapped_column(
        Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False
    )

    # 게시글 본문과 달리 CLOB이 아니다. 댓글은 1000자로 제한한다.
    content: Mapped[str] = mapped_column(String(1000), nullable=False)

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
        return f"<Comment id={self.id} post_id={self.post_id}>"
