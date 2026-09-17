"""내 식사 기록 라우터 (STEP 15~16)."""

from fastapi import APIRouter, Depends, Path, Query, status
from sqlalchemy.orm import Session

from app.core.deps import get_current_user
from app.database.database import get_db
from app.models.user import User
from app.schemas.history import (
    MealHistoryCreateRequest,
    MealHistoryPage,
    MealHistoryResponse,
    MealHistoryStats,
)
from app.schemas.page import DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE
from app.services.history_service import (
    create_history,
    delete_history,
    get_stats,
    list_histories,
)

router = APIRouter(prefix="/api/histories", tags=["histories"])

# 네 엔드포인트가 공통으로 낼 수 있는 응답.
# /docs 화면에 보여주는 설명일 뿐이고 실제 동작에는 영향이 없다.
AUTH_401 = {"description": "토큰 없음 · 만료 · 위조"}
AUTH_423 = {"description": "잠긴 계정"}


@router.get(
    "",
    response_model=MealHistoryPage,
    summary="내 식사 기록 목록 (페이징)",
    responses={401: AUTH_401, 423: AUTH_423},
)
def read_histories(
    page: int = Query(1, ge=1, description="1부터 시작하는 페이지 번호"),
    size: int = Query(
        DEFAULT_PAGE_SIZE, ge=1, le=MAX_PAGE_SIZE, description="한 페이지 개수"
    ),
    shared: bool | None = Query(
        None, description="true=공유한 것만 · false=아직 안 한 것만 · 생략=전부"
    ),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> MealHistoryPage:
    """최신순."""
    return list_histories(db, current_user, page, size, shared)


@router.get(
    "/stats",
    response_model=MealHistoryStats,
    summary="내 기록 요약 (건수 · 누적 비용 · 연속 방문일)",
    responses={401: AUTH_401, 423: AUTH_423},
)
def read_stats(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> MealHistoryStats:
    """주소가 /{history_id} 보다 위에 있어야 한다."""
    return get_stats(db, current_user)


@router.post(
    "",
    response_model=MealHistoryResponse,
    status_code=status.HTTP_201_CREATED,
    summary="식사 확정 (이걸로 먹을래요)",
    responses={
        401: AUTH_401,
        423: AUTH_423,
        422: {"description": "빈 음식 이름 · 레시피 모양이 아님"},
    },
)
def add_history(
    payload: MealHistoryCreateRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> MealHistoryResponse:
    """AI 가 만들어준 레시피를 확정 시점 스냅샷으로 저장한다."""
    return create_history(db, current_user, payload)


@router.delete(
    "/{history_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="식사 기록 삭제",
    responses={
        401: AUTH_401,
        423: AUTH_423,
        404: {"description": "없는 id이거나 내 기록이 아님"},
        409: {"description": "커뮤니티에 공유한 기록"},
    },
)
def remove_history(
    history_id: int = Path(..., ge=1),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> None:
    """행을 지우지 않고 use_yn='N' 으로 바꾼다."""
    delete_history(db, current_user, history_id)
