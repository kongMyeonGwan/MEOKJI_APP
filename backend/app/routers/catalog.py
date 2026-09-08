"""화면이 쓰는 고정 목록 라우터.

카테고리 칩 · 룰렛 조각 · 게임 목록 · 밸런스 질문 · 냉장고 재료 칩 ·
예시 검색어 · 예산 프리셋 · 공지사항을 내려준다.

전부 로그인 없이 볼 수 있다. Guest 도 대시보드를 쓰기 때문이다.

DB 가 아니라 파일(app/data/catalog.json)에서 읽는다.
사용자가 만드는 값이 아니라서 테이블을 만들 이유가 없고,
"테이블은 7개로 고정한다"는 설계 원칙도 지킬 수 있다.
"""

from fastapi import APIRouter

from app.data.regions import list_regions
from app.data import (
    BALANCE_QUESTIONS,
    BUDGET_PRESETS,
    CATEGORIES,
    FRIDGE_GROUPS,
    GAMES,
    NOTICES,
    ROULETTE,
    SEARCH_SUGGESTIONS,
)

router = APIRouter(prefix="/api/catalog", tags=["catalog"])


@router.get("", summary="화면용 고정 목록 전부")
def read_catalog() -> dict:
    """대시보드가 처음 뜰 때 필요한 목록을 한 번에 내려준다.

    항목마다 따로 부르면 화면 하나에 요청이 일곱 번 나간다.
    합쳐도 몇 KB 라서 한 번에 받는 편이 빠르다.
    """
    return {
        "categories": CATEGORIES,
        "roulette": ROULETTE,
        "games": GAMES,
        "balance_questions": BALANCE_QUESTIONS,
        "fridge_groups": FRIDGE_GROUPS,
        "search_suggestions": SEARCH_SUGGESTIONS,
        "budget_presets": BUDGET_PRESETS,
    }


@router.get("/regions", summary="시·도 · 시·군·구 목록")
def read_regions() -> list[dict]:
    """회원가입 · 정보수정의 주소 드롭다운이 쓰는 목록.

    /api/catalog 에 합치지 않고 따로 뺐다. 250개가 넘어서 합쳐두면
    대시보드를 열 때마다 쓰지도 않는 목록을 같이 받게 된다.
    이 목록이 필요한 화면은 회원가입과 마이페이지뿐이다.
    """
    return list_regions()


@router.get("/notices", summary="공지사항 목록")
def read_notices() -> list[dict]:
    """공지는 운영자만 쓴다. 고정 · 최신 순으로 내려준다."""
    pinned = []
    normal = []

    for notice in NOTICES:
        if notice.get("pinned"):
            pinned.append(notice)
        else:
            normal.append(notice)

    return pinned + normal
