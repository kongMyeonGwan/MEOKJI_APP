"""손으로 쓴 레시피 100종을 꺼내 쓴다 (STEP 41).

카탈로그(app/data/catalog.json)에 있는 음식 100종은 레시피를 **미리 써서**
app/data/recipes.json 에 넣어뒀다. 여기 있는 음식이면 AI 를 부르지 않는다.

왜 AI 를 안 부르나
------------------
카탈로그 100종은 목록이 고정돼 있다. 바뀌지 않는 음식의 레시피를 열 때마다
새로 생성하면 손해만 남는다.

    느리다      제미나이 응답이 7~10초다. 그동안 화면은 빈 채로 기다린다
    흔들린다    같은 김치찌개인데 열 때마다 재료 수와 순서가 달라진다.
                어제 보고 오늘 다시 연 사람에게는 다른 요리가 된다
    돈이 든다   고정된 답을 얻으려고 매번 호출한다
    끊긴다      제미나이가 붐비면 모델을 갈아타며 재시도하고, 그동안
                DB 커넥션을 쥐고 있어서 다른 요청까지 밀린다

카탈로그에 없는 이름(사용자가 직접 검색한 "마라탕")은 미리 쓸 수 없으므로
그때만 AI 로 넘어간다. 이 파일은 "아는 음식이냐" 만 답하고, 모르면 None 을
돌려주는 것으로 끝낸다 — 무엇으로 대신할지는 services/ai_service.py 가 정한다.

돌려주는 모양
-------------
integrations/ai_api.build_recipe() 와 **똑같은 딕셔너리**를 만든다.
그래야 ai_service 가 출처를 따지지 않고 같은 코드로 이어서 쓴다.

    recipe_name · cook_time_minutes · servings · difficulty
    steps         조리 순서 (문자열 목록)
    ingredients   [{name, quantity, unit, grams}]
    substitutes   [{original, substitute, reason}]
    estimated_cost

grams 는 파일에 적지 않고 여기서 채운다. 분량이 "1대" 인지 "80g" 인지는
사람이 쓰는 값이지만, 그게 몇 그램인지는 integrations/portion.py 의 표가
이미 알고 있다. 두 군데 적어두면 한쪽만 고쳤을 때 재료비가 조용히 틀린다.
"""

import json
from pathlib import Path

from app.integrations.ai_api import DEFAULT_SUBSTITUTE, SUBSTITUTES, matches_any
from app.integrations.portion import grams_for

RECIPES_PATH = Path(__file__).resolve().parent.parent / "data" / "recipes.json"


def load_recipes() -> dict:
    """파일을 읽어 {음식이름: 레시피} 로 만든다.

    앱이 뜰 때 한 번만 읽는다. 100건짜리 파일이라 통째로 메모리에 둬도 되고,
    요청마다 읽으면 디스크를 쓸데없이 때린다.
    """
    with open(RECIPES_PATH, encoding="utf-8") as file:
        data = json.load(file)

    return data.get("recipes") or {}


RECIPES = load_recipes()


def has_recipe(food_name: str) -> bool:
    """미리 써둔 레시피가 있는 음식인가."""
    return food_name.strip() in RECIPES


def build_recipe(food_name: str, context: dict) -> dict | None:
    """미리 써둔 레시피를 Ban 재료에 맞춰 다듬어 돌려준다.

    모르는 음식이면 None. 부르는 쪽이 AI 로 넘어갈지 정한다.
    """
    found = RECIPES.get(food_name.strip())
    if found is None:
        return None

    allergies = context.get("allergies") or []
    dislikes = context.get("dislikes") or []

    ingredients = []
    substitutes = []
    removed = []

    for row in found["ingredients"]:
        name = row["name"]

        # 알레르기 재료는 목록에서 아예 뺀다
        if matches_any(name, allergies):
            removed.append(name)
            continue

        # 싫어하는 재료는 그대로 두되 바꿀 방법을 함께 적는다
        if matches_any(name, dislikes):
            replacement, reason = SUBSTITUTES.get(name, DEFAULT_SUBSTITUTE)
            substitutes.append(
                {"original": name, "substitute": replacement, "reason": reason}
            )

        quantity = row["quantity"]
        unit = row["unit"]
        ingredients.append(
            {
                "name": name,
                "quantity": quantity,
                "unit": unit,
                "grams": grams_for(name, quantity, unit),
            }
        )

    steps = list(found["steps"])

    # 뺀 재료가 있으면 순서 맨 앞에 밝힌다.
    #
    # 조리 순서는 사람이 쓴 글이라 AI 처럼 다시 만들어낼 수 없다. 재료만
    # 조용히 빼면 "땅콩을 뿌린다" 가 그대로 남아 모순이 된다. 그렇다고 그
    # 재료가 나오는 단계를 지우면 같은 줄에 적힌 다른 지시까지 사라진다.
    # 그래서 지우지 않고 맨 앞에 한 줄로 알린다 — 빠뜨리는 것보다 낫다.
    if removed:
        steps.insert(
            0,
            "알레르기 재료 {}을(를) 뺀 레시피입니다. 아래 순서에 이 재료가 "
            "나오면 넣지 말고 다음 단계로 넘어가세요.".format(" · ".join(removed)),
        )

    return {
        "recipe_name": found["recipe_name"],
        "cook_time_minutes": found["cook_time_minutes"],
        "servings": found["servings"],
        "difficulty": found["difficulty"],
        "steps": steps,
        "ingredients": ingredients,
        "substitutes": substitutes,
        "estimated_cost": found["estimated_cost"],
    }
