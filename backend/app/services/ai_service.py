"""AI 추천 · 레시피 생성 비즈니스 로직 (STEP 10~14).

라우터는 HTTP 껍데기만 담당하고, 실제 규칙은 전부 여기에 있다.

이 파일이 하는 일은 세 가지다.

    1. DB 에서 조건을 모은다     — 로그인 사용자의 Ban 재료
    2. 바깥에서 조건을 모은다     — 오늘 날씨
    3. AI 를 부르고 결과를 검사한다 — 알레르기 재료가 섞여 오면 걸러낸다

3번을 하는 이유
---------------
프롬프트에 "땅콩은 절대 넣지 마세요" 라고 적어도 AI 가 지키지 않을 수 있다.
알레르기는 지키지 못했을 때의 결과가 심각하므로, 받은 답을 우리가 한 번 더
확인해서 걸러낸다. 프롬프트는 부탁이고, 이 검사가 실제 방어선이다.
"""

from fastapi import HTTPException, status
from sqlalchemy.orm import Session

from app.data import find_food
from app.integrations.ai_api import build_recipe, recommend_foods, uses_any
from app.models.user import User
from app.models.user_ban import BAN_TYPE_ALLERGY
from app.schemas.recommend import (
    FoodItem,
    RecommendRequest,
    RecommendResponse,
    RecipeResponse,
)
from app.services.ban_service import list_bans
from app.services.weather_service import get_current_weather

# AI 가 조건을 못 맞춰 후보를 0건으로 주는 일이 있다.
# 그때 예산을 이만큼 넓혀서 한 번 더 물어본다 (원).
BUDGET_RELAX = 3_000


def collect_bans(db: Session, user: User | None) -> tuple[list[str], list[str]]:
    """로그인 사용자의 Ban 재료를 (알레르기, 싫어함) 두 목록으로 나눈다.

    Guest 는 Ban 을 가질 수 없으므로 둘 다 빈 목록이다.
    """
    if user is None:
        return [], []

    allergies = []
    dislikes = []

    for ban in list_bans(db, user):
        if ban.ban_type == BAN_TYPE_ALLERGY:
            allergies.append(ban.ingredient_name)
        else:
            dislikes.append(ban.ingredient_name)

    return allergies, dislikes


def build_context(
    payload: RecommendRequest,
    allergies: list[str],
    dislikes: list[str],
    weather: dict | None,
) -> dict:
    """AI 에게 넘길 조건 딕셔너리를 만든다."""
    return {
        "limit": payload.limit,
        "category": payload.category,
        "budget_min": payload.budget_min,
        "budget_max": payload.budget_max,
        "query": payload.query,
        "ingredients": payload.ingredients,
        "allergies": allergies,
        "dislikes": dislikes,
        "weather": weather,
        "exclude": payload.exclude,
    }


def recommend(
    db: Session, user: User | None, payload: RecommendRequest
) -> RecommendResponse:
    """조건에 맞는 음식 후보를 만들어 돌려준다."""
    allergies, dislikes = collect_bans(db, user)

    weather = None
    weather_response = None
    if payload.use_weather:
        weather_response = get_current_weather(payload.lat, payload.lon)
        weather = weather_response.model_dump()

    context = build_context(payload, allergies, dislikes, weather)

    items = ask_ai(context, allergies)

    # 후보가 하나도 없으면 예산을 조금 넓혀서 한 번만 더 물어본다.
    # "8천원 이하"로 아무것도 안 나올 때 빈 화면을 주는 것보다,
    # 가장 가까운 금액의 대안을 보여주는 편이 낫다 (PROGRESS.md 요구사항 2).
    if not items and payload.budget_max is not None:
        context["budget_max"] = payload.budget_max + BUDGET_RELAX
        items = ask_ai(context, allergies)

    return RecommendResponse(
        condition=describe(payload, allergies, dislikes, weather),
        items=items,
        weather=weather_response,
    )


def ask_ai(context: dict, allergies: list[str]) -> list[FoodItem]:
    """AI 를 부르고, 알레르기 재료가 섞인 후보를 걸러낸다."""
    try:
        raw_items = recommend_foods(context)
    except RuntimeError as exc:
        # 설정이 잘못됐거나(키 없음) AI 가 응답하지 않은 경우.
        # 500 으로 새어나가면 화면에 "요청을 처리하지 못했습니다"만 뜬다.
        raise HTTPException(status.HTTP_503_SERVICE_UNAVAILABLE, str(exc))

    items = []

    for raw in raw_items:
        # AI 응답에 빠진 항목이 있으면 그 한 건만 버린다.
        # 하나 때문에 전체를 실패시키면 화면이 통째로 비어버린다.
        item = to_food_item(raw)
        if item is None:
            continue

        if uses_any(raw, allergies):
            continue

        items.append(item)

    return items


