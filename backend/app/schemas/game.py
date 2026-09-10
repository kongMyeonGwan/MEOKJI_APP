"""미니게임 기록 스키마.

기록하는 값은 셋뿐이다 — 누가 · 어떤 게임을 · 언제.
게임을 '시작할 때' 남기므로 점수는 아직 없다.
"""

from datetime import datetime

from pydantic import BaseModel


class GamePlayResponse(BaseModel):
    """게임 한 판을 기록했다는 응답.

    화면이 이 값을 그리지는 않는다. 기록이 실제로 남았는지 확인하려는
    용도이고, 나중에 "오늘 세 번째 판이에요" 같은 문구를 붙일 자리다.
    """

    # 'worldcup' · 'balance' · 'roulette'. 화면이 보낸 값을 그대로 돌려준다
    game_key: str

    # DB 에 저장한 글자. A · B · C
    game_type: str

    # 기록한 시각 (UTC)
    played_at: datetime
