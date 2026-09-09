"""DB 없이 도는 검증.

계산만 하는 코드(요리 레벨 · mock AI · 가격 응답 파싱 · KST 변환)를 확인한다.
Oracle 을 안 켜도 돌아가므로, 로직을 고칠 때마다 부담 없이 실행할 수 있다.
"""

from datetime import date, datetime, timedelta, timezone

import httpx
import pytest

from app.core.kst import KST, day_end_utc, day_start_utc, to_kst_date
from app.integrations import ai_api
from app.integrations.weather_api import cache_key, join_address
from app.integrations import ingredient_match
from app.integrations import price_api
from app.services import unit_convert
from app.services.history_service import count_streak
from app.services.user_service import make_level, make_title


# =========================================================
# 요리 레벨
# =========================================================


def test_레벨은_1부터_시작한다():
    level, exp, exp_next = make_level(0)

    assert level == 1
    assert exp == 0
    assert exp_next == 100


def test_경험치가_쌓이면_레벨이_오른다():
    # 100 을 채우면 2레벨이 되고, 2 -> 3 은 150 이 필요하다
    level, exp, exp_next = make_level(100)

    assert level == 2
    assert exp == 0
    assert exp_next == 150


def test_레벨업하고_남은_경험치가_이어진다():
    level, exp, exp_next = make_level(130)

    assert level == 2
    assert exp == 30
    assert exp_next == 150


def test_칭호는_레벨_구간을_따른다():
    assert make_title(1) == "요리 입문"
    assert make_title(4) == "요리 입문"
    assert make_title(5) == "요리 초보"
    assert make_title(12) == "요리 중수"
    assert make_title(99) == "요리 장인"


# =========================================================
# 연속 방문일 (streak)
# =========================================================


def test_기록이_없으면_연속은_0일():
    assert count_streak(set()) == 0


def test_오늘부터_사흘_연속():
    today = date.today()
    days = {today, today - timedelta(days=1), today - timedelta(days=2)}

    assert count_streak(days) == 3


def test_어제까지만_있어도_연속으로_친다():
    # 오늘 아직 아무것도 안 먹었다고 7일 연속이 0이 되면
    # 저녁 먹기 전까지 화면이 거짓말을 한다
    today = date.today()
    days = {today - timedelta(days=1), today - timedelta(days=2)}

    assert count_streak(days) == 2


def test_하루_비면_거기서_끊긴다():
    today = date.today()
    days = {today, today - timedelta(days=2), today - timedelta(days=3)}

    assert count_streak(days) == 1


# =========================================================
# 한국 시각 <-> UTC
# =========================================================


def test_한국_날짜의_0시는_전날_15시_UTC():
    start = day_start_utc(date(2026, 9, 6))

    assert start == datetime(2026, 9, 5, 15, 0, tzinfo=timezone.utc)


def test_끝_경계는_다음날_0시다():
    # 23:59:59 로 잡으면 그날 마지막 1초에 들어온 데이터가 빠진다
    end = day_end_utc(date(2026, 9, 6))

    assert end == datetime(2026, 9, 6, 15, 0, tzinfo=timezone.utc)


def test_UTC_새벽은_한국의_전날이_아니라_같은_날():
    # 2026-09-06 00:30 UTC = 2026-09-06 09:30 KST
    moment = datetime(2026, 9, 6, 0, 30, tzinfo=timezone.utc)

    assert to_kst_date(moment) == date(2026, 9, 6)


def test_UTC_늦은_밤은_한국의_다음날():
    # 2026-09-06 16:00 UTC = 2026-09-07 01:00 KST
    moment = datetime(2026, 9, 6, 16, 0, tzinfo=timezone.utc)

    assert to_kst_date(moment) == date(2026, 9, 7)


def test_KST는_9시간_앞선다():
    assert KST.utcoffset(None) == timedelta(hours=9)


