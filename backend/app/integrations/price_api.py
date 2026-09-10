"""재료 가격 조회 (STEP 17~19).

KAMIS 조사가격을 재료별로 모아 돌려준다.
화면(PriceTable)은 그중 최저가와 최고가를 뽑아 보여준다.

    PRICE_PROVIDERS=kamis

값을 지어내지 않는다
--------------------
전에는 KAMIS 에 없는 재료(두부·간장 등)를 mock 프로바이더가 그럴듯한
숫자로 채웠다. 그런데 화면에서 진짜 조사가격과 지어낸 값이 구분되지 않아,
표의 절반이 가짜인 채로 합계까지 계산됐다.

모르는 값은 모른다고 하는 편이 낫다. 지금은 KAMIS 에 없는 재료면
offers 를 빈 배열로 돌려주고, 화면이 '가격 정보 없음'으로 표시한다.

쇼핑몰이 아니라 '시장'을 본다
----------------------------
처음에는 네이버 쇼핑 검색 API 로 쇼핑몰 최저가를 붙였다. 그런데 이 API 는
2026-07-31 자로 종료됐고 공식 대체가 없다. 쿠팡·11번가는 제휴 승인이 필요해서
바로 못 쓴다. 그래서 공공데이터포털의 KAMIS 일별 도·소매 가격정보로 바꿨다.

    https://www.data.go.kr/data/15156057/openapi.do

같은 품목이 시장·지역·등급별로 여러 행 나오므로, 그 행들의 최저/최고가 곧
화면의 최저가/최고가가 된다. 다만 '어느 쇼핑몰에서 얼마'가 아니라
'어느 시장 조사가격이 얼마'라서, 구매 링크는 없고 조사일자를 반드시 같이 적는다.

바깥에서는 두 함수만 쓴다.

    collect_offers(ingredients)   여러 재료를 한 번에 (비동기, 병렬)

돌려주는 모양 — 프론트의 components/price/PriceTable.jsx 가 이 모양을 기대한다.

    [
      {"name": "돼지고기 앞다리살 200g",
       "offers": [
         {"shop": "서울 가락시장", "price": 3980, "link": "https://..."},
         {"shop": "부산 엄궁시장", "price": 4350, "link": "https://..."}
       ]}
    ]

왜 병렬로 부르는가
------------------
재료가 8개이고 조사처가 3곳이면 순차 호출은 24번을 줄 세우는 것이다.
한 번에 0.3초만 걸려도 7초가 넘어간다. httpx.AsyncClient 로 동시에 던지면
가장 느린 한 건만큼만 기다린다.

가격은 DB 에 저장하지 않는다. 조회 시점의 값이고, 저장하면 하루만 지나도
틀린 값을 보여주게 된다 (설계 원칙 §13).
"""

import asyncio
import time
from datetime import date, timedelta
from urllib.parse import quote

import httpx

from app.core.config import settings
from app.integrations.ingredient_match import find_codes, is_not_priced

# 외부 API 하나를 기다리는 최대 시간(초).
# 한 곳이 느리다고 레시피 화면 전체가 멈추면 안 된다.
PRICE_TIMEOUT_SECONDS = 6.0

# 한 재료당 받아올 최대 행 수. 많이 받아도 화면에는 최저/최고 둘만 쓴다.
KAMIS_ROWS = 100

# 조사일자가 비는 날(주말·공휴일)을 대비해 며칠까지 거슬러 올라가 볼지.
KAMIS_LOOKBACK_DAYS = 7

# 사용자가 값을 직접 확인할 수 있는 곳. 상품 페이지가 아니라 조사가격 페이지다.
KAMIS_REFERENCE_URL = "https://www.kamis.or.kr/customer/price/retail/item.do"

# 지원하는 PRICE_PROVIDERS 값. 에러 메시지에 그대로 쓴다.
SUPPORTED_PROVIDERS = ("kamis",)


