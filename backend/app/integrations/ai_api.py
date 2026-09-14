"""AI 음식 추천 · 레시피 생성 (STEP 10~11).

.env 의 AI_PROVIDER 값에 따라 세 가지로 동작한다.

    catalog  AI 를 부르지 않는다. 카탈로그(app/data/catalog.json)의 음식 100종에서
             오늘 날씨 · 예산 · 냉장고 재료 · Ban 재료에 맞는 것을 골라준다.
             응답 모양은 AI 와 똑같아서 바깥에서는 구분할 필요가 없다
    gemini   Google Gemini 를 부른다
    claude   Anthropic Claude 를 부른다

catalog 가 기본값인 이유
------------------------
추천이 카탈로그 안에서만 나오면, 추천받은 메뉴는 반드시 미리 써둔 레시피
(app/data/recipes.json)가 있다. AI 에게 물으면 카탈로그 밖의 이름이 나올 수
있고, 그 메뉴를 누르면 레시피를 만들려고 AI 를 한 번 더 불러야 한다.

'mock' 이라고 부르던 것이다. 가짜 응답을 흉내 내는 코드가 아니라 실제 추천
규칙이라서 이름을 바꿨다. 팀원 .env 가 아직 mock 이어도 동작하게 둘 다 받는다.

프로바이더가 달라도 프롬프트와 응답 스키마는 **똑같은 것을 쓴다**.
바꿔 끼웠을 때 추천의 성격까지 달라지면 비교가 불가능해지기 때문이다.
다르게 만드는 건 "그 회사 API 를 어떻게 부르느냐" 뿐이다.

바깥에서는 두 함수만 쓴다.

    recommend_foods(context)              후보 목록을 만든다
    build_recipe(food_name, context)      한 음식의 레시피를 만든다

context 는 app/services/ai_service.py 가 만들어 넘기는 딕셔너리다.
이 파일은 DB 를 모른다. "누구의 Ban 인지" 가 아니라 "빼야 할 재료 목록" 만 받는다.

    {
      "limit":       후보 개수,
      "category":    "한식" 또는 None,
      "budget_min":  하한(원) 또는 None,
      "budget_max":  상한(원) 또는 None,
      "query":       "8천원 이하 파스타" 같은 문장 또는 None,
      "ingredients": ["계란", "대파"]   냉장고에 있는 재료,
      "allergies":   ["땅콩"]           반드시 제외,
      "dislikes":    ["오이"]           가능하면 피하고 대체재료 제시,
      "weather":     {"temp": 28, "condition_text": "맑음", ...} 또는 None,
      "exclude":     ["김치찌개"]       이미 보여준 메뉴,
    }

왜 Structured Output 인가
-------------------------
자유 텍스트로 받으면 "1. 김치찌개 - 얼큰합니다" 같은 답이 오는데,
줄바꿈 하나만 달라져도 파싱이 깨진다. 아래 SCHEMA 로 모양을 못 박아두고
그 모양으로만 답하게 하면, 응답을 그대로 화면에 쓸 수 있다.
"""

import hashlib
import json
import time

from app.core.config import settings
from app.integrations.ingredient_match import vocabulary
from app.data import FOODS, HOT_WEATHER_PICKS, category_labels
from app.integrations.portion import grams_for, portion_for
from app.integrations.steps import build_steps

# AI 를 부르지 않고 카탈로그에서 고르는 모드의 이름.
#
# 'mock' 은 예전 이름이다. 계속 받아주는 이유는 .env 가 팀원마다 따로 있어서,
# 이름을 바꾼 순간 남의 로컬 서버가 "지원하지 않는 AI_PROVIDER" 로 안 뜨기
# 때문이다. 새로 적을 때는 catalog 를 쓴다.
CATALOG_PROVIDER = ("catalog", "mock")

# AI 응답을 기다리는 최대 시간(초).
# 프론트의 axios timeout 이 30초라, 그보다 짧게 잡아야 우리 에러 메시지가 나간다.
AI_TIMEOUT_SECONDS = 25.0

# 모델이 붐빌 때(503) 다시 시도하는 횟수와 간격(초).
#
# 무료 등급에서는 "This model is currently experiencing high demand" 가
# 심심찮게 나온다. 한 번 실패했다고 화면에 에러를 띄우면, 사용자는 자기가
# 뭘 잘못한 줄 안다. 잠깐 기다렸다 다시 부르면 대부분 통과한다.
AI_RETRY_COUNT = 2
AI_RETRY_WAIT_SECONDS = 1.5

# 한 번에 만들 수 있는 후보 개수의 상한.
# 많이 받아봐야 화면에서 다 안 보고, 토큰만 늘어난다.
MAX_FOODS = 12

# 난이도는 세 가지로 고정한다. 화면의 필터와 밸런스 게임이 이 값으로 비교한다.
DIFFICULTIES = ("쉬움", "보통", "어려움")


# =========================================================
# 응답 스키마 — claude 와 catalog 가 똑같은 모양을 만든다
# =========================================================

FOODS_SCHEMA = {
    "type": "object",
    "properties": {
        "foods": {
            "type": "array",
            "items": {
                "type": "object",
                "properties": {
                    "food_name": {"type": "string", "description": "음식 이름"},
                    "category": {
                        "type": "string",
                        "enum": category_labels(),
                        "description": "분류",
                    },
                    "reason": {
                        "type": "string",
                        "description": "왜 이걸 추천하는지 한 문장 (40자 이내)",
                    },
                    "cook_time_minutes": {"type": "integer"},
                    "difficulty": {"type": "string", "enum": list(DIFFICULTIES)},
                    "estimated_cost": {
                        "type": "integer",
                        "description": "2인분 기준 예상 재료비(원)",
                    },
                    "tags": {
                        "type": "array",
                        "items": {"type": "string"},
                        "description": "해시태그 2~3개 (예: 국물, 얼큰)",
                    },
                    "ingredients": {
                        "type": "array",
                        "items": {"type": "string"},
                        "description": "주재료 이름만 3~5개",
                    },
                },
                "required": [
                    "food_name",
                    "category",
                    "reason",
                    "cook_time_minutes",
                    "difficulty",
                    "estimated_cost",
                    "tags",
                    "ingredients",
                ],
            },
        }
    },
    "required": ["foods"],
}

