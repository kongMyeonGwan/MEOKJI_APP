"""재료 가격 조회 인터페이스.

서비스 계층은 이 인터페이스만 알고, 실제로 Mock인지 11번가인지 참가격인지는 모른다.
Provider를 갈아끼워도 비즈니스 로직은 그대로다.

가격 소스마다 성격이 크게 달라서 응답 모양을 하나로 묶어둘 필요가 있다.
    11번가   실시간 판매가.        조사일자 없음
    참가격   주 1회 조사한 공식가.  조사일자 있음
    KAMIS    일 1회 조사한 시세.    조사일자 있음, 농축수산 원물만
화면에서 "3,180원"만 보여주면 사용자는 그게 지금 가격인지 지난주 가격인지 알 수 없다.
그래서 PriceQuote는 가격뿐 아니라 출처와 조사일자를 함께 들고 다닌다.
"""

from dataclasses import dataclass
from datetime import date
from typing import Protocol


@dataclass(frozen=True)
class PriceQuote:
    """재료 하나에 대한 가격 조회 결과.

    금액은 원 단위 정수로 다룬다. 국내 소매가는 소수점이 없어서 Decimal이 필요 없고,
    정수로 두면 JSON 직렬화와 프론트 포맷팅이 단순해진다.
    """

    ingredient: str
    """조회를 요청한 재료명. 응답만 보고도 무엇을 물었는지 알 수 있게 남긴다."""

    low: int
    """최저가(원)."""

    high: int
    """최고가(원). 후보가 하나뿐이면 low와 같은 값이 들어간다."""

    unit: str
    """가격의 기준 단위. 예: "1kg", "500g", "10개"

    이게 없으면 "감자 2,900원"이 한 알인지 10kg 박스인지 알 수 없어서
    최저가라는 숫자 자체가 의미를 잃는다.
    """

    source: str
    """어느 Provider가 준 값인지. 화면 출처 표기와 디버깅에 쓴다."""

    sample_size: int
    """최저/최고를 계산할 때 사용한 후보 개수.

    1이면 사실상 비교를 안 한 것이므로 화면에서 가격대를 강조하지 않는 편이 낫다.
    """

    surveyed_at: date | None = None
    """조사일자. 실시간 소스(11번가)는 None.

    None이 아니면 화면에 반드시 함께 표기한다.
    지난주 가격을 오늘 가격으로 오해시키지 않기 위한 값이다.
    """

    reference_url: str | None = None
    """사용자가 직접 확인할 수 있는 링크. 없으면 None."""


class PriceProvider(Protocol):
    """가격 조회자가 갖춰야 할 모양."""

    name: str
    """`.env`의 PRICE_PROVIDERS에 적는 이름과 같은 값."""

    def fetch_quote(self, ingredient: str) -> PriceQuote | None:
        """재료명으로 최저/최고가를 조회한다.

        찾지 못하면 예외가 아니라 None을 돌려준다.
        소스마다 취급 품목이 달라서 "없음"은 오류가 아니라 정상 상황이기 때문이다.
        (KAMIS에는 간장이 아예 없고, 참가격에는 540개 상품만 있다.)
        None을 받으면 호출 측이 다음 Provider로 넘어간다.

        네트워크 오류처럼 진짜 실패일 때도 None을 돌려주고 로그만 남긴다.
        재료 하나의 가격을 못 가져왔다고 레시피 화면 전체가 500으로 죽으면 안 된다.
        """
        ...
