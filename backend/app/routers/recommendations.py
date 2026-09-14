"""AI 음식 추천 · 레시피 · 재료 가격 라우터 (STEP 10~19).

로그인하지 않아도 추천을 받을 수 있다. Guest 도 대시보드에서 오늘의 메뉴를 본다.
로그인했으면 get_optional_user 가 사용자를 찾아주고, 서버가 Ban 재료를
프롬프트에 알아서 넣는다.

추천 결과와 가격은 DB 에 저장하지 않는다.
사용자가 '이걸로 먹을래요'를 눌렀을 때만 POST /api/histories 로 저장된다.
"""

from fastapi import APIRouter, Depends, Path
from sqlalchemy.orm import Session

from app.core.deps import get_optional_user
from app.database.database import get_db
from app.models.user import User
from app.schemas.recommend import (
    PriceItem,
    PriceRequest,
    RecipeResponse,
    RecommendRequest,
    RecommendResponse,
)
from app.services.ai_service import make_recipe, recommend
from app.services.bargain_service import get_bargains
from app.services.price_service import get_prices, get_recipe_prices

router = APIRouter(prefix="/api", tags=["recommendations"])

# AI 나 쇼핑몰이 응답하지 않을 때 나가는 상태 코드.
# /docs 화면에 보여주는 설명일 뿐이고 실제 동작에는 영향이 없다.
AI_503 = {"description": "AI · 외부 API 설정 오류이거나 응답 없음"}


@router.post(
    "/recommendations",
    response_model=RecommendResponse,
    summary="AI 음식 추천 (Guest 가능)",
    responses={503: AI_503},
)
async def create_recommendations(
    payload: RecommendRequest,
    current_user: User | None = Depends(get_optional_user),
    db: Session = Depends(get_db),
) -> RecommendResponse:
    """조건에 맞는 메뉴 후보를 만든다.

    GET 이 아니라 POST 인 이유는 두 가지다.
      - 냉장고 재료처럼 배열로 보내는 조건이 있어 주소창에 담기 어렵다
      - 부를 때마다 AI 가 새로 만들어내므로 캐시되면 안 된다

    async def 인 이유는 use_bargains 를 켰을 때 KAMIS 시세를 먼저
    훑어야 하기 때문이다. 시세 조회는 비동기라 recommend() 안에서
    부를 수 없어서, 여기서 받아 넘긴다.
    """
    bargains = await get_bargains() if payload.use_bargains else None

    return recommend(db, current_user, payload, bargains)


@router.get(
    "/recipes/{food_name}",
    response_model=RecipeResponse,
    summary="레시피 + 재료 가격 (Guest 가능)",
    responses={
        404: {"description": "카탈로그에 없는 음식이라 준비된 레시피가 없음"},
        503: AI_503,
    },
)
async def read_recipe(
    food_name: str = Path(..., min_length=1, max_length=150),
    current_user: User | None = Depends(get_optional_user),
    db: Session = Depends(get_db),
) -> RecipeResponse:
    """레시피를 꺼내고, 그 재료의 시장 조사가격까지 함께 담아 돌려준다.

    카탈로그 100종은 미리 써둔 레시피(app/data/recipes.json)에서 꺼낸다.
    거기 없는 이름이면 404 다 — 지어내지 않는다(services/ai_service.py 참고).

    가격 조회를 따로 부르지 않는 이유는 화면이 둘을 같이 쓰기 때문이다.
    나눠 두면 레시피가 먼저 뜨고 가격표가 나중에 나타나 화면이 한 번 튄다.

    async def 인 이유는 재료 여러 개의 가격을 병렬로 조회하기 때문이다
    (integrations/price_api.py 참고).
    """
    recipe = make_recipe(db, current_user, food_name)
    recipe.prices = await get_recipe_prices(recipe.ingredients_json)
    return recipe


@router.post(
    "/prices",
    response_model=list[PriceItem],
    summary="재료 가격 비교 (Guest 가능)",
    responses={422: {"description": "재료를 하나도 안 보냄"}},
)
async def read_prices(payload: PriceRequest) -> list[PriceItem]:
    """재료 이름 목록으로 시장별 조사가격을 조회한다.

    화면(PriceTable)이 이 중에서 최저가와 최고가를 뽑아 보여준다.
    """
    return await get_prices(payload.ingredients)
