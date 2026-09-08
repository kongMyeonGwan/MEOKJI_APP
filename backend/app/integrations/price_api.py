"""재료 가격 조회 (STEP 17~19).

.env 의 PRICE_PROVIDERS 에 적은 곳을 전부 조회해서, 재료 하나에 여러 판매처
가격을 붙여 돌려준다. 화면(PriceTable)은 그중 최저가와 최고가를 뽑아 보여준다.

    PRICE_PROVIDERS=naver,mock

바깥에서는 두 함수만 쓴다.

    collect_offers(ingredients)   여러 재료를 한 번에 (비동기, 병렬)
    get_price(ingredient)         재료 하나의 최저/최고만 (동기)

돌려주는 모양 — 프론트의 components/price/PriceTable.jsx 가 이 모양을 기대한다.

    [
      {"name": "돼지고기 앞다리살 200g",
       "offers": [
         {"shop": "쿠팡",       "price": 3980, "link": "https://..."},
         {"shop": "네이버쇼핑", "price": 4350, "link": "https://..."}
       ]}
    ]

왜 병렬로 부르는가
------------------
재료가 8개이고 판매처가 3곳이면 순차 호출은 24번을 줄 세우는 것이다.
한 번에 0.3초만 걸려도 7초가 넘어간다. httpx.AsyncClient 로 동시에 던지면
가장 느린 한 건만큼만 기다린다.

가격은 DB 에 저장하지 않는다. 조회 시점의 값이고, 저장하면 하루만 지나도
틀린 값을 보여주게 된다 (설계 원칙 §13).
"""

import asyncio
import hashlib
from dataclasses import dataclass
from datetime import date
from urllib.parse import quote

import httpx

from app.core.config import settings

# 화면에 나란히 보여주는 판매처. mock 은 이 세 곳을 흉내낸다.
SHOP_NAVER = "네이버쇼핑"
SHOP_COUPANG = "쿠팡"
SHOP_ALI = "알리익스프레스"
MOCK_SHOPS = (SHOP_COUPANG, SHOP_NAVER, SHOP_ALI)

# 외부 API 하나를 기다리는 최대 시간(초).
# 한 곳이 느리다고 레시피 화면 전체가 멈추면 안 된다.
PRICE_TIMEOUT_SECONDS = 6.0

# 네이버 쇼핑 검색 API
# https://developers.naver.com/docs/serviceapi/search/shopping/
NAVER_SHOPPING_URL = "https://openapi.naver.com/v1/search/shop.json"

# 한 재료당 가져올 상품 개수. 많이 받아도 화면에는 최저/최고 둘만 쓴다.
NAVER_DISPLAY = 5


@dataclass
class PriceQuote:
    """재료 하나의 가격 조회 결과 (최저/최고 요약).

    금액은 원 단위 정수로 다룬다. 국내 소매가는 소수점이 없어서
    정수로 두는 편이 JSON 변환도 화면 표시도 단순하다.
    """

    ingredient: str  # 무엇을 물어봤는지. 응답만 봐도 알 수 있게 남긴다
    low: int         # 최저가(원)
    high: int        # 최고가(원). 후보가 하나뿐이면 low 와 같은 값
    unit: str        # 가격의 기준 단위. 예: "1kg", "500g", "10개"
    source: str      # 어느 곳이 준 값인지. 화면 출처 표기와 디버깅에 쓴다
    sample_size: int  # 최저/최고를 계산할 때 쓴 후보 개수

    # 조사일자. 실시간으로 받아오는 곳은 None 이다.
    # None 이 아니면 화면에 반드시 같이 적는다.
    # 지난주 가격을 오늘 가격으로 오해시키면 안 되기 때문이다.
    surveyed_at: date | None = None

    # 사용자가 직접 확인할 수 있는 링크. 없으면 None.
    reference_url: str | None = None


# ==========================================================
# 바깥에서 부르는 함수
# ==========================================================


