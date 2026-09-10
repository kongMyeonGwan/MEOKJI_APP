"""요즘 값이 내린 재료 찾기 (STEP 17~19 연장).

무엇을 하는가
-------------
KAMIS 가 조사하는 품목 전부의 최근 가격 흐름을 훑어서, 지금 값이 내린
재료를 골라낸다. 그 목록을 AI 추천에 넘기면 "요즘 싼 재료로 만드는 메뉴"
가 나온다.

    방울토마토 -21%  ·  고구마 -17%  ·  대파 -12%  ·  양파 -9%
      -> "고구마 맛탕", "대파 김치전" 같은 메뉴를 먼저 권한다

왜 이걸 만들었나
----------------
쇼핑몰 API 로는 못 하는 일이다. 네이버·11번가·쿠팡은 전부 "지금 얼마"
만 준다. KAMIS 는 일별 조사값이라 "요즘 싼지 비싼지" 를 말할 수 있다.

구매 링크를 붙일 수 없어서 밀렸던 자리에서, 이건 오히려 앞선다.

한 번 훑는 데 16초쯤 걸린다
---------------------------
품목이 101개이고 KAMIS 는 한 번에 한 품목만 답한다. 그래서 사용자가
기다리게 두지 않는다.

    서버가 뜰 때 백그라운드로 한 번 훑어둔다 (main.py)
    그 뒤 3시간 동안은 기억해둔 값을 쓴다

조사값은 하루 한 번만 바뀌므로 3시간 캐시가 정확도를 떨어뜨리지 않는다.
"""

import asyncio
import json
import time
from datetime import date
from pathlib import Path

import httpx

from app.integrations import price_api
from app.integrations.ingredient_match import KAMIS_ITEM_CODES

# kamis_offers 를 이름으로 직접 가져오지 않고 price_api 를 통해 부른다.
#
# 직접 가져오면 이 모듈이 로드되는 순간의 함수를 붙들어버려서, 나중에
# price_api.kamis_offers 를 바꿔치기해도(테스트가 그렇게 한다) 여기는
# 옛 함수를 계속 쓴다. 그러면 테스트가 조용히 공공데이터포털을 때린다.

# 한 번에 몇 개까지 동시에 물어볼지.
#
# KAMIS 는 초당 요청 수를 제한한다. 너무 몰아치면 응답 대신
# LIMITED_NUMBER_OF_SERVICE_REQUESTS_PER_SECOND_EXCEEDS_ERROR(HTTP 429)
# 가 돌아오고, 잠시 모든 요청이 막힌다.
#
# 6개로 던졌더니 혼자 돌 때는 16초에 다 받았는데, 사용자 요청과 겹치자
# 101개가 거의 전부 막혔다. 훑기는 서버 시작할 때 한 번 하는 일이라
# 빠를 이유가 없다. 느리고 안 막히는 쪽을 고른다.
SCAN_CONCURRENCY = 2

# 요청 사이에 쉬는 시간(초). 초당 제한을 넘지 않으려는 것이다.
SCAN_PAUSE = 0.15

# 막힌 품목을 다시 물어볼 횟수와 쉬는 시간(초).
# 제한은 잠깐이라 조금 쉬었다 부르면 대개 들어온다.
SCAN_RETRIES = 2
SCAN_RETRY_WAIT = 2.0

# 훑은 결과를 남겨두는 파일.
#
# 서버를 다시 켤 때마다 101개를 훑으면 개발 중에만 하루 수천 번이 나간다
# (uvicorn --reload 는 파일을 고칠 때마다 프로세스를 새로 띄운다).
# 같은 날 훑어둔 게 있으면 그걸 읽고 넘어간다.
#
# KAMIS 는 공공데이터라 저장·공유에 제한이 없다. 이 파일을 커밋해두면
# 팀원이 인증키 없이도 실제 시세로 화면을 볼 수 있다.
BARGAIN_FILE = Path(__file__).resolve().parents[1] / "data" / "bargains.json"

# 훑은 결과를 기억해두는 시간(초). 조사값은 하루 한 번 갱신된다.
BARGAIN_CACHE_SECONDS = 60 * 60 * 3

# "쌈" 이라고 부를 최소 하락폭(%).
# 3~4% 는 장 볼 때 체감되지 않는다. 배지를 붙일 만한 폭만 고른다.
BARGAIN_MIN_DROP = 5.0

# 화면과 프롬프트에 넘길 기본 개수.
# 너무 많으면 AI 가 아무거나 골라 오고, 화면도 읽히지 않는다.
DEFAULT_BARGAIN_LIMIT = 8

# (저장시각, 목록). 아직 안 훑었으면 None.
bargain_cache: tuple[float, list[dict]] | None = None

# 훑기는 한 번에 하나만 돈다.
#
# 이게 없으면 서버가 뜰 때 미리 훑는 작업과 사용자 요청이 겹쳐서 두 번
# 동시에 돈다. 요청 수가 두 배가 되어 초당 제한에 걸리고, 결국 양쪽 다
# 절반만 받아온다(실제로 8개 나와야 할 싼 재료가 3개만 나왔다).
scan_lock = asyncio.Lock()


def unique_items() -> list[str]:
    """코드가 겹치는 이름을 하나로 줄인다.

    품목표에는 별칭이 함께 들어 있다("소고기"와 "쇠고기"가 같은 코드).
    그대로 훑으면 같은 품목을 두 번 물어보게 된다.
    """
    seen: dict[tuple, str] = {}

    for name, codes in KAMIS_ITEM_CODES.items():
        seen.setdefault(codes, name)

    return list(seen.values())