RECIPE_SCHEMA = {
    "type": "object",
    "properties": {
        "recipe_name": {
            "type": "string",
            "description": "레시피 이름 (예: 돼지고기 김치찌개)",
        },
        "cook_time_minutes": {"type": "integer"},
        "servings": {"type": "integer", "description": "몇 인분인지"},
        "difficulty": {"type": "string", "enum": list(DIFFICULTIES)},
        "steps": {
            "type": "array",
            "items": {"type": "string"},
            "description": "만드는 순서. 한 줄에 한 동작",
        },
        "ingredients": {
            "type": "array",
            "items": {
                "type": "object",
                "properties": {
                    "name": {"type": "string"},
                    "quantity": {"type": "number"},
                    "unit": {"type": "string", "description": "g, 개, 큰술 등"},
                    # 가격 환산에 쓴다.
                    #
                    # KAMIS 는 "고춧가루 1kg 25,200원" 처럼 큰 단위로만 값을 준다.
                    # 레시피가 "1큰술"을 요구하는데 1kg 값을 그대로 더하면
                    # 재료비가 백 배 넘게 부풀려진다. 그램으로 받아야
                    # "필요한 만큼이 얼마인지" 를 계산할 수 있다.
                    #
                    # 큰술/작은술/컵 같은 계량 단위는 AI 가 사람보다 잘 안다.
                    # 우리가 변환표를 만드는 것보다 물어보는 편이 정확하다.
                    "grams": {
                        "type": "number",
                        "description": (
                            "이 재료의 필요량을 그램으로 환산한 값. "
                            "액체는 ml 을 g 으로 봅니다(물 100ml = 100). "
                            "예: 1큰술=15, 1작은술=5, 대파 0.5대=50, "
                            "감자 1개=170, 계란 1개=55"
                        ),
                    },
                },
                "required": ["name", "quantity", "unit", "grams"],
            },
        },
        "substitutes": {
            "type": "array",
            "items": {
                "type": "object",
                "properties": {
                    "original": {"type": "string"},
                    "substitute": {"type": "string"},
                    "reason": {"type": "string"},
                },
                "required": ["original", "substitute", "reason"],
            },
            "description": "싫어하는 재료를 바꿀 방법. 없으면 빈 배열",
        },
        "estimated_cost": {"type": "integer", "description": "예상 재료비(원)"},
    },
    "required": [
        "recipe_name",
        "cook_time_minutes",
        "servings",
        "difficulty",
        "steps",
        "ingredients",
        "estimated_cost",
    ],
}


# =========================================================
# 바깥에서 부르는 두 함수
# =========================================================


def recommend_foods(context: dict) -> list[dict]:
    """조건에 맞는 음식 후보를 만든다."""
    provider = settings.AI_PROVIDER.lower()

    if provider in CATALOG_PROVIDER:
        return catalog_recommend_foods(context)

    if provider == "gemini":
        raw_items = gemini_generate(build_foods_prompt(context), FOODS_SCHEMA)
        return cut_foods(raw_items, context)

    if provider == "claude":
        require_api_key()
        return claude_recommend_foods(context)

    # 모르는 값이면 조용히 catalog 로 넘어가지 않고 에러를 낸다.
    # 조용히 넘어가면 "AI 를 붙였는데 추천이 늘 똑같다"가 된다.
    raise RuntimeError(
        f"지원하지 않는 AI_PROVIDER: {settings.AI_PROVIDER}. "
        "사용 가능한 값: catalog, gemini, claude"
    )


def cut_foods(result: dict, context: dict) -> list[dict]:
    """AI 응답에서 후보 목록만 꺼내고 요청한 개수만큼 자른다.

    개수를 지켜달라고 프롬프트에 적어두지만 더 주는 경우가 있어서
    받는 쪽에서 한 번 더 자른다.
    """
    foods = result.get("foods")
    if not foods:
        return []

    return foods[: get_limit(context)]


def build_recipe(food_name: str, context: dict) -> dict:
    """음식 하나의 레시피를 만든다."""
    provider = settings.AI_PROVIDER.lower()

    if provider in CATALOG_PROVIDER:
        return catalog_build_recipe(food_name, context)

    if provider == "gemini":
        return gemini_generate(build_recipe_prompt(food_name, context), RECIPE_SCHEMA)

    if provider == "claude":
        require_api_key()
        return claude_build_recipe(food_name, context)

    raise RuntimeError(
        f"지원하지 않는 AI_PROVIDER: {settings.AI_PROVIDER}. "
        "사용 가능한 값: catalog, gemini, claude"
    )


def require_api_key() -> None:
    """키가 비어 있으면 여기서 바로 알려준다."""
    if not settings.ANTHROPIC_API_KEY:
        raise RuntimeError(
            "AI_PROVIDER=claude 인데 ANTHROPIC_API_KEY 가 비어 있습니다."
        )


# =========================================================
# Gemini — 진짜 AI
# =========================================================


