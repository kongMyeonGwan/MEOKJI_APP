"""재료 가격 조회 (STEP 17~19)."""

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


# 조사가격 캐시

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
    """조사가격과 추이를 기억해둔다."""
    if len(kamis_cache) > KAMIS_CACHE_MAX_ENTRIES:
        kamis_cache.clear()

    kamis_cache[key] = (
        time.time(),
        [dict(offer) for offer in offers],
        dict(trend) if trend else None,
    )


# 바깥에서 부르는 함수


async def collect_offers(ingredients: list[str]) -> list[dict]:
    """재료 목록의 가격을 조사처별로 모아온다 (병렬)."""
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
    """재료 하나의 조사처별 가격과 최근 가격 추이를 모은다."""
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
    """빈 값과 중복을 걸러낸다."""
    names = []

    for raw in ingredients:
        name = str(raw).strip()
        if name and name not in names:
            names.append(name)

    return names


def kamis_link(name: str) -> str:
    """사용자가 값을 직접 확인할 수 있는 KAMIS 가격 페이지 주소."""
    return f"{KAMIS_REFERENCE_URL}?keyword={quote(name)}"


# KAMIS — 공공데이터포털 일별 도·소매 가격정보 (진짜 가격)

KAMIS_URL = "https://apis.data.go.kr/B552845/perDay/price"

# 구분코드 01=소매, 02=중도매, 03/07=친환경.
# 소매만 본다. 도매 단가(10kg 상자 등)와 섞으면 최저/최고가 뒤죽박죽이 된다.
KAMIS_RETAIL_CODE = "01"

# 한 번에 받아올 최대 행 수 (명세서 max 1000).
# 품목 하나가 시장 × 품종 × 등급 × 날짜만큼 행으로 쪼개져 나온다.
KAMIS_ROWS = 1000

# 조사일자는 필수 파라미터인데 주말·공휴일은 조사가 없다.
KAMIS_LOOKBACK_DAYS = 7

# 응답에서 실제로 쓰는 컬럼만 받는다. 행 수는 그대로지만 응답이 가벼워진다.
KAMIS_COLUMNS = "exmn_ymd,sgg_nm,mrkt_nm,unit,unit_sz,exmn_dd_prc,grd_nm"

# 사용자가 값을 직접 확인할 수 있는 곳. 상품 페이지가 아니라 조사가격 페이지다.
KAMIS_REFERENCE_URL = "https://www.kamis.or.kr/customer/price/retail/item.do"


async def kamis_offers(
    client: httpx.AsyncClient, name: str
) -> tuple[list[dict], dict | None]:
    """KAMIS 소매 조사가격을 시장별 offer 목록과 최근 추이로 만든다."""
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
    """캐시에서 꺼낸 값에 이 재료 이름으로 만든 링크를 붙인다."""
    link = kamis_link(name)

    for offer in offers:
        offer["link"] = link

    return offers


def kamis_params(
    category_code: str, item_code: str, variety_code: str | None = None
) -> dict:
    """요청 파라미터를 만든다."""
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
    """에러 사유를 꺼낸다."""
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
    """응답에서 데이터 행 목록만 꺼낸다."""
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
    """가장 최근 조사일자의 행만 남긴다."""
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
    """단위가 같은 행만 남긴다."""
    if not rows:
        return []

    counts: dict[str, int] = {}
    for row in rows:
        counts[kamis_unit(row)] = counts.get(kamis_unit(row), 0) + 1

    # 가장 흔한 단위. 같으면 이름순으로 정해서 매번 같은 답이 나오게 한다.
    best = sorted(counts.items(), key=lambda pair: (-pair[1], pair[0]))[0][0]

    return [row for row in rows if kamis_unit(row) == best]


# 추이를 화면에 알릴 최소 변동폭(%).
TREND_MIN_PERCENT = 5.0


def daily_average(rows: list[dict]) -> dict[str, int]:
    """조사일자별 평균 가격."""
    buckets: dict[str, list[int]] = {}

    for row in rows:
        day = str(row.get("exmn_ymd") or "").strip()
        price = to_int(row.get("exmn_dd_prc"))
        if day and price and price > 0:
            buckets.setdefault(day, []).append(price)

    return {day: sum(prices) // len(prices) for day, prices in buckets.items()}


def price_trend(rows: list[dict]) -> dict | None:
    """일주일 전과 견줘 값이 얼마나 움직였는지."""
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
    """가격의 기준 단위."""
    size = str(row.get("unit_sz") or "").strip()
    unit = str(row.get("unit") or "").strip()

    return f"{size}{unit}".strip()


def kamis_link(name: str) -> str:
    """사용자가 값을 직접 확인할 수 있는 KAMIS 가격 페이지 주소."""
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
