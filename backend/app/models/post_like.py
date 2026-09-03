"""POST_LIKES 테이블 모델 (좋아요).

스키마의 단일 소스는 db/01_schema.sql 이다.
uq_post_likes(user_id, post_id)가 중복 좋아요를 DB 레벨에서 막는다.
"""

from datetime import datetime

from sqlalchemy import ForeignKey, Identity, Integer, text
from sqlalchemy.orm import Mapped, mapped_column

from app.database.database import Base
from app.models.types import UtcTimestamp


class PostLike(Base):
    __tablename__ = "post_likes"

    id: Mapped[int] = mapped_column(Integer, Identity(always=False), primary_key=True)

    user_id: Mapped[int] = mapped_column(
        Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False
    )
    post_id: Mapped[int] = mapped_column(
        Integer, ForeignKey("posts.id", ondelete="CASCADE"), nullable=False
    )

    created_at: Mapped[datetime] = mapped_column(
        UtcTimestamp(),
        nullable=False,
        server_default=text("SYS_EXTRACT_UTC(SYSTIMESTAMP)"),
    )

    def __repr__(self) -> str:
        return f"<PostLike user_id={self.user_id} post_id={self.post_id}>"