# =========================================================
# mock AI — 추천
#
# 분기 함수(recommend_foods)가 아니라 mock 구현(mock_recommend_foods)을
# 직접 부른다. 분기 함수를 부르면 .env 의 AI_PROVIDER 를 따라가서,
# gemini 로 켜둔 사람의 컴퓨터에서는 이 테스트가 진짜 API 를 호출한다.
# =========================================================


def context(**overrides) -> dict:
    """테스트용 기본 조건. 필요한 것만 덮어쓴다."""
    base = {
        "limit": 5,
        "category": None,
        "budget_min": None,
        "budget_max": None,
        "query": None,
        "ingredients": [],
        "allergies": [],
        "dislikes": [],
        "weather": None,
        "exclude": [],
    }
    base.update(overrides)
    return base


def names_of(items) -> list[str]:
    result = []
    for item in items:
        result.append(item["food_name"])
    return result


def test_요청한_개수만큼_돌려준다():
    items = ai_api.mock_recommend_foods(context(limit=3))

    assert len(items) == 3


def test_같은_조건이면_같은_결과가_나온다():
    # 랜덤을 쓰면 새로고침할 때마다 순서가 바뀌어 화면 버그를 못 잡는다
    first = ai_api.mock_recommend_foods(context(limit=5))
    second = ai_api.mock_recommend_foods(context(limit=5))

    assert names_of(first) == names_of(second)


def test_알레르기_재료가_든_메뉴는_빠진다():
    items = ai_api.mock_recommend_foods(context(limit=12, allergies=["돼지고기"]))

    assert "김치찌개" not in names_of(items)
    assert "마파두부" not in names_of(items)


def test_예산_상한을_넘는_메뉴는_빠진다():
    items = ai_api.mock_recommend_foods(context(limit=12, budget_max=5000))

    for item in items:
        assert item["estimated_cost"] <= 5000


def test_분류를_주면_그_분류만_나온다():
    items = ai_api.mock_recommend_foods(context(limit=12, category="중식"))

    assert len(items) > 0
    for item in items:
        assert item["category"] == "중식"


def test_이미_본_메뉴는_다시_안_나온다():
    first = ai_api.mock_recommend_foods(context(limit=5))
    shown = names_of(first)

    second = ai_api.mock_recommend_foods(context(limit=5, exclude=shown))

    for name in names_of(second):
        assert name not in shown


def test_검색어가_있으면_날씨보다_검색어가_우선이다():
    # '매운 국물요리'를 찾았는데 더운 날이라고 냉면이 위로 오면
    # 검색이 고장 난 것처럼 보인다
    items = ai_api.mock_recommend_foods(
        context(limit=3, query="매운 국물요리", weather={"temp": 30})
    )

    tags = items[0].get("tags") or []
    assert "국물" in tags


def test_더운_날에는_시원한_메뉴가_먼저_나온다():
    items = ai_api.mock_recommend_foods(context(limit=3, weather={"temp": 30}))

    assert "시원한음식" in (items[0].get("tags") or [])


def test_추운_날에는_국물_메뉴가_먼저_나온다():
    items = ai_api.mock_recommend_foods(context(limit=3, weather={"temp": 0}))

    tags = items[0].get("tags") or []
    assert "국물" in tags or "얼큰" in tags


def test_냉장고_재료가_많이_겹치는_메뉴가_먼저_나온다():
    items = ai_api.mock_recommend_foods(
        context(limit=3, ingredients=["계란", "대파", "밥"])
    )

    top = items[0]
    matched = 0
    for need in top["ingredients"]:
        if need in ("계란", "대파", "밥"):
            matched += 1

    assert matched >= 2


# =========================================================
# mock AI — 레시피
# =========================================================


def test_레시피_모양이_스키마와_맞는다():
    recipe = ai_api.mock_build_recipe("김치찌개", {})

    assert recipe["recipe_name"]
    assert recipe["servings"] == 2
    assert len(recipe["steps"]) > 0
    assert len(recipe["ingredients"]) > 0

    first = recipe["ingredients"][0]
    # grams 는 가격 환산에 쓴다. 없으면 "고춧가루 1큰술" 에 1kg 값이 붙는다.
    assert set(first.keys()) == {"name", "quantity", "unit", "grams"}
    assert first["grams"] > 0


