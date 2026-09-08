"""재료 가격 비교 로직 (STEP 17~19).

integrations/price_api.py 가 "쇼핑몰에 물어보는 일"을 하고,
이 파일은 "레시피의 재료 목록을 가격 조회에 맞는 모양으로 바꾸는 일"을 한다.

왜 이름을 두 번 다루는가
------------------------
화면에는 "돼지고기 앞다리살 200g" 처럼 분량까지 적어야 무엇을 사야 할지 알 수 있다.
그런데 쇼핑몰에 "돼지고기 앞다리살 200g" 으로 검색하면 결과가 거의 안 나온다.
그래서 조회는 재료 이름으로 하고, 화면에 붙이는 이름만 분량을 더해 만든다.
"""

from app.integrations.price_api import collect_offers
from app.schemas.recommend import PriceItem


async def get_prices(ingredients: list[str]) -> list[PriceItem]:
    """재료 이름 목록의 판매처별 가격을 가져온다."""
    rows = await collect_offers(ingredients)

    items = []
    for row in rows:
        items.append(PriceItem.model_validate(row))

    return items


async def get_recipe_prices(ingredients_json: list) -> list[PriceItem]:
    """레시피의 재료 목록(이름 · 분량 · 단위)으로 가격을 가져온다.

    조회는 재료 이름으로 하고, 화면에 보여줄 이름에는 분량을 붙인다.
    """
    names = []
    labels = {}

    for item in ingredients_json:
        if not isinstance(item, dict):
            continue

        name = str(item.get("name") or "").strip()
        if not name:
            continue

        names.append(name)
        labels[name] = make_label(name, item)

    rows = await collect_offers(names)

    items = []
    for row in rows:
        # 조회는 "돼지고기"로 했지만 화면에는 "돼지고기 200g"으로 보여준다
        row["name"] = labels.get(row["name"], row["name"])
        items.append(PriceItem.model_validate(row))

    return items


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
