"""RECIPE_REVIEWS 테이블 모델 (레시피 후기 게시판)."""

from datetime import datetime

from sqlalchemy import (
    BigInteger,
    ForeignKey,
    ForeignKeyConstraint,
    Identity,
    Integer,
    String,
    Text,
    text,
)
from sqlalchemy.orm import Mapped, mapped_column

from app.database.database import Base
from app.models.types import USE_YN_NO, USE_YN_YES, UtcTimestamp


class RecipeReview(Base):
    __tablename__ = "recipe_reviews"

    # DDL의 fk_reviews_meal 은 두 컬럼짜리 합성 FK다.
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

    # ===== 첨부파일 (후기 하나당 파일 하나) =====

    # 'Y' = 첨부 있음. file_path IS NOT NULL로도 알 수 있지만, 목록 조회에서
    # 500자짜리 경로 대신 CHAR(1) 하나만 읽으면 된다.
    file_yn: Mapped[str] = mapped_column(
        String(1), nullable=False, server_default=text(f"'{USE_YN_NO}'")
    )

    # 서버에 실제로 저장한 이름. 사용자가 올린 이름을 그대로 쓰지 않는다 —
    # 같은 이름끼리 덮어쓰고, '../../.env' 같은 경로가 섞여 들어올 수 있다.
    file_name: Mapped[str | None] = mapped_column(String(255), nullable=True)

    # 사용자가 올린 원래 이름. 화면에 보여주고 내려받을 때 이 이름으로 준다.
    file_org_name: Mapped[str | None] = mapped_column(String(255), nullable=True)

    # 저장 폴더 경로. 'uploads/2026/09'처럼 파일명은 넣지 않는다.
    file_path: Mapped[str | None] = mapped_column(String(500), nullable=True)

    # 확장자. 점 없이 'jpg', 'png'. 허용목록 검사와 아이콘 표시에 쓴다.
    file_ext: Mapped[str | None] = mapped_column(String(20), nullable=True)

    # 바이트. 크기를 못 구해도 파일은 정상이라 file_yn='Y'의 필수 항목에서 뺐다.
    file_size: Mapped[int | None] = mapped_column(BigInteger, nullable=True)

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

    def set_file(
        self, name: str, org_name: str, path: str, ext: str, size: int | None = None
    ) -> None:
        """첨부파일을 붙인다. file_yn 까지 같이 세워야 CHECK 를 통과한다."""
        self.file_yn = USE_YN_YES
        self.file_name = name
        self.file_org_name = org_name
        self.file_path = path
        self.file_ext = ext
        self.file_size = size

    def clear_file(self) -> None:
        """첨부파일을 뗀다."""
        self.file_yn = USE_YN_NO
        self.file_name = None
        self.file_org_name = None
        self.file_path = None
        self.file_ext = None
        self.file_size = None

    def __repr__(self) -> str:
        return (
            f"<RecipeReview id={self.id} user_id={self.user_id} "
            f"title={self.title!r} use_yn={self.use_yn!r}>"
        )