def test_알레르기_재료는_레시피에서_아예_빠진다():
    recipe = ai_api.mock_build_recipe("김치찌개", {"allergies": ["돼지고기"]})

    names = []
    for item in recipe["ingredients"]:
        names.append(item["name"])

    assert "돼지고기" not in names


def test_싫어하는_재료는_두되_대체안을_붙인다():
    recipe = ai_api.mock_build_recipe("김치찌개", {"dislikes": ["돼지고기"]})

    names = []
    for item in recipe["ingredients"]:
        names.append(item["name"])

    assert "돼지고기" in names
    assert recipe["substitutes"][0]["original"] == "돼지고기"
    assert recipe["substitutes"][0]["substitute"]


def test_카탈로그에_없는_음식도_레시피가_나온다():
    recipe = ai_api.mock_build_recipe("할머니표 비밀 국수", {})

    assert recipe["recipe_name"]
    assert len(recipe["steps"]) > 0


def test_조사를_받침에_맞춰_고른다():
    # "돼지고기 을(를)" 처럼 둘 다 적으면 기계가 쓴 글로 읽힌다
    assert ai_api.pick_josa("돼지고기", "을", "를") == "를"
    assert ai_api.pick_josa("계란", "을", "를") == "을"
    assert ai_api.pick_josa("두부", "을", "를") == "를"
    # 한글이 아니면 받침을 알 수 없다
    assert ai_api.pick_josa("pasta", "을", "를") == "를"


# =========================================================
# 가격을 못 구한 재료
#
# KAMIS 는 농수산물만 조사한다. 두부·간장 같은 가공식품은 값이 없다.
# 예전에는 mock 프로바이더가 그럴듯한 숫자로 채웠는데, 화면에서 진짜
# 조사가격과 구분되지 않아 표의 절반이 가짜인 채로 합계까지 계산됐다.
# 지금은 값을 지어내지 않고 빈 채로 내보낸다.
# =========================================================


def test_코드표에_없는_재료는_가격이_비어서_나간다():
    import asyncio

    from app.integrations.price_api import collect_offers

    # conftest 가 KAMIS_ITEM_CODES 를 비워둬서 바깥을 타지 않는다
    rows = asyncio.run(collect_offers(["두부", "간장"]))

    assert len(rows) == 2
    for row in rows:
        assert row["offers"] == []


def test_가격을_못_구해도_재료_줄은_남는다():
    import asyncio

    from app.integrations.price_api import collect_offers

    # 줄째로 사라지면 화면에서 "이 재료는 왜 없지?" 가 되고,
    # 합계가 전체 재료비인 것처럼 보인다.
    rows = asyncio.run(collect_offers(["두부", "대파", "간장"]))

    assert [row["name"] for row in rows] == ["두부", "대파", "간장"]


def test_지원하지_않는_프로바이더는_에러를_낸다():
    import asyncio

    from app.core.config import settings
    from app.integrations.price_api import collect_offers

    before = settings.PRICE_PROVIDERS
    settings.PRICE_PROVIDERS = "naver"
    try:
        # 조용히 건너뛰면 "가격이 안 나오는데 이유를 모르겠다"가 된다
        with pytest.raises(RuntimeError, match="naver"):
            asyncio.run(collect_offers(["대파"]))
    finally:
        settings.PRICE_PROVIDERS = before


# =========================================================
# 한글 주소 조합 (역지오코딩 결과 정리)
#
# Nominatim 이 채워주는 키가 지역마다 달라서, 그대로 이으면 같은 동네 이름이
# 두 번 찍히는 곳이 있다. 실제로 서귀포에서 "서귀포시 서홍동 서홍동" 이 나왔다.
# =========================================================


def test_시_구_동을_순서대로_잇는다():
    assert join_address("서울", "종로구", "종로1가") == "서울 종로구 종로1가"


def test_빈_칸은_건너뛴다():
    assert join_address("부산", None, "우1동") == "부산 우1동"
    assert join_address(None, None, "역삼동") == "역삼동"


