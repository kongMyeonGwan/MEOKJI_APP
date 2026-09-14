"""재료 가격 비교 로직 (STEP 17~19).

integrations/price_api.py 가 "가격을 물어보는 일"을 하고,
이 파일은 "레시피의 재료 목록을 가격 조회에 맞는 모양으로 바꾸는 일"을 한다.

왜 이름을 두 번 다루는가
------------------------
화면에는 "돼지고기 앞다리살 200g" 처럼 분량까지 적어야 무엇을 사야 할지 알 수 있다.
그런데 가격 조회는 품목 단위라서 "돼지고기 앞다리살 200g" 으로는 아무것도 안 나온다.
그래서 조회는 재료 이름으로 하고, 화면에 붙이는 이름만 분량을 더해 만든다.
"""

from app.integrations.ingredient_match import find_codes
from app.integrations.price_api import collect_offers
from app.schemas.recommend import PriceItem
from app.services.unit_convert import buy_ratio, needed_ratio, parse_unit, scale_price


async def get_prices(ingredients: list[str]) -> list[PriceItem]:
    """재료 이름 목록의 시장별 조사가격을 가져온다."""
    rows = await collect_offers(ingredients)

    items = []
    for row in rows:
        items.append(PriceItem.model_validate(row))

    return items


async def get_recipe_prices(ingredients_json: list) -> list[PriceItem]:
    """레시피의 재료 목록(이름 · 분량 · 단위)으로 가격을 가져온다.

    조회는 재료 이름으로 하고, 화면에 보여줄 이름에는 분량을 붙인다.
    가격은 판매 단위 값과 함께 "필요한 만큼의 값"까지 계산해서 담는다.
    """
    names = []
    recipes = {}

    for item in ingredients_json:
        if not isinstance(item, dict):
            continue

        name = str(item.get("name") or "").strip()
        if not name:
            continue

        names.append(name)
        recipes[name] = item

    rows = await collect_offers(names)

    items = []
    for row in rows:
        item = recipes.get(row["name"], {})

        # 값이 없을 때 "조사 대상이 아니다" 와 "이번에 못 가져왔다" 를
        # 가려주기 위한 표시. 품목표만 보므로 API 가 죽어도 값이 흔들리지 않는다.
        row["surveyed"] = find_codes(row["name"]) is not None

        # 조회는 "돼지고기"로 했지만 화면에는 "돼지고기 200g"으로 보여준다
        row["name"] = make_label(row["name"], item)
        row.update(scale_offers(row.get("offers") or [], item))

        items.append(PriceItem.model_validate(row))

    return items


def scale_offers(offers: list[dict], item: dict) -> dict:
    """판매 단위 가격을 '이 레시피 때문에 내야 하는 돈' 으로 바꾼다.

    두 단계다.

        환산   판매 단위 하나를 1 로 볼 때 필요한 양이 몇 배인가
        올림   그 배율로 실제로 살 수 있는가. 못 사면 살 수 있는 데까지 올린다

    무 0.2개가 필요하면 환산은 0.2 지만, 무를 0.2개 파는 가게가 없으니
    실제로 내는 돈은 1개 값이다. 올림까지 해야 합계가 장바구니 금액이 된다.
    쪼개 파는 무게 단위는 올리지 않는다 (unit_convert.buy_ratio).

    offers 는 가격 오름차순이라 첫 번째가 가장 싼 시장, 마지막이 가장 비싼
    시장이다. 환산할 수 없으면 전부 None 으로 두고 화면이 합계에서 뺀다.
    """
    if not offers:
        return {}

    need_text = make_need_text(item)
    sale_unit = offers[0].get("unit") or ""

    ratio = needed_ratio(
        item.get("grams"),
        item.get("quantity"),
        item.get("unit"),
        sale_unit,
        # 세는 단위("1마리")는 품목마다 무게가 달라서 이름이 있어야 푼다
        item.get("name") or "",
    )
    if ratio is None:
        return {"need_text": need_text}

    buy = buy_ratio(ratio, sale_unit)

    # 필요한 양보다 더 사야 했는가. 화면이 이 값으로 '최소단위' 표시를
    # 붙인다. buy > ratio 일 때만 참이므로, 딱 떨어지는 재료
    # (고등어 1마리 · 판매 1마리)에는 표시가 붙지 않는다.
    min_unit = buy > ratio

    return {
        "need_text": need_text,
        "low_cost": scale_price(offers[0]["price"], buy),
        "high_cost": scale_price(offers[-1]["price"], buy),
        "min_unit": min_unit,
        "buy_text": make_buy_text(buy, sale_unit) if min_unit else None,
    }


def make_buy_text(buy: float, sale_unit: str) -> str | None:
    """실제로 사게 되는 양.  1.0 배 x "30구"  ->  "30구"

    판매 단위 문자열을 배율만큼 늘려서 다시 적는다. 화면에는 '0.2개 필요 ->
    1개 구입' 처럼 붙으므로, 여기서 단위를 빼먹으면 "1" 만 남아 뜻이 없다.

    올림이 일어난 재료(min_unit)에만 쓴다. 달아 파는 재료까지 적으면
    고춧가루 1큰술이 "0.007kg 구입" 이 되어 읽을 수 없는 말이 된다.
    """
    sale = parse_unit(sale_unit)
    if sale is None:
        return None

    size, token = sale

    return f"{format_quantity(round(size * buy, 3))}{token}"


def make_need_text(item: dict) -> str | None:
    """화면에 적을 필요량.  '1큰술', '0.5개'"""
    quantity = item.get("quantity")
    unit = item.get("unit")

    if quantity is None or not unit:
        return None

    return f"{format_quantity(quantity)}{unit}"


def make_label(name: str, item: dict) -> str:
    """화면에 보여줄 재료 이름을 만든다.  '두부' + 1모 -> '두부 1모'"""
    quantity = item.get("quantity")
    unit = item.get("unit")

    if quantity is None or not unit:
        return name

    return f"{name} {format_quantity(quantity)}{unit}"


def format_quantity(quantity) -> str:
    """0.5 는 "0.5", 2.0 은 "2" 로 적는다.

    float 를 그대로 문자열로 만들면 "2.0개" 가 되어 어색하다.
    """
    if isinstance(quantity, float) and quantity.is_integer():
        return str(int(quantity))

    return str(quantity)
