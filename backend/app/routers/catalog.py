"""화면이 쓰는 고정 목록 라우터."""

from fastapi import APIRouter

from app.data.regions import list_regions
from app.data import (
    BALANCE_QUESTIONS,
    BUDGET_PRESETS,
    CATEGORIES,
    GAMES,
    ROULETTE,
    SEARCH_SUGGESTIONS,
)

router = APIRouter(prefix="/api/catalog", tags=["catalog"])


@router.get("", summary="화면용 고정 목록 전부")
def read_catalog() -> dict:
    """대시보드가 처음 뜰 때 필요한 목록을 한 번에 내려준다."""
    return {
        "categories": CATEGORIES,
        "roulette": ROULETTE,
        "games": GAMES,
        "balance_questions": BALANCE_QUESTIONS,
        "search_suggestions": SEARCH_SUGGESTIONS,
        "budget_presets": BUDGET_PRESETS,
    }


@router.get("/regions", summary="시·도 · 시·군·구 목록")
def read_regions() -> list[dict]:
    """회원가입 · 정보수정의 주소 드롭다운이 쓰는 목록."""
    return list_regions()
