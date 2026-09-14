"""DB 없이 도는 검증.

계산만 하는 코드(요리 레벨 · 카탈로그 추천 · 가격 응답 파싱 · KST 변환)를 확인한다.
Oracle 을 안 켜도 돌아가므로, 로직을 고칠 때마다 부담 없이 실행할 수 있다.
"""

from datetime import date, datetime, timedelta, timezone

import time

import httpx
import pytest

from fastapi import HTTPException

from app.core.config import settings
from app.core.kst import KST, day_end_utc, day_start_utc, to_kst_date
from app.data import find_food
from app.integrations import ai_api
from app.integrations import recipe_book
from app.integrations.weather_api import cache_key, join_address
from app.integrations import ingredient_match
from app.integrations import price_api
from app.integrations import steps as steps_module
from app.services import ai_service
from app.services import bargain_service
from app.services import file_service
from app.services import game_service
from app.services import unit_convert
from app.services import weather_service
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
# 카탈로그 추천
#
# 분기 함수(recommend_foods)가 아니라 카탈로그 구현(catalog_recommend_foods)을
# 직접 부른다. 분기 함수를 부르면 .env 의 AI_PROVIDER 를 따라가서,
# gemini 로 켜둔 사람의 컴퓨터에서는 이 테스트가 진짜 API 를 호출한다.
# =========================================================


PICK_LEADS_HOT = "더운 날씨엔 시원한 한 그릇 어떠세요?"


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
    items = ai_api.catalog_recommend_foods(context(limit=3))

    assert len(items) == 3


def test_같은_조건이면_같은_결과가_나온다():
    # 랜덤을 쓰면 새로고침할 때마다 순서가 바뀌어 화면 버그를 못 잡는다
    first = ai_api.catalog_recommend_foods(context(limit=5))
    second = ai_api.catalog_recommend_foods(context(limit=5))

    assert names_of(first) == names_of(second)


def test_알레르기_재료가_든_메뉴는_빠진다():
    items = ai_api.catalog_recommend_foods(context(limit=12, allergies=["돼지고기"]))

    assert "김치찌개" not in names_of(items)
    assert "마파두부" not in names_of(items)


def test_예산_상한을_넘는_메뉴는_빠진다():
    items = ai_api.catalog_recommend_foods(context(limit=12, budget_max=5000))

    for item in items:
        assert item["estimated_cost"] <= 5000


def test_분류를_주면_그_분류만_나온다():
    items = ai_api.catalog_recommend_foods(context(limit=12, category="중식"))

    assert len(items) > 0
    for item in items:
        assert item["category"] == "중식"


def test_이미_본_메뉴는_다시_안_나온다():
    first = ai_api.catalog_recommend_foods(context(limit=5))
    shown = names_of(first)

    second = ai_api.catalog_recommend_foods(context(limit=5, exclude=shown))

    for name in names_of(second):
        assert name not in shown


def test_검색어가_있으면_날씨보다_검색어가_우선이다():
    # '매운 국물요리'를 찾았는데 더운 날이라고 냉면이 위로 오면
    # 검색이 고장 난 것처럼 보인다
    items = ai_api.catalog_recommend_foods(
        context(limit=3, query="매운 국물요리", weather={"temp": 30})
    )

    tags = items[0].get("tags") or []
    assert "국물" in tags


def test_더운_날에는_시원한_메뉴가_먼저_나온다():
    items = ai_api.catalog_recommend_foods(context(limit=3, weather={"temp": 30}))

    assert "시원한음식" in (items[0].get("tags") or [])


def test_추운_날에는_국물_메뉴가_먼저_나온다():
    items = ai_api.catalog_recommend_foods(context(limit=3, weather={"temp": 0}))

    tags = items[0].get("tags") or []
    assert "국물" in tags or "얼큰" in tags


# 하늘 상태(비 · 눈)
# ------------------
# 기온만 보던 때는 28도 장마날에 냉면이 1등이었다.


def test_같은_기온이어도_비가_오면_추천이_달라진다():
    clear = names_of(
        ai_api.catalog_recommend_foods(
            context(limit=4, weather={"temp": 28, "condition": "CLEAR"})
        )
    )
    rain = names_of(
        ai_api.catalog_recommend_foods(
            context(limit=4, weather={"temp": 28, "condition": "RAIN"})
        )
    )

    assert clear != rain