def to_food_item(raw: dict) -> FoodItem | None:
    """AI 응답 한 건을 화면용 스키마로 바꾼다. 모양이 깨졌으면 None.

    카탈로그에 있는 음식이면 일러스트(art)를 붙여준다.
    AI 가 만든 새 메뉴에는 그림이 없으므로 화면이 기본 그림을 쓴다.
    """
    name = raw.get("food_name")
    if not name:
        return None

    art = raw.get("art")
    image_url = raw.get("image_url")

    known = find_food(name)
    if known is not None:
        if art is None:
            art = known.get("art")
        if image_url is None:
            image_url = known.get("image_url")

    try:
        return FoodItem(
            food_name=name,
            category=raw.get("category") or "한식",
            reason=raw.get("reason") or "",
            cook_time_minutes=int(raw.get("cook_time_minutes") or 20),
            difficulty=raw.get("difficulty") or "보통",
            estimated_cost=int(raw.get("estimated_cost") or 0),
            tags=list(raw.get("tags") or []),
            ingredients=list(raw.get("ingredients") or []),
            art=art,
            image_url=image_url,
        )
    except (TypeError, ValueError):
        # cook_time_minutes 에 "20분" 같은 문자열이 온 경우 등
        return None


def describe(
    payload: RecommendRequest,
    allergies: list[str],
    dislikes: list[str],
    weather: dict | None,
) -> str:
    """무슨 조건으로 골랐는지 한 줄로 적는다.

    화면의 'AI 추천 기록'이 이 문장을 그대로 저장한다.
    조건을 안 남기면 "저번에 나왔던 그 메뉴"를 다시 찾을 방법이 없다.
    """
    parts = []

    if weather:
        parts.append(
            f"{weather.get('city')} · {weather.get('condition_text')} "
            f"{weather.get('temp')}°"
        )

    if payload.category:
        parts.append(payload.category)

    if payload.query:
        parts.append(f'"{payload.query}"')

    if payload.ingredients:
        parts.append(" · ".join(payload.ingredients))

    budget = describe_budget(payload)
    if budget:
        parts.append(budget)

    ban_count = len(allergies) + len(dislikes)
    if ban_count:
        parts.append(f"못 먹는 재료 {ban_count}건 제외")

    if not parts:
        return "조건 없음"

    return " · ".join(parts)


def describe_budget(payload: RecommendRequest) -> str:
    """예산 조건을 사람이 읽는 문구로 바꾼다."""
    low = payload.budget_min
    high = payload.budget_max

    if low is None and high is None:
        return ""

    if low is None:
        return f"{high:,}원 이하"

    if high is None:
        return f"{low:,}원 이상"

    return f"{low:,}원 ~ {high:,}원"


def make_recipe(db: Session, user: User | None, food_name: str) -> RecipeResponse:
    """음식 하나의 레시피를 만든다 (아직 저장하지 않는다).

    Ban 재료를 반영한다.
      알레르기  재료 목록에서 아예 뺀다
      싫어함    그대로 두되 substitutes 에 바꿀 방법을 함께 적는다
    """
    food_name = food_name.strip()
    if not food_name:
        raise HTTPException(status.HTTP_422_UNPROCESSABLE_ENTITY, "음식 이름이 없습니다.")

    allergies, dislikes = collect_bans(db, user)

    context = {
        "allergies": allergies,
        "dislikes": dislikes,
    }

    try:
        raw = build_recipe(food_name, context)
    except RuntimeError as exc:
        raise HTTPException(status.HTTP_503_SERVICE_UNAVAILABLE, str(exc))

    known = find_food(food_name)

    category = None
    art = None
    image_url = None
    if known is not None:
        category = known.get("category")
        art = known.get("art")
        image_url = known.get("image_url")

    return RecipeResponse(
        food_name=food_name,
        recipe_name=raw.get("recipe_name") or food_name,
        # meal_histories.recipe_json 에 그대로 들어갈 모양으로 묶는다
        recipe_json={
            "cook_time_minutes": raw.get("cook_time_minutes") or 20,
            "servings": raw.get("servings") or 2,
            "difficulty": raw.get("difficulty") or "보통",
            "steps": list(raw.get("steps") or []),
        },
        ingredients_json=list(raw.get("ingredients") or []),
        substitutes_json=list(raw.get("substitutes") or []),
        estimated_cost=int(raw.get("estimated_cost") or 0),
        category=category,
        art=art,
        image_url=image_url,
    )
