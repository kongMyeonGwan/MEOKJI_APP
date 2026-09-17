"""AI 음식 추천 · 레시피 라우터 (STEP 10~19)."""

from fastapi import APIRouter, Depends, Path
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
    """레시피를 꺼내고, 그 재료의 시장 조사가격까지 함께 담아 돌려준다."""
    recipe = make_recipe(db, current_user, food_name)
    recipe.prices = await get_recipe_prices(recipe.ingredients_json)
    return recipe
