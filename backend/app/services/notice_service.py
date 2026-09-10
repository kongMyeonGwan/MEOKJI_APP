"""공지사항 (STEP 27).

전에는 catalog.json 에 있었다
-----------------------------
"사용자가 만들지 않는 목록" 이라 파일에 두고 내려줬다. 그런데 공지는
운영하면서 계속 새로 쓰는 값이라, 파일에 두면 공지 하나 올릴 때마다
서버를 다시 배포해야 한다. 카테고리 칩이나 룰렛 조각과는 성격이 다르다.

그래서 NOTICES 표로 옮겼다. catalog.json 에 남아 있던 다섯 건은
db/05_seed_notices.sql 로 옮겼다.

쓰기 API 는 없다
----------------
USERS 에 관리자 구분 컬럼이 없어서 "운영자만 작성" 을 가릴 방법이 지금은
없다. 로그인만 하면 아무나 공지를 쓸 수 있는 API 는 만들지 않는 편이 낫다.
공지는 DB 에 직접 넣는다. 관리자 구분이 생기면 그때 붙인다.
"""

from fastapi import HTTPException, status
from sqlalchemy import select
from sqlalchemy.orm import Session

from app.models.notice import Notice
from app.models.types import USE_YN_YES
from app.schemas.notice import NoticeResponse, NoticeViewResponse


def to_response(notice: Notice) -> NoticeResponse:
    """DB 행을 화면이 쓰는 모양으로. pinned_yn('Y'/'N')을 bool 로 바꾼다."""
    return NoticeResponse(
        id=notice.id,
        title=notice.title,
        content=notice.content,
        category=notice.category,
        view_count=notice.view_count,
        pinned=notice.pinned_yn == USE_YN_YES,
        created_at=notice.created_at,
    )


def list_notices(db: Session) -> list[NoticeResponse]:
    """살아있는 공지를 고정 · 최신 순으로 돌려준다. 로그인 없이 볼 수 있다.

    정렬을 화면에 맡기지 않고 서버가 한다. 고정 공지가 위로 와야 한다는 건
    화면 취향이 아니라 공지의 규칙이라, 화면이 여럿 생겨도 같아야 한다.

    본문(CLOB)을 목록에서도 읽어온다. 게시판과 달리 화면이 그 자리에서
    펼치기 때문이다. 공지는 몇 건뿐이라 CLOB 을 다 읽어도 부담이 없다.
    """
    rows = db.execute(
        select(Notice)
        .where(Notice.use_yn == USE_YN_YES)
        .order_by(
            # 'Y' > 'N' 이라 내림차순이면 고정이 위로 온다
            Notice.pinned_yn.desc(),
            Notice.created_at.desc(),
            Notice.id.desc(),
        )
    ).scalars()

    return [to_response(notice) for notice in rows]


def increase_view(db: Session, notice_id: int) -> NoticeViewResponse:
    """조회수를 1 올린다.

    목록 GET 에서 같이 올리지 않는다. GET 은 몇 번을 보내도 결과가 같아야
    하는데, 화면을 열 때마다 다섯 건이 한꺼번에 오르면 숫자가 뜻을 잃는다.
    화면이 공지를 펼칠 때 이 함수를 부른다 (게시판의 /view 와 같은 방식이다).
    """
    notice = db.get(Notice, notice_id)

    if notice is None or notice.use_yn != USE_YN_YES:
        raise HTTPException(status.HTTP_404_NOT_FOUND, "공지를 찾을 수 없습니다.")

    notice.view_count += 1
    db.commit()

    return NoticeViewResponse(view_count=notice.view_count)
