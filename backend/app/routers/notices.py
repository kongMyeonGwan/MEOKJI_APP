"""공지사항 라우터 (STEP 27).

전에는 /api/catalog/notices 였다. catalog 는 "사용자가 만들지 않는 고정
목록" 을 파일에서 내려주는 곳인데, 공지는 운영하면서 계속 새로 쓰는 값이라
성격이 다르다. DB 로 옮기면서 주소도 옮겼다.

쓰기 엔드포인트가 없다. USERS 에 관리자 구분 컬럼이 없어서 "운영자만" 을
가릴 방법이 지금은 없기 때문이다. 자세한 이유는 notice_service 의 주석 참고.
"""

from fastapi import APIRouter, Depends, Path
from sqlalchemy.orm import Session

from app.database.database import get_db
from app.schemas.notice import NoticeResponse, NoticeViewResponse
from app.services.notice_service import increase_view, list_notices

router = APIRouter(prefix="/api/notices", tags=["notices"])


@router.get(
    "",
    response_model=list[NoticeResponse],
    summary="공지사항 목록 (Guest 가능)",
)
def read_notices(db: Session = Depends(get_db)) -> list[NoticeResponse]:
    """고정 공지가 위로, 그 안에서는 최신순이다."""
    return list_notices(db)


@router.post(
    "/{notice_id}/view",
    response_model=NoticeViewResponse,
    summary="조회수 +1",
    responses={404: {"description": "없는 공지"}},
)
def count_notice_view(
    notice_id: int = Path(..., ge=1),
    db: Session = Depends(get_db),
) -> NoticeViewResponse:
    """목록 GET 에서 같이 올리지 않고 따로 뺐다.

    GET 은 몇 번을 보내도 결과가 같아야 하는데, 화면을 열 때마다 목록에
    있는 공지가 전부 1씩 오르면 숫자가 뜻을 잃는다.
    화면은 공지를 펼칠 때 이걸 부른다.
    """
    return increase_view(db, notice_id)
