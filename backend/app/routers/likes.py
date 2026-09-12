"""찜한 레시피 라우터.

전부 로그인이 필요하다. 비로그인 상태에서 하트를 누르면 화면이 로그인으로
보내므로, 여기까지 Guest 요청이 올 일은 없다 — 그래도 서버가 막는다.
버튼을 숨기는 것은 잠그는 것이 아니다.

찜을 켜고 끄는 것을 한 엔드포인트로 받는 이유는 schemas/like.py 참고.
"""

from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from app.core.deps import get_current_user
from app.database.database import get_db
from app.models.user import User
from app.schemas.like import LikeListResponse, LikeToggleRequest, LikeToggleResponse
from app.services.like_service import clear_likes, list_likes, toggle_like

router = APIRouter(prefix="/api/likes", tags=["likes"])

AUTH_401 = {"description": "토큰 없음 · 만료 · 위조"}
AUTH_423 = {"description": "잠긴 계정"}


@router.get(
    "",
    response_model=LikeListResponse,
    summary="내 찜 목록",
    responses={401: AUTH_401, 423: AUTH_423},
)
def read_likes(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> LikeListResponse:
    """최근 찜한 순. 페이징하지 않는다.

    화면이 "이 음식이 찜돼 있나" 를 판단하려면 목록 전체를 들고 있어야 한다.
    한 페이지만 받으면 2페이지에 있는 음식의 하트가 빈 채로 그려진다.
    """
    return list_likes(db, current_user)


@router.put(
    "",
    response_model=LikeToggleResponse,
    summary="찜하기 · 찜 해제 (누를 때마다 뒤집힘)",
    responses={401: AUTH_401, 423: AUTH_423, 422: {"description": "빈 음식 이름"}},
)
def put_like(
    payload: LikeToggleRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> LikeToggleResponse:
    """하트를 누른 결과를 돌려준다 — 바뀐 상태(liked)와 전체 개수(count).

    화면이 개수를 직접 세지 않게 서버가 계산해 준다. 게시글 좋아요와 같다.
    """
    return toggle_like(db, current_user, payload.food_name)


@router.delete(
    "",
    response_model=LikeListResponse,
    summary="찜 전체 비우기",
    responses={401: AUTH_401, 423: AUTH_423},
)
def delete_likes(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> LikeListResponse:
    """설정 화면의 '찜 비우기'. 행을 지우지 않고 use_yn='N' 으로 바꾼다."""
    return clear_likes(db, current_user)
