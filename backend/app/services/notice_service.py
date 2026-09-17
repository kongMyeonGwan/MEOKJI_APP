"""공지사항 (STEP 27)."""

from fastapi import HTTPException, status
from sqlalchemy import func, select
from sqlalchemy.orm import Session

from app.models.notice import Notice
from app.models.types import USE_YN_NO, USE_YN_YES
from app.models.user import User
from app.schemas.notice import (
    NoticeCreateRequest,
    NoticePage,
    NoticeResponse,
    NoticeUpdateRequest,
    NoticeViewResponse,
)
from app.schemas.page import DEFAULT_PAGE_SIZE, count_pages


def to_pinned_yn(pinned: bool) -> str:
    """화면이 쓰는 bool 을 DB 의 'Y'/'N' 으로. to_response 의 반대 방향이다."""
    return USE_YN_YES if pinned else USE_YN_NO


def to_response(notice: Notice) -> NoticeResponse:
    """DB 행을 화면이 쓰는 모양으로. pinned_yn('Y'/'N')을 bool 로 바꾼다."""
    return NoticeResponse(
        id=notice.id,
        title=notice.title,
        content=notice.content,
        view_count=notice.view_count,
        pinned=notice.pinned_yn == USE_YN_YES,
        created_at=notice.created_at,
    )


def list_notices(
    db: Session, page: int = 1, size: int = DEFAULT_PAGE_SIZE
) -> NoticePage:
    """살아있는 공지를 고정 · 최신 순으로 한 페이지씩 돌려준다."""
    total = db.execute(
        select(func.count(Notice.id)).where(Notice.use_yn == USE_YN_YES)
    ).scalar_one()

    rows = db.execute(
        select(Notice)
        .where(Notice.use_yn == USE_YN_YES)
        .order_by(
            # 'Y' > 'N' 이라 내림차순이면 고정이 위로 온다
            Notice.pinned_yn.desc(),
            Notice.created_at.desc(),
            # created_at 이 같은 공지 둘의 순서를 DB 가 마음대로 정하면
            # 같은 공지가 두 페이지에 다 나오거나 어디에도 안 나온다
            Notice.id.desc(),
        )
        .offset((page - 1) * size)
        .limit(size)
    ).scalars()

    return NoticePage(
        items=[to_response(notice) for notice in rows],
        total=total,
        page=page,
        size=size,
        total_pages=count_pages(total, size),
    )


def get_live_notice(db: Session, notice_id: int) -> Notice:
    """살아있는 공지 하나를 찾는다."""
    notice = db.get(Notice, notice_id)

    if notice is None or notice.use_yn != USE_YN_YES:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "공지를 찾을 수 없습니다.")

    return notice


def increase_view(db: Session, notice_id: int) -> NoticeViewResponse:
    """조회수를 1 올린다."""
    notice = get_live_notice(db, notice_id)

    notice.view_count += 1
    db.commit()

    return NoticeViewResponse(view_count=notice.view_count)


# 쓰기 (관리자 전용)


def create_notice(db: Session, user: User, payload: NoticeCreateRequest) -> NoticeResponse:
    """공지를 새로 쓴다."""
    notice = Notice(
        # 작성자는 요청 본문이 아니라 토큰에서 나온 사람이다.
        # 본문으로 받으면 남의 이름으로 공지를 쓸 수 있게 된다.
        user_id=user.id,
        title=payload.title,
        content=payload.content,
        pinned_yn=to_pinned_yn(payload.pinned),
    )
    db.add(notice)
    db.commit()

    db.refresh(notice)  # DB 가 채워준 id · view_count · created_at 을 읽어온다

    return to_response(notice)


def update_notice(
    db: Session, notice_id: int, payload: NoticeUpdateRequest
) -> NoticeResponse:
    """공지를 고친다."""
    notice = get_live_notice(db, notice_id)

    notice.title = payload.title
    notice.content = payload.content
    notice.pinned_yn = to_pinned_yn(payload.pinned)
    db.commit()

    return to_response(notice)


def delete_notice(db: Session, notice_id: int) -> None:
    """공지를 지운다."""
    notice = get_live_notice(db, notice_id)

    notice.use_yn = USE_YN_NO
    db.commit()