def test_아무것도_없으면_None():
    assert join_address(None, None, None) is None


def test_똑같은_이름이_두_번_들어오면_한_번만_쓴다():
    # 서귀포 실제 응답: city_district 와 quarter 가 둘 다 "서홍동"
    assert join_address("서귀포시", "서홍동", "서홍동") == "서귀포시 서홍동"


def test_한쪽이_다른_쪽을_품으면_뺀다():
    assert join_address("서귀포시", None, "서귀포시 서홍동") == "서귀포시 서홍동"


def test_구와_동은_앞글자가_겹쳐도_둘_다_남긴다():
    # "종로구" 와 "종로1·2·3·4가동" 은 다른 단위라 둘 다 필요하다.
    result = join_address("서울", "종로구", "종로1·2·3·4가동")

    assert result == "서울 종로구 종로1·2·3·4가동"


def test_시_이름이_구_이름에_붙어_있어도_둘_다_남긴다():
    # 글자 단위로 "품는다"를 판정하면 "부산" 이 "부산진구" 를 삼켜서
    # 구 이름이 사라진다. 낱말 단위로 봐야 갈린다.
    assert join_address("부산", "부산진구", "부전동") == "부산 부산진구 부전동"


# =========================================================
# 날씨 캐시 열쇠
# =========================================================


def test_1km_안쪽은_같은_자리로_친다():
    # 소수점 2자리 = 약 1.1km. 그 안에서는 날씨도 동네 이름도 같다.
    assert cache_key(37.5711, 126.9885) == cache_key(37.5708, 126.9881)


def test_먼_곳은_다른_자리다():
    assert cache_key(37.5711, 126.9885) != cache_key(35.1587, 129.1604)


# =========================================================
# 날씨 API 가 죽었을 때
#
# 실제 외부 서비스에 의존하는 부분이라, 남의 서버가 흔들렸을 때
# 우리 화면이 어떻게 되는지가 중요하다.
# =========================================================


def call_weather_with(broken):
    """httpx.get 을 고장난 함수로 바꿔치기하고 /api/weather 를 부른다.

    provider 를 openweather 로 강제하는 이유
    ---------------------------------------
    .env 가 WEATHER_PROVIDER=mock 이면 get_weather() 가 httpx 를 타지 않고
    고정값을 그대로 돌려준다. 그러면 고장난 함수가 불릴 일이 없어서
    이 테스트는 늘 200 을 받고 실패한다.

    확인하려는 건 "바깥 서버가 죽었을 때 우리 API 가 503 을 주는가" 이므로,
    바깥을 부르는 경로를 타도록 여기서 설정을 갈아끼운다. .env 값에 따라
    통과했다 실패했다 하면 테스트가 아니라 운이다.
    """
    from unittest.mock import patch

    from fastapi.testclient import TestClient

    from app.core.config import settings
    from app.integrations import weather_api
    from app.main import app

    # 캐시가 남아 있으면 바깥을 안 부르고 저장된 값을 돌려준다
    weather_api.weather_cache.clear()

    before = (settings.WEATHER_PROVIDER, settings.OPENWEATHER_API_KEY)
    settings.WEATHER_PROVIDER = "openweather"
    # 키가 비어 있으면 httpx 를 부르기 전에 RuntimeError 로 먼저 막힌다.
    settings.OPENWEATHER_API_KEY = "test-key"

    try:
        client = TestClient(app, raise_server_exceptions=False)
        with patch.object(weather_api.httpx, "get", broken):
            return client.get("/api/weather", params={"lat": 37.6, "lon": 127.0})
    finally:
        settings.WEATHER_PROVIDER, settings.OPENWEATHER_API_KEY = before
        weather_api.weather_cache.clear()


def test_날씨_API_가_안_되면_500이_아니라_503():
    def refuse(*args, **kwargs):
        raise httpx.ConnectError("연결 실패")

    response = call_weather_with(refuse)

    # 500 이면 화면에 "요청을 처리하지 못했습니다" 만 뜨고 원인을 알 수 없다
    assert response.status_code == 503
    assert "날씨 API" in response.json()["detail"]


