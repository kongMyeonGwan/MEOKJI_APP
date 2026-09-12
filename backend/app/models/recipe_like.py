"""RECIPE_LIKES 테이블 모델 (찜한 레시피).

스키마의 단일 소스는 db/01_schema.sql 이다.

찜은 왜 음식 id 가 아니라 이름인가
----------------------------------
추천 결과를 DB 에 저장하지 않으므로(설계 원칙 §13) 서버가 만들어준 음식 id 가
존재하지 않는다. 화면도 이미 이름으로 레시피를 연다(/recipe/김치찌개).
USER_BANS 가 재료를 이름으로 들고 있는 것과 같은 판단이다.

로그인한 회원만 찜한다
----------------------
전에는 브라우저(localStorage)에만 있었다. 그래서 기기를 바꾸면 안 따라오고,
한 브라우저를 여러 사람이 쓰면 앞사람 찜이 보였다 — 저장 키에 회원 구분이
없었기 때문이다. 이제 비로그인 상태에서 하트를 누르면 화면이 로그인으로
보내므로 '회원 없는 찜' 이라는 상태 자체가 없다.
"""

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