async def collect_offers(ingredients: list[str]) -> list[dict]:
    """재료 목록의 가격을 판매처별로 모아온다 (병렬).

    가격을 못 구한 재료는 offers 가 빈 배열로 나간다.
    그 재료 하나 때문에 화면 전체가 죽으면 안 되기 때문에 예외를 던지지 않는다.
    """
    names = clean_names(ingredients)
    if not names:
        return []

    providers = settings.price_provider_list
    if not providers:
        # 조용히 빈 값을 돌려주면 "가격이 아무 데도 안 나오는데 이유를 모르겠다"가 된다.
        raise RuntimeError(
            "PRICE_PROVIDERS 가 비어 있습니다. backend/.env 에 최소 한 개를 적으세요. "
            "사용 가능한 값: mock, naver"
        )

    # 재료 하나당 할 일을 하나씩 만들어서 한꺼번에 던진다.
    # AsyncClient 를 하나만 만들어 돌려쓰면 연결을 재사용해서 더 빠르다.
    async with httpx.AsyncClient(timeout=PRICE_TIMEOUT_SECONDS) as client:
        jobs = []
        for name in names:
            jobs.append(collect_one(client, name, providers))

        results = await asyncio.gather(*jobs)

    return list(results)


async def collect_one(
    client: httpx.AsyncClient, name: str, providers: list[str]
) -> dict:
    """재료 하나의 판매처별 가격을 모은다."""
    offers = []

    for provider in providers:
        key = provider.lower()

        if key == "mock":
            offers.extend(mock_offers(name))
        elif key == "naver":
            offers.extend(await naver_offers(client, name))
        else:
            # 오타 하나 때문에 전부 조용히 건너뛰는 것보다 에러가 낫다.
            raise RuntimeError(
                f"지원하지 않는 PRICE_PROVIDERS 항목: {provider}. "
                "사용 가능한 값: mock, naver"
            )

    # 싼 곳부터 보여준다. 화면도 최저가를 먼저 읽는다.
    offers.sort(key=offer_price)

    return {"name": name, "offers": offers}


def offer_price(offer: dict) -> int:
    """정렬용. offers 를 가격 오름차순으로 세운다."""
    return offer["price"]


def clean_names(ingredients: list[str]) -> list[str]:
    """빈 값과 중복을 걸러낸다.

    같은 재료를 두 번 조회하면 합계가 두 배로 부풀려진다.
    """
    names = []

    for raw in ingredients:
        name = str(raw).strip()
        if name and name not in names:
            names.append(name)

    return names


def get_price(ingredient: str) -> PriceQuote | None:
    """재료 하나의 최저/최고만 간단히 알아본다 (동기).

    예산 계산처럼 판매처를 나눠 볼 필요가 없는 곳에서 쓴다.
    못 찾으면 None 을 돌려준다.
    """
    name = ingredient.strip()
    if not name:
        return None

    offers = mock_offers(name)
    if not offers:
        return None

    prices = []
    for offer in offers:
        prices.append(offer["price"])

    return PriceQuote(
        ingredient=name,
        low=min(prices),
        high=max(prices),
        unit=base_unit(name),
        source="mock",
        sample_size=len(prices),
    )


# ==========================================================
# 개발용 가짜 가격
#
# 외부 API 키 없이도 레시피 화면과 가격 비교표를 만들 수 있게 한다.
# 값은 재료명에서 계산해서 만들기 때문에, 같은 재료는 서버를 껐다 켜도
# 늘 같은 가격이 나온다. 랜덤을 쓰면 새로고침할 때마다 가격이 바뀌어서
# 오히려 화면 버그를 못 잡는다.
# ==========================================================

