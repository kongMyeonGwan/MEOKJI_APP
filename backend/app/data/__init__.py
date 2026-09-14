"""화면이 필요로 하는 고정 목록 모음.

카테고리 칩 · 룰렛 조각 · 밸런스 게임 질문 · 냉장고 재료 칩처럼
"사용자가 만들지 않는 값"들이다. 테이블을 만들 이유가 없어서 파일
하나(catalog.json)에 담아두고 API 로 내려준다.

공지사항은 여기 있다가 NOTICES 표로 옮겼다. 운영하면서 계속 새로 쓰는
값이라, 파일에 두면 공지 하나 올릴 때마다 서버를 다시 배포해야 한다.

왜 프론트엔드에 두지 않는가
---------------------------
전에는 frontend/src/mocks/index.js 에 있었다. 그러면 목록을 하나 고칠 때마다
프론트를 다시 배포해야 하고, 백엔드(AI 프롬프트)와 프론트(화면)가 서로 다른
음식 목록을 들고 있게 된다. 실제로 냉장고 찾기와 검색이 같은 재료 이름을
봐야 매칭이 되므로, 한 곳에서 관리한다.

foods 목록의 쓰임
-----------------
AI_PROVIDER=catalog 일 때 추천 후보로 쓰는 데이터다.
API 키가 없어도 화면 전체를 눌러볼 수 있게 하기 위한 것이며,
AI_PROVIDER=claude 로 바꾸면 이 목록 대신 AI 가 만든 후보가 나간다.

쓰는 법
-------
    from app.data import CATALOG, FOODS

    for food in FOODS:
        print(food["food_name"])
"""

import json
from pathlib import Path

# 이 파일(app/data/__init__.py) 옆에 있는 catalog.json 을 절대 경로로 찾는다.
# 상대 경로로 열면 서버를 어느 폴더에서 켰는지에 따라 실패한다.
CATALOG_PATH = Path(__file__).resolve().parent / "catalog.json"

with CATALOG_PATH.open(encoding="utf-8") as catalog_file:
    CATALOG: dict = json.load(catalog_file)

# 자주 쓰는 것들은 이름을 따로 붙여둔다.
FOODS: list[dict] = CATALOG["foods"]
CATEGORIES: list[dict] = CATALOG["categories"]
ROULETTE: list[dict] = CATALOG["roulette"]
GAMES: list[dict] = CATALOG["games"]
BALANCE_QUESTIONS: list[dict] = CATALOG["balance_questions"]
FRIDGE_GROUPS: list[dict] = CATALOG["fridge_groups"]
SEARCH_SUGGESTIONS: list[str] = CATALOG["search_suggestions"]
BUDGET_PRESETS: list[dict] = CATALOG["budget_presets"]

# 더운 날에 먼저 보여줄 메뉴 이름들. mock 추천이 날씨를 반영하는 유일한 통로다.
HOT_WEATHER_PICKS: list[str] = CATALOG["hot_weather_picks"]


def find_food(food_name: str) -> dict | None:
    """이름으로 음식 하나를 찾는다. 없으면 None.

    레시피 화면이 /api/recipes/{음식이름} 으로 들어올 때 쓴다.
    """
    for food in FOODS:
        if food["food_name"] == food_name:
            return food
    return None


def category_labels() -> list[str]:
    """'전체'를 뺀 카테고리 이름 목록. AI 프롬프트에 넣어 분류를 고정한다."""
    labels = []
    for category in CATEGORIES:
        if category["key"] != "ALL":
            labels.append(category["label"])
    return labels
