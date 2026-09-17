"""화면이 필요로 하는 고정 목록 모음."""

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
SEARCH_SUGGESTIONS: list[str] = CATALOG["search_suggestions"]
BUDGET_PRESETS: list[dict] = CATALOG["budget_presets"]

# 더운 날에 먼저 보여줄 메뉴 이름들. mock 추천이 날씨를 반영하는 유일한 통로다.
HOT_WEATHER_PICKS: list[str] = CATALOG["hot_weather_picks"]


def find_food(food_name: str) -> dict | None:
    """이름으로 음식 하나를 찾는다."""
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