# 자주 쓰는 재료는 실제와 비슷한 값을 적어둔다.
# 시연할 때 "감자 1kg 47,300원" 이 나오면 설득력이 떨어진다.
#   재료명: (최저가, 최고가, 단위)
KNOWN_PRICES = {
    "감자": (2_900, 6_500, "1kg"),
    "양파": (2_400, 5_800, "1kg"),
    "대파": (2_100, 4_900, "1단"),
    "당근": (2_800, 5_200, "1kg"),
    "배추": (4_500, 9_800, "1포기"),
    "무": (2_200, 4_300, "1개"),
    "애호박": (1_800, 3_400, "1개"),
    "양배추": (3_200, 6_400, "1통"),
    "고구마": (4_100, 8_800, "1kg"),
    "버섯": (2_600, 5_400, "200g"),
    "토마토": (4_300, 9_100, "1kg"),
    "깻잎": (1_500, 3_200, "30장"),
    "콩나물": (1_400, 2_900, "300g"),
    "마늘": (7_900, 14_500, "1kg"),
    "계란": (5_800, 9_200, "30구"),
    "달걀": (5_800, 9_200, "30구"),
    "두부": (1_500, 3_200, "300g"),
    "돼지고기": (11_800, 24_000, "1kg"),
    "소고기": (32_000, 68_000, "1kg"),
    "닭고기": (5_900, 12_400, "1kg"),
    "연어": (18_000, 39_000, "500g"),
    "해물": (8_900, 17_500, "500g"),
    "어묵": (3_200, 6_900, "500g"),
    "참치캔": (2_400, 4_800, "150g"),
    "고등어": (4_200, 8_900, "1마리"),
    "간장": (3_400, 8_700, "500ml"),
    "고추장": (5_600, 12_900, "500g"),
    "된장": (4_800, 11_200, "500g"),
    "참기름": (7_200, 16_800, "320ml"),
    "설탕": (2_600, 4_900, "1kg"),
    "소금": (1_900, 4_100, "1kg"),
    "식용유": (4_700, 9_300, "900ml"),
    "밀가루": (2_300, 4_600, "1kg"),
    "쌀": (24_000, 42_000, "10kg"),
    "밥": (1_200, 2_400, "210g"),
    "소면": (2_100, 4_300, "900g"),
    "스파게티면": (3_400, 7_200, "500g"),
    "떡": (3_800, 7_400, "500g"),
    "신김치": (6_200, 13_500, "1kg"),
    "라면": (3_900, 6_800, "5개입"),
    "우유": (2_700, 4_200, "1L"),
    "치즈": (5_400, 11_800, "180g"),
    "생크림": (3_600, 6_900, "500ml"),
    "요거트": (3_100, 6_200, "450g"),
    "버터": (5_900, 12_400, "450g"),
    "바나나": (3_200, 5_800, "1송이"),
    "딸기": (7_900, 16_500, "500g"),
    "오이": (1_600, 3_400, "3개"),
}

# 표에 없는 재료에 쓸 가격 범위. 식료품 한 품목의 현실적인 가격대에 맞췄다.
FALLBACK_MIN_PRICE = 1_500
FALLBACK_MAX_PRICE = 9_000

FALLBACK_UNITS = ("1개", "100g", "300g", "500g", "1kg")

# 판매처별 가격 성향(%). 100 이 기준가다.
#   쿠팡     로켓배송이라 조금 싸게
#   네이버   중간
#   알리     상품가는 제일 싸지만 배송비가 붙어 실제로는 비쌀 때가 많다
SHOP_RATES = {
    SHOP_COUPANG: 96,
    SHOP_NAVER: 104,
    SHOP_ALI: 118,
}


def base_prices(name: str) -> tuple[int, int]:
    """재료명으로 기준 최저가/최고가를 정한다."""
    if name in KNOWN_PRICES:
        low, high, _unit = KNOWN_PRICES[name]
        return low, high

    return make_price_from_name(name)


def base_unit(name: str) -> str:
    """재료명으로 판매 단위를 정한다."""
    if name in KNOWN_PRICES:
        return KNOWN_PRICES[name][2]

    digest = hashlib.md5(name.encode("utf-8")).digest()
    return FALLBACK_UNITS[digest[3] % len(FALLBACK_UNITS)]


