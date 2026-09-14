"""레시피 필요량 ↔ KAMIS 판매 단위 환산 (STEP 17~19).

왜 필요한가
-----------
KAMIS 는 시장 조사 단위로만 값을 준다. 고춧가루는 1kg 에 25,200원이고,
소금은 5kg 에 3,750원이다. 그런데 레시피가 요구하는 건 "고춧가루 1큰술",
"소금 0.5작은술" 이다.

환산하지 않고 조사 단위 가격을 그대로 더하면 이렇게 된다.

    고춧가루 1큰술(7g)    25,200원 더해짐  →  실제 176원   (143배)
    소금 0.5작은술(2g)     3,750원 더해짐  →  실제 2원   (2000배)
    ---------------------------------------------------------
    김치수제비 한 그릇     31,905원        →  실제 1,179원 (27배)

그래서 "필요한 만큼이 얼마인지" 를 계산해야 재료비 합계가 뜻을 갖는다.

두 가지 길로 환산한다
---------------------
KAMIS 단위는 두 종류다 (90개 품목 기준).

    무게·부피  1kg · 100g · 500g · 1L ...   59개
    개수       1개 · 10개 · 1마리 · 30구 ... 37개

무게·부피면 AI 가 준 grams 로 나눈다. 개수면 세는 단위끼리 맞춘다.
둘 다 안 되면 환산하지 않는다 — 억지로 맞춘 숫자를 합계에 넣는 것보다
"환산할 수 없다"고 두는 편이 낫다.

환산한 값이 곧 살 수 있는 값은 아니다 (STEP 42)
------------------------------------------------
환산까지만 하면 "무 0.2개 = 640원" 이 나온다. 계산은 맞지만 장바구니에는
못 담는다. 무를 0.2개 파는 가게가 없기 때문이다. 계란은 더 심하다 —
2개가 필요해도 KAMIS 는 30구 한 판으로 판다.

    무 0.2개      환산 640원    실제로는 1개 3,200원을 낸다
    계란 2개      환산 466원    실제로는 30구 7,000원을 낸다

그래서 환산 배율을 한 번 더 "살 수 있는 배율" 로 올린다. 다만 전부
올리지는 않는다. 판매 단위가 무게·부피면 저울로 달아 파니까 쪼갤 수 있다.

    무게·부피  1kg · 100g · 1L    쪼갤 수 있다   고춧가루 7g 은 7g 만 산다
    세는 단위  1개 · 30구 · 1마리  쪼갤 수 없다   올림해서 통째로 산다

여기서 무게 쪽까지 올려버리면 고춧가루 1큰술이 1kg(25,200원)이 되어,
애초에 이 파일이 없애려던 그 문제로 되돌아간다.
"""

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
#
# 계란은 KAMIS 가 "30구" 로 파는데 레시피는 "2개" 라고 적는다. 구와 개는
# 같은 것을 센다. 고등어 "1손" 은 두 마리를 묶어 파는 단위다.
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
    """KAMIS 단위 문자열을 (크기, 단위) 로 쪼갠다.

        "1kg"   -> (1.0, "kg")
        "100g"  -> (100.0, "g")
        "10개"  -> (10.0, "개")
        "30구"  -> (30.0, "구")

    앞에 숫자가 없으면 1 로 본다 ("kg" -> (1.0, "kg")).
    """
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
    """세는 단위면 (표준 이름, 표준 단위 개수) 로 바꾼다. 아니면 None.

        (1, "손")  -> ("마리", 2.0)
        (30, "구") -> ("개", 30.0)
    """
    alias = COUNT_ALIASES.get(unit)
    if alias is None:
        return None

    name, per = alias
    return name, size * per


