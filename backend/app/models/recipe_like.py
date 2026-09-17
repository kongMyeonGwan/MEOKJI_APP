"""RECIPE_LIKES 테이블 모델 (찜한 레시피)."""

from datetime import datetime

from sqlalchemy import ForeignKey, Identity, Integer, String, text
from sqlalchemy.orm import Mapped, mapped_column

from app.database.database import Base
from app.models.types import USE_YN_YES, UtcTimestamp


class RecipeLike(Base):
    __tablename__ = "recipe_likes"

    id: Mapped[int] = mapped_column(Integer, Identity(always=False), primary_key=True)

    user_id: Mapped[int] = mapped_column(
        Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False
    )

    # DDL 은 VARCHAR2(150 CHAR). MEAL_HISTORIES.FOOD_NAME 과 같은 길이다 —
    # 같은 값이 오가는 자리라 한쪽만 짧으면 긴 이름이 잘려 두 표가 어긋난다.
    food_name: Mapped[str] = mapped_column(String(150), nullable=False)

    # 'N' = 찜 해제. 하트를 다시 누르면 이 행을 'Y' 로 되살린다.
    # uq_recipe_likes 인덱스가 use_yn='Y' 인 행만 보므로 재찜이 막히지 않는다.
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
            f"<RecipeLike id={self.id} user_id={self.user_id} "
            f"food={self.food_name!r} use_yn={self.use_yn!r}>"
        )
