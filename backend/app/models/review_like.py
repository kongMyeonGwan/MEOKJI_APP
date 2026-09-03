"""REVIEW_LIKES 테이블 모델 (좋아요).

스키마의 단일 소스는 db/01_schema.sql 이다.

좋아요 취소는 행을 지우는 게 아니라 use_yn='N' 이다.
그래서 유일성을 평범한 UNIQUE 로 걸 수 없다. 취소한 행이 자리를 차지해
다시 누르는 순간 ORA-00001 이 나기 때문이다.
DDL은 CASE WHEN use_yn='Y' 로 감싼 함수 기반 UNIQUE 인덱스(uq_review_likes)를
쓰며, 'N' 행은 인덱스에서 빠져 살아있는 좋아요끼리만 중복을 본다.
"""

from datetime import datetime

from sqlalchemy import ForeignKey, Identity, Integer, String, text
from sqlalchemy.orm import Mapped, mapped_column

from app.database.database import Base
from app.models.types import USE_YN_YES, UtcTimestamp


class ReviewLike(Base):
    __tablename__ = "review_likes"

    id: Mapped[int] = mapped_column(Integer, Identity(always=False), primary_key=True)

    user_id: Mapped[int] = mapped_column(
        Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False
    )
    review_id: Mapped[int] = mapped_column(
        Integer, ForeignKey("recipe_reviews.id", ondelete="CASCADE"), nullable=False
    )

    # 'N' = 취소한 좋아요. 개수를 셀 때 반드시 걸러야 한다.
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
            f"<ReviewLike user_id={self.user_id} review_id={self.review_id} "
            f"use_yn={self.use_yn!r}>"
        )