def gemini_generate(prompt: str, schema: dict) -> dict:
    """Gemini 를 부르고, 스키마대로 채워진 딕셔너리를 돌려준다.

    Claude 는 "도구를 하나 주고 그걸로만 답하게" 해서 모양을 고정하는데,
    Gemini 는 응답 스키마를 직접 받는다(response_schema). 방식만 다르고
    목적은 같다 — 문장이 아니라 정해진 JSON 으로 받는 것.

    같은 FOODS_SCHEMA / RECIPE_SCHEMA 를 두 회사가 그대로 쓴다.
    """
    # import 를 함수 안에서 한다. AI_PROVIDER=catalog 로 쓰는 팀원이
    # google-genai 를 안 깔았어도 서버가 뜨게 하기 위함이다.
    from google import genai
    from google.genai import errors, types

    if not settings.GEMINI_API_KEY:
        raise RuntimeError(
            "AI_PROVIDER=gemini 인데 GEMINI_API_KEY 가 비어 있습니다. "
            "https://aistudio.google.com 에서 발급받아 backend/.env 에 넣으세요."
        )

    client = genai.Client(api_key=settings.GEMINI_API_KEY)

    config = types.GenerateContentConfig(
        # 이 둘이 한 쌍이다. mime_type 만 주면 스키마 없는 아무 JSON 이 오고,
        # schema 만 주면 그걸 무시한 문장이 올 수 있다.
        response_mime_type="application/json",
        response_schema=schema,
        http_options=types.HttpOptions(
            # 밀리초 단위다. 초로 착각해서 25 를 넣으면 0.025초 만에 끊긴다.
            timeout=int(AI_TIMEOUT_SECONDS * 1000),
        ),
    )

    response = call_gemini_with_retry(client, prompt, config, errors)

    # response_schema 를 줬으므로 본문은 JSON 문자열이다.
    # 그래도 파싱은 감싸둔다. 안전 필터에 걸리면 본문이 비어서 올 수 있다.
    try:
        return json.loads(response.text)
    except (TypeError, ValueError):
        raise RuntimeError(
            "AI 응답을 해석하지 못했습니다. 잠시 후 다시 시도해주세요."
        )


def call_gemini_with_retry(client, prompt: str, config, errors):
    """붐빌 때(503) 잠깐 쉬었다 다시 부르고, 그래도 안 되면 예비 모델로 넘어간다.

    무료 등급에서는 "This model is currently experiencing high demand" 가
    심심찮게 나온다. 사용자가 뭘 잘못한 게 아니라 남의 서버가 붐비는 것이라,
    한 번 실패했다고 바로 에러 화면을 띄우지 않는다.

    순서는 이렇다.
        1번 모델 -> (붐빔) 잠깐 쉬고 다시 -> 또 붐비면 -> 2번 모델 -> ...

    429(할당량 초과)도 같이 재시도한다. 분당 제한이라면 곧 풀린다.
    그 외(잘못된 키 · 없는 모델)는 기다려도 안 풀리므로 바로 올린다.
    """
    models = settings.gemini_model_list

    if not models:
        raise RuntimeError(
            "GEMINI_MODEL 이 비어 있습니다. backend/.env 를 확인해주세요."
        )

    last_error = None

    for model in models:
        for attempt in range(AI_RETRY_COUNT + 1):
            try:
                return client.models.generate_content(
                    model=model,
                    contents=prompt,
                    config=config,
                )
            except errors.APIError as exc:
                last_error = exc

                # 기다린다고 풀릴 오류가 아니면(잘못된 키 등) 바로 알린다
                if exc.code not in (429, 503):
                    raise RuntimeError(describe_gemini_error(exc, model))

                # 이 모델의 마지막 시도였으면 쉬지 말고 다음 모델로 넘어간다
                if attempt < AI_RETRY_COUNT:
                    time.sleep(AI_RETRY_WAIT_SECONDS)

        print(f"[Gemini] {model} 이(가) 붐벼서 다음 모델로 넘어갑니다.", flush=True)

    raise RuntimeError(describe_gemini_error(last_error, models[-1]))


def describe_gemini_error(exc, model: str) -> str:
    """Gemini 오류를 화면에 띄울 만한 문구로 바꾼다.

    원문은 영어라 그대로 내보내면 사용자가 읽지 못한다.
    개발자가 원인을 짚을 수 있을 만큼만 남긴다.
    """
    code = getattr(exc, "code", None)

    if code == 400:
        return (
            "AI 요청이 거부되었습니다. GEMINI_API_KEY 가 올바른지 확인해주세요."
        )
    if code == 404:
        return (
            f"AI 모델을 찾을 수 없습니다: {model}. "
            "backend/.env 의 GEMINI_MODEL 을 확인해주세요."
        )
    if code == 429:
        return "AI 사용량 한도를 넘었습니다. 잠시 후 다시 시도해주세요."
    if code == 503:
        return "AI 서버가 붐비고 있습니다. 잠시 후 다시 시도해주세요."

    return "AI 응답을 받지 못했습니다. 잠시 후 다시 시도해주세요."


# =========================================================
# Claude — 진짜 AI
# =========================================================


def call_claude(prompt: str, tool_name: str, schema: dict) -> dict:
    """Claude 를 부르고, 스키마대로 채워진 딕셔너리를 돌려준다.

    tools 에 함수 하나를 알려주고 tool_choice 로 "반드시 이걸 써서 답해라" 고
    시킨다. 그러면 답이 문장이 아니라 schema 모양의 JSON 으로 온다.
    """
    # import 를 함수 안에서 한다. AI_PROVIDER=catalog 로 쓰는 팀원이
    # anthropic 패키지를 안 깔았어도 서버가 뜨게 하기 위함이다.
    import anthropic

    client = anthropic.Anthropic(
        api_key=settings.ANTHROPIC_API_KEY,
        timeout=AI_TIMEOUT_SECONDS,
    )

    message = client.messages.create(
        model=settings.AI_MODEL,
        max_tokens=4096,
        tools=[
            {
                "name": tool_name,
                "description": "이 형식에 맞춰 결과를 제출한다",
                "input_schema": schema,
            }
        ],
        # "무조건 이 도구를 써라". 안 주면 그냥 문장으로 답할 때가 있다.
        tool_choice={"type": "tool", "name": tool_name},
        messages=[{"role": "user", "content": prompt}],
    )

    for block in message.content:
        if block.type == "tool_use":
            return block.input

    # tool_choice 를 줬으므로 정상적으로는 여기까지 오지 않는다.
    raise RuntimeError("AI 응답에서 결과를 찾지 못했습니다. 잠시 후 다시 시도해주세요.")


