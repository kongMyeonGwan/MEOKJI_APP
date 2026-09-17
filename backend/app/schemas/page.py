"""목록 한 페이지를 감싸는 공통 모양."""

from pydantic import BaseModel, ConfigDict

# 한 페이지에 보여줄 기본 개수. 라우터가 ?size= 로 덮어쓸 수 있다.
DEFAULT_PAGE_SIZE = 20

# 한 페이지 상한.
MAX_PAGE_SIZE = 50


def count_pages(total: int, size: int) -> int:
    """전체 건수와 한 페이지 크기로 마지막 페이지 번호를 구한다."""
    return (total + size - 1) // size


class Page(BaseModel):
    """페이지 정보 네 칸. 목록(items)은 상속받는 쪽이 선언한다."""

    model_config = ConfigDict(from_attributes=True)

    # 검색어·필터까지 반영한 전체 건수. 페이지 안의 개수가 아니다.
    total: int

    # 1부터 시작한다. 범위를 벗어나도 404 가 아니라 빈 items 다 —
    page: int

    size: int

    total_pages: int