async def scan_bargains() -> list[dict]:
    """모든 품목의 가격 흐름을 훑어 값이 내린 순으로 돌려준다.

    실패한 품목은 조용히 건너뛴다. 몇 개 못 받았다고 추천 화면 전체가
    비면 안 된다.
    """
    names = unique_items()
    limiter = asyncio.Semaphore(SCAN_CONCURRENCY)

    async def one(client: httpx.AsyncClient, name: str) -> dict | None:
        offers: list[dict] = []
        trend = None

        for attempt in range(SCAN_RETRIES + 1):
            async with limiter:
                try:
                    offers, trend = await price_api.kamis_offers(client, name)
                except Exception as exc:  # 한 품목 때문에 전체가 죽으면 안 된다
                    print(
                        f"[시세 훑기 실패] {name}: {type(exc).__name__}: {exc}",
                        flush=True,
                    )
                    return None

                # 다음 요청을 바로 던지지 않는다. 초당 제한을 넘으면
                # 한동안 전부 막혀서 오히려 더 오래 걸린다.
                await asyncio.sleep(SCAN_PAUSE)

            if offers:
                break

            # 값이 없는 품목(가공식품)인지, 제한에 걸린 것인지 여기서는
            # 구분되지 않는다. 그래서 몇 번만 더 물어보고 그만둔다.
            if attempt < SCAN_RETRIES:
                await asyncio.sleep(SCAN_RETRY_WAIT)

        if not offers or not trend:
            return None

        return {
            "name": name,
            "percent": trend["percent"],
            "price": offers[0]["price"],
            "unit": offers[0]["unit"],
            "surveyed_at": offers[0].get("surveyed_at"),
        }

    async with httpx.AsyncClient(timeout=price_api.PRICE_TIMEOUT_SECONDS) as client:
        results = await asyncio.gather(*[one(client, name) for name in names])

    found = [row for row in results if row is not None]

    # 많이 내린 것부터. 오른 것도 목록에 남겨두면 화면이 "오늘은 배추가 비싸요"
    # 까지 말할 수 있다. 자르는 건 꺼내 쓰는 쪽에서 한다.
    found.sort(key=lambda row: row["percent"])

    return found


def read_bargain_file() -> list[dict] | None:
    """오늘 훑어둔 파일이 있으면 읽는다. 없거나 어제 것이면 None.

    조사값은 하루 한 번 바뀌므로 날짜가 같으면 다시 훑을 이유가 없다.
    """
    try:
        saved = json.loads(BARGAIN_FILE.read_text(encoding="utf-8"))
    except (OSError, ValueError):
        return None

    if not isinstance(saved, dict):
        return None

    if saved.get("scanned_on") != date.today().isoformat():
        return None

    rows = saved.get("rows")

    return rows if isinstance(rows, list) else None


def write_bargain_file(rows: list[dict]) -> None:
    """훑은 결과를 남긴다. 실패해도 넘어간다 — 메모리 캐시로도 돌아간다."""
    try:
        BARGAIN_FILE.parent.mkdir(parents=True, exist_ok=True)
        BARGAIN_FILE.write_text(
            json.dumps(
                {"scanned_on": date.today().isoformat(), "rows": rows},
                ensure_ascii=False,
                indent=1,
            ),
            encoding="utf-8",
        )
    except OSError as exc:
        print(f"[시세] 파일 저장 실패: {exc}", flush=True)


def fresh_bargains() -> list[dict] | None:
    """기억해둔 목록이 아직 쓸 만하면 돌려준다. 아니면 None."""
    if bargain_cache is None:
        return None

    saved_at, rows = bargain_cache
    if time.time() - saved_at >= BARGAIN_CACHE_SECONDS:
        return None

    return rows


async def get_bargains(limit: int = DEFAULT_BARGAIN_LIMIT) -> list[dict]:
    """값이 내린 재료를 꺼낸다. 기억해둔 게 없거나 오래됐으면 새로 훑는다."""
    global bargain_cache

    rows = fresh_bargains()
    if rows is not None:
        return pick_cheap(rows, limit)

    # 훑기는 한 번에 하나만. 여럿이 동시에 돌면 초당 제한에 걸려
    # 양쪽 다 절반만 받아온다.
    async with scan_lock:
        # 기다리는 사이에 다른 쪽이 다 채워놨을 수 있다.
        rows = fresh_bargains()
        if rows is not None:
            return pick_cheap(rows, limit)

        # 오늘 훑어둔 파일이 있으면 그걸 쓴다.
        # 서버를 다시 켤 때마다 101개를 훑지 않기 위함이다.
        rows = read_bargain_file()

        if rows is None:
            rows = await scan_bargains()
            write_bargain_file(rows)

        bargain_cache = (time.time(), rows)

    return pick_cheap(rows, limit)


def pick_cheap(rows: list[dict], limit: int) -> list[dict]:
    """많이 내린 재료만 골라낸다. 오른 것과 조금 움직인 것은 뺀다."""
    cheap = [row for row in rows if row["percent"] <= -BARGAIN_MIN_DROP]

    return cheap[:limit]


async def warm_bargains() -> None:
    """서버가 뜰 때 미리 훑어둔다 (main.py 에서 백그라운드로 부른다).

    사용자가 첫 요청에서 16초를 기다리지 않게 하려는 것뿐이다.
    실패해도 서버는 떠야 한다 — 다음 요청 때 다시 훑는다.
    """
    try:
        rows = await get_bargains()
        print(f"[시세] 값이 내린 재료 {len(rows)}개를 찾았습니다.", flush=True)
    except Exception as exc:
        print(f"[시세] 미리 훑기 실패: {type(exc).__name__}: {exc}", flush=True)
