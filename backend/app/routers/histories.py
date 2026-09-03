"""내 식사 기록 라우터 (STEP 16).

로그인이 필요하다. 남의 기록은 어떤 경로로도 조회할 수 없다.
"""

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.core.deps import get_current_user
from app.database.database import get_db
from app.models.user import User
from app.schemas.history import MealHistoryResponse
from app.services.history_service import list_histories

router = APIRouter(prefix="/api/histories", tags=["histories"])


@router.get(
    "",
    response_model=list[MealHistoryResponse],
    summary="내 식사 기록 목록",
    responses={
        401: {"description": "토큰 없음 · 만료 · 위조"},
        423: {"description": "잠긴 계정"},
    },
)
def read_histories(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> list[MealHistoryResponse]:
    """최신순. 이미 공유한 기록에는 shared_post_id 가 채워져 온다."""
    return list_histories(db, current_user)
