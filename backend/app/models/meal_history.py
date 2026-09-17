"""MEAL_HISTORIES 테이블 모델 (사용자가 최종 확정한 식사)."""

from datetime import datetime

from sqlalchemy import ForeignKey, Identity, Integer, String, text
from sqlalchemy.orm import Mapped, mapped_column

from app.database.database import Base
from app.models.types import USE_YN_YES, OracleJSON, UtcTimestamp


class MealHistory(Base):
    __tablename__ = "meal_histories"

    id: Mapped[int] = mapped_column(Integer, Identity(always=False), primary_key=True)

    user_id: Mapped[int] = mapped_column(
        Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False
    )

    # recipe_json 안에도 있지만, 목록 조회에서 CLOB을 파싱하지 않으려고 컬럼으로 승격했다.
    food_name: Mapped[str] = mapped_column(String(150), nullable=False)
    recipe_name: Mapped[str | None] = mapped_column(String(200), nullable=True)

    # DDL은 CLOB + CHECK(... IS JSON). OracleJSON이 dict/list <-> 문자열 변환을 맡는다.
    recipe_json: Mapped[dict] = mapped_column(OracleJSON(), nullable=False)
    ingredients_json: Mapped[list] = mapped_column(OracleJSON(), nullable=False)
    substitutes_json: Mapped[list | None] = mapped_column(OracleJSON(), nullable=True)

    # 확정 시점의 예상 재료비(원). 계산에 실패했으면 NULL.
    estimated_cost: Mapped[int | None] = mapped_column(Integer, nullable=True)

    # 'N' = 삭제한 기록. 물리 삭제가 아니라 이 값을 바꾼다.
    use_yn: Mapped[str] = mapped_column(
        String(1), nullable=False, server_default=text(f"'{USE_YN_YES}'")
    )

    selected_at: Mapped[datetime] = mapped_column(
        UtcTimestamp(),
        nullable=False,
        server_default=text("SYS_EXTRACT_UTC(SYSTIMESTAMP)"),
    )

    def __repr__(self) -> str:
        return (
            f"<MealHistory id={self.id} user_id={self.user_id} "
            f"food={self.food_name!r}>"
        )
