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

import time

from fastapi import HTTPException, status
from sqlalchemy.orm import Session

from app.data import find_food
from app.integrations.ai_api import build_recipe, recommend_foods, uses_any
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
#
# 같은 음식의 레시피는 몇 시간이 지나도 달라질 이유가 없다. 그런데 제미나이를
# 부르는 데 7~10초가 걸린다. 발표 리허설에서 김치찌개를 다섯 번 열면
# 다섯 번 다 기다려야 하고, AI 호출도 다섯 번 나간다.
RECIPE_CACHE_SECONDS = 60 * 60 * 6  # 6시간

# 너무 커지면 통째로 비운다. 다시 물어보면 채워지는 값이라
# 오래된 것만 골라 지우는 정교함이 필요 없다 (weather_api 와 같은 방식).
RECIPE_CACHE_MAX_ENTRIES = 200

# {(음식이름, 알레르기, 싫어함): (저장시각, 레시피)}
#
# Ban 을 열쇠에 넣는 이유: 같은 김치찌개라도 돼지고기 알레르기가 있는 사람에게는
# 다른 레시피가 나간다. 이름만으로 묶으면 남의 알레르기 재료가 섞인 레시피를
# 보여주게 된다.
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
    """레시피를 기억해둔다. 가격은 빼고 저장한다.

    가격은 조회 시점의 값이라 캐시하면 안 된다(설계 원칙 §13).
    6시간 전 가격을 오늘 가격으로 보여주게 된다.
    """
    if len(recipe_cache) > RECIPE_CACHE_MAX_ENTRIES:
        recipe_cache.clear()

    stored = recipe.model_copy(deep=True)
    stored.prices = []

    recipe_cache[key] = (time.time(), stored)


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


def pick_coords(payload: RecommendRequest, user: User | None):
    """어느 좌표로 날씨를 볼지 정한다. 우선순위가 있다.

        1) 브라우저가 준 위치      지금 있는 곳이 가장 정확하다
        2) 회원가입 때 고른 주소   위치 권한을 거부했거나 지원 안 하는 브라우저
        3) None                   서버가 기본 좌표(종로2가)로 답한다

    2번이 없으면 위치 권한을 거부한 사람은 서울에 살든 부산에 살든
    영영 종로 날씨를 보게 된다. 주소를 받아둔 이유가 이것이다.
    """
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
        "ingredients": payload.ingredients,
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
    """조건에 맞는 음식 후보를 만들어 돌려준다.

    bargains 는 라우터가 미리 훑어 넘겨준다. 시세 조회가 비동기라
    이 함수(동기) 안에서 부를 수 없기 때문이다.
    """
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
    # "8천원 이하"로 아무것도 안 나올 때 빈 화면을 주는 것보다,
    # 가장 가까운 금액의 대안을 보여주는 편이 낫다 (PROGRESS.md 요구사항 2).
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
    bargains: list[dict] | None = None,
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

    # 같은 음식을 다시 열면 AI 를 부르지 않는다. 7~10초가 0초가 된다.
    key = recipe_cache_key(food_name, allergies, dislikes)
    cached = read_recipe_cache(key)
    if cached is not None:
        return cached

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
