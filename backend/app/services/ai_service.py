"""AI 추천 · 레시피 생성 비즈니스 로직 (STEP 10~14)."""

import time

from fastapi import HTTPException, status
from sqlalchemy.orm import Session

from app.core.config import settings
from app.data import find_food
from app.integrations import recipe_book
from app.integrations.ai_api import (
    CATALOG_PROVIDER,
    build_recipe,
    pick_josa,
    recommend_foods,
    uses_any,
)
from app.models.user import User
from app.models.user_ban import BAN_TYPE_ALLERGY
from app.schemas.recommend import (
    BargainItem,
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


# 레시피를 기억해두는 시간(초).
RECIPE_CACHE_SECONDS = 60 * 60 * 6  # 6시간

# 너무 커지면 통째로 비운다. 다시 물어보면 채워지는 값이라
# 오래된 것만 골라 지우는 정교함이 필요 없다 (weather_api 와 같은 방식).
RECIPE_CACHE_MAX_ENTRIES = 200

# {(음식이름, 알레르기, 싫어함): (저장시각, 레시피)}
recipe_cache: dict[tuple, tuple[float, RecipeResponse]] = {}


def recipe_cache_key(
    food_name: str, allergies: list[str], dislikes: list[str]
) -> tuple:
    """레시피 캐시 열쇠. Ban 목록은 순서가 달라도 같은 열쇠가 되게 정렬한다."""
    return (food_name, tuple(sorted(allergies)), tuple(sorted(dislikes)))


def read_recipe_cache(key: tuple) -> RecipeResponse | None:
    """기억해둔 레시피를 꺼낸다. 없거나 오래됐으면 None."""
    found = recipe_cache.get(key)
    if found is None:
        return None

    saved_at, recipe = found
    if time.time() - saved_at >= RECIPE_CACHE_SECONDS:
        return None

    # 복사본을 준다. 부르는 쪽이 prices 를 채워 넣는데,
    # 원본을 그대로 주면 캐시에 남의 조회 시점 가격이 눌어붙는다.
    return recipe.model_copy(deep=True)


def write_recipe_cache(key: tuple, recipe: RecipeResponse) -> None:
    """레시피를 기억해둔다."""
    if len(recipe_cache) > RECIPE_CACHE_MAX_ENTRIES:
        recipe_cache.clear()

    stored = recipe.model_copy(deep=True)
    stored.prices = []

    recipe_cache[key] = (time.time(), stored)


def collect_bans(db: Session, user: User | None) -> tuple[list[str], list[str]]:
    """로그인 사용자의 Ban 재료를 (알레르기, 싫어함) 두 목록으로 나눈다."""
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


def pick_coords(payload: RecommendRequest, user: User | None):
    """어느 좌표로 날씨를 볼지 정한다."""
    if payload.lat is not None and payload.lon is not None:
        return payload.lat, payload.lon

    if user is not None and user.latitude is not None and user.longitude is not None:
        # DB 는 NUMBER(9,6) 이라 Decimal 로 온다. float 로 바꿔서 넘긴다.
        return float(user.latitude), float(user.longitude)

    return None, None


def build_context(
    payload: RecommendRequest,
    allergies: list[str],
    dislikes: list[str],
    weather: dict | None,
    bargains: list[dict] | None = None,
) -> dict:
    """AI 에게 넘길 조건 딕셔너리를 만든다."""
    return {
        "limit": payload.limit,
        "category": payload.category,
        "budget_min": payload.budget_min,
        "budget_max": payload.budget_max,
        "query": payload.query,
        "allergies": allergies,
        "dislikes": dislikes,
        "weather": weather,
        # 요즘 값이 내린 재료. 라우터가 미리 훑어서 넘겨준다
        # (시세를 받아오는 일은 비동기라 여기서 부르지 않는다).
        "bargains": bargains or [],
        "exclude": payload.exclude,
    }


def recommend(
    db: Session,
    user: User | None,
    payload: RecommendRequest,
    bargains: list[dict] | None = None,
) -> RecommendResponse:
    """조건에 맞는 음식 후보를 만들어 돌려준다."""
    allergies, dislikes = collect_bans(db, user)

    weather = None
    weather_response = None
    if payload.use_weather:
        lat, lon = pick_coords(payload, user)
        weather_response = get_current_weather(lat, lon)
        weather = weather_response.model_dump()

    context = build_context(payload, allergies, dislikes, weather, bargains)

    items = ask_ai(context, allergies)

    # 후보가 하나도 없으면 예산을 조금 넓혀서 한 번만 더 물어본다.
    if not items and payload.budget_max is not None:
        context["budget_max"] = payload.budget_max + BUDGET_RELAX
        items = ask_ai(context, allergies)

    return RecommendResponse(
        condition=describe(payload, allergies, dislikes, weather, bargains),
        items=items,
        weather=weather_response,
        bargains=[BargainItem.model_validate(row) for row in (bargains or [])],
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
    """AI 응답 한 건을 화면용 스키마로 바꾼다."""
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
    bargains: list[dict] | None = None,
) -> str:
    """무슨 조건으로 골랐는지 한 줄로 적는다."""
    parts = []

    if weather:
        parts.append(
            f"{weather.get('city')} · {weather.get('condition_text')} "
            f"{weather.get('temp')}°"
        )

    if bargains:
        # "대파 12%↓ 외 3건" — 무슨 근거로 골랐는지 한눈에 보이게 한다
        first = bargains[0]
        text = f"{first['name']} {abs(first['percent']):.0f}%↓"
        if len(bargains) > 1:
            text += f" 외 {len(bargains) - 1}건"
        parts.append(f"시세 {text}")

    if payload.category:
        parts.append(payload.category)

    if payload.query:
        parts.append(f'"{payload.query}"')

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


def raise_or_ask_ai(food_name: str) -> None:
    """미리 써둔 레시피가 없는 음식을 어떻게 할지 정한다."""
    if settings.AI_PROVIDER.lower() not in CATALOG_PROVIDER:
        return

    josa = pick_josa(food_name, "은", "는")

    raise HTTPException(
        status.HTTP_404_NOT_FOUND,
        f"'{food_name}'{josa} 준비된 레시피가 없는 메뉴입니다. "
        "추천이나 검색에서 고른 메뉴로 다시 시도해주세요.",
    )


def make_recipe(db: Session, user: User | None, food_name: str) -> RecipeResponse:
    """음식 하나의 레시피를 만든다 (아직 저장하지 않는다)."""
    food_name = food_name.strip()
    if not food_name:
        raise HTTPException(status.HTTP_422_UNPROCESSABLE_ENTITY, "음식 이름이 없습니다.")

    allergies, dislikes = collect_bans(db, user)

    # 같은 음식을 다시 열면 AI 를 부르지 않는다. 7~10초가 0초가 된다.
    key = recipe_cache_key(food_name, allergies, dislikes)
    cached = read_recipe_cache(key)
    if cached is not None:
        return cached

    context = {
        "allergies": allergies,
        "dislikes": dislikes,
    }

    # 카탈로그 100종은 레시피를 미리 써뒀다(app/data/recipes.json).
    raw = recipe_book.build_recipe(food_name, context)

    if raw is None:
        raise_or_ask_ai(food_name)

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

    recipe = RecipeResponse(
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

    write_recipe_cache(key, recipe)

    return recipe