def mock_offers(name: str) -> list[dict]:
    """개발용 가짜 판매처 가격 세 건.

    기준가를 판매처 성향(SHOP_RATES)만큼 올리고 내려서 만든다.
    실제 쇼핑몰처럼 어디가 싼지 갈리게 하려고, 재료명으로 뽑은 숫자를 섞는다.
    """
    low, high = base_prices(name)
    digest = hashlib.md5(name.encode("utf-8")).digest()

    offers = []
    index = 0

    for shop in MOCK_SHOPS:
        rate = SHOP_RATES[shop]

        # 재료마다 판매처 순위가 조금씩 달라지도록 -8% ~ +8% 를 더 섞는다
        jitter = 92 + digest[index % len(digest)] % 17
        price = low * rate // 100 * jitter // 100

        # 최고가를 넘지 않게 눌러둔다. 표의 "최고가" 칸이 의미를 잃지 않도록.
        if price > high:
            price = high

        # 100원 단위로 떨어뜨려야 진짜 가격처럼 보인다
        price = max(100, price // 100 * 100)

        offers.append({"shop": shop, "price": price, "link": shop_link(shop, name)})
        index += 1

    return offers


def shop_link(shop: str, name: str) -> str:
    """판매처 검색 결과 페이지 주소.

    가짜 가격이라 상품 하나를 특정할 수 없다. 대신 검색 결과로 보내면
    사용자가 실제 가격을 바로 확인할 수 있다.
    """
    keyword = quote(name)

    if shop == SHOP_COUPANG:
        return f"https://www.coupang.com/np/search?q={keyword}"
    if shop == SHOP_ALI:
        return f"https://ko.aliexpress.com/w/wholesale-{keyword}.html"

    return f"https://search.shopping.naver.com/search/all?query={keyword}"


def make_price_from_name(name: str) -> tuple[int, int]:
    """재료명을 숫자로 바꿔서 기준 가격을 만든다.

    파이썬 내장 hash() 를 안 쓰는 이유: 파이썬은 실행할 때마다 문자열 해시에
    무작위 값을 섞어서, 서버를 다시 켜면 가격이 바뀐다.
    md5 는 언제 어디서 돌려도 같은 값이 나온다.
    (암호 용도가 아니라 "항상 같은 숫자"를 뽑으려고 쓴다.)
    """
    digest = hashlib.md5(name.encode("utf-8")).digest()

    # 앞 2바이트로 최저가를 정한다 (1,500 ~ 9,000원 사이)
    price_range = FALLBACK_MAX_PRICE - FALLBACK_MIN_PRICE
    low = FALLBACK_MIN_PRICE + int.from_bytes(digest[:2], "big") % price_range

    # 3번째 바이트로 최고가 배율을 정한다 (최저가의 1.4배 ~ 2.6배)
    spread_percent = 140 + digest[2] % 121
    high = low * spread_percent // 100

    return low // 100 * 100, high // 100 * 100


# ==========================================================
# 네이버 쇼핑 — 진짜 가격
# ==========================================================


async def naver_offers(client: httpx.AsyncClient, name: str) -> list[dict]:
    """네이버 쇼핑에서 최저가 상품 하나를 찾아 offer 로 만든다.

    실패해도 예외를 던지지 않는다. 재료 하나의 가격을 못 구했다고
    레시피 화면 전체가 에러가 되면 안 된다.
    """
    if not settings.NAVER_CLIENT_ID or not settings.NAVER_CLIENT_SECRET:
        raise RuntimeError(
            "PRICE_PROVIDERS 에 naver 를 켰는데 NAVER_CLIENT_ID / "
            "NAVER_CLIENT_SECRET 이 비어 있습니다."
        )

    try:
        response = await client.get(
            NAVER_SHOPPING_URL,
            params={"query": name, "display": NAVER_DISPLAY, "sort": "asc"},
            headers={
                "X-Naver-Client-Id": settings.NAVER_CLIENT_ID,
                "X-Naver-Client-Secret": settings.NAVER_CLIENT_SECRET,
            },
        )
        response.raise_for_status()
        items = response.json().get("items", [])
    except (httpx.HTTPError, ValueError) as exc:
        print(f"[네이버 가격 조회 실패] {name}: {type(exc).__name__}: {exc}", flush=True)
        return []

    offers = []

    for item in items:
        price = to_int(item.get("lprice"))
        if price is None:
            continue

        # mallName 이 비어 있는 상품이 있다. 그때는 "네이버쇼핑" 으로 묶는다.
        shop = item.get("mallName") or SHOP_NAVER

        offers.append({"shop": shop, "price": price, "link": item.get("link", "")})

    return offers


def to_int(value) -> int | None:
    """네이버가 문자열로 주는 가격("4350")을 정수로 바꾼다. 실패하면 None."""
    if value is None:
        return None

    try:
        return int(value)
    except (TypeError, ValueError):
        return None
