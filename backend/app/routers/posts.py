"""게시판 라우터 (STEP 20~23).

조회는 Guest도 가능하므로 get_current_user 가 아니라 get_optional_user 를 쓴다.
토큰이 만료됐다고 게시판이 에러 페이지로 바뀌면 안 된다.

반대로 글쓰기 · 좋아요 · 댓글은 get_current_user 로 로그인을 강제한다.
"""

from fastapi import APIRouter, Depends, Path, Query, status
from sqlalchemy.orm import Session

from app.core.deps import get_current_user, get_optional_user
from app.database.database import get_db
from app.models.user import User
from app.schemas.post import (
    CommentCreateRequest,
    CommentResponse,
    LikeResponse,
    PostCreateRequest,
    PostDetailResponse,
    PostListItem,
)
from app.services.post_service import (
    create_comment,
    create_post,
    get_post,
    list_posts,
    set_like,
)

router = APIRouter(prefix="/api/posts", tags=["posts"])

# 로그인이 필요한 엔드포인트가 공통으로 낼 수 있는 응답
_AUTH_RESPONSES = {
    401: {"description": "토큰 없음 · 만료 · 위조"},
    423: {"description": "잠긴 계정"},
}


@router.get(
    "",
    response_model=list[PostListItem],
    summary="게시글 목록 (Guest 가능)",
)
def read_posts(
    keyword: str | None = Query(None, max_length=100, description="제목·본문 검색어"),
    db: Session = Depends(get_db),
) -> list[PostListItem]:
    """최신순. content 는 목록용으로 잘린 미리보기다."""
    return list_posts(db, keyword)


@router.get(
    "/{post_id}",
    response_model=PostDetailResponse,
    summary="게시글 상세 + 댓글 (Guest 가능)",
    responses={404: {"description": "없는 게시글"}},
)
def read_post(
    post_id: int = Path(..., ge=1),
    current_user: User | None = Depends(get_optional_user),
    db: Session = Depends(get_db),
) -> PostDetailResponse:
    """로그인 상태면 liked / is_mine 이 함께 계산돼 온다."""
    return get_post(db, post_id, current_user)


@router.post(
    "",
    response_model=PostDetailResponse,
    status_code=status.HTTP_201_CREATED,
    summary="글쓰기 (확정한 식사에서만)",
    responses={
        **_AUTH_RESPONSES,
        404: {"description": "없는 기록이거나 내 기록이 아님"},
        409: {"description": "이미 공유한 기록"},
        422: {"description": "빈 제목·내용"},
    },
)
def write_post(
    payload: PostCreateRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> PostDetailResponse:
    """레시피/재료는 요청 본문이 아니라 서버가 원본 기록에서 복사해 넣는다."""
    return create_post(db, current_user, payload)


@router.put(
    "/{post_id}/like",
    response_model=LikeResponse,
    summary="좋아요 켜기",
    responses={**_AUTH_RESPONSES, 404: {"description": "없는 게시글"}},
)
def like_post(
    post_id: int = Path(..., ge=1),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> LikeResponse:
    """POST가 아니라 PUT이다. 같은 요청을 두 번 보내도 결과가 같다(멱등)."""
    return set_like(db, current_user, post_id, liked=True)


@router.delete(
    "/{post_id}/like",
    response_model=LikeResponse,
    summary="좋아요 끄기",
    responses={**_AUTH_RESPONSES, 404: {"description": "없는 게시글"}},
)
def unlike_post(
    post_id: int = Path(..., ge=1),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> LikeResponse:
    return set_like(db, current_user, post_id, liked=False)


@router.post(
    "/{post_id}/comments",
    response_model=CommentResponse,
    status_code=status.HTTP_201_CREATED,
    summary="댓글 작성",
    responses={
        **_AUTH_RESPONSES,
        404: {"description": "없는 게시글"},
        422: {"description": "빈 댓글 · 1000자 초과"},
    },
)
def write_comment(
    payload: CommentCreateRequest,
    post_id: int = Path(..., ge=1),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> CommentResponse:
    return create_comment(db, current_user, post_id, payload)