# ==========================================================
# 조사가격 캐시
#
# KAMIS 조사가격은 하루에 한 번만 바뀐다. 조사원이 시장을 돌며 적어온
# 값이라 오전·오후로 달라지지 않는다(같은 시장·같은 날의 값은 딱 하나다).
# 그런데 지금은 사용자가 레시피를 열 때마다 재료 수만큼 물어본다.
#
# 그래서 한 번 받아온 값을 잠시 기억해둔다. 캐시가 오차를 늘리지 않는다 —
# 어차피 다음 조사 전까지는 같은 값이 돌아온다.
#
# 얻는 것
#   레시피 화면에서 KAMIS 왕복 3초가 사라진다
#   발표장에서 공공데이터포털이 느리거나 막혀도 화면이 뜬다
#   일일 할당량(10,000건)을 아낀다
# ==========================================================

# 기억해두는 시간(초). 조사값은 하루 한 번 갱신되므로 3시간이면 넉넉하다.
# 더 길게 잡으면 새 조사일자가 나왔는데 옛 값을 보여주는 시간이 늘어난다.
KAMIS_CACHE_SECONDS = 60 * 60 * 3

# 너무 커지면 통째로 비운다. 다시 물어보면 채워지는 값이라
# 오래된 것만 골라 지우는 정교함이 필요 없다 (weather_api 와 같은 방식).
KAMIS_CACHE_MAX_ENTRIES = 300

# {(부류, 품목, 품종): (저장시각, offers, 추이)}
kamis_cache: dict[tuple, tuple[float, list[dict], dict | None]] = {}


def read_kamis_cache(key: tuple) -> tuple[list[dict], dict | None] | None:
    """기억해둔 조사가격과 추이를 꺼낸다. 없거나 오래됐으면 None."""
    found = kamis_cache.get(key)
    if found is None:
        return None

    saved_at, offers, trend = found
    if time.time() - saved_at >= KAMIS_CACHE_SECONDS:
        return None

    # 복사본을 준다. 부르는 쪽이 정렬하고 라벨을 바꾸는데,
    # 원본을 그대로 주면 캐시가 조금씩 망가진다.
    return [dict(offer) for offer in offers], (dict(trend) if trend else None)


def write_kamis_cache(key: tuple, offers: list[dict], trend: dict | None) -> None:
    """조사가격과 추이를 기억해둔다.

    빈 결과도 기억한다. KAMIS 에 없는 품목을 3초마다 다시 물어볼 이유가 없고,
    없다는 사실도 다음 조사까지 바뀌지 않는다.
    """
    if len(kamis_cache) > KAMIS_CACHE_MAX_ENTRIES:
        kamis_cache.clear()

    kamis_cache[key] = (
        time.time(),
        [dict(offer) for offer in offers],
        dict(trend) if trend else None,
    )


# ==========================================================
# 바깥에서 부르는 함수
# ==========================================================


