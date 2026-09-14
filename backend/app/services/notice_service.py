"""공지사항 (STEP 27).

전에는 catalog.json 에 있었다
-----------------------------
"사용자가 만들지 않는 목록" 이라 파일에 두고 내려줬다. 그런데 공지는
운영하면서 계속 새로 쓰는 값이라, 파일에 두면 공지 하나 올릴 때마다
서버를 다시 배포해야 한다. 카테고리 칩이나 룰렛 조각과는 성격이 다르다.

그래서 NOTICES 표로 옮겼다. catalog.json 에 남아 있던 다섯 건은
db/05_seed_notices.sql 로 옮겼다.

쓰기는 관리자만
---------------
오래 "쓰기 API 를 만들지 않는다" 였다. USERS 에 관리자 구분 컬럼이 없어서
"운영자만 작성" 을 가릴 방법이 없었고, 로그인만 하면 아무나 공지를 쓸 수 있는
API 를 두느니 DB 에 직접 넣는 편이 나았기 때문이다.

USERS.USER_LEVEL (1=관리자) 이 생기면서 그 조건이 풀렸다. 등급 확인은
라우터의 get_admin_user 가 하고, 여기까지 온 요청은 이미 관리자다.
이 계층에서 다시 등급을 보지 않는다 — 같은 검사를 두 군데서 하면 한쪽만
고쳐질 때 조용히 어긋난다.

작성자를 따지지 않는 이유
-------------------------
게시판은 '내 글만' 고칠 수 있다. 공지는 반대로, 관리자면 누가 쓴 공지든
고치고 지울 수 있다. 공지는 개인의 글이 아니라 운영팀이 서비스 이름으로
내는 글이라서, 쓴 사람이 자리를 비웠다고 아무도 못 고치면 곤란하다.
화면도 작성자를 '뭐먹지? 운영팀' 하나로만 보여준다.
"""

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
    """살아있는 공지를 고정 · 최신 순으로 한 페이지씩 돌려준다.

    로그인 없이 볼 수 있다.

    정렬을 화면에 맡기지 않고 서버가 한다. 고정 공지가 위로 와야 한다는 건
    화면 취향이 아니라 공지의 규칙이라, 화면이 여럿 생겨도 같아야 한다.

    본문을 목록에서도 읽어온다. 게시판과 달리 화면이 그 자리에서
    펼치기 때문이다. 한 페이지 몫만 읽으므로 건수가 늘어도 그대로다.

    고정 공지는 1페이지에만 보인다
    ------------------------------
    정렬이 pinned 먼저라 고정은 전부 앞쪽에 몰린다. 2페이지로 넘어가면
    화면에서 사라지는데, 그게 맞다 — 고정은 "목록의 맨 위" 라는 뜻이지
    "모든 페이지의 맨 위" 가 아니다. 페이지마다 따라다니면 2페이지의 절반이
    이미 읽은 공지가 된다.
    """
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
    """살아있는 공지 하나를 찾는다. 없으면 404.

    db.get() 만으로는 부족하다. 삭제가 행을 지우는 게 아니라 use_yn 을 'N' 으로
    바꾸는 것이라서, 지운 공지도 db.get() 에는 그대로 잡힌다.
    """
    notice = db.get(Notice, notice_id)

    if notice is None or notice.use_yn != USE_YN_YES:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "공지를 찾을 수 없습니다.")

    return notice


def increase_view(db: Session, notice_id: int) -> NoticeViewResponse:
    """조회수를 1 올린다.

    목록 GET 에서 같이 올리지 않는다. GET 은 몇 번을 보내도 결과가 같아야
    하는데, 화면을 열 때마다 다섯 건이 한꺼번에 오르면 숫자가 뜻을 잃는다.
    화면이 공지를 펼칠 때 이 함수를 부른다 (게시판의 /view 와 같은 방식이다).
    """
    notice = get_live_notice(db, notice_id)

    notice.view_count += 1
    db.commit()

    return NoticeViewResponse(view_count=notice.view_count)


# ==========================================================
# 쓰기 (관리자 전용)
#
# 등급 확인은 라우터의 get_admin_user 가 끝냈다. 여기 오는 user 는 관리자다.
# ==========================================================


def create_notice(db: Session, user: User, payload: NoticeCreateRequest) -> NoticeResponse:
    """공지를 새로 쓴다.

    view_count 는 넣지 않는다. DDL 의 DEFAULT 0 이 채운다 — 새 공지가 0 회부터
    시작하는 건 예외가 없는 규칙이라 코드가 매번 적을 이유가 없다.
    """
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
    """공지를 고친다. 관리자면 누가 쓴 공지든 고칠 수 있다 (파일 상단 참고).

    user_id 는 그대로 둔다. 처음 쓴 사람이 누구였는지는 기록이고, 고쳤다고
    해서 바뀌는 값이 아니다. 화면도 작성자를 '운영팀' 으로만 보여주므로
    바꿔봐야 보이는 것도 없다.

    view_count 도 그대로 둔다. 오타 하나 고쳤다고 "이 공지를 몇 명이 읽었나"
    가 0 으로 돌아가면 안 된다.
    """
    notice = get_live_notice(db, notice_id)

    notice.title = payload.title
    notice.content = payload.content
    notice.pinned_yn = to_pinned_yn(payload.pinned)
    db.commit()

    return to_response(notice)


def delete_notice(db: Session, notice_id: int) -> None:
    """공지를 지운다. 행을 지우지 않고 use_yn='N' 으로 바꾼다.

    잘못 지운 공지를 DB 에서 한 줄 되돌리면 조회수까지 그대로 살아난다.
    진짜 DELETE 면 그 방법이 없다.
    """
    notice = get_live_notice(db, notice_id)

    notice.use_yn = USE_YN_NO
    db.commit()
