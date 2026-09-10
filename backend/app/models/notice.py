"""NOTICES : 공지사항.

운영자가 쓰고 회원은 읽기만 한다. RECIPE_REVIEWS 와 달리 MEAL_HISTORIES 에서
파생되지 않는 독립 게시판이고, 좋아요·댓글도 붙지 않는다.

USERS 에 관리자 구분 컬럼이 없다. 그래서 "운영자만 작성" 을 DB 가 강제하지
못한다 — 지금은 쓰기 API 자체를 만들지 않아서 문제가 없지만, 나중에 붙인다면
서비스 계층이 막아야 한다.

fk_notices_user 에는 ON DELETE CASCADE 가 없다. 운영자 계정이 사라진다고
공지가 같이 사라지면 안 되기 때문이다. 공지는 서비스 전체를 향한 글이라
작성자 계정보다 오래 남아야 한다.
"""

from datetime import datetime

from sqlalchemy import ForeignKey, Identity, Integer, String, Text, text
from sqlalchemy.orm import Mapped, mapped_column

from app.database.database import Base
from app.models.types import USE_YN_NO, USE_YN_YES, UtcTimestamp


class Notice(Base):
    __tablename__ = "notices"

    id: Mapped[int] = mapped_column(Identity(), primary_key=True)

    user_id: Mapped[int] = mapped_column(
        ForeignKey("users.id"), nullable=False
    )

    title: Mapped[str] = mapped_column(String(200), nullable=False)
    content: Mapped[str] = mapped_column(Text, nullable=False)

    # 목록의 분류 배지. '점검' · '안내' · '업데이트' 처럼 운영하면서 늘어나는 값이라
    # DDL 에 CHECK 를 걸지 않았다. 화면은 '점검' 만 경고색으로 그리고 나머지는
    # 기본색이라, 모르는 값이 들어와도 배지가 깨지지 않는다.
    category: Mapped[str] = mapped_column(
        String(20), nullable=False, server_default=text("'안내'")
    )

    view_count: Mapped[int] = mapped_column(Integer, nullable=False, server_default="0")

    # 'Y' = 목록 상단 고정. 삭제 플래그(use_yn)와 헷갈리기 쉬우니 주의한다.
    pinned_yn: Mapped[str] = mapped_column(
        String(1), nullable=False, server_default=text(f"'{USE_YN_NO}'")
    )

    # 'N' = 삭제된 공지. 조회에 반드시 use_yn == USE_YN_YES 조건을 붙인다.
    use_yn: Mapped[str] = mapped_column(
        String(1), nullable=False, server_default=text(f"'{USE_YN_YES}'")
    )

    created_at: Mapped[datetime] = mapped_column(
        UtcTimestamp,
        nullable=False,
        server_default=text("SYS_EXTRACT_UTC(SYSTIMESTAMP)"),
    )

    updated_at: Mapped[datetime] = mapped_column(
        UtcTimestamp,
        nullable=False,
        server_default=text("SYS_EXTRACT_UTC(SYSTIMESTAMP)"),
        onupdate=text("SYS_EXTRACT_UTC(SYSTIMESTAMP)"),
    )

    def __repr__(self) -> str:
        return f"<Notice id={self.id} category={self.category} title={self.title!r}>"
