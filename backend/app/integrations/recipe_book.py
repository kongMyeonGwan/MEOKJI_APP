"""손으로 쓴 레시피 100종을 꺼내 쓴다 (STEP 41)."""

import json
from pathlib import Path

from app.integrations.ai_api import DEFAULT_SUBSTITUTE, SUBSTITUTES, matches_any
from app.integrations.portion import grams_for

RECIPES_PATH = Path(__file__).resolve().parent.parent / "data" / "recipes.json"


def load_recipes() -> dict:
    """파일을 읽어 {음식이름: 레시피} 로 만든다."""
    with open(RECIPES_PATH, encoding="utf-8") as file:
        data = json.load(file)

    return data.get("recipes") or {}


RECIPES = load_recipes()


def has_recipe(food_name: str) -> bool:
    """미리 써둔 레시피가 있는 음식인가."""
    return food_name.strip() in RECIPES


def build_recipe(food_name: str, context: dict) -> dict | None:
    """미리 써둔 레시피를 Ban 재료에 맞춰 다듬어 돌려준다."""
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