def test_키가_틀리면_키를_확인하라고_알려준다():
    def unauthorized(*args, **kwargs):
        request = httpx.Request("GET", "https://example.com")
        response = httpx.Response(401, request=request)
        raise httpx.HTTPStatusError("401", request=request, response=response)

    response = call_weather_with(unauthorized)

    assert response.status_code == 503
    assert "OPENWEATHER_API_KEY" in response.json()["detail"]


# =========================================================
# Gemini 오류 문구
#
# 실제 호출 없이 확인할 수 있는 부분만 본다.
# 원문이 영어라 그대로 내보내면 사용자가 못 읽는다.
# =========================================================


class FakeApiError:
    """google.genai 의 APIError 를 흉내 낸다. code 만 있으면 된다."""

    def __init__(self, code):
        self.code = code


def test_붐빌_때는_기다리라고_알려준다():
    message = ai_api.describe_gemini_error(FakeApiError(503), "gemini-3.7-flash")

    assert "붐비" in message
    assert "다시 시도" in message


def test_할당량_초과는_한도라고_알려준다():
    message = ai_api.describe_gemini_error(FakeApiError(429), "gemini-3.7-flash")

    assert "한도" in message


def test_없는_모델이면_모델_이름을_짚어준다():
    # 어느 모델이 문제인지 안 적어주면 예비 모델까지 있을 때 원인을 못 찾는다
    message = ai_api.describe_gemini_error(FakeApiError(404), "gemini-9-flash")

    assert "gemini-9-flash" in message
    assert "GEMINI_MODEL" in message


def test_인증_오류는_키를_확인하라고_알려준다():
    message = ai_api.describe_gemini_error(FakeApiError(400), "gemini-3.7-flash")

    assert "GEMINI_API_KEY" in message


def test_모르는_오류도_영어를_그대로_내보내지_않는다():
    message = ai_api.describe_gemini_error(FakeApiError(418), "gemini-3.7-flash")

    assert "AI 응답을 받지 못했습니다" in message


def test_모델_목록은_쉼표로_나뉜다():
    from app.core.config import split_by_comma

    assert split_by_comma("gemini-3.7-flash, gemini-3.5-flash") == [
        "gemini-3.7-flash",
        "gemini-3.5-flash",
    ]


# =========================================================
# 주소 -> 좌표
#
# 회원가입에서 고른 주소를 좌표로 바꿔 저장하고, 그 좌표로 날씨를 부른다.
# 목록에서 찾기만 하므로 바깥 API 를 부르지 않는다.
# =========================================================


def test_시군구까지_고르면_그_좌표가_나온다():
    from app.data.regions import find_coords

    coords = find_coords("서울특별시", "강남구")

    assert coords is not None
    lat, lon = coords
    # 강남구는 대략 북위 37.5, 동경 127.0 언저리다
    assert 37.0 < lat < 38.0
    assert 126.5 < lon < 127.5


def test_시도만_골라도_중심_좌표가_나온다():
    # 시·군·구를 안 골랐다고 좌표가 아예 없으면, 위치 권한을 거부한 사람은
    # 영영 기본 좌표(종로2가) 날씨를 보게 된다
    from app.data.regions import find_coords

    assert find_coords("부산광역시", None) is not None


def test_시군구가_없는_시도도_좌표가_나온다():
    # 세종특별자치시는 아래에 시·군·구가 없는 단층 자치시다
    from app.data.regions import find_coords

    assert find_coords("세종특별자치시", None) is not None


def test_먼_지역은_좌표도_멀다():
    from app.data.regions import find_coords

    seoul = find_coords("서울특별시", "종로구")
    jeju = find_coords("제주특별자치도", "서귀포시")

    # 서울과 서귀포는 위도가 4도 넘게 차이 난다
    assert seoul[0] - jeju[0] > 3