def test_비_오는_날에는_찬_음식이_위로_안_온다():
    items = ai_api.catalog_recommend_foods(
        context(limit=4, weather={"temp": 28, "condition": "RAIN"})
    )

    # 28도라 기온만 보면 냉국 · 냉면이 올라올 자리다
    for item in items:
        assert not ai_api.is_cold_dish(item), item["food_name"]


def test_비_오는_날에는_국물이나_튀김이_먼저_나온다():
    items = ai_api.catalog_recommend_foods(
        context(limit=3, weather={"temp": 28, "condition": "RAIN"})
    )

    tags = items[0].get("tags") or []
    assert "국물" in tags or "튀김" in tags


def test_눈_오는_날은_같은_기온의_맑은_날보다_찬_음식을_더_민다():
    cold_dish = find_food("물냉면")

    clear = ai_api.score_weather(cold_dish, {"temp": 2, "condition": "CLEAR"})
    snow = ai_api.score_weather(cold_dish, {"temp": 2, "condition": "SNOW"})

    assert snow < clear


def test_맑음과_흐림은_점수에_영향이_없다():
    food = find_food("김치찌개")

    assert ai_api.score_sky(food, "CLEAR") == 0
    assert ai_api.score_sky(food, "CLOUDS") == 0

    # 날씨 API 가 모르는 코드를 줘도 터지지 않고 0 이어야 한다
    assert ai_api.score_sky(food, "TORNADO") == 0
    assert ai_api.score_sky(food, None) == 0


def test_condition_이_없는_날씨도_그대로_돈다():
    # 예전 호출부와 테스트가 {"temp": 30} 만 넘긴다.
    # 하늘 상태가 없으면 기온 점수만 나와야 한다.
    food = find_food("콩국수")

    assert ai_api.score_weather(food, {"temp": 30}) == ai_api.score_temperature(
        food, 30
    )


def test_여름메뉴_태그만_있어도_찬_음식으로_친다():
    # 회냉면 · 비빔국수는 '시원한음식' 태그가 없다.
    # 태그 하나만 보면 눈 오는 날 추천에 그대로 남는다.
    assert ai_api.is_cold_dish(find_food("회냉면"))
    assert ai_api.is_cold_dish(find_food("비빔국수"))

    assert not ai_api.is_cold_dish(find_food("김치찌개"))


# 날씨 문구 — 왼쪽 문구와 오른쪽 카드가 같은 말을 해야 한다


def test_비_오는_날_문구는_기온_대신_비를_말한다():
    comment = weather_service.make_comment("RAIN", 18)

    # 첫 줄은 하늘과 기온을 다 말한다
    assert "비 오고 선선한" in comment

    # 둘째 줄은 기온용 문구("가볍고 든든한")가 아니라 비 문구여야 한다
    assert "튀김" in comment
    assert "가볍고 든든한" not in comment


def test_비_오는_날_머리말도_비를_말한다():
    assert weather_service.make_pick_lead("RAIN", 18) == (
        "비 오는 날엔 뜨끈한 국물 어떠세요?"
    )
    assert weather_service.make_pick_lead("SNOW", 2) == (
        "눈 오는 날엔 뜨끈한 한 그릇 어떠세요?"
    )


def test_맑은_날_문구는_그대로_기온을_따른다():
    assert weather_service.make_pick_lead("CLEAR", 30) == PICK_LEADS_HOT
    assert "시원하고 담백한" in weather_service.make_comment("CLEAR", 30)


