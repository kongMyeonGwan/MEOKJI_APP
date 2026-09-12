"""목록 한 페이지를 감싸는 공통 모양.

왜 목록(list)을 그대로 안 내려주고 한 겹 감쌌나
------------------------------------------------
화면이 "3 / 12" 같은 페이지 번호를 그리려면 **전체가 몇 건인지** 알아야 한다.
20건짜리 배열만 오면 이게 마지막 페이지인지 알 방법이 없다.

개수를 응답 헤더에 담는 방법도 있지만, 본문에 같이 두면 /docs 에도 보이고
프론트에서도 data.total 로 그냥 읽힌다.

왜 Page[T] 제네릭이 아닌가
--------------------------
Pydantic 제네릭을 쓰면 /docs 의 스키마 이름이 Page_PostListItem_ 처럼 나온다.
읽는 사람이 늘어나는 문서에서 이름이 기계가 지은 것처럼 보이는 건 손해다.
상속으로 두면 PostPage · NoticePage 라는 이름이 그대로 남고, 공통 네 칸은
여기 한 번만 적는다.

쓰는 쪽은 items 만 선언하면 된다.

    class PostPage(Page):
        items: list[PostListItem]
"""

from pydantic import BaseModel, ConfigDict

# 한 페이지에 보여줄 기본 개수. 라우터가 ?size= 로 덮어쓸 수 있다.
DEFAULT_PAGE_SIZE = 20

# 한 페이지 상한.
#
# 안 두면 ?size=100000 한 번으로 전체를 끌어올 수 있다. 페이징을 붙여놓고
# 우회로를 열어두는 셈이라 상한이 있어야 한다. 넘으면 422 다.
#
# Oracle 의 IN 절 상한이 1000 인데, 게시판이 페이지 몫만큼 좋아요·댓글을
# 셀 때 그 절을 쓴다. 50 이면 걸릴 일이 없다.
MAX_PAGE_SIZE = 50


def count_pages(total: int, size: int) -> int:
    """전체 건수와 한 페이지 크기로 마지막 페이지 번호를 구한다.

    45건을 20개씩 보면 3페이지다. 남는 5건도 한 페이지를 차지하므로
    나누기 전에 (size - 1) 을 더해서 올림으로 만든다.

    0건이면 0 이다. 화면은 이 값이 1 이하일 때 페이지 버튼을 통째로 숨긴다.
    """
    return (total + size - 1) // size


class Page(BaseModel):
    """페이지 정보 네 칸. 목록(items)은 상속받는 쪽이 선언한다."""

    model_config = ConfigDict(from_attributes=True)

    # 검색어·필터까지 반영한 전체 건수. 페이지 안의 개수가 아니다.
    total: int

    # 1부터 시작한다. 범위를 벗어나도 404 가 아니라 빈 items 다 —
    # 있지도 않은 9999 페이지를 열었다고 화면이 에러로 바뀔 일은 아니고,
    # total_pages 를 보면 화면이 알아서 1페이지로 되돌릴 수 있다.
    page: int

    size: int

    total_pages: int
