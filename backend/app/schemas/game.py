"""미니게임 기록 스키마."""

from datetime import datetime

from pydantic import BaseModel


class GamePlayResponse(BaseModel):
    """게임 한 판을 기록했다는 응답."""

    # 'worldcup' · 'balance' · 'roulette'. 화면이 보낸 값을 그대로 돌려준다
    game_key: str

    # DB 에 저장한 글자. A · B · C
    game_type: str

    # 기록한 시각 (UTC)
    played_at: datetime
