"""미니게임 플레이 기록 (STEP 27)."""

from datetime import datetime, timezone

from fastapi import HTTPException, status
from sqlalchemy import insert
from sqlalchemy.orm import Session

from app.models.game_history import game_histories
from app.models.user import User
from app.schemas.game import GamePlayResponse

# 화면의 게임 키 -> DB 에 넣을 글자.
GAME_TYPES = {
    "worldcup": "A",  # 음식 월드컵  — 후보를 둘씩 비교해 좁힌다
    "balance": "B",   # 취향 밸런스  — 질문 다섯 개로 조건을 만든다
    "roulette": "C",  # 랜덤 돌림판  — 운에 맡긴다
}


def to_game_type(game_key: str) -> str:
    """게임 키를 DB 글자로 바꾼다."""
    game_type = GAME_TYPES.get(game_key)

    if game_type is None:
        raise HTTPException(
            status.HTTP_404_NOT_FOUND, f"그런 게임이 없습니다: {game_key}"
        )

    return game_type


def record_play(db: Session, user: User, game_key: str) -> GamePlayResponse:
    """게임을 시작했다는 사실을 한 줄 남긴다."""
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