def test_냉장고_재료가_많이_겹치는_메뉴가_먼저_나온다():
    items = ai_api.catalog_recommend_foods(
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
    recipe = ai_api.catalog_build_recipe("김치찌개", {})

    assert recipe["recipe_name"]
    assert recipe["servings"] == 2
    assert len(recipe["steps"]) > 0
    assert len(recipe["ingredients"]) > 0

    first = recipe["ingredients"][0]
    # grams 는 가격 환산에 쓴다. 없으면 "고춧가루 1큰술" 에 1kg 값이 붙는다.
    assert set(first.keys()) == {"name", "quantity", "unit", "grams"}
    assert first["grams"] > 0


def test_알레르기_재료는_레시피에서_아예_빠진다():
    recipe = ai_api.catalog_build_recipe("김치찌개", {"allergies": ["돼지고기"]})

    names = []
    for item in recipe["ingredients"]:
        names.append(item["name"])

    assert "돼지고기" not in names


def test_싫어하는_재료는_두되_대체안을_붙인다():
    recipe = ai_api.catalog_build_recipe("김치찌개", {"dislikes": ["돼지고기"]})

    names = []
    for item in recipe["ingredients"]:
        names.append(item["name"])

    assert "돼지고기" in names
    assert recipe["substitutes"][0]["original"] == "돼지고기"
    assert recipe["substitutes"][0]["substitute"]


# =========================================================
# 레시피 — 미리 써둔 100종 (app/data/recipes.json)
# =========================================================


def test_카탈로그_100종은_모두_레시피가_있다():
    from app.data import FOODS

    for food in FOODS:
        assert recipe_book.has_recipe(food["food_name"]), food["food_name"]


def test_미리_써둔_레시피가_스키마와_맞는다():
    recipe = recipe_book.build_recipe("김치찌개", {})

    assert recipe["recipe_name"]
    assert recipe["servings"] == 2
    assert len(recipe["steps"]) >= 10
    assert len(recipe["ingredients"]) > 0

    first = recipe["ingredients"][0]
    # grams 는 가격 환산에 쓴다. 파일에 안 적고 portion.py 표에서 채운다.
    assert set(first.keys()) == {"name", "quantity", "unit", "grams"}
    assert first["grams"] > 0


def test_미리_써둔_레시피도_알레르기_재료를_뺀다():
    recipe = recipe_book.build_recipe("김치찌개", {"allergies": ["돼지고기"]})

    names = [item["name"] for item in recipe["ingredients"]]
    assert "돼지고기" not in names

    # 순서는 사람이 쓴 글이라 다시 만들 수 없다. 그래서 지우지 않고
    # 맨 앞에 뺐다고 알린다 — 안 알리면 "돼지고기를 볶는다" 가 그냥 남는다.
    assert "돼지고기" in recipe["steps"][0]


def test_미리_써둔_레시피도_싫어하는_재료에_대체안을_붙인다():
    recipe = recipe_book.build_recipe("김치찌개", {"dislikes": ["돼지고기"]})

    names = [item["name"] for item in recipe["ingredients"]]
    assert "돼지고기" in names
    assert recipe["substitutes"][0]["original"] == "돼지고기"


def test_카탈로그에_없는_음식은_레시피가_없다():
    assert recipe_book.build_recipe("할머니표 비밀 국수", {}) is None


def test_카탈로그에_없는_음식은_404다():
    # 지어내지 않는다. 빈 화면보다 틀린 레시피가 나쁘다 —
    # 요리는 따라 만드는 글이라 사실이 아닌 순서는 재료를 실제로 버리게 한다.
    original = settings.AI_PROVIDER
    settings.AI_PROVIDER = "catalog"
    try:
        with pytest.raises(HTTPException) as caught:
            ai_service.make_recipe(None, None, "할머니표 비밀 국수")
    finally:
        settings.AI_PROVIDER = original

    assert caught.value.status_code == 404
    assert "할머니표 비밀 국수" in caught.value.detail


def test_AI_를_쓰면_카탈로그_밖도_물어본다():
    # catalog 가 아닐 때는 404 로 끊지 않고 AI 에게 넘긴다.
    # 여기서는 넘어가는지만 본다(키가 없으므로 503 으로 끝난다).
    original = settings.AI_PROVIDER
    settings.AI_PROVIDER = "gemini"
    try:
        with pytest.raises(HTTPException) as caught:
            ai_service.make_recipe(None, None, "할머니표 비밀 국수")
    finally:
        settings.AI_PROVIDER = original

    assert caught.value.status_code != 404


def test_AI_폴백용_레시피_생성기는_그대로_있다():
    # catalog 모드에서는 더 이상 불리지 않지만, gemini · claude 로 바꿨을 때
    # 쓰던 코드라 그대로 둔다.
    recipe = ai_api.catalog_build_recipe("할머니표 비밀 국수", {})

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


# =========================================================
# 레시피 캐시
#
# 같은 음식의 레시피는 달라질 이유가 없는데 AI 를 부르는 데 7~10초가 걸린다
# (모델이 붐비면 30초를 넘기도 한다). 두 번째부터는 기억해둔 걸 준다.
# =========================================================


def make_cached_recipe(name="김치찌개"):
    from app.schemas.recommend import RecipeResponse

    return RecipeResponse(
        food_name=name,
        recipe_name=f"{name} 레시피",
        recipe_json={"steps": ["끓인다"]},
        ingredients_json=[{"name": "대파", "quantity": 1, "unit": "대", "grams": 100}],
        substitutes_json=[],
        estimated_cost=5000,
    )


def test_레시피를_기억했다가_다시_준다():
    ai_service.recipe_cache.clear()

    key = ai_service.recipe_cache_key("김치찌개", [], [])
    ai_service.write_recipe_cache(key, make_cached_recipe())

    found = ai_service.read_recipe_cache(key)

    assert found is not None
    assert found.recipe_name == "김치찌개 레시피"


def test_가격은_캐시에_남지_않는다():
    # 가격은 조회 시점의 값이다. 6시간 전 값을 오늘 값으로 보여주면 안 된다.
    from app.schemas.recommend import PriceItem

    ai_service.recipe_cache.clear()

    recipe = make_cached_recipe()
    recipe.prices = [PriceItem(name="대파 1대")]

    key = ai_service.recipe_cache_key("김치찌개", [], [])
    ai_service.write_recipe_cache(key, recipe)

    assert ai_service.read_recipe_cache(key).prices == []


def test_캐시를_고쳐도_원본은_그대로다():
    # 부르는 쪽이 prices 를 채워 넣는다. 원본을 그대로 주면
    # 다음 사람이 남의 조회 시점 가격을 보게 된다.
    from app.schemas.recommend import PriceItem

    ai_service.recipe_cache.clear()

    key = ai_service.recipe_cache_key("김치찌개", [], [])
    ai_service.write_recipe_cache(key, make_cached_recipe())

    first = ai_service.read_recipe_cache(key)
    first.prices = [PriceItem(name="대파 1대")]

    assert ai_service.read_recipe_cache(key).prices == []


def test_못먹는_재료가_다르면_다른_레시피다():
    # 같은 김치찌개라도 돼지고기 알레르기가 있는 사람에게는 다른 레시피가 나간다.
    # 이름만으로 묶으면 남의 알레르기 재료가 섞인 레시피를 보여주게 된다.
    plain = ai_service.recipe_cache_key("김치찌개", [], [])
    allergic = ai_service.recipe_cache_key("김치찌개", ["돼지고기"], [])

    assert plain != allergic


def test_못먹는_재료의_순서는_상관없다():
    one = ai_service.recipe_cache_key("김치찌개", ["돼지고기", "계란"], [])
    two = ai_service.recipe_cache_key("김치찌개", ["계란", "돼지고기"], [])

    assert one == two


def test_오래된_캐시는_쓰지_않는다():
    ai_service.recipe_cache.clear()

    key = ai_service.recipe_cache_key("김치찌개", [], [])
    ai_service.write_recipe_cache(key, make_cached_recipe())

    # 저장 시각을 유효기간 밖으로 되돌린다
    _saved_at, recipe = ai_service.recipe_cache[key]
    old = time.time() - ai_service.RECIPE_CACHE_SECONDS - 1
    ai_service.recipe_cache[key] = (old, recipe)

    assert ai_service.read_recipe_cache(key) is None


def test_캐시가_너무_커지면_비운다():
    ai_service.recipe_cache.clear()

    for i in range(ai_service.RECIPE_CACHE_MAX_ENTRIES + 2):
        key = ai_service.recipe_cache_key(f"음식{i}", [], [])
        ai_service.write_recipe_cache(key, make_cached_recipe(f"음식{i}"))

    assert len(ai_service.recipe_cache) <= ai_service.RECIPE_CACHE_MAX_ENTRIES + 1


# =========================================================
# 조사가격 캐시
#
# KAMIS 조사값은 하루 한 번만 바뀐다(같은 시장·같은 날의 값은 하나뿐).
# 그래서 캐시가 오차를 늘리지 않는다 — 다시 물어도 같은 답이 온다.
# =========================================================


def sample_offers():
    return [
        {"shop": "서울 경동", "price": 1300, "unit": "1kg",
         "link": "", "surveyed_at": "20260908"},
    ]


def test_조사가격을_기억했다가_다시_준다():
    price_api.kamis_cache.clear()

    key = ("200", "245", None)
    price_api.write_kamis_cache(key, sample_offers(), None)

    found = price_api.read_kamis_cache(key)

    assert found is not None
    offers, _trend = found
    assert offers[0]["price"] == 1300


def test_캐시를_고쳐도_원본은_그대로다():
    # 부르는 쪽이 링크를 갈아끼운다. 원본을 그대로 주면 캐시가 망가진다.
    price_api.kamis_cache.clear()

    key = ("200", "245", None)
    price_api.write_kamis_cache(key, sample_offers(), None)

    first, _trend = price_api.read_kamis_cache(key)
    first[0]["price"] = 99999

    assert price_api.read_kamis_cache(key)[0][0]["price"] == 1300


def test_오래된_캐시는_쓰지_않는다():
    price_api.kamis_cache.clear()

    key = ("200", "245", None)
    price_api.write_kamis_cache(key, sample_offers(), None)

    _saved_at, offers, trend = price_api.kamis_cache[key]
    old = time.time() - price_api.KAMIS_CACHE_SECONDS - 1
    price_api.kamis_cache[key] = (old, offers, trend)

    assert price_api.read_kamis_cache(key) is None


def test_빈_결과도_기억한다():
    # KAMIS 에 없는 품목을 3초마다 다시 물어볼 이유가 없다.
    # 없다는 사실도 다음 조사까지 바뀌지 않는다.
    price_api.kamis_cache.clear()

    key = ("999", "999", None)
    price_api.write_kamis_cache(key, [], None)

    assert price_api.read_kamis_cache(key) == ([], None)


def test_캐시가_너무_커지면_비운다():
    price_api.kamis_cache.clear()

    for i in range(price_api.KAMIS_CACHE_MAX_ENTRIES + 2):
        price_api.write_kamis_cache(("200", str(i), None), sample_offers(), None)

    assert len(price_api.kamis_cache) <= price_api.KAMIS_CACHE_MAX_ENTRIES + 1


def test_링크는_부른_재료명으로_붙는다():
    # 품목이 같아도 확인 링크는 사용자가 본 이름으로 가야 한다.
    # "다진 마늘" 을 눌렀는데 "마늘" 로 검색되면 어색하다.
    offers = price_api.relabel(sample_offers(), "다진 마늘")

    assert "%EB%8B%A4%EC%A7%84" in offers[0]["link"]  # '다진'


# =========================================================
# 가격 추이
#
# 일주일치를 이미 받아놓고 최근 하루만 쓰고 버리고 있었다.
# 추이는 그 버려지던 데이터로 만든다 — 추가 호출이 없다.
#
# 쇼핑몰 API 로는 못 하는 일이다. 그쪽은 지금 값만 준다.
# =========================================================


def trend_row(day, price):
    return {"exmn_ymd": day, "exmn_dd_prc": str(price)}


def test_조사일자별로_평균을_낸다():
    # 같은 날에도 시장마다 값이 다르다. 날짜끼리 비교하려면 하루를 하나로 줄인다.
    rows = [
        trend_row("20260901", 1000),
        trend_row("20260901", 2000),
        trend_row("20260908", 1500),
    ]

    assert price_api.daily_average(rows) == {"20260901": 1500, "20260908": 1500}


def test_값이_내리면_음수로_알려준다():
    rows = [trend_row("20260901", 2000), trend_row("20260908", 1700)]

    trend = price_api.price_trend(rows)

    assert trend is not None
    assert trend["percent"] == -15.0
    assert trend["from_price"] == 2000
    assert trend["to_price"] == 1700
    assert trend["to_day"] == "20260908"


def test_값이_오르면_양수로_알려준다():
    rows = [trend_row("20260901", 1000), trend_row("20260908", 1200)]

    assert price_api.price_trend(rows)["percent"] == 20.0


def test_조금_움직인_건_알리지_않는다():
    # 시장 조사가격은 며칠 사이에도 1~2%씩 흔들린다. 그걸 다 알리면
    # 배지가 늘 떠 있어서 아무 뜻이 없어진다.
    rows = [trend_row("20260901", 1000), trend_row("20260908", 1020)]

    assert price_api.price_trend(rows) is None


def test_조사일이_하루뿐이면_추이가_없다():
    assert price_api.price_trend([trend_row("20260908", 1000)]) is None
    assert price_api.price_trend([]) is None


def test_날짜_순서가_뒤섞여_와도_처음과_끝을_찾는다():
    rows = [
        trend_row("20260908", 1700),
        trend_row("20260901", 2000),
        trend_row("20260903", 1900),
    ]

    trend = price_api.price_trend(rows)

    assert trend["from_day"] == "20260901"
    assert trend["to_day"] == "20260908"


# =========================================================
# 요즘 싼 재료 고르기
#
# 쇼핑몰 API 로는 못 하는 일이다. 그쪽은 "지금 얼마" 만 준다.
# KAMIS 가 일별 조사값이라 "요즘 싼지" 를 말할 수 있고, 그 목록을
# AI 에게 넘겨 "싼 재료로 만드는 메뉴" 를 받는다.
# =========================================================


def bargain_row(name, percent):
    return {"name": name, "percent": percent, "price": 1000, "unit": "1kg"}


def test_많이_내린_재료만_고른다():
    rows = [
        bargain_row("고구마", -17.0),
        bargain_row("대파", -12.0),
        bargain_row("양파", -2.0),    # 조금 내렸다 — 장 볼 때 체감 안 된다
        bargain_row("배추", 13.5),    # 올랐다
    ]

    picked = bargain_service.pick_cheap(rows, limit=10)

    assert [row["name"] for row in picked] == ["고구마", "대파"]


def test_개수를_넘겨서_주지_않는다():
    # 너무 많으면 AI 가 아무거나 골라 오고, 화면도 읽히지 않는다
    rows = [bargain_row(f"재료{i}", -10.0) for i in range(20)]

    assert len(bargain_service.pick_cheap(rows, limit=8)) == 8


def test_같은_품목을_두_번_묻지_않는다():
    # 품목표에는 별칭이 함께 있다("소고기"와 "쇠고기"가 같은 코드).
    # 그대로 훑으면 같은 품목을 두 번 물어보게 된다.
    names = bargain_service.unique_items()

    assert len(names) < len(ingredient_match.KAMIS_ITEM_CODES)
    assert len(names) == len(set(names))

    codes = [ingredient_match.KAMIS_ITEM_CODES[name] for name in names]
    assert len(codes) == len(set(codes))


def test_싼_재료가_없으면_빈_목록이다():
    # 시세가 잔잔한 날. 화면은 이때 카드를 아예 띄우지 않는다.
    rows = [bargain_row("양파", -1.0), bargain_row("배추", 3.0)]

    assert bargain_service.pick_cheap(rows, limit=8) == []


# =========================================================
# 후기 첨부파일
#
# 사용자가 올린 파일명을 그대로 믿지 않는다는 것을 확인한다.
# 이름이 겹치면 서로 덮어쓰고, 경로가 섞여 있으면 서버 파일을 건드린다.
# =========================================================


def test_허용한_확장자만_받는다():
    assert file_service.check_ext("사진.JPG") == "jpg"
    assert file_service.check_ext("a.png") == "png"


def test_실행파일은_거부한다():
    # 금지 목록이 아니라 허용 목록이라, 목록에 없는 건 전부 막힌다
    for name in ("악성.exe", "스크립트.sh", "노트.txt", "확장자없음"):
        with pytest.raises(HTTPException) as caught:
            file_service.check_ext(name)

        assert caught.value.status_code == 422


def test_이름_없는_파일은_거부한다():
    for name in (None, "", "   "):
        with pytest.raises(HTTPException):
            file_service.check_ext(name)


def test_원본_파일명에서_경로를_떼어낸다():
    # "../../.env" 를 그대로 두면 내려받기 이름에 경로가 섞인다
    assert file_service.safe_org_name("../../.env") == ".env"
    assert file_service.safe_org_name("C:\비밀\키.jpg") == "키.jpg"
    assert file_service.safe_org_name("김치찌개.jpg") == "김치찌개.jpg"


def test_원본_파일명은_255자를_넘지_않는다():
    # FILE_ORG_NAME 이 VARCHAR2(255 CHAR) 다. 넘으면 ORA-12899 가 난다.
    long_name = "가" * 400 + ".jpg"

    assert len(file_service.safe_org_name(long_name)) == 255


def test_저장_폴더는_월별로_나뉜다():
    today = date.today()

    assert file_service.month_path() == (
        f"{settings.UPLOAD_DIR}/{today.year:04d}/{today.month:02d}"
    )


def test_업로드_폴더_밖으로_나가면_막는다():
    # 지금은 경로도 파일명도 서버가 지어내지만, DB 값을 그대로 믿고
    # 파일을 열어주는 코드는 언젠가 사고가 난다
    with pytest.raises(HTTPException) as caught:
        file_service.absolute_path("uploads/2026/09", "../../../.env")

    assert caught.value.status_code == 404


def test_정상_경로는_업로드_폴더_안을_가리킨다():
    target = file_service.absolute_path(file_service.month_path(), "a1b2.jpg")

    assert settings.upload_root.resolve() in target.parents


def test_파일_크기를_사람이_읽는_말로_바꾼다():
    assert file_service.human_size(512) == "512 B"
    assert file_service.human_size(188_412) == "184 KB"
    assert file_service.human_size(3 * 1024 * 1024) == "3.0 MB"

    # 크기를 못 구한 파일도 정상이다 (DDL 이 file_size 를 필수에서 뺐다)
    assert file_service.human_size(None) == ""


# =========================================================
# 미니게임 종류
#
# 화면의 게임 키를 DB 글자(A·B·C)로 바꾸는 표가 전부다.
# 이 표와 DDL 의 ck_game_histories_type 이 어긋나면 INSERT 가 ORA-02290 으로
# 막히는데, 그건 게임을 눌러봐야 알게 된다. 여기서 먼저 잡는다.
# =========================================================


def test_게임_세_개가_각각_다른_글자를_받는다():
    types = game_service.GAME_TYPES

    assert types["worldcup"] == "A"
    assert types["balance"] == "B"
    assert types["roulette"] == "C"

    # 두 게임이 같은 글자를 쓰면 통계가 섞인다
    assert len(set(types.values())) == len(types)


def test_게임_키가_카탈로그와_같다():
    # 화면이 catalog.json 의 games[].key 를 그대로 보내온다.
    # 한쪽 이름만 바꾸면 기록이 통째로 비는데, 눈치채기가 매우 어렵다.
    from app.data import GAMES

    catalog_keys = {game["key"] for game in GAMES}

    assert catalog_keys == set(game_service.GAME_TYPES)


def test_모르는_게임은_404다():
    # 조용히 넘기면 오타 하나로 기록이 안 쌓이는 걸 나중에 알게 된다
    with pytest.raises(HTTPException) as caught:
        game_service.to_game_type("worldcupp")

    assert caught.value.status_code == 404


def test_게임_글자는_한_글자다():
    # DDL 이 CHAR(1) 이다. 두 글자를 넣으면 ORA-12899 가 난다.
    for game_type in game_service.GAME_TYPES.values():
        assert len(game_type) == 1


# ==========================================================
# 조사 대상이 아닌 재료 (KAMIS 는 농수산물만 조사한다)
# ==========================================================

def test_가공식품은_원물_시세를_빌려_쓰지_않는다():
    # 부분일치가 '고추장' 안에서 '고추' 를 찾아 고추 시세를 붙이고 있었다.
    # 값이 틀리는 것도 문제지만, 상세페이지가 "이건 조사 대상이다" 라고
    # 말하게 되는 게 더 나쁘다.
    from app.integrations.ingredient_match import find_codes

    assert find_codes("고추장") is None
    assert find_codes("초고추장") is None
    assert find_codes("배추김치") is None
    assert find_codes("깨소금") is None

    # 원물은 그대로 찾아져야 한다. 가공식품을 막다가 이걸 같이 막으면
    # 재료비가 통째로 사라진다.
    assert find_codes("고추") is not None
    assert find_codes("배추") is not None
    assert find_codes("소금") is not None


def test_멸치액젓은_멸치가_아니라_액젓이다():
    # 둘 다 두 글자라 길이로는 못 가린다. 별칭으로 못박아 둔 것을 지키는지 본다.
    from app.integrations.ingredient_match import find_codes

    assert find_codes("멸치액젓") == find_codes("액젓")
    assert find_codes("멸치액젓") != find_codes("멸치")


def test_카탈로그_재료는_모두_조사_대상이다():
    # 100종의 재료가 전부 시세를 받아야 예상비용이 맞는다.
    # 재료를 새로 추가했는데 KAMIS 품목명과 다르면 여기서 걸린다.
    from app.data import FOODS
    from app.integrations.ingredient_match import find_codes

    names = {name for food in FOODS for name in food["ingredients"]}
    missing = sorted(name for name in names if find_codes(name) is None)

    assert missing == []


# ==========================================================
# 조리 순서
# ==========================================================

def test_조리_순서에_재료_분량이_들어간다():
    # 전에는 "돼지고기를 넣고 7분간 끓인다" 가 한계였다. 얼마나 넣는지가
    # 안 적혀 있으면 레시피를 보고도 다시 찾아봐야 한다.
    recipe = ai_api.catalog_build_recipe("김치찌개", {})
    text = " ".join(recipe["steps"])

    for item in recipe["ingredients"]:
        assert f"{item['name']} {item['quantity']:g}{item['unit']}" in text


def test_재료마다_손질_단계가_따로_있다():
    # "재료를 먹기 좋은 크기로 썬다" 한 줄로 뭉뚱그리면 어느 재료를 어떻게
    # 다뤄야 하는지가 사라진다.
    recipe = ai_api.catalog_build_recipe("김치찌개", {})

    for item in recipe["ingredients"]:
        assert any(step.startswith(item["name"]) for step in recipe["steps"])


def test_향채는_조리_마지막에_들어간다():
    # 대파를 처음부터 끓이면 향이 다 날아간다. 순서 자체가 레시피의 내용이라
    # 손질 단계를 뺀 나머지에서 대파가 마지막 쪽에 있어야 한다.
    recipe = ai_api.catalog_build_recipe("김치찌개", {})
    steps = recipe["steps"]

    cooking = [i for i, step in enumerate(steps) if "끓인다" in step or "끓어오르면" in step]
    파 = next(i for i, step in enumerate(steps) if "대파" in step and "넣고" in step)

    assert 파 == max(cooking)


def test_알레르기로_뺀_재료는_조리_순서에도_안_나온다():
    # 재료 목록에서만 지우고 순서에 남겨두면 "없는 재료를 넣으라" 가 된다.
    recipe = ai_api.catalog_build_recipe("김치찌개", {"allergies": ["돼지고기"]})
    text = " ".join(recipe["steps"])

    assert "돼지고기" not in text


def test_칼국수와_비빔국수는_만드는_법이_다르다():
    # 둘 다 '면' 태그 하나로 묶여 있다. 같은 뼈대를 쓰면 칼국수를 찬물에
    # 헹궈 양념에 무치라는 글이 나온다.
    칼국수 = " ".join(ai_api.catalog_build_recipe("바지락칼국수", {})["steps"])
    비빔국수 = " ".join(ai_api.catalog_build_recipe("비빔국수", {})["steps"])

    assert "뜨거운 국물을 부어" in 칼국수
    assert "무친다" in 비빔국수
    assert "뜨거운 국물" not in 비빔국수


def test_육회는_익히지_않는다():
    # '무침' 태그를 그대로 따르면 절여서 무치라고 나오고, 고기를 볶는
    # 뼈대로 떨어지면 생고기를 익히라고 나온다.
    steps = " ".join(ai_api.catalog_build_recipe("육회", {})["steps"])

    assert "익히지 않는" in steps
    for 익힘 in ("볶는다", "끓인다", "굽는다", "튀긴다"):
        assert 익힘 not in steps


def test_비빔밥은_한_팬에_섞지_않는다():
    # 재료를 같이 볶아 얹으면 그건 덮밥이다.
    steps = " ".join(ai_api.catalog_build_recipe("전주비빔밥", {})["steps"])

    assert "비빈다" in steps
    assert "따로 볶아" in steps


def test_양념이_하나뿐이면_양념장이라_부르지_않는다():
    # "소금 1작은술을 계량해 둔다" 고 해놓고 "양념장을 풀고" 라고 하면,
    # 만들지도 않은 것을 넣으라는 말이 된다.
    recipe = ai_api.catalog_build_recipe("고등어구이", {})
    seasonings = [i["name"] for i in recipe["ingredients"] if i["name"] in steps_module.SEASONINGS]
    text = " ".join(recipe["steps"])

    assert len(seasonings) == 1
    assert "양념장" not in text


def test_로마자_단위_뒤에도_조사가_맞는다():
    # "300g" 은 "삼백그램" 이라 받침이 있고, "200ml" 는 "밀리리터" 라 없다.
    # 글자 코드만 보면 둘 다 받침 없음으로 떨어져 "300g를" 이 된다.
    assert steps_module.eul("돼지고기 300g") == "돼지고기 300g을"
    assert steps_module.eul("우유 200ml") == "우유 200ml를"
    assert steps_module.eun("돼지고기 300g") == "돼지고기 300g은"
    assert steps_module.eul("배추 0.25포기") == "배추 0.25포기를"
    assert steps_module.eul("갈치 2토막") == "갈치 2토막을"


def test_모든_요리가_충분히_자세한_순서를_갖는다():
    # 다섯 줄짜리 뼈대로 돌아가는 것을 막는다. 카탈로그에 요리를 추가했을 때
    # 뼈대가 없어 기본값으로 떨어지면 단계가 확 줄어든다.
    from app.data import FOODS

    for food in FOODS:
        steps = ai_api.catalog_build_recipe(food["food_name"], {})["steps"]

        assert len(steps) >= 7, f"{food['food_name']} 의 조리 순서가 {len(steps)}단계뿐입니다"
        assert all(step.endswith(".") for step in steps)
        # 서식 자리표시자가 그대로 남으면 화면에 중괄호가 보인다
        assert not any("{" in step for step in steps)