def test_시군구가_안_맞으면_시도_중심으로_떨어진다():
    """해운대구는 부산의 구다. 서울과 짝지으면 맞는 조합이 아니다.

    이때 None 을 주면 그 회원은 기본 좌표(종로2가) 날씨를 보게 된다.
    적어도 고른 시·도 안에서는 맞는 날씨를 보여주는 편이 낫다.
    """
    from app.data.regions import find_coords

    seoul_center = find_coords("서울특별시", None)

    assert find_coords("서울특별시", "해운대구") == seoul_center


def test_아예_없는_시도는_None():
    from app.data.regions import find_coords

    assert find_coords("없는도", "없는구") is None
    assert find_coords(None, None) is None
    assert find_coords("", "강남구") is None


def test_목록에는_좌표가_없다():
    # 화면은 이름만 쓴다. 250개가 넘는 항목마다 숫자 두 개씩 붙으면
    # 응답만 커진다.
    from app.data.regions import list_regions

    regions = list_regions()

    assert len(regions) == 17  # 시·도 17개
    for item in regions:
        assert set(item.keys()) == {"name", "sigungu"}


def test_모든_시군구에_좌표가_있다():
    # 좌표가 빠진 항목이 있으면 그 지역 사람은 주소를 골라도 날씨가 안 맞는다.
    # 목록을 만들 때 지명을 잘못 적으면 여기서 걸린다.
    from app.data.regions import REGIONS

    missing = []
    for sido in REGIONS:
        if "lat" not in sido:
            missing.append(sido["name"])
        for sigungu in sido["sigungu"]:
            if "lat" not in sigungu:
                missing.append(f"{sido['name']} {sigungu['name']}")

    assert missing == [], f"좌표 없는 지역: {missing}"


# =========================================================
# KAMIS 응답 파싱
#
# 실제 호출은 인증키가 있어야 하므로, 명세서에 적힌 응답 모양을
# 그대로 흉내낸 값으로 파싱만 확인한다.
# =========================================================


def kamis_row(day, region, market, price, unit="1", size="kg"):
    return {
        "exmn_ymd": day,
        "sgg_nm": region,
        "mrkt_nm": market,
        "unit": unit,
        "unit_sz": size,
        "exmn_dd_prc": price,
    }


def test_KAMIS_응답에서_행을_꺼낸다():
    payload = {
        "response": {
            "header": {"resultCode": "00", "resultMsg": "NORMAL SERVICE"},
            "body": {"items": {"item": [kamis_row("20260908", "서울", "가락시장", "3200")]}},
        }
    }

    rows = price_api.kamis_rows(payload)

    assert len(rows) == 1
    assert rows[0]["mrkt_nm"] == "가락시장"


def test_KAMIS_행이_하나면_객체로_와도_읽는다():
    # 기관에 따라 행이 하나일 때 배열이 아니라 객체로 온다
    payload = {"response": {"body": {"items": {"item": kamis_row("20260908", "서울", "가락시장", "3200")}}}}

    assert len(price_api.kamis_rows(payload)) == 1


def test_KAMIS_가장_최근_조사일자만_남긴다():
    # 주말을 건너뛰려고 일주일치를 받아온다. 섞어서 최저/최고를 내면
    # '시장 차이'가 아니라 '날짜 차이'를 보여주게 된다.
    rows = [
        kamis_row("20260902", "서울", "가락시장", "9000"),
        kamis_row("20260908", "서울", "가락시장", "3200"),
        kamis_row("20260908", "부산", "엄궁시장", "3000"),
    ]

    latest = price_api.latest_rows(rows)

    assert len(latest) == 2
    assert {row["exmn_dd_prc"] for row in latest} == {"3200", "3000"}


def test_KAMIS_인증키_오류를_읽어낸다():
    # 인증키 문제는 서비스 봉투가 아니라 게이트웨이 봉투로 온다
    payload = {
        "OpenAPI_ServiceResponse": {
            "cmmMsgHeader": {
                "errMsg": "SERVICE_KEY_IS_NOT_REGISTERED_ERROR",
                "returnAuthMsg": "등록되지 않은 서비스키",
                "returnReasonCode": "30",
            }
        }
    }

    message = price_api.kamis_error(payload)

    assert message is not None
    assert "등록되지 않은 서비스키" in message


