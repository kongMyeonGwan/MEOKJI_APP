"""미니게임 플레이 기록 (STEP 27).

무엇을 남기나
-------------
게임을 시작하는 순간 누가 · 어떤 게임을 · 언제 시작했는지를 GAME_HISTORIES 에
한 줄 넣는다. 점수는 넣지 않는다 — 시작 시점에는 아직 없다.

왜 '시작' 인가
--------------
세 게임 모두 끝까지 안 가고 도중에 나가는 일이 흔하다. 월드컵은 일곱 번을
눌러야 하고, 밸런스는 질문 다섯 개를 지나야 한다. 끝난 판만 세면 "이 게임을
얼마나 열어보는가" 를 알 수 없다. 지금 알고 싶은 것은 그쪽이다.

글자 하나로 두는 이유
---------------------
표에 'worldcup' 같은 이름을 매 행에 넣으면 로그가 그만큼 무거워진다.
글자와 게임의 대응은 아래 GAME_TYPES 한 곳에만 적어두고, DB 에는 글자만 넣는다.
"""

from datetime import datetime, timezone

from fastapi import HTTPException, status
from sqlalchemy import insert
from sqlalchemy.orm import Session

from app.models.game_history import game_histories
from app.models.user import User
from app.schemas.game import GamePlayResponse

# 화면의 게임 키 -> DB 에 넣을 글자.
#
# 키는 catalog.json 의 games[].key 와 같아야 한다. 화면이 그 값을 그대로
# 보내오기 때문이다. DDL 의 ck_game_histories_type 은 여기 값들만 허용한다 —
# 한쪽만 늘리면 INSERT 가 ORA-02290 으로 막힌다.
GAME_TYPES = {
    "worldcup": "A",  # 음식 월드컵  — 후보를 둘씩 비교해 좁힌다
    "balance": "B",   # 취향 밸런스  — 질문 다섯 개로 조건을 만든다
    "roulette": "C",  # 랜덤 돌림판  — 운에 맡긴다
}


def to_game_type(game_key: str) -> str:
    """게임 키를 DB 글자로 바꾼다. 모르는 키면 404.

    조용히 넘기지 않는 이유는, 오타 하나로 기록이 통째로 비는 것을
    나중에 알아채기가 매우 어렵기 때문이다.
    """
    game_type = GAME_TYPES.get(game_key)

    if game_type is None:
        raise HTTPException(
            status.HTTP_404_NOT_FOUND, f"그런 게임이 없습니다: {game_key}"
        )

    return game_type


def record_play(db: Session, user: User, game_key: str) -> GamePlayResponse:
    """게임을 시작했다는 사실을 한 줄 남긴다.

    created_at 을 파이썬에서 넣지 않고 DB 기본값(SYS_EXTRACT_UTC)에 맡긴다.
    서버 여러 대의 시계가 조금씩 다를 수 있어서, 시각은 DB 한 곳에서
    찍는 편이 일관된다.
    """
    game_type = to_game_type(game_key)

    db.execute(insert(game_histories).values(user_id=user.id, game_type=game_type))
    db.commit()

    # 방금 넣은 행을 다시 읽어오지 않는다. PK 가 없어서 "방금 그 행" 을
    # 정확히 집을 방법이 없고, 화면도 이 시각을 쓰지 않는다.
    return GamePlayResponse(
        game_key=game_key,
        game_type=game_type,
        played_at=datetime.now(timezone.utc),
    )
