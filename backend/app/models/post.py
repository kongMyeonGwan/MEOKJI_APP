"""POSTS 테이블 모델 (음식/레시피 공유 게시판).

스키마의 단일 소스는 db/01_schema.sql 이다.

게시글은 반드시 확정한 식사(MealHistory)에서 출발한다(meal_history_id NOT NULL).
다만 화면에 뿌릴 값은 참조가 아니라 **복사본**(food_name / recipe_json /
ingredients_json)에서 읽는다. Guest도 게시글을 보기 때문에 남의 개인 기록을
JOIN해서 읽는 구조를 피하고, 공유 후 글을 수정해도 원본 스냅샷이 흔들리지 않게 한다.
meal_history_id 는 오직 출처(provenance) 추적용이다.
"""

from datetime import datetime

from sqlalchemy import ForeignKey, Identity, Integer, String, Text, text
from sqlalchemy.orm import Mapped, mapped_column

from app.database.database import Base
from app.models.types import OracleJSON, UtcTimestamp


class Post(Base):
    __tablename__ = "posts"

    id: Mapped[int] = mapped_column(Integer, Identity(always=False), primary_key=True)

    user_id: Mapped[int] = mapped_column(
        Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False
    )

    # ondelete를 지정하지 않는다 = RESTRICT.
    # 공유한 히스토리를 지우려 하면 ORA-02292가 나고, 서비스 계층이 그걸 잡아
    # "게시글을 먼저 삭제하세요"로 안내한다. CASCADE면 남의 좋아요/댓글이 달린
    # 커뮤니티 글이 개인 기록 삭제만으로 사라진다.
    meal_history_id: Mapped[int] = mapped_column(
        Integer, ForeignKey("meal_histories.id"), nullable=False, unique=True
    )

    title: Mapped[str] = mapped_column(String(200), nullable=False)
    content: Mapped[str] = mapped_column(Text, nullable=False)

    # --- 아래 셋은 확정 당시 MealHistory에서 복사해 온 값이다 (참조 아님) ---
    food_name: Mapped[str | None] = mapped_column(String(150), nullable=True)
    recipe_json: Mapped[dict | None] = mapped_column(OracleJSON(), nullable=True)
    ingredients_json: Mapped[list | None] = mapped_column(OracleJSON(), nullable=True)

    view_count: Mapped[int] = mapped_column(Integer, nullable=False, server_default="0")

    created_at: Mapped[datetime] = mapped_column(
        UtcTimestamp(),
        nullable=False,
        server_default=text("SYS_EXTRACT_UTC(SYSTIMESTAMP)"),
    )
    # DDL에 갱신 트리거가 없으므로 애플리케이션이 채운다.
    updated_at: Mapped[datetime] = mapped_column(
        UtcTimestamp(),
        nullable=False,
        server_default=text("SYS_EXTRACT_UTC(SYSTIMESTAMP)"),
        onupdate=text("SYS_EXTRACT_UTC(SYSTIMESTAMP)"),
    )

    def __repr__(self) -> str:
        return f"<Post id={self.id} user_id={self.user_id} title={self.title!r}>"