def test_KAMIS_정상응답은_오류가_아니다():
    payload = {"response": {"header": {"resultCode": "00", "resultMsg": "NORMAL SERVICE"}}}

    assert price_api.kamis_error(payload) is None


def test_KAMIS_단위를_붙여_읽는다():
    assert price_api.kamis_unit({"unit_sz": "1", "unit": "kg"}) == "1kg"
    assert price_api.kamis_unit({"unit_sz": "", "unit": ""}) == ""


def test_가격에_쉼표가_붙어도_정수로_읽는다():
    assert price_api.to_int("4,350") == 4350
    assert price_api.to_int("3200") == 3200
    assert price_api.to_int("") is None


def test_코드표에_있는_재료는_부류와_품목을_모두_가진다():
    # 표는 ingredient_match 가 갖고 있다 (price_api 는 그걸 빌려 쓴다)
    for name, codes in ingredient_match.KAMIS_ITEM_CODES.items():
        category, item, variety = codes
        assert category in {"100", "200", "300", "400", "500", "600"}, name
        assert item.isdigit(), name
        assert variety is None or variety.isdigit(), name


def test_단위가_섞이면_가장_흔한_단위만_남긴다():
    # 계란은 10구와 30구가 같이 온다. 섞으면 최저 10구 ~ 최고 30구가 되어
    # 세 배쯤 부풀려진다.
    rows = [
        {"unit_sz": "30", "unit": "구", "exmn_dd_prc": "7000"},
        {"unit_sz": "30", "unit": "구", "exmn_dd_prc": "8000"},
        {"unit_sz": "10", "unit": "구", "exmn_dd_prc": "3000"},
    ]

    kept = price_api.same_unit_rows(rows)

    assert len(kept) == 2
    assert {price_api.kamis_unit(r) for r in kept} == {"30구"}


def test_품종코드가_있으면_요청에_넣는다():
    with_variety = price_api.kamis_params("200", "246", "00")
    without = price_api.kamis_params("200", "245", None)

    assert with_variety["cond[vrty_cd::EQ]"] == "00"
    assert "cond[vrty_cd::EQ]" not in without


# =========================================================
# 필요량 환산
#
# KAMIS 는 "고춧가루 1kg 25,200원" 처럼 판매 단위로만 값을 준다.
# 레시피는 "1큰술" 을 요구한다. 환산하지 않고 더하면 재료비가
# 백 배 넘게 부풀려진다 (김치수제비 1,179원 -> 31,905원).
# =========================================================


def test_무게_단위는_그램으로_환산한다():
    # 고춧가루 1큰술(7g) 이 필요하고 KAMIS 는 1kg 단위로 판다
    ratio = unit_convert.needed_ratio(7, 1, "큰술", "1kg")

    assert ratio == pytest.approx(0.007)
    assert unit_convert.scale_price(25_200, ratio) == 176


def test_판매단위가_100g_이면_그램수를_그대로_나눈다():
    # 돼지고기 240g 이 필요하고 KAMIS 는 100g 단위
    ratio = unit_convert.needed_ratio(240, 240, "g", "100g")

    assert ratio == pytest.approx(2.4)
    assert unit_convert.scale_price(1_573, ratio) == 3_775


def test_세는_단위는_개수끼리_맞춘다():
    # 애호박 0.5개가 필요하고 KAMIS 는 1개 단위.
    # 개당 무게를 몰라도 개수끼리는 맞출 수 있다.
    ratio = unit_convert.needed_ratio(140, 0.5, "개", "1개")

    assert ratio == pytest.approx(0.5)


def test_구와_개는_같은_것을_센다():
    # 계란은 KAMIS 가 30구로 파는데 레시피는 2개라고 적는다
    ratio = unit_convert.needed_ratio(110, 2, "개", "30구")

    assert ratio == pytest.approx(2 / 30)


