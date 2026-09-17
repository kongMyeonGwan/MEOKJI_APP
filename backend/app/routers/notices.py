"""공지사항 라우터 (STEP 27).

! 아직 만들지 않은 기능이다. 담당자가 채운다.
  주소 · 요청/응답 스키마는 그대로 두었으니 함수 안만 구현하면 된다.
  화면(frontend/src/pages/CommunityPage.jsx)은 지금 하드코딩한
  더미 데이터로 그려지고 있다. 여기가 완성되면 그쪽도 API 로 바꾸면 된다.
"""

from fastapi import APIRouter, Depends, HTTPException, Path, Query, status
from sqlalchemy.orm import Session

from app.core.deps import get_admin_user
from app.database.database import get_db
from app.models.user import User
from app.schemas.notice import (
    NoticeCreateRequest,
    NoticePage,
    NoticeResponse,
    NoticeUpdateRequest,
    NoticeViewResponse,
)
from app.schemas.page import DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE
router = APIRouter(prefix="/api/notices", tags=["notices"])

# 아직 구현하지 않은 엔드포인트가 내려주는 문구.
TODO_MESSAGE = "아직 만들지 않은 기능입니다. (공지사항 담당자 작업 예정)"

# 관리자 전용 엔드포인트가 공통으로 낼 수 있는 응답.
# /docs 화면에 적어주는 설명일 뿐, 실제 동작에는 영향이 없다.
ADMIN_401 = {"description": "토큰 없음 · 만료 · 위조"}
ADMIN_403 = {"description": "로그인은 했지만 관리자가 아님"}
ADMIN_423 = {"description": "잠긴 계정"}
NOT_FOUND = {"description": "없는 공지"}


@router.get(
    "",
    response_model=NoticePage,
    summary="공지사항 목록 (Guest 가능, 페이징)",
)
def read_notices(
    page: int = Query(1, ge=1, description="1부터 시작하는 페이지 번호"),
    size: int = Query(
        DEFAULT_PAGE_SIZE, ge=1, le=MAX_PAGE_SIZE, description="한 페이지 개수"
    ),
    db: Session = Depends(get_db),
) -> NoticePage:
    """고정 공지가 위로, 그 안에서는 최신순이다.

    TODO: 담당자가 구현할 것. 서비스 함수를 만들어 여기서 부른다.
    """
    raise HTTPException(status.HTTP_501_NOT_IMPLEMENTED, TODO_MESSAGE)


@router.post(
    "/{notice_id}/view",
    response_model=NoticeViewResponse,
    summary="조회수 +1",
    responses={404: NOT_FOUND},
)
def count_notice_view(
    notice_id: int = Path(..., ge=1),
    db: Session = Depends(get_db),
) -> NoticeViewResponse:
    """목록 GET 에서 같이 올리지 않고 따로 뺐다.

    TODO: 담당자가 구현할 것. 서비스 함수를 만들어 여기서 부른다.
    """
    raise HTTPException(status.HTTP_501_NOT_IMPLEMENTED, TODO_MESSAGE)


@router.post(
    "",
    response_model=NoticeResponse,
    status_code=status.HTTP_201_CREATED,
    summary="공지 작성 (관리자만)",
    responses={
        401: ADMIN_401,
        403: ADMIN_403,
        423: ADMIN_423,
        422: {"description": "빈 제목·내용"},
    },
)
def write_notice(
    payload: NoticeCreateRequest,
    admin: User = Depends(get_admin_user),
    db: Session = Depends(get_db),
) -> NoticeResponse:
    """작성자는 본문이 아니라 토큰에서 나온 사람으로 들어간다.

    TODO: 담당자가 구현할 것. 서비스 함수를 만들어 여기서 부른다.
    """
    raise HTTPException(status.HTTP_501_NOT_IMPLEMENTED, TODO_MESSAGE)


@router.put(
    "/{notice_id}",
    response_model=NoticeResponse,
    summary="공지 수정 (관리자만)",
    responses={
        401: ADMIN_401,
        403: ADMIN_403,
        423: ADMIN_423,
        404: NOT_FOUND,
        422: {"description": "빈 제목·내용"},
    },
)
def edit_notice(
    payload: NoticeUpdateRequest,
    notice_id: int = Path(..., ge=1),
    admin: User = Depends(get_admin_user),
    db: Session = Depends(get_db),
) -> NoticeResponse:
    """'내 공지만' 이 아니다.

    TODO: 담당자가 구현할 것. 서비스 함수를 만들어 여기서 부른다.
    """
    raise HTTPException(status.HTTP_501_NOT_IMPLEMENTED, TODO_MESSAGE)


@router.delete(
    "/{notice_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="공지 삭제 (관리자만)",
    responses={
        401: ADMIN_401,
        403: ADMIN_403,
        423: ADMIN_423,
        404: NOT_FOUND,
    },
)
def remove_notice(
    notice_id: int = Path(..., ge=1),
    admin: User = Depends(get_admin_user),
    db: Session = Depends(get_db),
) -> None:
    """행을 지우지 않고 use_yn='N' 으로 바꾼다.

    TODO: 담당자가 구현할 것. 서비스 함수를 만들어 여기서 부른다.
    """
    raise HTTPException(status.HTTP_501_NOT_IMPLEMENTED, TODO_MESSAGE)
