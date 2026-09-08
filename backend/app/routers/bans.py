"""Ban 재료 라우터 (STEP 9).

세 엔드포인트 전부 로그인이 필요하다. Guest는 Ban을 가질 수 없다.
STEP 10(AI 추천)에서 이 목록을 프롬프트에 넣는다.
"""

from fastapi import APIRouter, Depends, Path, status
from sqlalchemy.orm import Session

from app.core.deps import get_current_user
from app.database.database import get_db
from app.models.user import User
from app.schemas.ban import BanCreateRequest, BanResponse
from app.services.ban_service import create_ban, delete_ban, list_bans

router = APIRouter(prefix="/api/bans", tags=["bans"])

# 세 엔드포인트가 공통으로 낼 수 있는 응답.
# /docs 화면에 보여주는 설명일 뿐이고 실제 동작에는 영향이 없다.
AUTH_401 = {"description": "토큰 없음 · 만료 · 위조"}
AUTH_423 = {"description": "잠긴 계정"}


@router.get(
    "",
    response_model=list[BanResponse],
    summary="내 Ban 재료 목록",
    responses={401: AUTH_401, 423: AUTH_423},
)
def read_bans(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> list[BanResponse]:
    bans = list_bans(db, current_user)

    # DB에서 꺼낸 모델 객체를 응답용 스키마로 하나씩 바꿔 담는다.
    result = []
    for ban in bans:
        result.append(BanResponse.model_validate(ban))
    return result


@router.post(
    "",
    response_model=BanResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Ban 재료 추가",
    responses={
        401: AUTH_401,
        423: AUTH_423,
        409: {"description": "이미 등록한 재료"},
        422: {"description": "빈 재료명 · ban_type이 DISLIKE/ALLERGY가 아님"},
    },
)
def add_ban(
    payload: BanCreateRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> BanResponse:
    ban = create_ban(db, current_user, payload)
    return BanResponse.model_validate(ban)


@router.delete(
    "/{ban_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="Ban 재료 삭제",
    responses={
        401: AUTH_401,
        423: AUTH_423,
        404: {"description": "없는 id이거나 내 Ban이 아님"},
    },
)
def remove_ban(
    ban_id: int = Path(..., ge=1),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> None:
    """성공하면 204라서 본문이 없다. 프론트는 상태코드만 보면 된다."""
    delete_ban(db, current_user, ban_id)