def claude_recommend_foods(context: dict) -> list[dict]:
    """Claude 에게 후보 목록을 받아온다."""
    result = call_claude(build_foods_prompt(context), "submit_foods", FOODS_SCHEMA)

    return cut_foods(result, context)


def claude_build_recipe(food_name: str, context: dict) -> dict:
    """Claude 에게 레시피 한 건을 받아온다."""
    prompt = build_recipe_prompt(food_name, context)
    return call_claude(prompt, "submit_recipe", RECIPE_SCHEMA)


def build_foods_prompt(context: dict) -> str:
    """후보 추천용 프롬프트를 만든다."""
    lines = [
        "당신은 한국 가정식 요리 추천 도우미입니다.",
        f"집에서 직접 만들어 먹을 메뉴를 {get_limit(context)}개 제안해주세요.",
        "",
        "규칙",
        "- 배달 음식이 아니라 집에서 만드는 요리만 제안합니다.",
        "- estimated_cost 는 2인분 기준 재료비를 원 단위 정수로 적습니다.",
        "- reason 은 왜 지금 이 메뉴인지 한 문장으로 적습니다.",
        "- 같은 메뉴를 두 번 넣지 않습니다.",
        "",
        "조건",
    ]
    lines.extend(describe_conditions(context))

    return "\n".join(lines)


def build_recipe_prompt(food_name: str, context: dict) -> str:
    """레시피 생성용 프롬프트를 만든다."""
    lines = [
        f"'{food_name}' 을(를) 집에서 만드는 레시피를 알려주세요.",
        "",
        "규칙",
        "- steps 는 순서대로, 한 줄에 한 동작만 적습니다.",
        "- ingredients 의 quantity 는 숫자, unit 은 g · 개 · 큰술 처럼 짧게 적습니다.",
        "- ingredients 의 grams 에는 그 양을 그램으로 환산한 값을 반드시 적습니다.",
        "  (1큰술=15, 1작은술=5, 종이컵 1컵=180, 대파 1대=100, 감자 1개=170)",
        "  물처럼 값을 매기지 않는 재료도 grams 는 적습니다.",
        "- ingredients 의 name 에는 재료 이름만 적습니다.",
        "  손질 방법과 괄호 설명은 넣지 않습니다. 그건 steps 에 적습니다.",
        "  (O) 마늘 · 밀가루 · 대파      (X) 다진 마늘 · 밀가루(중력분) · 잘게 썬 대파",
        "- 아래 목록에 있는 재료는 반드시 목록의 이름 그대로 적습니다.",
        "  시세를 붙일 수 있는 이름이라, 다르게 적으면 가격이 나오지 않습니다.",
        "  목록에 없는 재료는 자유롭게 적으면 됩니다.",
        f"  [{' · '.join(vocabulary())}]",
        "- estimated_cost 는 재료비 합계를 원 단위 정수로 적습니다.",
        "",
        "조건",
    ]
    lines.extend(describe_conditions(context))

    return "\n".join(lines)


def describe_conditions(context: dict) -> list[str]:
    """context 를 사람이 읽는 조건 문장으로 바꾼다.

    프롬프트의 '조건' 아래에 그대로 붙는다.
    두 프롬프트가 같은 문장을 써야 결과의 성격도 같아진다.
    """
    lines = []

    allergies = context.get("allergies") or []
    if allergies:
        # 알레르기는 대체재료로도 넣으면 안 된다. 가장 강한 말로 적는다.
        lines.append(
            "- 알레르기 재료(절대 사용 금지, 대체재료로도 넣지 말 것): "
            + ", ".join(allergies)
        )

    dislikes = context.get("dislikes") or []
    if dislikes:
        lines.append(
            "- 싫어하는 재료(가능하면 빼고, 꼭 필요하면 substitutes 에 대체안을 적을 것): "
            + ", ".join(dislikes)
        )

    category = context.get("category")
    if category:
        lines.append(f"- 분류: {category}")

    budget_max = context.get("budget_max")
    budget_min = context.get("budget_min")
    if budget_max is not None:
        # 만들어 놓고 거르면 후보가 0건이 되기 쉬우므로 생성 단계에서 제한한다.
        lines.append(f"- 재료비 상한: {budget_max}원 이하로만 제안할 것")
    if budget_min:
        lines.append(f"- 재료비 하한: {budget_min}원 이상")

    ingredients = context.get("ingredients") or []
    if ingredients:
        lines.append(
            "- 지금 냉장고에 있는 재료(이 재료를 최대한 쓰고, 새로 살 재료는 적게): "
            + ", ".join(ingredients)
        )

    bargains = context.get("bargains") or []
    if bargains:
        cheap = ", ".join(
            f"{row['name']}({row['percent']:.0f}%)" for row in bargains
        )
        lines.append(
            "- 요즘 값이 내린 재료입니다(괄호는 일주일 전 대비 등락률): "
            + cheap
        )
        lines.append(
            "  이 중 두 가지 이상을 주재료로 쓰는 메뉴를 우선 추천하세요. "
            "reason 에 어떤 재료가 싸서 골랐는지 적으세요."
        )

    weather = context.get("weather")
    if weather:
        lines.append(
            f"- 오늘 날씨: {weather.get('city', '')} "
            f"{weather.get('condition_text', '')} {weather.get('temp')}도 "
            f"(체감 {weather.get('feels_like')}도). 이 날씨에 어울리는 메뉴로."
        )

    query = context.get("query")
    if query:
        lines.append(f'- 사용자가 적은 말: "{query}" (이 요청을 최우선으로 해석할 것)')

    exclude = context.get("exclude") or []
    if exclude:
        lines.append("- 이미 보여준 메뉴(빼고 새로 제안): " + ", ".join(exclude))

    if not lines:
        lines.append("- 특별한 조건 없음. 무난하고 대중적인 메뉴로.")

    return lines