def test_생선_한_손은_두_마리다():
    ratio = unit_convert.needed_ratio(None, 1, "마리", "1손")

    assert ratio == pytest.approx(0.5)


def test_세는_단위가_서로_다르면_환산하지_않는다():
    # '마리' 짜리를 '개' 로 나누면 엉뚱한 값이 나온다.
    # 억지로 맞춘 숫자를 합계에 넣느니 환산 불가로 두는 편이 낫다.
    assert unit_convert.needed_ratio(None, 2, "개", "1마리") is None


def test_무게_판매인데_그램을_모르면_환산하지_않는다():
    assert unit_convert.needed_ratio(None, 1, "큰술", "1kg") is None
    assert unit_convert.needed_ratio(0, 1, "큰술", "1kg") is None


def test_환산가는_0원이_되지_않는다():
    # 소금 0.5작은술은 2원이다. 반올림해서 0 이 되면 합계에서 사라진다.
    assert unit_convert.scale_price(3_750, 0.0000001) == 1


def test_판매단위_문자열을_쪼갠다():
    assert unit_convert.parse_unit("1kg") == (1.0, "kg")
    assert unit_convert.parse_unit("100g") == (100.0, "g")
    assert unit_convert.parse_unit("30구") == (30.0, "구")
    assert unit_convert.parse_unit("") is None


# =========================================================
# 재료명 맞추기
#
# 제미나이는 "다진 마늘" 이라고 적고 KAMIS 는 "마늘" 로만 찾을 수 있다.
# 이름이 조금만 달라도 값이 안 붙어서, 실제로 김치수제비 재료 14개 중
# 4개만 가격이 나왔다.
# =========================================================


def test_손질_표현을_떼어낸다():
    assert ingredient_match.normalize("다진 마늘") == "마늘"
    assert ingredient_match.normalize("잘게 썬 대파") == "대파"
    assert ingredient_match.normalize("국산 돼지고기") == "돼지고기"


def test_괄호_설명을_떼어낸다():
    assert ingredient_match.normalize("밀가루 (중력분)") == "밀가루"
    assert ingredient_match.normalize("고추장(태양초)") == "고추장"


def test_다진마늘은_마늘로_찾는다():
    codes = ingredient_match.find_codes("다진 마늘")

    assert codes is not None
    assert codes[:2] == ("200", "258")


def test_별칭으로도_찾는다():
    # 청양고추는 KAMIS 품목명이 아니지만 풋고추로 조사된다
    assert ingredient_match.find_codes("청양고추") == ingredient_match.find_codes("고추")


def test_긴_이름_안에_든_품목을_찾는다():
    codes = ingredient_match.find_codes("돼지고기 앞다리살")

    assert codes is not None
    assert codes[:2] == ("500", "4304")


def test_짧은_품목명이_긴_이름을_가로채지_않는다():
    # '김치' 에서 '김'(해조류)을, '양파' 에서 '파'를 찾으면 안 된다.
    # 한 글자 품목은 정확히 일치할 때만 쓴다.
    assert ingredient_match.find_codes("김치") is None
    assert ingredient_match.find_codes("김칫국물") is None

    assert ingredient_match.find_codes("양파")[:2] == ("200", "245")
    assert ingredient_match.find_codes("파")[:2] == ("200", "246")
    assert ingredient_match.find_codes("배추")[:2] == ("200", "211")


def test_물은_가격_조회_대상이_아니다():
    assert ingredient_match.is_not_priced("물")
    assert ingredient_match.is_not_priced("반죽용 물")
    assert ingredient_match.is_not_priced("멸치 다시마 육수용 물")
    assert not ingredient_match.is_not_priced("대파")


def test_별칭은_모두_실제_품목을_가리킨다():
    # 없는 품목으로 이으면 코드를 못 찾아 조용히 값이 빈다
    for name, target in ingredient_match.ALIASES.items():
        assert target in ingredient_match.KAMIS_ITEM_CODES, name


def test_프롬프트_어휘는_품목표와_같다():
    assert set(ingredient_match.vocabulary()) == set(ingredient_match.KAMIS_ITEM_CODES)
