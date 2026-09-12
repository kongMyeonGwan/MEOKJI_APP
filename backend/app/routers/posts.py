"""게시판 라우터 (STEP 20~23).

조회는 Guest도 가능하므로 get_current_user 가 아니라 get_optional_user 를 쓴다.
토큰이 만료됐다고 게시판이 에러 페이지로 바뀌면 안 된다.

반대로 글쓰기 · 수정 · 삭제 · 좋아요 · 댓글은 get_current_user 로 로그인을 강제한다.

주소를 적는 순서에 주의
-----------------------
/popular 를 /{post_id} 보다 **위에** 적어야 한다.
아래에 적으면 FastAPI 가 "popular" 를 post_id 로 읽으려다 422 를 낸다.
"""

from datetime import date, timedelta

from fastapi import APIRouter, Depends, File, Path, Query, UploadFile, status
from fastapi.responses import FileResponse
from sqlalchemy.orm import Session

from app.core.deps import get_current_user, get_optional_user
from app.core.kst import today_kst
from app.database.database import get_db
from app.models.user import User
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
from app.services.post_service import (
    DEFAULT_PAGE_SIZE,
    attach_file,
    create_comment,
    create_post,
    delete_comment,
    delete_post,
    detach_file,
    get_post,
    increase_view,
    list_popular,
    list_posts,
    open_post_file,
    set_like,
    update_comment,
    update_post,
)

router = APIRouter(prefix="/api/posts", tags=["posts"])

# 로그인이 필요한 엔드포인트가 공통으로 낼 수 있는 응답.
# /docs 화면에 "이 API 는 401 도 낼 수 있다" 고 적어주는 설명일 뿐,
# 실제 동작에는 영향이 없다.
AUTH_401 = {"description": "토큰 없음 · 만료 · 위조"}
AUTH_423 = {"description": "잠긴 계정"}
NOT_FOUND = {"description": "없는 게시글"}

# 인기 레시피 기본 기간: 오늘 포함 최근 30일
DEFAULT_POPULAR_DAYS = 29

# 한 페이지 상한. 기본값(DEFAULT_PAGE_SIZE)은 post_service 에 있는 것을 쓴다.
MAX_PAGE_SIZE = 50


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
    """최신순. content 는 목록용으로 잘린 미리보기다.

    글 목록 대신 {items, total, page, size, total_pages} 가 온다.
    화면이 "3 / 12" 를 그리려면 전체 건수를 알아야 하기 때문이다.

    범위를 벗어난 page 는 에러가 아니라 빈 items 다.
    있지도 않은 9999 페이지를 열었다고 화면이 에러로 바뀔 일은 아니고,
    total_pages 를 보면 화면이 알아서 1페이지로 되돌릴 수 있다.

    size 에 상한(MAX_PAGE_SIZE)을 두는 이유
    ---------------------------------------
    안 두면 ?size=100000 한 번으로 글 전체를 CLOB 까지 끌어오게 만들 수 있다.
    페이징을 붙여놓고 우회로를 열어두는 셈이라 상한이 있어야 한다.
    """
    return list_posts(db, keyword, page, size)


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

    누적 좋아요가 아니라 그 기간에 눌린 좋아요만 센다.
    누적으로 세면 오래된 글이 영원히 1위라 순위가 움직이지 않는다.
    """
    if end is None:
        end = today_kst()
    if start is None:
        start = end - timedelta(days=DEFAULT_POPULAR_DAYS)

    # ?start=2026-09-30&end=2026-09-01 처럼 뒤집힌 값이 들어와도
    # 빈 목록이 아니라 정상 결과가 나오게 정렬해 둔다
    if start > end:
        start, end = end, start

    return list_popular(db, start, end, limit)


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
    """로그인 상태면 liked / is_mine 이 함께 계산돼 온다."""
    return get_post(db, post_id, current_user)


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
    """레시피/재료는 요청 본문이 아니라 서버가 원본 기록에서 복사해 넣는다."""
    return create_post(db, current_user, payload)


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
    """제목과 본문만 바뀐다. 레시피는 원본 기록에서 읽어오므로 그대로다."""
    return update_post(db, current_user, post_id, payload)


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
    """행을 지우지 않고 use_yn='N' 으로 바꾼다."""
    delete_post(db, current_user, post_id)


# ==========================================================
# 첨부파일 (후기 하나당 파일 하나)
#
# 글쓰기(POST "")는 JSON 그대로 두고 파일만 따로 주고받는다.
# 한 엔드포인트에서 UploadFile 과 Pydantic 본문을 같이 받을 수 없어,
# 합치면 PostCreateRequest 의 검증기를 Form 으로 다시 짜야 한다.
#
# 화면은 글을 먼저 등록하고, 파일이 선택돼 있으면 한 번 더 보낸다.
# 두 번째가 실패해도 남는 건 '첨부 없는 멀쩡한 글' 이다.
# ==========================================================


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
    """첨부가 이미 있으면 새 파일로 갈아 끼운다. 옛 파일은 서버가 지운다."""
    return attach_file(db, current_user, post_id, file)


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
    """첨부가 없어도 204 다. 요청의 목적이 '첨부 없는 상태' 이기 때문이다."""
    detach_file(db, current_user, post_id)


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

      - 삭제된 글(use_yn='N')의 사진을 막을 수 있다.
        정적 서빙이면 URL 만 알면 글을 지운 뒤에도 계속 열린다
      - 사용자가 올린 원래 이름으로 돌려줄 수 있다.
        디스크에는 UUID 로 저장돼 있어서 그대로 주면 'a1b2....jpg' 가 된다

    inline 으로 준다. attachment 로 주면 <img> 에 넣었을 때 그려지지 않고
    내려받기 창이 뜬다. 오른쪽 클릭으로 저장하면 아래 filename 이 쓰인다.
    """
    target, org_name, _ext = open_post_file(db, post_id)

    return FileResponse(
        target,
        filename=org_name,
        content_disposition_type="inline",
    )


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

    GET 상세에서 같이 올리지 않는 이유는, GET 은 몇 번을 보내도 결과가
    같아야 하기 때문이다. 새로고침마다 1씩 오르는 카운터는 그 원칙에 어긋난다.
    """
    return {"view_count": increase_view(db, post_id)}


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
    """POST가 아니라 PUT이다. 같은 요청을 두 번 보내도 결과가 같다(멱등)."""
    return set_like(db, current_user, post_id, liked=True)


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
    return set_like(db, current_user, post_id, liked=False)


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
    return create_comment(db, current_user, post_id, payload)


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
    return update_comment(db, current_user, post_id, comment_id, payload)


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
    delete_comment(db, current_user, post_id, comment_id)
