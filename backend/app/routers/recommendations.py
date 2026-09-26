"""AI 음식 추천 · 레시피 라우터 (STEP 10~19)."""

from fastapi import APIRouter, Depends, HTTPException, Path, status
from sqlalchemy.orm import Session

from app.core.deps import get_optional_user
from app.database.database import get_db
from app.models.user import User
from app.schemas.recommend import (
    RecipeResponse,
    RecommendRequest,
    RecommendResponse,
)
from app.services.ai_service import make_recipe, recommend
from app.services.bargain_service import get_bargains
from app.services.price_service import get_recipe_prices

router = APIRouter(prefix="/api", tags=["recommendations"])

# AI 나 쇼핑몰이 응답하지 않을 때 나가는 상태 코드.
# /docs 화면에 보여주는 설명일 뿐이고 실제 동작에는 영향이 없다.
AI_503 = {"description": "AI · 외부 API 설정 오류이거나 응답 없음"}


def as_503(exc: RuntimeError) -> HTTPException:
    """설정이 잘못됐을 때 500 대신 읽을 수 있는 503 으로 바꾼다.

    price_api 는 PRICE_PROVIDERS 에 모르는 값이 적혀 있거나 인증키가 비면
    RuntimeError 를 낸다 — 오타를 조용히 건너뛰면 "가격이 아무 데도 안 나오는데
    이유를 모르겠다"가 되기 때문이다. 그런데 이걸 아무도 잡지 않아서 화면에는
    원인 없는 500 만 떴다. ai_service 가 하던 것과 같은 방식으로 맞춘다.
    """
    return HTTPException(status.HTTP_503_SERVICE_UNAVAILABLE, str(exc))


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
    """조건에 맞는 메뉴 후보를 만든다."""
    try:
        bargains = await get_bargains() if payload.use_bargains else None
    except RuntimeError as exc:
        raise as_503(exc)

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
    """레시피를 꺼내고, 그 재료의 시장 조사가격까지 함께 담아 돌려준다."""
    recipe = make_recipe(db, current_user, food_name)

    try:
        recipe.prices = await get_recipe_prices(recipe.ingredients_json)
    except RuntimeError as exc:
        raise as_503(exc)

    return recipe
