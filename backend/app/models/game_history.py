"""GAME_HISTORIES : 미니게임 플레이 기록.

누가 · 어떤 게임을 · 언제 시작했는지만 남기는 누적 로그다.
수정도 삭제도 하지 않으므로 USE_YN 이 없고, 행 하나를 지목할 일도 없어
DDL 에 PK 도 없다.

    game_type   A  음식 월드컵   (/game/worldcup)
                B  취향 밸런스   (/game/balance)
                C  랜덤 돌림판   (/roulette)

여기만 클래스가 아니라 Table 인 이유
------------------------------------
다른 모델은 전부 `class X(Base)` 다. 이 표만 SQLAlchemy Core 의 Table 로 뒀다.

선언형 모델(Base 상속)은 PK 를 반드시 요구한다. SQLAlchemy 가 "이 객체가
DB 의 어느 행인지" 를 PK 로 따라다니기 때문이다. 그런데 이 표에는 PK 가
없다. 억지로 (user_id, created_at) 을 PK 라고 적어두면 DDL 에 없는 제약을
코드에만 적는 셈이고, 같은 회원이 같은 순간에 두 번 기록되면 SQLAlchemy 가
두 행을 하나로 착각한다.

이 표에 필요한 것은 INSERT 하나뿐이라 객체로 다룰 이유가 없다. Table 을
쓰면 PK 를 지어내지 않아도 되고, DDL 과 코드가 같은 말을 하게 된다.

    db.execute(insert(game_histories).values(user_id=1, game_type="A"))
"""

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
