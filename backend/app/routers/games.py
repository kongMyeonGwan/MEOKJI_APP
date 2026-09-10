"""미니게임 라우터 (STEP 27).

게임 목록 자체는 /api/catalog 가 내려준다(화면에 뭘 그릴지는 고정 목록이다).
여기는 "누가 어떤 게임을 언제 시작했는가" 를 남기는 곳이다.

로그인이 필요하다. GAME_HISTORIES.USER_ID 가 NOT NULL 이라 Guest 의 판은
남길 자리가 없다. 그래서 화면은 로그인했을 때만 이 API 를 부르고,
Guest 도 게임 자체는 그대로 할 수 있다.
"""

from fastapi import APIRouter, Depends, Path, status
from sqlalchemy.orm import Session

from app.core.deps import get_current_user
from app.database.database import get_db
from app.models.user import User
from app.schemas.game import GamePlayResponse
from app.services.game_service import record_play

router = APIRouter(prefix="/api/games", tags=["games"])


@router.post(
    "/{game_key}/plays",
    response_model=GamePlayResponse,
    status_code=status.HTTP_201_CREATED,
    summary="게임 시작을 기록한다",
    responses={
        401: {"description": "토큰 없음 · 만료 · 위조"},
        423: {"description": "잠긴 계정"},
        404: {"description": "그런 게임이 없음"},
    },
)
def create_game_play(
    game_key: str = Path(..., description="worldcup · balance · roulette"),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> GamePlayResponse:
    """게임 화면이 열릴 때 한 번 부른다.

    끝난 판이 아니라 시작한 판을 센다. 세 게임 모두 도중에 나가는 일이
    흔해서, 끝난 것만 세면 "이 게임을 얼마나 열어보는가" 를 알 수 없다.
    """
    return record_play(db, current_user, game_key)