# =========================================================
# catalog — AI 를 부르지 않고 카탈로그에서 고른다
#
# 카탈로그(app/data/catalog.json)에서 조건에 맞는 것을 골라 점수를 매긴다.
# 랜덤을 쓰지 않는다. 같은 조건이면 늘 같은 결과가 나와야 화면 버그를 잡을 수 있다.
# =========================================================


def get_limit(context: dict) -> int:
    """만들 후보 개수. 값이 없거나 이상하면 5개."""
    limit = context.get("limit")

    if not isinstance(limit, int) or limit < 1:
        return 5

    return min(limit, MAX_FOODS)


def catalog_recommend_foods(context: dict) -> list[dict]:
    """카탈로그에서 조건에 맞는 후보를 골라준다."""
    allergies = context.get("allergies") or []
    dislikes = context.get("dislikes") or []
    exclude = context.get("exclude") or []
    category = context.get("category")
    budget_min = context.get("budget_min")
    budget_max = context.get("budget_max")
    fridge = context.get("ingredients") or []
    query = context.get("query")
    weather = context.get("weather")

    scored = []

    for food in FOODS:
        name = food["food_name"]

        if name in exclude:
            continue

        # 알레르기 재료가 들어간 메뉴는 후보에서 통째로 뺀다
        if uses_any(food, allergies):
            continue

        if category and food["category"] != category:
            continue

        cost = food["estimated_cost"]
        if budget_min is not None and cost < budget_min:
            continue
        if budget_max is not None and cost > budget_max:
            continue

        score = score_food(food, query, fridge, weather)

        # 싫어하는 재료가 들어가면 순위를 뒤로 미룬다(빼지는 않는다).
        # 대체재료를 제시할 수 있기 때문이다.
        if uses_any(food, dislikes):
            score -= 4

        scored.append((score, cost, name, food))

    # 점수가 높은 순 -> 싼 순 -> 이름순.
    # 마지막 이름순이 없으면 점수가 같을 때 순서가 들쭉날쭉해 보인다.
    scored.sort(key=sort_key)

    results = []
    for _score, _cost, _name, food in scored[: get_limit(context)]:
        results.append(to_food_response(food))

    return results


def sort_key(row):
    """정렬 기준: 점수 내림차순, 가격 오름차순, 이름 오름차순."""
    score, cost, name, _food = row
    return (-score, cost, name)


def uses_any(food: dict, names: list[str]) -> bool:
    """이 음식이 주어진 재료 중 하나라도 쓰는지 확인한다.

    글자가 완전히 같은지만 보지 않는다.
    '돼지고기'를 등록했는데 레시피에 '돼지고기 앞다리살'로 적혀 있으면
    못 걸러내기 때문이다. 한쪽이 다른 쪽을 품고 있으면 같은 재료로 본다.
    """
    if not names:
        return False

    haystack = list(food.get("ingredients") or [])
    haystack.append(food["food_name"])

    for banned in names:
        banned = banned.strip()
        if not banned:
            continue

        for item in haystack:
            if banned in item or item in banned:
                return True

    return False


def score_food(food: dict, query, fridge: list[str], weather) -> int:
    """조건에 얼마나 맞는지 점수를 매긴다. 높을수록 앞에 온다."""
    score = 0

    if query:
        score += score_query(food, query)

    if fridge:
        score += score_fridge(food, fridge)

    # 검색어가 있으면 날씨는 보지 않는다.
    # '매운 국물요리'를 찾았는데 더운 날이라고 냉면이 위로 올라오면
    # 검색이 고장 난 것처럼 보인다. 사용자가 직접 말한 조건이 우선이다.
    if weather and not query:
        score += score_weather(food, weather)

    return score


def score_query(food: dict, query: str) -> int:
    """검색 문장이 이 음식과 얼마나 맞는지.

    이름이 맞을 때를 가장 높게 준다. '김치찌개'를 찾는 사람에게
    김치가 들어간 다른 메뉴가 먼저 나오면 검색이 고장 난 것처럼 보인다.
    """
    words = split_words(query)
    score = 0

    for word in words:
        if word in food["food_name"]:
            score += 10
        elif word in food["category"]:
            score += 6
        elif word_in_list(word, food.get("tags") or []):
            score += 6
        elif word_in_list(word, food.get("ingredients") or []):
            score += 4
        elif word in food["reason"]:
            score += 2

    return score


def word_in_list(word: str, items: list[str]) -> bool:
    """낱말이 목록의 어느 값에든 걸치는지."""
    for item in items:
        if word in item or item in word:
            return True
    return False


def split_words(query: str) -> list[str]:
    """검색 문장을 낱말로 쪼갠다. 한 글자짜리는 아무 데나 걸려서 뺀다."""
    words = []

    for raw in query.replace(",", " ").split():
        word = raw.strip("?!.·")
        if len(word) > 1:
            words.append(word)

    return words


