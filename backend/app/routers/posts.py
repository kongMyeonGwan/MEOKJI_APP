"""게시판 라우터 (STEP 20~23).

! 아직 만들지 않은 기능이다. 담당자가 채운다.
  주소 · 요청/응답 스키마는 그대로 두었으니 함수 안만 구현하면 된다.
  화면(frontend/src/pages/CommunityPage.jsx 등)은 지금 하드코딩한
  더미 데이터로 그려지고 있다. 여기가 완성되면 그쪽도 API 로 바꾸면 된다.
"""

from datetime import date

from fastapi import (
    APIRouter,
    Depends,
    File,
    HTTPException,
    Path,
    Query,
    UploadFile,
    status,
)
from fastapi.responses import FileResponse
from sqlalchemy.orm import Session

from app.core.deps import get_current_user, get_optional_user
from app.database.database import get_db
from app.models.user import User
from app.schemas.page import DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE
from app.schemas.post import (
    CommentCreateRequest,
    CommentResponse,
    LikeResponse,
    PopularItem,
    PostCreateRequest,
    PostDetailResponse,
    PostPage,
    PostUpdateRequest,
)

router = APIRouter(prefix="/api/posts", tags=["posts"])

# 아직 구현하지 않은 엔드포인트가 내려주는 문구.
TODO_MESSAGE = "아직 만들지 않은 기능입니다. (게시판 담당자 작업 예정)"

# 로그인이 필요한 엔드포인트가 공통으로 낼 수 있는 응답.
AUTH_401 = {"description": "토큰 없음 · 만료 · 위조"}
AUTH_423 = {"description": "잠긴 계정"}
NOT_FOUND = {"description": "없는 게시글"}

@router.get(
    "",
    response_model=PostPage,
    summary="게시글 목록 (Guest 가능, 페이징)",
)
def read_posts(
    keyword: str | None = Query(None, max_length=100, description="제목·본문 검색어"),
    page: int = Query(1, ge=1, description="1부터 시작하는 페이지 번호"),
    size: int = Query(
        DEFAULT_PAGE_SIZE, ge=1, le=MAX_PAGE_SIZE, description="한 페이지 글 개수"
    ),
    db: Session = Depends(get_db),
) -> PostPage:
    """최신순.

    TODO: 담당자가 구현할 것. 서비스 함수를 만들어 여기서 부른다.
    """
    raise HTTPException(status.HTTP_501_NOT_IMPLEMENTED, TODO_MESSAGE)


@router.get(
    "/popular",
    response_model=list[PopularItem],
    summary="인기 레시피 (기간 안에 새로 받은 좋아요 기준, Guest 가능)",
)
def read_popular(
    start: date | None = Query(None, description="시작일 (YYYY-MM-DD, 한국 날짜)"),
    end: date | None = Query(None, description="종료일 (YYYY-MM-DD, 한국 날짜)"),
    limit: int = Query(20, ge=1, le=100),
    db: Session = Depends(get_db),
) -> list[PopularItem]:
    """기간을 안 주면 오늘 포함 최근 30일이다.

    TODO: 담당자가 구현할 것. 서비스 함수를 만들어 여기서 부른다.
    """
    raise HTTPException(status.HTTP_501_NOT_IMPLEMENTED, TODO_MESSAGE)


@router.get(
    "/{post_id}",
    response_model=PostDetailResponse,
    summary="게시글 상세 + 댓글 (Guest 가능)",
    responses={404: NOT_FOUND},
)
def read_post(
    post_id: int = Path(..., ge=1),
    current_user: User | None = Depends(get_optional_user),
    db: Session = Depends(get_db),
) -> PostDetailResponse:
    """로그인 상태면 liked / is_mine 이 함께 계산돼 온다.

    TODO: 담당자가 구현할 것. 서비스 함수를 만들어 여기서 부른다.
    """
    raise HTTPException(status.HTTP_501_NOT_IMPLEMENTED, TODO_MESSAGE)


