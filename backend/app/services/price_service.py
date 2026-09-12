"""재료 가격 비교 로직 (STEP 17~19).

integrations/price_api.py 가 "가격을 물어보는 일"을 하고,
이 파일은 "레시피의 재료 목록을 가격 조회에 맞는 모양으로 바꾸는 일"을 한다.

왜 이름을 두 번 다루는가
------------------------
화면에는 "돼지고기 앞다리살 200g" 처럼 분량까지 적어야 무엇을 사야 할지 알 수 있다.
그런데 가격 조회는 품목 단위라서 "돼지고기 앞다리살 200g" 으로는 아무것도 안 나온다.
그래서 조회는 재료 이름으로 하고, 화면에 붙이는 이름만 분량을 더해 만든다.
"""

from app.integrations.price_api import collect_offers
from app.schemas.recommend import PriceItem
from app.services.unit_convert import needed_ratio, scale_price


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

        # 조회는 "돼지고기"로 했지만 화면에는 "돼지고기 200g"으로 보여준다
        row["name"] = make_label(row["name"], item)
        row.update(scale_offers(row.get("offers") or [], item))

        items.append(PriceItem.model_validate(row))

    return items


def scale_offers(offers: list[dict], item: dict) -> dict:
    """판매 단위 가격을 레시피 필요량 기준으로 환산한다.

    offers 는 가격 오름차순이라 첫 번째가 가장 싼 시장, 마지막이 가장 비싼
    시장이다. 환산할 수 없으면 전부 None 으로 두고 화면이 합계에서 뺀다.
    """
    if not offers:
        return {}

    need_text = make_need_text(item)

    ratio = needed_ratio(
        item.get("grams"),
        item.get("quantity"),
        item.get("unit"),
        offers[0].get("unit"),
        # 세는 단위("1마리")는 품목마다 무게가 달라서 이름이 있어야 푼다
        item.get("name") or "",
    )
    if ratio is None:
        return {"need_text": need_text}

    return {
        "need_text": need_text,
        "low_cost": scale_price(offers[0]["price"], ratio),
        "high_cost": scale_price(offers[-1]["price"], ratio),
    }


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