def score_fridge(food: dict, fridge: list[str]) -> int:
    """가진 재료가 많이 겹칠수록, 더 살 게 적을수록 높게."""
    need = food.get("ingredients") or []
    if not need:
        return 0

    have = 0
    for item in need:
        for mine in fridge:
            if mine in item or item in mine:
                have += 1
                break

    if have == 0:
        return -5  # 가진 재료가 하나도 없으면 이 화면에서 보여줄 이유가 없다

    missing = len(need) - have

    # 가진 것 하나당 +3, 더 사야 하는 것 하나당 -2
    return have * 3 - missing * 2


def is_cold_dish(food: dict) -> bool:
    """차게 먹는 음식인지.

    태그 하나만 봐서는 새는 것이 있다. 회냉면 · 비빔국수는 '시원한음식'
    태그가 없고 '여름메뉴' 만 달려 있어서, 태그만 보면 눈 오는 날 추천
    목록에 그대로 남는다.

    HOT_WEATHER_PICKS 는 "더운 날 먼저 보여줄 메뉴" 로 만든 목록인데,
    들어있는 15건이 전부 차게 먹는 것이라 여기서도 그대로 쓸 수 있다.
    """
    tags = food.get("tags") or []

    return (
        food["food_name"] in HOT_WEATHER_PICKS
        or "시원한음식" in tags
        or "여름메뉴" in tags
    )


def score_weather(food: dict, weather: dict) -> int:
    """날씨에 어울리면 가산점.

    기온과 하늘 상태를 따로 보고 더한다. 둘은 서로 독립이다 —
    장마철에는 28도인데 비가 오고, 12월에는 5도인데 맑다.

    더하는 쪽으로 겹치면 그만큼 더 세게 밀리고(눈 오는 추운 날의 국물),
    반대로 겹치면 서로 깎는다(비 오는 더운 날의 냉면). 어느 쪽도
    한쪽이 다른 쪽을 덮어쓰지 않는다.
    """
    return score_temperature(food, weather.get("temp")) + score_sky(
        food, weather.get("condition")
    )


def score_temperature(food: dict, temp: int | None) -> int:
    """기온에 어울리면 가산점.

    규칙은 셋이다.
      더운 날(25도 이상)    시원한 메뉴
      추운 날(15도 미만)    국물 · 얼큰한 메뉴
      선선한 날(15~24도)   '선선한' 태그를 살짝 앞으로, 찬 음식은 살짝 뒤로

    가운데 구간을 나중에 넣었다. 서울은 봄·가을이 통째로 이 구간이라,
    비워두면 일 년의 상당 부분에서 날씨가 추천에 반영되지 않는다.
    """
    if temp is None:
        return 0

    tags = food.get("tags") or []

    if temp >= 25:
        if food["food_name"] in HOT_WEATHER_PICKS:
            return 8
        if "시원한음식" in tags or "면" in tags:
            return 4
        if "국물" in tags:
            return -3  # 더운 날 뜨끈한 국물은 뒤로
        return 0

    if temp < 15:
        if "국물" in tags or "얼큰" in tags:
            return 6
        if is_cold_dish(food):
            return -6
        return 0

    # 15 ~ 24도. 덥지도 춥지도 않은 구간이다.
    #
    # 전에는 여기서 곧바로 0 을 돌려줬다. 그런데 서울은 봄·가을 두 철이 통째로
    # 이 구간이라, 일 년의 상당 부분에서 날씨가 추천에 전혀 반영되지 않았다.
    #
    # 점수를 작게 주는 이유
    # --------------------
    # 이 구간에는 "반드시 이것" 이 없다. 더운 날의 냉면이나 추운 날의 국물처럼
    # 강한 선호가 생기지 않는다. 그래서 +3 / -2 로 살짝만 기울인다.
    # 크게 주면 Ban · 냉장고 재료 같은 다른 조건을 덮어써 버린다.
    if "선선한" in tags:
        return 3

    # 찬 음식은 조금 뒤로. 20도에 냉면이 1등으로 오르는 것은 이르다.
    if is_cold_dish(food):
        return -2

    return 0


def score_sky(food: dict, condition: str | None) -> int:
    """하늘 상태(비 · 눈)에 어울리면 가산점.

    맑음 · 흐림은 0 이다. 메뉴를 미는 근거가 되지 않는다.

    비 오는 날에 튀김을 올리는 이유
    -------------------------------
    기온만 보면 비 오는 28도 장마날에 냉면이 1등으로 올라온다. 그 날
    실제로 사람들이 찾는 건 부침개 · 칼국수 쪽이다. 온도가 놓치는 자리가
    여기라서 하늘 상태를 따로 본다.

    눈이 더 센 이유
    ---------------
    눈이 오면 기온도 낮아서 기온 점수와 겹친다. 겹치는 쪽으로 확실히
    밀어주는 편이 맞다 — 눈 오는 날 냉면을 추천받고 싶은 사람은 없다.
    """
    if condition == "RAIN":
        tags = food.get("tags") or []

        if "국물" in tags:
            return 6
        if "튀김" in tags:
            return 4
        if is_cold_dish(food):
            return -5
        return 0

    if condition == "SNOW":
        if "국물" in (food.get("tags") or []):
            return 7
        if is_cold_dish(food):
            return -8
        return 0

    return 0


def to_food_response(food: dict) -> dict:
    """카탈로그의 음식 하나를 AI 응답과 같은 모양으로 바꾼다."""
    return {
        "food_name": food["food_name"],
        "category": food["category"],
        "reason": food["reason"],
        "cook_time_minutes": food["cook_time_minutes"],
        "difficulty": food["difficulty"],
        "estimated_cost": food["estimated_cost"],
        "tags": list(food.get("tags") or []),
        "ingredients": list(food.get("ingredients") or []),
        "art": food.get("art"),
        "image_url": food.get("image_url"),
    }


