"""재료 가격 비교 로직 (STEP 17~19)."""

from app.integrations.ingredient_match import find_codes
from app.integrations.price_api import collect_offers
from app.schemas.recommend import PriceItem
from app.services.unit_convert import buy_ratio, needed_ratio, parse_unit, scale_price


async def get_recipe_prices(ingredients_json: list) -> list[PriceItem]:
    """레시피의 재료 목록(이름 · 분량 · 단위)으로 가격을 가져온다."""
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
    """판매 단위 가격을 '이 레시피 때문에 내야 하는 돈' 으로 바꾼다."""
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
    min_unit = buy > ratio

    return {
        "need_text": need_text,
        "low_cost": scale_price(offers[0]["price"], buy),
        "high_cost": scale_price(offers[-1]["price"], buy),
        "min_unit": min_unit,
        "buy_text": make_buy_text(buy, sale_unit) if min_unit else None,
    }


def make_buy_text(buy: float, sale_unit: str) -> str | None:
    """실제로 사게 되는 양."""
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
    """0.5 는 "0.5", 2.0 은 "2" 로 적는다."""
    if isinstance(quantity, float) and quantity.is_integer():
        return str(int(quantity))

    return str(quantity)