async def collect_offers(ingredients: list[str]) -> list[dict]:
    """재료 목록의 가격을 조사처별로 모아온다 (병렬).

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
            f"사용 가능한 값: {', '.join(SUPPORTED_PROVIDERS)}"
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
    """재료 하나의 조사처별 가격과 최근 가격 추이를 모은다.

    적은 순서대로 시도하다가 값이 나오면 거기서 멈춘다. 여러 곳의 결과를
    합치지 않는다 — 곳마다 단위 기준이 달라 한 표에 담으면 최저/최고가
    시장 차이가 아니라 단위 차이가 되기 때문이다.

    어디서도 못 구하면 offers 가 빈 배열로 나간다. 화면은 그 줄을
    '가격 정보 없음'으로 표시한다.
    """
    for provider in providers:
        key = provider.lower()

        if key == "kamis":
            offers, trend = await kamis_offers(client, name)
        else:
            # 오타 하나 때문에 전부 조용히 건너뛰는 것보다 에러가 낫다.
            raise RuntimeError(
                f"지원하지 않는 PRICE_PROVIDERS 항목: {provider}. "
                f"사용 가능한 값: {', '.join(SUPPORTED_PROVIDERS)}"
            )

        if offers:
            # 싼 곳부터 보여준다. 화면도 최저가를 먼저 읽는다.
            offers.sort(key=offer_price)
            return {"name": name, "offers": offers, "trend": trend}

    # 어디서도 못 구했다. 화면은 이 재료 줄을 '가격 정보 없음'으로 표시한다.
    return {"name": name, "offers": [], "trend": None}


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


def kamis_link(name: str) -> str:
    """사용자가 값을 직접 확인할 수 있는 KAMIS 가격 페이지 주소.

    쇼핑몰과 달리 '이 상품을 사는 링크'가 없다. 조사가격 원문을 보여주는
    페이지로 보내서, 화면 숫자가 어디서 왔는지 확인할 수 있게 한다.
    """
    return f"{KAMIS_REFERENCE_URL}?keyword={quote(name)}"


# ==========================================================
# KAMIS — 공공데이터포털 일별 도·소매 가격정보 (진짜 가격)
#
# https://www.data.go.kr/data/15156057/openapi.do
# 무료 · 개발계정 자동승인 · 일 10,000건
#
# 인증키만 .env 로 뺐다(팀원마다 다르다). URL 은 명세서 값 그대로다.
#     DATA_GO_KR_SERVICE_KEY=...  (마이페이지의 일반 인증키, Decoding 값)
# ==========================================================

KAMIS_URL = "https://apis.data.go.kr/B552845/perDay/price"

# 구분코드 01=소매, 02=중도매, 03/07=친환경.
# 소매만 본다. 도매 단가(10kg 상자 등)와 섞으면 최저/최고가 뒤죽박죽이 된다.
KAMIS_RETAIL_CODE = "01"

# 한 번에 받아올 최대 행 수 (명세서 max 1000).
# 품목 하나가 시장 × 품종 × 등급 × 날짜만큼 행으로 쪼개져 나온다.
KAMIS_ROWS = 1000

# 조사일자는 필수 파라미터인데 주말·공휴일은 조사가 없다.
# 오늘 하루만 물으면 월요일 아침에 0건이 나오므로 며칠 거슬러 올라가 묻고,
# 받아온 것 중 가장 최근 조사일자만 골라 쓴다.
KAMIS_LOOKBACK_DAYS = 7

# 응답에서 실제로 쓰는 컬럼만 받는다. 행 수는 그대로지만 응답이 가벼워진다.
KAMIS_COLUMNS = "exmn_ymd,sgg_nm,mrkt_nm,unit,unit_sz,exmn_dd_prc,grd_nm"

# 사용자가 값을 직접 확인할 수 있는 곳. 상품 페이지가 아니라 조사가격 페이지다.
KAMIS_REFERENCE_URL = "https://www.kamis.or.kr/customer/price/retail/item.do"


async def kamis_offers(
    client: httpx.AsyncClient, name: str
) -> tuple[list[dict], dict | None]:
    """KAMIS 소매 조사가격을 시장별 offer 목록과 최근 추이로 만든다.

    실패해도 예외를 던지지 않는다. 재료 하나의 가격을 못 구했다고
    레시피 화면 전체가 에러가 되면 안 된다.
    """
    if not settings.DATA_GO_KR_SERVICE_KEY:
        raise RuntimeError(
            "PRICE_PROVIDERS 에 kamis 를 켰는데 DATA_GO_KR_SERVICE_KEY 가 "
            "비어 있습니다. 공공데이터포털에서 활용신청 후 일반 인증키의 "
            "Decoding 값을 backend/.env 에 넣으세요."
        )

    if is_not_priced(name):
        # 물·얼음처럼 사는 재료가 아닌 것. 물어볼 일이 아니다.
        return [], None

    codes = find_codes(name)
    if not codes:
        # KAMIS 는 농수산물만 조사한다. 간장·두부·라면 같은 가공식품은
        # 여기 없다. 값을 지어내지 않고 빈 채로 돌려준다.
        return [], None

    # 같은 품목을 이미 물어봤으면 그 값을 쓴다.
    # 조사값은 하루 한 번만 바뀌므로 다시 물어도 같은 답이 온다.
    cached = read_kamis_cache(codes)
    if cached is not None:
        offers, trend = cached
        return relabel(offers, name), trend

    try:
        response = await client.get(KAMIS_URL, params=kamis_params(*codes))
    except httpx.HTTPError as exc:
        print(f"[KAMIS 조회 실패] {name}: {type(exc).__name__}: {exc}", flush=True)
        return [], None

    # raise_for_status() 를 쓰지 않는다. 인증키가 틀리면 403 과 함께
    # 사유가 본문에 실려 오는데, 예외로 던져버리면 "403" 만 남고
    # 무엇이 잘못됐는지는 사라진다.
    try:
        payload = response.json()
    except ValueError:
        print(
            f"[KAMIS 조회 실패] {name}: HTTP {response.status_code} "
            f"{response.text[:200]}",
            flush=True,
        )
        return [], None

    message = kamis_error(payload)
    if message:
        # 인증키 오타·트래픽 초과는 조용히 빈 값을 돌려주면 원인을 못 찾는다.
        print(f"[KAMIS 오류] {name}: {message}", flush=True)
        return [], None

    # 단위를 먼저 맞춘 뒤 두 갈래로 쓴다.
    #   전체 기간  가격 추이 계산
    #   최근 하루  화면에 보여줄 시장별 가격
    #
    # 일주일치를 이미 받아놓고 최근 하루만 쓰고 버리고 있었다.
    # 추이는 그 버려지던 데이터로 만든다 — 추가 호출이 없다.
    history = same_unit_rows(kamis_rows(payload))
    trend = price_trend(history)
    rows = latest_rows(history)

    offers = []
    for row in rows:
        price = to_int(row.get("exmn_dd_prc"))
        if price is None or price <= 0:
            continue

        offers.append(
            {
                "shop": kamis_market_label(row),
                "price": price,
                "unit": kamis_unit(row),
                # 링크는 재료명이 들어가므로 캐시할 때는 비워둔다.
                # 같은 품목코드라도 부른 이름이 다를 수 있다("마늘" / "다진 마늘").
                "link": "",
                "surveyed_at": str(row.get("exmn_ymd") or "").strip() or None,
            }
        )

    write_kamis_cache(codes, offers, trend)

    return relabel(offers, name), trend


def relabel(offers: list[dict], name: str) -> list[dict]:
    """캐시에서 꺼낸 값에 이 재료 이름으로 만든 링크를 붙인다.

    가격과 시장은 품목이 같으면 같지만, 확인 링크는 사용자가 본 이름으로
    가야 한다. "다진 마늘" 을 눌렀는데 "마늘" 로 검색되면 어색하다.
    """
    link = kamis_link(name)

    for offer in offers:
        offer["link"] = link

    return offers


def kamis_params(
    category_code: str, item_code: str, variety_code: str | None = None
) -> dict:
    """요청 파라미터를 만든다.

    조사일자(GTE/LTE)·부류코드·품목코드가 전부 필수다. 하나라도 빠지면
    에러가 아니라 0건이 돌아와서, 데이터가 없는 것처럼 보인다.
    파라미터 이름의 대괄호와 :: 는 명세서 그대로다.
    """
    today = date.today()
    start = today - timedelta(days=KAMIS_LOOKBACK_DAYS)

    params = {
        "serviceKey": settings.DATA_GO_KR_SERVICE_KEY,
        "returnType": "JSON",
        "pageNo": 1,
        "numOfRows": KAMIS_ROWS,
        "cond[exmn_ymd::GTE]": start.strftime("%Y%m%d"),
        "cond[exmn_ymd::LTE]": today.strftime("%Y%m%d"),
        "cond[se_cd::EQ]": KAMIS_RETAIL_CODE,
        "cond[ctgry_cd::EQ]": category_code,
        "cond[item_cd::EQ]": item_code,
        "selectable": KAMIS_COLUMNS,
    }

    if variety_code:
        params["cond[vrty_cd::EQ]"] = variety_code

    return params


def kamis_error(payload: dict) -> str | None:
    """에러 사유를 꺼낸다. 정상이면 None.

    공공데이터포털은 실패 지점에 따라 봉투가 두 가지다. 하나만 보면
    나머지 절반을 놓친다.

    1. 게이트웨이가 막은 것 — 인증키 오타 · 미등록 · 트래픽 초과
           {"OpenAPI_ServiceResponse": {"cmmMsgHeader": {"errMsg": ...}}}

    2. 서비스까지 갔다가 실패한 것 — 파라미터 오류 등
           {"response": {"header": {"resultCode": "01", "resultMsg": ...}}}
    """
    if not isinstance(payload, dict):
        return None

    # 1. 게이트웨이 봉투
    gateway = payload.get("OpenAPI_ServiceResponse")
    if isinstance(gateway, dict):
        header = gateway.get("cmmMsgHeader")
        if isinstance(header, dict):
            reason = str(header.get("errMsg") or "").strip()
            detail = str(header.get("returnAuthMsg") or "").strip()
            code = str(header.get("returnReasonCode") or "").strip()
            return " ".join(part for part in (code, reason, detail) if part)

    # 2. 서비스 봉투
    header = payload.get("response")
    if isinstance(header, dict):
        header = header.get("header")

    if not isinstance(header, dict):
        return None

    code = str(header.get("resultCode") or "").strip()
    if not code or code in ("00", "0"):
        return None

    return f"{code} {header.get('resultMsg') or ''}".strip()


def kamis_rows(payload: dict) -> list[dict]:
    """응답에서 데이터 행 목록만 꺼낸다.

    명세서 기준 response.body.items.item 이다. 행이 하나뿐일 때 배열이
    아니라 객체로 오는 기관이 있어서 그 경우도 받아준다.
    """
    node = payload
    for key in ("response", "body", "items", "item"):
        if isinstance(node, dict) and key in node:
            node = node[key]

    if isinstance(node, dict):
        node = [node]

    if not isinstance(node, list):
        return []

    return [row for row in node if isinstance(row, dict)]


def latest_rows(rows: list[dict]) -> list[dict]:
    """가장 최근 조사일자의 행만 남긴다.

    주말을 건너뛰려고 일주일치를 받아왔다. 그대로 두면 지난 화요일 가격과
    오늘 가격이 한 표에 섞여서, 최저/최고가 '시장 차이'가 아니라
    '날짜 차이'가 되어버린다.
    """
    days = []
    for row in rows:
        day = str(row.get("exmn_ymd") or "").strip()
        if day:
            days.append(day)

    if not days:
        return []

    newest = max(days)
    return [row for row in rows if str(row.get("exmn_ymd") or "").strip() == newest]


def same_unit_rows(rows: list[dict]) -> list[dict]:
    """단위가 같은 행만 남긴다. 가장 많이 나온 단위를 기준으로 삼는다.

    품종을 골라도 단위가 갈리는 품목이 있다. 계란은 10구와 30구가,
    쌀은 10kg 과 20kg 이 같이 나온다. 그대로 최저/최고를 내면
    '10구 최저가 ~ 30구 최고가'가 되어 세 배쯤 부풀려진다.

    품종코드로 대부분 걸러지지만, 코드가 새로 생기거나 조사 방식이
    바뀌어도 표가 이상해지지 않도록 마지막에 한 번 더 통일한다.
    """
    if not rows:
        return []

    counts: dict[str, int] = {}
    for row in rows:
        counts[kamis_unit(row)] = counts.get(kamis_unit(row), 0) + 1

    # 가장 흔한 단위. 같으면 이름순으로 정해서 매번 같은 답이 나오게 한다.
    best = sorted(counts.items(), key=lambda pair: (-pair[1], pair[0]))[0][0]

    return [row for row in rows if kamis_unit(row) == best]


# 추이를 화면에 알릴 최소 변동폭(%).
#
# 시장 조사가격은 며칠 사이에도 1~2% 씩 흔들린다. 그걸 다 "올랐다/내렸다"고
# 적으면 배지가 늘 떠 있어서 아무 뜻이 없어진다. 장 볼 때 체감되는 폭만 알린다.
TREND_MIN_PERCENT = 5.0


def daily_average(rows: list[dict]) -> dict[str, int]:
    """조사일자별 평균 가격.

    같은 날에도 시장마다 값이 다르다. 날짜끼리 비교하려면 하루를
    숫자 하나로 줄여야 한다.
    """
    buckets: dict[str, list[int]] = {}

    for row in rows:
        day = str(row.get("exmn_ymd") or "").strip()
        price = to_int(row.get("exmn_dd_prc"))
        if day and price and price > 0:
            buckets.setdefault(day, []).append(price)

    return {day: sum(prices) // len(prices) for day, prices in buckets.items()}


def price_trend(rows: list[dict]) -> dict | None:
    """일주일 전과 견줘 값이 얼마나 움직였는지.

    주말·공휴일은 조사가 없어서 날짜가 띄엄띄엄하다. 그래서 "7일 전"을
    딱 집지 않고, 받아온 기간의 처음과 끝을 견준다.

    쇼핑몰 API 로는 못 하는 일이다. 그쪽은 지금 값만 준다.
    KAMIS 는 일별 조사값이라 "요즘 싼지 비싼지" 를 말할 수 있다.

    변동이 작으면 None 을 돌려준다. 화면에 배지를 안 붙인다는 뜻이다.
    """
    days = daily_average(rows)
    if len(days) < 2:
        return None

    ordered = sorted(days)
    first_day, last_day = ordered[0], ordered[-1]
    was, now = days[first_day], days[last_day]

    if was <= 0:
        return None

    percent = (now - was) / was * 100
    if abs(percent) < TREND_MIN_PERCENT:
        return None

    return {
        "percent": round(percent, 1),
        "from_price": was,
        "to_price": now,
        "from_day": first_day,
        "to_day": last_day,
    }


def kamis_market_label(row: dict) -> str:
    """화면에 보여줄 조사처 이름.  '서울' + '가락시장' -> '서울 가락시장'"""
    region = str(row.get("sgg_nm") or "").strip()
    market = str(row.get("mrkt_nm") or "").strip()

    if region and market:
        return f"{region} {market}"

    return market or region or "조사가격"


def kamis_unit(row: dict) -> str:
    """가격의 기준 단위.  단위크기 '1' + 단위 'kg' -> '1kg'

    단위를 안 적으면 쌀 10kg 값과 두부 300g 값이 한 표에 나란히 서서
    쌀이 터무니없이 비싸 보인다.
    """
    size = str(row.get("unit_sz") or "").strip()
    unit = str(row.get("unit") or "").strip()

    return f"{size}{unit}".strip()


def kamis_link(name: str) -> str:
    """사용자가 값을 직접 확인할 수 있는 KAMIS 가격 페이지 주소.

    쇼핑몰과 달리 '이 상품을 사는 링크'가 없다. 조사가격 원문을 보여주는
    페이지로 보내서, 화면 숫자가 어디서 왔는지 확인할 수 있게 한다.
    """
    return f"{KAMIS_REFERENCE_URL}?keyword={quote(name)}"


def to_int(value) -> int | None:
    """가격이 문자열("4,350")로 와도 정수로 바꾼다. 실패하면 None."""
    if value is None:
        return None

    if isinstance(value, int):
        return value

    # 천 단위 쉼표가 붙어서 오는 기관이 있다
    text = str(value).replace(",", "").strip()

    try:
        return int(float(text))
    except (TypeError, ValueError):
        return None