# ---------------------------------------------------------
# 카탈로그 레시피 만들기
#
# 카탈로그에는 주재료 이름만 있고 분량과 조리 순서가 없다.
# 여기서 재료명을 숫자로 바꿔 분량을 만들고, 태그를 보고 조리 순서를 만든다.
# md5 를 쓰는 이유는 파이썬 내장 hash() 가 서버를 다시 켤 때마다 값이 달라져서,
# 같은 음식인데 새로고침할 때마다 분량이 바뀌기 때문이다.
# ---------------------------------------------------------

# 재료 성격별 기본 분량과 단위.
# 앞의 글자가 재료명에 들어 있으면 그 규칙을 쓴다. 위에 있는 것이 우선이다.
QUANTITY_RULES = [
    (("소금", "설탕", "간장", "고추장", "된장", "참기름", "식용유"), (1, 3, "큰술")),
    (("들깨가루", "고춧가루", "다진마늘"), (1, 3, "큰술")),
    (("돼지고기", "소고기", "닭고기", "닭다리살", "닭가슴살", "해물", "연어"), (200, 400, "g")),
    (("밀가루", "소면", "스파게티면", "떡", "신김치", "김치", "양배추"), (100, 300, "g")),
    (("계란", "달걀", "감자", "양파", "애호박", "고구마", "토마토", "오이", "바나나"), (1, 3, "개")),
    (("대파",), (1, 2, "대")),
    (("두부",), (1, 1, "모")),
    (("우유", "생크림"), (100, 300, "ml")),
    (("밥",), (1, 2, "공기")),
]

# 위 표에 없는 재료에 쓸 기본값
DEFAULT_QUANTITY = (50, 200, "g")

# 카탈로그 레시피의 grams 를 채울 때 쓰는 단위별 대표 무게.
# 진짜 AI 는 재료마다 다르게 답한다. 이건 화면과 계산 경로가 도는지만
# 확인하려고 두는 값이라 정확할 필요가 없다.
CATALOG_GRAMS_PER_UNIT = {
    "g": 1, "ml": 1,
    "큰술": 15, "작은술": 5, "컵": 180, "공기": 210,
    "개": 100, "대": 100, "모": 300, "장": 3, "마리": 300, "포기": 2500,
}

# 싫어하는 재료를 뺄 때 제안할 대체재료.
SUBSTITUTES = {
    "오이": ("애호박", "아삭한 식감을 살리면서 향은 약합니다"),
    "고수": ("깻잎", "향채 역할을 대신하면서 거부감이 적습니다"),
    "당근": ("파프리카", "단맛과 색을 비슷하게 냅니다"),
    "가지": ("애호박", "볶았을 때 식감이 비슷합니다"),
    "돼지고기": ("닭고기", "기름기가 적고 값도 쌉니다"),
    "소고기": ("돼지고기", "국물 맛을 비슷하게 내면서 훨씬 쌉니다"),
    "닭고기": ("두부", "단백질을 채우면서 조리가 간단합니다"),
    "해물": ("두부", "비린내 없이 국물 맛을 냅니다"),
    "우유": ("두유", "질감이 비슷하고 유당이 없습니다"),
    "치즈": ("두부", "고소한 맛을 대신합니다"),
    "계란": ("두부", "부드러운 식감을 대신합니다"),
    "밀가루": ("쌀가루", "글루텐 없이 비슷하게 반죽됩니다"),
}

DEFAULT_SUBSTITUTE = ("생략", "빼고 만들어도 맛이 크게 달라지지 않습니다")


def catalog_build_recipe(food_name: str, context: dict) -> dict:
    """카탈로그를 바탕으로 레시피 한 건을 만든다.

    카탈로그에 없는 음식(사용자가 직접 검색한 이름)이면 재료를 모르므로,
    이름에서 만들어낸 기본 재료로 최소한의 레시피를 만든다.
    """
    food = None
    for item in FOODS:
        if item["food_name"] == food_name:
            food = item
            break

    if food is None:
        food = make_unknown_food(food_name)

    names = list(food.get("ingredients") or [])
    if not names:
        names = ["양파", "대파", "소금"]

    allergies = context.get("allergies") or []
    dislikes = context.get("dislikes") or []

    ingredients = []
    substitutes = []

    for name in names:
        # 알레르기 재료는 레시피에서 아예 뺀다
        if matches_any(name, allergies):
            continue

        if matches_any(name, dislikes):
            replacement, reason = SUBSTITUTES.get(name, DEFAULT_SUBSTITUTE)
            substitutes.append(
                {"original": name, "substitute": replacement, "reason": reason}
            )

        # 분량은 integrations/portion.py 의 표에서 가져온다.
        # 전에는 이름에서 지어냈는데(make_quantity) 참깨 90g 처럼 말이 안 되는
        # 값이 나왔고, 그 분량이 재료비 계산에 그대로 들어갔다.
        quantity, unit = portion_for(name)
        ingredients.append(
            {
                "name": name,
                "quantity": quantity,
                "unit": unit,
                "grams": grams_for(name, quantity, unit),
            }
        )

    minutes = food["cook_time_minutes"]

    return {
        "recipe_name": make_recipe_name(food),
        "cook_time_minutes": minutes,
        "servings": 2,
        "difficulty": food["difficulty"],
        "steps": make_steps(food, ingredients),
        "ingredients": ingredients,
        "substitutes": substitutes,
        "estimated_cost": food["estimated_cost"],
    }


