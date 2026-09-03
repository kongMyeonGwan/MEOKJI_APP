"""RECIPE_REVIEWS 테이블 모델 (레시피 후기 게시판).

스키마의 단일 소스는 db/01_schema.sql 이다.

후기는 반드시 자기가 확정한 식사(MealHistory)에서 출발한다.
DDL의 FK가 (meal_history_id, user_id) 합성이라, 남의 히스토리를 자기 후기로
공유하려 하면 애플리케이션까지 오기 전에 DB가 ORA-02291로 막는다.

food_name / recipe_json / ingredients_json 컬럼을 두지 않는다.
MEAL_HISTORIES 가 이미 "확정 당시 스냅샷, 이후 UPDATE 없음"이라 값이 변할 일이
없으므로, 복사본을 두면 두 값이 어긋날 여지만 생긴다. JOIN 해서 읽는다.
"""

from datetime import datetime

from sqlalchemy import ForeignKey, ForeignKeyConstraint, Identity, Integer, String, Text, text
from sqlalchemy.orm import Mapped, mapped_column

from app.database.database import Base
from app.models.types import USE_YN_YES, UtcTimestamp


class RecipeReview(Base):
    __tablename__ = "recipe_reviews"

    # DDL의 fk_reviews_meal 은 두 컬럼짜리 합성 FK다.
    # mapped_column(ForeignKey(...))로는 표현할 수 없어 테이블 레벨에 선언한다.
    #
    # ondelete를 지정하지 않는다 = RESTRICT.
    # 후기가 달린 히스토리를 물리 삭제하려 하면 ORA-02292가 난다.
    # 다만 우리는 물리 삭제를 쓰지 않으므로, 논리 삭제(use_yn='N') 차단은
    # 서비스 계층이 "살아있는 후기가 있는가"를 확인해서 막아야 한다.
    __table_args__ = (
        ForeignKeyConstraint(
            ["meal_history_id", "user_id"],
            ["meal_histories.id", "meal_histories.user_id"],
            name="fk_reviews_meal",
        ),
    )

    id: Mapped[int] = mapped_column(Integer, Identity(always=False), primary_key=True)

    user_id: Mapped[int] = mapped_column(
        Integer, ForeignKey("users.id", ondelete="CASCADE"), nullable=False
    )

    # unique=True를 주지 않는다. 유일성은 uq_reviews_meal 함수 기반 인덱스가
    # 담당하며, use_yn='Y'인 행만 대상으로 한다(지운 뒤 다시 쓰기 허용).
    meal_history_id: Mapped[int] = mapped_column(Integer, nullable=False)

    title: Mapped[str] = mapped_column(String(200), nullable=False)
    content: Mapped[str] = mapped_column(Text, nullable=False)

    view_count: Mapped[int] = mapped_column(Integer, nullable=False, server_default="0")

    # 'N' = 삭제된 후기. 조회에 반드시 use_yn == USE_YN_YES 조건을 붙인다.
    use_yn: Mapped[str] = mapped_column(
        String(1), nullable=False, server_default=text(f"'{USE_YN_YES}'")
    )

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
        return (
            f"<RecipeReview id={self.id} user_id={self.user_id} "
            f"title={self.title!r} use_yn={self.use_yn!r}>"
        )