@router.post(
    "",
    response_model=PostDetailResponse,
    status_code=status.HTTP_201_CREATED,
    summary="글쓰기 (확정한 식사에서만)",
    responses={
        401: AUTH_401,
        423: AUTH_423,
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
    """레시피/재료는 요청 본문이 아니라 서버가 원본 기록에서 복사해 넣는다.

    TODO: 담당자가 구현할 것. 서비스 함수를 만들어 여기서 부른다.
    """
    raise HTTPException(status.HTTP_501_NOT_IMPLEMENTED, TODO_MESSAGE)


@router.put(
    "/{post_id}",
    response_model=PostDetailResponse,
    summary="글 수정 (내 글만)",
    responses={
        401: AUTH_401,
        423: AUTH_423,
        404: {"description": "없는 게시글이거나 내 글이 아님"},
        422: {"description": "빈 제목·내용"},
    },
)
def edit_post(
    payload: PostUpdateRequest,
    post_id: int = Path(..., ge=1),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> PostDetailResponse:
    """제목과 본문만 바뀐다. 레시피는 원본 기록에서 읽어오므로 그대로다.

    TODO: 담당자가 구현할 것. 서비스 함수를 만들어 여기서 부른다.
    """
    raise HTTPException(status.HTTP_501_NOT_IMPLEMENTED, TODO_MESSAGE)


@router.delete(
    "/{post_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="글 삭제 (내 글만)",
    responses={
        401: AUTH_401,
        423: AUTH_423,
        404: {"description": "없는 게시글이거나 내 글이 아님"},
    },
)
def remove_post(
    post_id: int = Path(..., ge=1),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> None:
    """행을 지우지 않고 use_yn='N' 으로 바꾼다.

    TODO: 담당자가 구현할 것. 서비스 함수를 만들어 여기서 부른다.
    """
    raise HTTPException(status.HTTP_501_NOT_IMPLEMENTED, TODO_MESSAGE)


# 첨부파일 (후기 하나당 파일 하나)


@router.post(
    "/{post_id}/file",
    response_model=PostDetailResponse,
    summary="첨부파일 올리기 · 교체 (내 글만)",
    responses={
        401: AUTH_401,
        423: AUTH_423,
        404: {"description": "없는 게시글이거나 내 글이 아님"},
        413: {"description": "용량 초과"},
        422: {"description": "허용하지 않는 확장자이거나 빈 파일"},
    },
)
def upload_post_file(
    post_id: int = Path(..., ge=1),
    file: UploadFile = File(..., description="이미지 파일 1개"),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> PostDetailResponse:
    """첨부가 이미 있으면 새 파일로 갈아 끼운다. 옛 파일은 서버가 지운다.

    TODO: 담당자가 구현할 것. 서비스 함수를 만들어 여기서 부른다.
    """
    raise HTTPException(status.HTTP_501_NOT_IMPLEMENTED, TODO_MESSAGE)


@router.delete(
    "/{post_id}/file",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="첨부파일 떼기 (내 글만)",
    responses={
        401: AUTH_401,
        423: AUTH_423,
        404: {"description": "없는 게시글이거나 내 글이 아님"},
    },
)
def remove_post_file(
    post_id: int = Path(..., ge=1),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> None:
    """첨부가 없어도 204 다. 요청의 목적이 '첨부 없는 상태' 이기 때문이다.

    TODO: 담당자가 구현할 것. 서비스 함수를 만들어 여기서 부른다.
    """
    raise HTTPException(status.HTTP_501_NOT_IMPLEMENTED, TODO_MESSAGE)


@router.get(
    "/{post_id}/file",
    response_class=FileResponse,
    summary="첨부파일 내려받기 (Guest 가능)",
    responses={404: {"description": "없는 게시글이거나 첨부가 없음"}},
)
def download_post_file(
    post_id: int = Path(..., ge=1),
    db: Session = Depends(get_db),
) -> FileResponse:
    """업로드 폴더를 정적 서빙하지 않고 이 주소로 내려주는 이유가 두 가지다.

    TODO: 담당자가 구현할 것. 서비스 함수를 만들어 여기서 부른다.
    """
    raise HTTPException(status.HTTP_501_NOT_IMPLEMENTED, TODO_MESSAGE)


@router.post(
    "/{post_id}/view",
    summary="조회수 올리기 (Guest 가능)",
    responses={404: NOT_FOUND},
)
def count_view(
    post_id: int = Path(..., ge=1),
    db: Session = Depends(get_db),
) -> dict:
    """상세 화면이 열릴 때 한 번 부른다.

    TODO: 담당자가 구현할 것. 서비스 함수를 만들어 여기서 부른다.
    """
    raise HTTPException(status.HTTP_501_NOT_IMPLEMENTED, TODO_MESSAGE)


@router.put(
    "/{post_id}/like",
    response_model=LikeResponse,
    summary="좋아요 켜기",
    responses={401: AUTH_401, 423: AUTH_423, 404: NOT_FOUND},
)
def like_post(
    post_id: int = Path(..., ge=1),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> LikeResponse:
    """POST가 아니라 PUT이다. 같은 요청을 두 번 보내도 결과가 같다(멱등).

    TODO: 담당자가 구현할 것. 서비스 함수를 만들어 여기서 부른다.
    """
    raise HTTPException(status.HTTP_501_NOT_IMPLEMENTED, TODO_MESSAGE)


@router.delete(
    "/{post_id}/like",
    response_model=LikeResponse,
    summary="좋아요 끄기",
    responses={401: AUTH_401, 423: AUTH_423, 404: NOT_FOUND},
)
def unlike_post(
    post_id: int = Path(..., ge=1),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> LikeResponse:
    """TODO: 담당자가 구현할 것. 서비스 함수를 만들어 여기서 부른다."""
    raise HTTPException(status.HTTP_501_NOT_IMPLEMENTED, TODO_MESSAGE)


@router.post(
    "/{post_id}/comments",
    response_model=CommentResponse,
    status_code=status.HTTP_201_CREATED,
    summary="댓글 작성",
    responses={
        401: AUTH_401,
        423: AUTH_423,
        404: NOT_FOUND,
        422: {"description": "빈 댓글 · 1000자 초과"},
    },
)
def write_comment(
    payload: CommentCreateRequest,
    post_id: int = Path(..., ge=1),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> CommentResponse:
    """TODO: 담당자가 구현할 것. 서비스 함수를 만들어 여기서 부른다."""
    raise HTTPException(status.HTTP_501_NOT_IMPLEMENTED, TODO_MESSAGE)


@router.put(
    "/{post_id}/comments/{comment_id}",
    response_model=CommentResponse,
    summary="댓글 수정 (내 댓글만)",
    responses={
        401: AUTH_401,
        423: AUTH_423,
        404: {"description": "없는 댓글이거나 내 댓글이 아님"},
        422: {"description": "빈 댓글 · 1000자 초과"},
    },
)
def edit_comment(
    payload: CommentCreateRequest,
    post_id: int = Path(..., ge=1),
    comment_id: int = Path(..., ge=1),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> CommentResponse:
    """TODO: 담당자가 구현할 것. 서비스 함수를 만들어 여기서 부른다."""
    raise HTTPException(status.HTTP_501_NOT_IMPLEMENTED, TODO_MESSAGE)


@router.delete(
    "/{post_id}/comments/{comment_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="댓글 삭제 (내 댓글만)",
    responses={
        401: AUTH_401,
        423: AUTH_423,
        404: {"description": "없는 댓글이거나 내 댓글이 아님"},
    },
)
def remove_comment(
    post_id: int = Path(..., ge=1),
    comment_id: int = Path(..., ge=1),
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> None:
    """TODO: 담당자가 구현할 것. 서비스 함수를 만들어 여기서 부른다."""
    raise HTTPException(status.HTTP_501_NOT_IMPLEMENTED, TODO_MESSAGE)
