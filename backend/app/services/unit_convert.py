"""레시피 필요량 ↔ KAMIS 판매 단위 환산 (STEP 17~19)."""

import math
import re

from app.integrations.portion import COUNT_GRAMS

# 무게·부피 단위를 그램으로 바꾸는 배수.
# 요리에서 물 1ml 는 1g 으로 봐도 무방하다.
GRAM_PER_UNIT = {
    "kg": 1000.0,
    "g": 1.0,
    "l": 1000.0,
    "L": 1000.0,
    "ml": 1.0,
    "mL": 1.0,
}

# 세는 단위를 같은 뜻끼리 묶는다.  (표준 이름, 표준 단위 몇 개인가)
COUNT_ALIASES = {
    "개": ("개", 1.0),
    "구": ("개", 1.0),
    "알": ("개", 1.0),
    "통": ("개", 1.0),
    "송이": ("개", 1.0),
    "마리": ("마리", 1.0),
    "손": ("마리", 2.0),   # 생선 1손 = 2마리
    "포기": ("포기", 1.0),
    "장": ("장", 1.0),
}


def parse_unit(text: str) -> tuple[float, str] | None:
    """KAMIS 단위 문자열을 (크기, 단위) 로 쪼갠다."""
    if not text:
        return None

    match = re.match(r"^\s*([\d.]*)\s*(.+?)\s*$", str(text))
    if not match:
        return None

    size, unit = match.group(1), match.group(2)
    if not unit:
        return None

    try:
        value = float(size) if size else 1.0
    except ValueError:
        return None

    if value <= 0:
        return None

    return value, unit


def to_grams(size: float, unit: str) -> float | None:
    """무게·부피 단위면 그램으로 바꾼다. 세는 단위면 None."""
    factor = GRAM_PER_UNIT.get(unit)
    if factor is None:
        return None

    return size * factor


def to_count(size: float, unit: str) -> tuple[str, float] | None:
    """세는 단위면 (표준 이름, 표준 단위 개수) 로 바꾼다."""
    alias = COUNT_ALIASES.get(unit)
    if alias is None:
        return None

    name, per = alias
    return name, size * per


def needed_ratio(
    need_grams, need_quantity, need_unit, sale_unit: str, name: str = ""
) -> float | None:
    """판매 단위 하나를 1 로 볼 때, 필요한 양이 몇 배인지 돌려준다."""
    sale = parse_unit(sale_unit)
    if sale is None:
        return None

    sale_size, sale_token = sale

    # 1) 판매 단위가 무게·부피면 grams 로 나눈다
    sale_grams = to_grams(sale_size, sale_token)
    if sale_grams is not None:
        grams = positive_number(need_grams)
        if grams is None:
            return None
        return grams / sale_grams

    # 2) 판매 단위가 세는 단위면 세는 단위끼리 맞춘다
    sale_count = to_count(sale_size, sale_token)
    if sale_count is None:
        return None

    need = parse_unit(f"{need_quantity}{need_unit}")
    need_count = to_count(*need) if need else None

    # 같은 것을 세는 단위끼리만 맞춘다. "개" 를 "마리" 로 나누면 안 된다
    if need_count is not None and need_count[0] == sale_count[0]:
        return need_count[1] / sale_count[1]

    # 3) 단위가 서로 다르면 무게로 돌아간다
    per = COUNT_GRAMS.get(name)
    if per is None:
        return None

    grams = positive_number(need_grams)
    if grams is None:
        return None

    # 판매 단위가 "10마리" 면 sale_count[1] 이 10 이다
    return grams / (per * sale_count[1])


def divisible(sale_unit: str) -> bool:
    """이 판매 단위를 쪼개서 살 수 있는가."""
    sale = parse_unit(sale_unit)
    if sale is None:
        return False

    return to_grams(*sale) is not None


def buy_ratio(ratio: float | None, sale_unit: str) -> float | None:
    """필요량 배율을 '실제로 사야 하는 배율' 로 올린다."""
    if ratio is None or ratio <= 0:
        return None

    if divisible(sale_unit):
        return ratio

    # round 를 먼저 거치는 이유: 2개/1개 가 float 로 2.0000000000000004 가
    # 되는 경우가 있다. 그대로 올리면 두 개 살 걸 세 개 사게 된다.
    return float(math.ceil(round(ratio, 6)))


def positive_number(value) -> float | None:
    """숫자로 바꾼다."""
    if value is None or isinstance(value, bool):
        return None

    try:
        number = float(value)
    except (TypeError, ValueError):
        return None

    if number <= 0:
        return None

    return number


def scale_price(price: int, ratio: float) -> int:
    """판매 단위 가격에 배율을 곱해 필요한 만큼의 값을 낸다."""
    return max(1, round(price * ratio))
