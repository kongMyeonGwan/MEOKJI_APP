"""GAME_HISTORIES : 미니게임 플레이 기록."""

from sqlalchemy import CHAR, Column, ForeignKey, Integer, Table, text

from app.database.database import Base
from app.models.types import UtcTimestamp

# 게임 종류 글자. DDL 의 ck_game_histories_type 과 같은 값이어야 한다.
# 글자와 게임의 대응은 services/game_service.py 의 GAME_TYPES 가 정한다.
GAME_TYPE_LENGTH = 1

game_histories = Table(
    "game_histories",
    Base.metadata,
    Column(
        "user_id",
        ForeignKey("users.id", ondelete="CASCADE"),
        nullable=False,
    ),
    Column(
        "game_type",
        CHAR(GAME_TYPE_LENGTH),
        nullable=False,
        server_default=text("'A'"),
    ),
    # 이번 판의 점수. 게임을 '시작할 때' 기록하므로 아직 점수가 없어 0 이다.
    # 결과까지 남기고 싶어지면 이 값을 채우는 쪽으로 늘리면 된다.
    Column("game_point", Integer, nullable=False, server_default="0"),
    Column(
        "created_at",
        UtcTimestamp,
        nullable=False,
        server_default=text("SYS_EXTRACT_UTC(SYSTIMESTAMP)"),
    ),
)
