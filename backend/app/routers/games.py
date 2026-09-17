"""미니게임 라우터 (STEP 27)."""

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
    """게임 화면이 열릴 때 한 번 부른다."""
    return record_play(db, current_user, game_key)
