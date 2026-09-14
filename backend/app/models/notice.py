"""NOTICES : 공지사항.

운영자가 쓰고 회원은 읽기만 한다. RECIPE_REVIEWS 와 달리 MEAL_HISTORIES 에서
파생되지 않는 독립 게시판이고, 좋아요·댓글도 붙지 않는다.

"운영자만 작성" 은 USERS.USER_LEVEL(1=관리자) 로 가린다. DB 가 강제하는 것은
아니다 — 라우터의 get_admin_user 가 등급을 보고 막는다.

fk_notices_user 에는 ON DELETE CASCADE 가 없다. 운영자 계정이 사라진다고
공지가 같이 사라지면 안 되기 때문이다. 공지는 서비스 전체를 향한 글이라
작성자 계정보다 오래 남아야 한다.
"""

from datetime import datetime

from sqlalchemy import ForeignKey, Identity, Integer, String, text
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
    # DDL 은 VARCHAR2(4000) 이다. 후기 본문(recipe_reviews.content)과 달리 CLOB 이
    # 아닌 이유는 db/01_schema.sql 의 NOTICES 주석 참고 — 목록에서 본문을 같이
    # 싣는데, LOB 이면 그 자리에서 한 건씩 더 읽게 된다.
    #
    # 4000 은 바이트 기준이라 한글은 약 1333자가 상한이다. 길이 검증은
    # schemas/notice.py 가 바이트로 잰다 (여기 숫자는 매핑용일 뿐 막아주지 않는다).
    content: Mapped[str] = mapped_column(String(4000), nullable=False)

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
        return f"<Notice id={self.id} title={self.title!r}>"