def make_unknown_food(food_name: str) -> dict:
    """카탈로그에 없는 이름으로 최소한의 음식 정보를 만든다."""
    digest = hashlib.md5(food_name.encode("utf-8")).digest()

    return {
        "food_name": food_name,
        "category": "한식",
        "reason": "직접 찾으신 메뉴예요.",
        "cook_time_minutes": 20 + digest[0] % 25,
        "difficulty": DIFFICULTIES[digest[1] % 2],  # 쉬움 또는 보통
        "estimated_cost": (5_000 + digest[2] * 40) // 100 * 100,
        "tags": [],
        "ingredients": ["양파", "대파", "계란", "소금"],
        "art": None,
        "image_url": None,
    }


def matches_any(name: str, names: list[str]) -> bool:
    """재료 이름 하나가 목록의 어느 값에든 걸치는지."""
    for other in names:
        other = other.strip()
        if other and (other in name or name in other):
            return True
    return False


def catalog_grams(quantity: float, unit: str) -> float:
    """카탈로그 레시피의 grams 를 대충 채운다.

    진짜 AI 는 재료마다 다르게 답하지만, 여기서는 화면과 계산 경로가
    도는지만 보면 되므로 단위별 대표값 하나로 곱한다.
    """
    per = CATALOG_GRAMS_PER_UNIT.get(unit, 100)
    return round(quantity * per, 1)


def make_quantity(name: str) -> tuple[float, str]:
    """재료명으로 분량과 단위를 정한다. 같은 재료는 늘 같은 값이 나온다."""
    low, high, unit = DEFAULT_QUANTITY

    for keywords, rule in QUANTITY_RULES:
        matched = False
        for keyword in keywords:
            if keyword in name:
                matched = True
                break
        if matched:
            low, high, unit = rule
            break

    digest = hashlib.md5(name.encode("utf-8")).digest()

    if low == high:
        return low, unit

    span = high - low
    value = low + digest[0] % (span + 1)

    # g · ml 은 10 단위로 떨어뜨려야 진짜 레시피처럼 보인다
    if unit in ("g", "ml") and value >= 50:
        value = value // 10 * 10

    return value, unit


# 레시피 이름 앞에 붙이지 않는 재료.
#
# 양념과 기본 재료다. '소금 오이무침' 이나 '밀가루 부침개' 는 주재료를
# 말해주지 않는다.
NOT_A_HEADLINE = {
    "소금", "설탕", "밥", "밀가루", "식용유", "참깨", "고춧가루",
    "마늘", "생강", "대파", "쪽파", "양파", "레몬", "액젓", "천일염",
}


def make_recipe_name(food: dict) -> str:
    """레시피 이름을 만든다. 주재료를 앞에 붙인다.

    '김치찌개' -> '돼지고기 김치찌개'

    앞에 안 붙이는 경우가 둘 있다.
      - 양념·기본 재료뿐일 때      '소금 오이무침' 은 말이 안 된다
      - 이미 이름에 들어 있을 때    '닭고기 닭가슴살 샐러드' 가 된다

    둘 중 하나라도 걸리면 원래 이름을 그대로 쓴다. 억지로 앞에 뭔가 붙이는
    것보다 '수박 주스' 로 두는 편이 낫다.
    """
    food_name = food["food_name"]

    for name in food.get("ingredients") or []:
        if name in NOT_A_HEADLINE:
            continue

        # 이미 이름에 들어 있으면 겹쳐 쓰지 않는다.
        # '닭가슴살 샐러드' 의 '닭고기' 처럼 글자가 겹치는 경우도 막는다.
        if name in food_name or food_name[:2] in name:
            continue

        return f"{name} {food_name}"

    return food_name


def make_steps(food: dict, ingredients: list[dict]) -> list[str]:
    """조리 순서를 만든다.

    실제 문장은 integrations/steps.py 가 만든다. 여기서는 어떤 요리인지
    (tags)와 무엇이 얼마나 들어가는지(ingredients)만 넘긴다.

    이름 목록이 아니라 재료 dict 를 넘기는 이유
    -------------------------------------------
    전에는 이름만 넘겨서 "돼지고기를 넣고 7분간 끓인다" 까지가 한계였다.
    분량을 같이 넘기면 "돼지고기 300g 을 넣고" 라고 쓸 수 있고, 재료마다
    손질법도 붙일 수 있다. 알레르기로 빠진 재료가 조리 순서에도 안 나오는
    효과는 덤이다 — ingredients 는 이미 걸러진 목록이다.
    """
    return build_steps(
        food["food_name"], food.get("tags") or [], ingredients, food["cook_time_minutes"]
    )


def pick_josa(word: str, with_final: str, without_final: str) -> str:
    """앞 글자의 받침에 따라 조사를 고른다.

        돼지고기 -> 를      (받침 없음)
        신김치   -> 를
        두부     -> 를
        대파     -> 를
        달걀     -> 을      (받침 있음)

    "돼지고기 을(를)" 처럼 두 개를 다 적으면 레시피가 기계가 쓴 글처럼 읽힌다.

    한글 코드는 가(0xAC00)부터 힣(0xD7A3)까지 순서대로 놓여 있고,
    한 글자는 (초성 x 21 + 중성) x 28 + 종성 으로 만들어진다.
    그래서 28로 나눈 나머지가 0이면 받침이 없는 글자다.
    """
    if not word:
        return without_final

    last = word[-1]
    code = ord(last)

    # 한글이 아니면(영어 · 숫자) 받침을 알 수 없다. 무난한 쪽을 쓴다.
    if code < 0xAC00 or code > 0xD7A3:
        return without_final

    if (code - 0xAC00) % 28 == 0:
        return without_final

    return with_final


def dump_context(context: dict) -> str:
    """디버깅용. 서버 콘솔에 조건을 한 줄로 찍을 때 쓴다."""
    return json.dumps(context, ensure_ascii=False)
