"""가격 Provider 선택.

`.env`의 PRICE_PROVIDERS 값에 따라 구현체를 고른다.

이메일(EMAIL_PROVIDER)과 달리 이 설정은 쉼표로 구분한 **목록**이다.
가격 소스 하나로는 레시피 재료 전체를 덮지 못하기 때문이다.
    11번가        실시간 판매가. 가공식품까지 있지만 판매자 편차가 크다
    참가격        공식 조사가. 가공식품 포함이지만 540개 상품뿐
    KAMIS         농축수산 원물 시세. 간장·참기름 같은 건 아예 없다
그래서 목록에 적은 **순서대로 시도하고, 값을 찾은 첫 Provider에서 멈춘다.**
앞에 적을수록 우선순위가 높다.

    PRICE_PROVIDERS=11st,chamgagyeok,mock

지금은 mock만 구현되어 있다. 나머지는 인증키가 나오는 대로 추가한다.
"""

from functools import lru_cache

from app.core.config import settings
from app.integrations.shopping.base import PriceProvider, PriceQuote
from app.integrations.shopping.mock import MockPriceProvider

__all__ = [
    "PriceProvider",
    "PriceQuote",
    "get_price_providers",
    "fetch_price_quote",
]

# 이름 -> 만드는 법. 새 Provider는 여기에 한 줄만 추가하면 된다.
_BUILDERS: dict[str, type[PriceProvider]] = {
    "mock": MockPriceProvider,
}


@lru_cache
def get_price_providers() -> tuple[PriceProvider, ...]:
    """설정에 적힌 순서대로 Provider를 만들어 돌려준다 (한 번만 만들어 재사용).

    tuple로 돌려주는 이유: lru_cache가 준 객체를 호출 측에서 실수로 append/정렬해도
    다음 호출자가 바뀐 목록을 받는 일이 없도록 불변으로 고정한다.
    """
    names = settings.price_provider_list

    if not names:
        raise RuntimeError(
            "PRICE_PROVIDERS 가 비어 있습니다. backend/.env 에 최소 한 개를 적으세요. "
            f"사용 가능한 값: {', '.join(sorted(_BUILDERS))}"
        )

    providers: list[PriceProvider] = []
    for name in names:
        builder = _BUILDERS.get(name.lower())

        # 조용히 건너뛰면 운영에서 가격이 안 나오는데 아무도 원인을 모르는 상황이 된다.
        # 오타 하나로 전체가 mock으로 떨어지는 것보다 기동 시 터지는 편이 낫다.
        if builder is None:
            raise RuntimeError(
                f"지원하지 않는 PRICE_PROVIDERS 항목: {name!r}. "
                f"사용 가능한 값: {', '.join(sorted(_BUILDERS))}"
            )

        providers.append(builder())

    return tuple(providers)


def fetch_price_quote(ingredient: str) -> PriceQuote | None:
    """재료 하나의 가격을 우선순위대로 조회한다.

    앞에서부터 물어보고 값을 준 첫 Provider의 결과를 쓴다.
    모두 못 찾으면 None. 호출 측은 그 재료의 가격을 표시하지 않으면 된다.
    가격을 못 구한 재료 하나 때문에 레시피 화면 전체가 실패하면 안 된다.
    """
    for provider in get_price_providers():
        quote = provider.fetch_quote(ingredient)
        if quote is not None:
            return quote

    return None