def needed_ratio(
    need_grams, need_quantity, need_unit, sale_unit: str, name: str = ""
) -> float | None:
    """판매 단위 하나를 1 로 볼 때, 필요한 양이 몇 배인지 돌려준다.

        고춧가루 1큰술(15g) 이 필요하고 KAMIS 가 "1kg" 이면  ->  0.015
        애호박 0.5개 가 필요하고 KAMIS 가 "1개" 이면         ->  0.5
        계란 2개 가 필요하고 KAMIS 가 "30구" 이면            ->  0.0666
        고등어 1마리 가 필요하고 KAMIS 가 "1마리" 이면        ->  1.0

    환산할 수 없으면 None. 화면은 그때 가격을 합계에서 빼고
    "환산 불가" 로 표시한다.

    name 을 받는 이유
    -----------------
    세는 단위는 품목마다 무게가 다르다. "1마리" 가 고등어면 400g, 조기면
    150g 이다. 이름을 알아야 그 표(portion.COUNT_GRAMS)를 볼 수 있다.
    """
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
    #
    # 레시피는 "고등어 120g" 인데 KAMIS 는 "1마리" 로 파는 경우다. 전에는
    # 여기서 포기해서, 생선이 든 요리는 재료 넷 중 하나만 값이 붙고 합계가
    # 100원이 됐다. 한 마리가 몇 그램인지 적어두면 g 쪽으로 맞출 수 있다.
    per = COUNT_GRAMS.get(name)
    if per is None:
        return None

    grams = positive_number(need_grams)
    if grams is None:
        return None

    # 판매 단위가 "10마리" 면 sale_count[1] 이 10 이다
    return grams / (per * sale_count[1])


def divisible(sale_unit: str) -> bool:
    """이 판매 단위를 쪼개서 살 수 있는가.

    무게·부피는 저울로 달아 판다. "1kg 25,200원" 이라고 적혀 있어도
    200g 만 달라고 할 수 있다.

    세는 단위는 쪼갤 수 없다. 무 0.2개, 계란 2/30판, 고등어 0.3마리는
    세상에 없는 물건이다.

    단위를 못 읽으면 쪼갤 수 없는 쪽으로 본다. 모르는 단위를 쪼갤 수 있다고
    보면 살 수 없는 금액을 합계에 넣게 된다 — 틀리는 방향이 나쁘다.
    """
    sale = parse_unit(sale_unit)
    if sale is None:
        return False

    return to_grams(*sale) is not None


def buy_ratio(ratio: float | None, sale_unit: str) -> float | None:
    """필요량 배율을 '실제로 사야 하는 배율' 로 올린다.

        무 0.2개 · 판매 "1개"        -> 1.0   한 개를 통째로 산다
        계란 2개 · 판매 "30구"       -> 1.0   한 판을 산다
        배추 1.2포기 · 판매 "1포기"   -> 2.0   두 포기를 산다
        고등어 120g · 판매 "1마리"    -> 1.0   한 마리를 산다
        돼지고기 240g · 판매 "100g"   -> 2.4   달아서 파니까 그대로

    쪼갤 수 있는 단위면 needed_ratio 를 그대로 돌려준다. 그래서 이 함수를
    거쳐도 고춧가루 1큰술은 176원 그대로다.

    환산이 안 된 재료(ratio=None)는 여기서도 None 이다. 살 수 있는 양을
    모르는데 "한 개는 사야 한다" 고 말할 수는 없다.
    """
    if ratio is None or ratio <= 0:
        return None

    if divisible(sale_unit):
        return ratio

    # round 를 먼저 거치는 이유: 2개/1개 가 float 로 2.0000000000000004 가
    # 되는 경우가 있다. 그대로 올리면 두 개 살 걸 세 개 사게 된다.
    return float(math.ceil(round(ratio, 6)))


def positive_number(value) -> float | None:
    """숫자로 바꾼다. 숫자가 아니거나 0 이하면 None.

    grams 가 0 이면 나눗셈이 0 원을 만든다. 물처럼 값을 안 매기는 재료가
    아니라 AI 가 값을 빠뜨린 경우일 수 있어서, 0 은 '모름' 으로 본다.
    """
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
    """판매 단위 가격에 배율을 곱해 필요한 만큼의 값을 낸다.

    10원 미만으로 떨어지는 재료(소금 2원)가 있어서 반올림만 하고
    100원 단위로 자르지 않는다. 0 원으로 만들면 합계에서 사라진다.
    """
    return max(1, round(price * ratio))
