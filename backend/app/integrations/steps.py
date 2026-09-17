"""조리 순서를 만든다."""

# 재료별 손질법
PREP = {
    # --- 고기 ---
    "돼지고기": "한 입 크기(3cm)로 썰고 키친타월로 핏물을 닦는다. 물기가 남으면 볶을 때 기름이 튀고 겉이 잘 익지 않는다",
    "삼겹살": "0.5cm 두께로 썬다. 너무 얇게 썰면 볶는 사이 기름만 빠지고 살이 줄어든다",
    "목살": "1cm 두께로 썰어 칼등으로 가볍게 두드린다. 질긴 결이 끊어져 부드러워진다",
    "돼지갈비": "찬물에 30분간 담가 핏물을 뺀다. 중간에 물을 한 번 갈아준다",
    "소고기": "결 반대 방향으로 썬다. 결을 따라 썰면 익은 뒤 질겨진다",
    "소고기안심": "1.5cm 두께로 썰어 상온에 15분간 둔다. 차가운 채로 구우면 겉만 타고 속이 안 익는다",
    "소고기등심": "1.5cm 두께로 썰어 상온에 15분간 두고, 가장자리 힘줄에 칼집을 넣는다",
    "소고기설도": "기름기를 떼고 결 반대로 얇게 썬다. 기름이 적은 부위라 오래 익히면 퍽퍽해진다",
    "소갈비": "찬물에 1시간 담가 핏물을 빼고, 뼈 사이 기름덩이를 잘라낸다",
    "닭": "토막 내어 흐르는 물에 씻고, 껍질 안쪽 노란 기름덩이를 떼어낸다. 이 기름이 국물 누린내의 원인이다",
    "닭고기": "한 입 크기로 썰어 우유나 청주에 10분간 담갔다 건진다",

    # --- 생선 ---
    "고등어": "내장을 빼고 흐르는 물에 씻은 뒤, 등쪽에 칼집을 두세 번 넣는다. 칼집이 있어야 속까지 익고 양념도 밴다",
    "갈치": "지느러미를 가위로 자르고 은빛 비늘을 칼등으로 살살 긁어낸다. 비늘을 남기면 비린맛이 돈다",
    "조기": "비늘을 긁고 내장을 뺀 뒤 굵은소금을 살짝 뿌려 10분간 둔다",
    "굴비": "쌀뜨물에 20분간 담가 짠기를 뺀다",
    "명태": "먹기 좋은 크기로 토막 내고, 뱃속 검은 막을 숟가락으로 긁어낸다. 이 막이 쓴맛을 낸다",
    "삼치": "토막 내어 소금을 뿌리고 15분간 둔 뒤 물기를 닦는다",
    "꽁치": "머리와 내장을 떼고 씻어 물기를 뺀다",

    # --- 해물 ---
    "오징어": "몸통을 갈라 내장과 뼈를 빼고 껍질을 벗긴 뒤, 안쪽에 사선으로 칼집을 넣는다. 칼집대로 말려 모양이 산다",
    "새우": "머리를 떼고 등쪽 두 번째 마디에 이쑤시개를 넣어 내장을 빼낸다",
    "전복": "솔로 껍데기를 문질러 씻고, 숟가락으로 살을 떼어 이빨을 잘라낸다",
    "꽃게": "배딱지를 떼고 몸통을 갈라 아가미를 떼어낸 뒤 반으로 자른다",
    "바지락": "소금물(물 1L + 소금 1큰술)에 담가 어두운 곳에서 30분간 해감한다. 이걸 건너뛰면 국물에 모래가 씹힌다",
    "홍합": "껍데기에 붙은 수염을 잡아 뽑고 겉을 문질러 씻는다",
    "멸치": "머리와 내장을 떼어낸다. 내장을 두면 국물이 쓰고 텁텁해진다",
    "미역": "찬물에 15분간 불린 뒤 바락바락 주물러 씻고 먹기 좋게 자른다. 불리면 대여섯 배로 늘어나니 그릇을 넉넉히 쓴다",

    # --- 김치거리 · 잎채소 ---
    "배추": "겉잎을 떼고 심을 도려낸 뒤 3~4cm 폭으로 썬다",
    "알배기배추": "밑동을 자르고 길이로 반 갈라 4cm 폭으로 썬다",
    "얼갈이배추": "밑동을 자르고 4cm 길이로 썰어 흐르는 물에 두세 번 헹군다",
    "양배추": "심을 도려내고 1cm 폭으로 채 썬다",
    "열무": "밑동을 자르고 4cm 길이로 썬다. 억센 겉잎은 골라낸다",
    "시금치": "뿌리 쪽 흙을 칼끝으로 긁어내고 큰 것은 반 가른다. 끓는 물에 30초만 데쳐 찬물에 헹군 뒤 물기를 짠다",
    "상추": "한 장씩 떼어 씻고 물기를 완전히 턴다. 물기가 남으면 양념이 겉돈다",
    "부추": "5cm 길이로 썬다. 숨이 금방 죽으니 쓰기 직전에 썬다",
    "미나리": "굵은 줄기의 잎을 훑어내고 5cm 길이로 썬다",
    "깻잎": "꼭지를 떼고 씻어 물기를 턴 뒤 돌돌 말아 채 썬다",

    # --- 뿌리 · 열매 채소 ---
    "무": "껍질을 벗기고 1cm 두께로 나박 썬다. 얇게 썰면 익는 사이 뭉그러지므로 도톰하게 썬다",
    "양파": "반으로 갈라 결을 따라 굵게 채 썬다. 결 반대로 썰면 볶는 사이 다 풀어진다",
    "대파": "어슷 썬다. 흰 부분은 볶거나 끓일 때 쓰고, 푸른 부분은 마지막에 넣으면 향이 산다",
    "감자": "껍질을 벗기고 한 입 크기로 썬다. 썰어두면 금방 갈변하니 쓰기 직전까지 찬물에 담가둔다",
    "고구마": "껍질을 벗기고 1.5cm 두께로 썰어 찬물에 담가둔다",
    "당근": "껍질을 벗기고 0.5cm 두께로 썬다. 단단해서 다른 채소보다 먼저 넣어야 한다",
    "애호박": "반달 모양으로 0.7cm 두께로 썬다",
    "가지": "길이로 반 갈라 어슷 썬다. 기름을 많이 먹으니 볶을 때는 센 불에 빠르게 한다",
    "오이": "굵은소금으로 겉을 문질러 씻고 어슷 썬다",
    "파프리카": "꼭지와 씨를 빼고 1cm 폭으로 썬다",
    "피망": "꼭지와 씨를 빼고 링 모양으로 썬다",
    "고추": "어슷 썰고, 매운 것이 싫으면 씨를 털어낸다",
    "붉은고추": "어슷 썬다. 색을 내는 용도라 마지막에 넣는다",
    "꽈리고추": "꼭지를 떼고 포크로 두세 군데 찔러둔다. 안 찌르면 익으면서 터진다",
    "배": "껍질을 벗기고 강판에 간다. 고기를 연하게 하고 단맛을 낸다",
    "레몬": "굵은소금으로 겉을 문질러 씻고 반 갈라 즙을 짠다. 씨는 걸러낸다",

    # --- 버섯 ---
    "새송이버섯": "씻지 말고 젖은 행주로 닦은 뒤 길이로 도톰하게 썬다. 물에 담그면 향이 빠진다",
    "팽이버섯": "밑동을 잘라내고 손으로 가닥가닥 뗀다",

    # --- 곡물 · 콩 ---
    "쌀": "맑은 물이 나올 때까지 서너 번 씻어 30분간 불린 뒤 체에 받쳐 물기를 뺀다",
    "찹쌀": "씻어서 1시간 이상 충분히 불린다. 덜 불면 속이 딱딱하게 남는다",
    "콩": "찬물에 6시간 이상 불린다. 여름에는 냉장고에서 불려야 쉬지 않는다",
    "녹두": "물에 6시간 불린 뒤 손으로 비벼 껍질을 벗겨낸다",
    "땅콩": "마른 팬에 중약불로 3분간 볶아 고소한 향을 낸다",

    # --- 알 ---
    "계란": "그릇에 깨뜨려 알끈을 젓가락으로 건져내고 곱게 푼다. 알끈을 두면 비린내가 남는다",

    # --- 그 밖 ---
    "구운김": "가위로 먹기 좋게 자르거나 손으로 부순다. 마지막에 올려야 눅지 않는다",
    "마늘": "곱게 다진다",
    "생강": "껍질을 숟가락으로 긁어내고 곱게 다진다",
    "두부": "1.5cm 두께로 썰어 키친타월 위에 올려 물기를 뺀다",
}

DEFAULT_PREP = "씻어서 먹기 좋은 크기로 썬다"


# 역할 나누기

# 양념. 조리 중간에 양념장으로 한꺼번에 들어간다.
SEASONINGS = {
    "고춧가루", "소금", "천일염", "설탕", "간장", "고추장", "된장", "액젓",
    "새우젓", "참기름", "들기름", "식용유", "후추", "식초", "맛술", "물엿",
    "마늘", "생강",
}

# 고명. 불을 끄고 올린다.
GARNISH = {"참깨", "구운김", "김", "깻잎", "레몬"}

# 주재료 후보. 이 중 처음 나오는 것이 그 요리의 주인공이다.
MAINS = {
    "돼지고기", "삼겹살", "목살", "돼지갈비", "소고기", "소고기안심",
    "소고기등심", "소고기설도", "소갈비", "한우", "닭", "닭고기",
    "고등어", "갈치", "조기", "굴비", "명태", "삼치", "꽁치",
    "오징어", "마른오징어", "새우", "전복", "꽃게", "바지락", "홍합",
    "계란", "두부",
}

# 마지막에 넣어야 향이 사는 것들.
LATE = {"대파", "쪽파", "부추", "미나리", "붉은고추", "고추", "팽이버섯"}

# 오래 끓여야 익는 것들. 다른 채소보다 먼저 들어간다.
SLOW = {"무", "감자", "고구마", "당근", "배추", "양배추", "콩", "녹두", "쌀", "찹쌀"}


def split_roles(ingredients: list[dict]) -> dict:
    """재료를 역할별로 가른다."""
    roles = {
        "main": None,
        "slow": [],
        "normal": [],
        "late": [],
        "season": [],
        "garnish": [],
    }

    for item in ingredients:
        name = item.get("name") or ""

        if name in GARNISH:
            roles["garnish"].append(item)
        elif name in SEASONINGS:
            roles["season"].append(item)
        elif roles["main"] is None and name in MAINS:
            roles["main"] = item
        elif name in LATE:
            roles["late"].append(item)
        elif name in SLOW:
            roles["slow"].append(item)
        else:
            roles["normal"].append(item)

    # 고기도 생선도 없는 요리(채소 반찬 · 주스)면 앞선 재료가 주인공이 된다.
    if roles["main"] is None:
        for key in ("slow", "normal", "late"):
            if roles[key]:
                roles["main"] = roles[key].pop(0)
                break

    return roles


def amount(item: dict) -> str:
    """'돼지고기 300g' 처럼 이름에 분량을 붙인다."""
    name = item.get("name") or ""
    quantity = item.get("quantity")
    unit = item.get("unit")

    if quantity is None or not unit:
        return name

    # 0.5 는 0.5 로, 2.0 은 2 로 적는다
    return f"{name} {quantity:g}{unit}"


def listing(items: list[dict]) -> str:
    """'양파 1개 · 당근 0.5개와 대파 1대' 처럼 늘어놓는다."""
    if not items:
        return ""
    if len(items) == 1:
        return amount(items[0])
    return " · ".join(amount(item) for item in items[:-1]) + f"와 {amount(items[-1])}"


# 로마자로 끝나는 분량의 읽는 소리. 받침이 있는지는 소리로 정해진다.
UNIT_SOUND = {
    "g": True, "kg": True, "mg": True,   # 그램 · 킬로그램 · 밀리그램
    "ml": False, "l": False, "cc": False,  # 밀리리터 · 리터 · 시시
}


def pick_josa(word: str, with_final: str, without_final: str) -> str:
    """앞 글자의 받침에 따라 조사를 고른다. (ai_api 의 같은 함수와 같은 규칙)"""
    if not word:
        return without_final

    code = ord(word[-1])
    if code < 0xAC00 or code > 0xD7A3:
        # 한글이 아니면 로마자 단위인지 본다. 긴 것부터 봐야 kg이 g에
        # 가로채이지 않는다.
        lowered = word.lower()
        for unit in sorted(UNIT_SOUND, key=len, reverse=True):
            if lowered.endswith(unit):
                return with_final if UNIT_SOUND[unit] else without_final
        return without_final

    return with_final if (code - 0xAC00) % 28 else without_final


def eul(text: str) -> str:
    return text + pick_josa(text, "을", "를")


def eun(text: str) -> str:
    return text + pick_josa(text, "은", "는")


# 시간 나누기
def timings(minutes: int) -> dict:
    cook = max(6, int(minutes or 20) - 5)

    return {
        "long": max(5, cook // 2),
        "mid": max(3, cook // 3),
        "short": max(2, cook // 5),
        # 국물 양. 조리시간이 길수록 졸아들 몫을 더 잡는다. 2인분 기준.
        "water": 500 + min(cook, 40) * 10,
    }


# 조리법별 단계

def steps_soup(roles, t):
    main = roles["main"]
    out = [f"냄비에 물 {t['water']}ml를 붓고 센 불에 올린다."]

    if main:
        out.append(
            f"물이 끓어오르면 {eul(amount(main))} 넣고 중강불에서 {t['mid']}분간 끓인다. "
            "위로 떠오르는 거품은 국자로 걷어낸다. 이걸 안 걷으면 국물이 텁텁해진다."
        )

    if roles["slow"]:
        out.append(
            f"{eul(listing(roles['slow']))} 넣고 {t['mid']}분 더 끓인다. "
            "단단한 재료라 먼저 넣어야 다른 재료와 익는 때가 맞는다."
        )

    if roles["normal"]:
        out.append(f"{eul(listing(roles['normal']))} 넣고 {t['short']}분간 끓인다.")

    if roles["season"]:
        out.append(
            f"준비한 {eul(sauce_name(roles))} 국물에 풀고 "
            f"중불로 낮춰 {t['short']}분간 더 끓인다. "
            "양념을 처음부터 넣으면 재료가 질겨지고 국물이 탁해진다."
        )

    if roles["late"]:
        out.append(
            f"{eul(listing(roles['late']))} 넣고 1분만 더 끓인다. "
            "오래 끓이면 향이 다 날아가므로 파는 늘 마지막이다."
        )

    # "불을 끈다" 를 따로 한 줄 두지 않는다. 향채가 없는 요리에서 그 줄만
    # 덩그러니 남아 단계 수만 늘리기 때문이다.
    out.append(
        "불을 끄고 간을 본다. 모자라면 소금이나 국간장으로 맞추되, "
        "국물은 식으면서 간이 세지므로 지금은 살짝 싱거운 편이 낫다."
    )
    return out


def steps_stirfry(roles, t):
    main = roles["main"]
    out = [
        "팬을 센 불에 1분간 달군 뒤 식용유를 2큰술 두른다. "
        "팬이 덜 달궈지면 재료에서 물이 나와 볶음이 아니라 찜이 된다."
    ]

    if main:
        out.append(
            f"{eul(amount(main))} 팬에 펼쳐 넣고 {t['short']}분간 그대로 둔다. "
            "자꾸 뒤적이면 팬 온도가 떨어져 겉면이 익지 않는다."
        )
        out.append(f"바닥에 닿은 면에 색이 오르면 뒤적이며 {t['short']}분 더 볶는다.")

    hard = roles["slow"] + roles["normal"]
    if hard:
        out.append(
            f"{eul(listing(hard))} 넣고 센 불에서 {t['mid']}분간 볶는다. "
            "단단한 것부터 넣어야 다 같이 알맞게 익는다."
        )

    if roles["season"] and sauce_is_dry(roles):
        out.append(
            f"{eul(sauce_name(roles))} 재료 위에 고루 뿌리고 크게 뒤적여 섞는다. "
            "한곳에 몰아 넣으면 그 부분만 짜고 나머지는 심심해진다."
        )
    elif roles["season"]:
        out.append(
            f"{eul(sauce_name(roles))} 팬 가장자리에 둘러 넣는다. 가운데 부으면 양념만 타지만, "
            "달궈진 가장자리에 닿으면 지글거리며 향이 올라온다."
        )
        out.append(f"{t['short']}분간 재료에 양념이 고루 입혀지도록 뒤적인다.")

    if roles["late"]:
        out.append(f"{eul(listing(roles['late']))} 넣고 30초만 더 볶아 향을 낸다.")

    out.append(
        "불을 끄고 바로 그릇에 옮겨 담는다. "
        "뜨거운 팬에 두면 남은 열로 계속 익어 채소가 물러진다."
    )
    return out


def steps_noodle_mix(roles, t):
    """비빔국수 · 막국수. 찬물에 헹궈 양념에 무친다."""
    out = [
        "냄비에 물을 넉넉히 붓고 센 불로 팔팔 끓인다. 면 200g에 물 2L는 되어야 한다 — "
        "물이 적으면 면끼리 붙고 국물이 전분으로 걸쭉해진다.",
        "면을 부채꼴로 펼쳐 넣고 봉지에 적힌 시간보다 30초 짧게 삶는다. "
        "건진 뒤에도 남은 열로 조금 더 익기 때문이다.",
        "삶은 면을 체에 건져 찬물에 두세 번 헹구며 겉의 전분을 씻어낸다. "
        "이 과정을 건너뛰면 면이 미끈거리고 금방 분다.",
        "면을 손으로 꽉 쥐어 물기를 뺀다. 물기가 남으면 양념이 묽어진다.",
    ]

    if roles["season"]:
        out.append(
            f"큰 볼에 면과 {eul(sauce_name(roles))} 넣고 손으로 조물조물 무친다. "
            "젓가락으로 섞으면 양념이 겉돌고 면이 끊어진다."
        )

    rest = (
        ([roles["main"]] if roles["main"] else [])
        + roles["slow"] + roles["normal"] + roles["late"]
    )
    if rest:
        out.append(f"{eul(listing(rest))} 올리고 가볍게 섞는다.")

    out.append("그릇에 담아 바로 낸다. 두면 면이 양념을 먹어 뻑뻑해진다.")
    return out


def steps_pancake(roles, t):
    out = [
        "부침가루(또는 밀가루) 1컵에 찬물 1컵을 붓고 멍울이 없게 푼다. "
        "찬물로 해야 반죽이 쫄깃하고, 냉장고에 10분 두었다 부치면 더 바삭하다."
    ]

    every = (
        ([roles["main"]] if roles["main"] else [])
        + roles["slow"] + roles["normal"] + roles["late"]
    )
    if every:
        out.append(
            f"썰어 둔 {eul(listing(every))} 반죽에 넣고 고루 섞는다. "
            "반죽은 재료가 겨우 붙을 만큼만 넣는다 — 많으면 밀가루 맛이 난다."
        )

    if roles["season"]:
        out.append(
            "양념을 넣어 반죽의 간을 맞춘다. 여기서 한 번 맛을 봐두면 다 부친 뒤에 후회하지 않는다."
        )

    out.append("팬을 중불에 올리고 기름을 넉넉히 두른다. 기름이 적으면 부침이 아니라 구이가 된다.")
    out.append(f"반죽 한 국자를 올려 얇게 펴고 {t['short']}분간 건드리지 않는다.")
    out.append(
        "가장자리가 노릇해지고 윗면 반죽이 반투명해지면 뒤집는다. "
        "일찍 뒤집으면 부서지므로 이 두 가지를 확인하고 뒤집는다."
    )
    out.append(f"뒤집은 뒤 주걱으로 가볍게 눌러가며 {t['short']}분 더 부친다.")
    out.append("마지막에 한 번 더 뒤집어 앞뒤로 30초씩 구우면 겉이 훨씬 바삭해진다.")
    out.append("도마에 옮겨 한 김 식힌 뒤 썬다. 뜨거울 때 썰면 속이 눌려 뭉개진다.")
    return out


def steps_fry(roles, t):
    main = roles["main"]
    out = []

    if main:
        out.append(
            f"{eul(amount(main))} 소금과 후추로 밑간해 {t['short']}분간 재운다. "
            "튀김옷 안쪽까지 간이 들어가야 소스 없이도 먹을 만하다."
        )

    out.append(
        "튀김가루(또는 밀가루) 1컵에 얼음물 1컵을 붓고 젓가락으로 여덟 번에서 열 번만 휘젓는다. "
        "많이 저으면 끈기가 생겨 튀김옷이 질겨진다. 멍울이 조금 남아 있어야 바삭하다."
    )
    out.append(
        "재료에 마른 가루를 얇게 묻힌 뒤 튀김옷을 입힌다. "
        "마른 가루를 먼저 묻혀야 튀기는 중에 옷이 벗겨지지 않는다."
    )
    out.append(
        "기름을 170도로 달군다. 온도계가 없으면 튀김옷을 한 방울 떨어뜨려 보고, "
        "바닥까지 가라앉았다가 곧바로 떠오르면 알맞은 온도다."
    )
    out.append(
        f"기름 표면의 3분의 2만 차도록 나눠 넣고 {t['short']}분간 튀긴다. "
        "한 번에 많이 넣으면 기름 온도가 떨어져 눅눅해진다."
    )
    out.append(
        "건져서 3분간 쉬게 둔 뒤, 기름을 190도로 올려 30초만 한 번 더 튀긴다. "
        "두 번 튀겨야 식어도 바삭하다."
    )
    out.append("채반에 비스듬히 세워 기름을 뺀다. 접시에 바로 올리면 밑면이 눅는다.")
    return out


def steps_grill(roles, t):
    main = roles["main"]
    out = []

    if roles["season"] and main:
        out.append(
            f"{eul(amount(main))} {sauce_name(roles)}에 고루 버무려 {t['mid']}분간 재운다. "
            "냉장고보다 상온에 두어야 속까지 온도가 고르게 오른다."
        )

    out.append(
        "팬이나 그릴을 센 불에 2분간 충분히 달군다. "
        "덜 달군 팬에 올리면 재료가 들러붙고 물이 배어 나온다."
    )

    if main:
        out.append(
            f"기름을 얇게 두르고 {eul(amount(main))} 올린 뒤 {t['short']}분간 건드리지 않는다. "
            "겉면이 익어 껍질이 생기면 저절로 팬에서 떨어진다."
        )
        out.append(
            f"가장자리에 색이 오르면 한 번만 뒤집어 {t['short']}분 더 굽는다. "
            "여러 번 뒤집으면 육즙이 빠져 퍽퍽해진다."
        )

    out.append("불을 약불로 낮추고 뚜껑을 덮어 2분간 두어 속까지 익힌다.")

    side = roles["slow"] + roles["normal"] + roles["late"]
    if side:
        out.append(f"곁들일 {eun(listing(side))} 같은 팬에 남은 기름으로 가볍게 굽는다.")

    out.append("불을 끄고 3분간 그대로 둔 뒤 썬다. 바로 썰면 육즙이 도마로 다 빠져나간다.")
    return out


def steps_braise(roles, t):
    main = roles["main"]
    bottom = roles["slow"] + roles["normal"]
    out = []

    if bottom:
        target = eul(amount(main)) if main else "남은 재료를"
        out.append(
            f"냄비 바닥에 {eul(listing(bottom))} 깔고 그 위에 {target} 올린다. "
            "바닥에 깐 재료가 받침이 되어 눌어붙지 않고, 국물 맛도 배어든다."
        )
    elif main:
        out.append(f"냄비에 {eul(amount(main))} 겹치지 않게 펼쳐 담는다.")

    if len(roles["season"]) > 1:
        out.append(f"양념장에 물 {t['water'] // 2}ml를 섞어 재료가 반쯤 잠기도록 붓는다.")
    elif roles["season"]:
        # 양념이 하나뿐이면 "마늘에 물을 섞어" 가 되어 말이 안 된다
        out.append(
            f"{eul(sauce_name(roles))} 넣고 물 {t['water'] // 2}ml를 "
            "재료가 반쯤 잠기도록 붓는다."
        )
    else:
        out.append(f"물 {t['water'] // 2}ml를 재료가 반쯤 잠기도록 붓는다.")

    out.append("뚜껑을 덮고 센 불에서 끓이다가, 끓어오르면 중불로 낮춘다.")
    out.append(
        f"{t['mid']}분간 끓이며 국물을 숟가락으로 떠서 위에 끼얹어 준다. "
        "뒤적이면 살이 부서지므로 뒤집는 대신 끼얹는다."
    )

    if roles["late"]:
        out.append(f"{eul(listing(roles['late']))} 올린다.")

    out.append(
        f"뚜껑을 열고 {t['short']}분간 더 조린다. "
        "국물이 재료 높이의 3분의 1쯤 남아 자작해지면 알맞다."
    )
    out.append("불을 끄고 5분간 그대로 둔다. 식는 동안 양념이 속까지 배어든다.")
    return out


def steps_seasoned(roles, t):
    main = roles["main"]
    out = []

    if main:
        out.append(
            f"{eul(amount(main))} 볼에 담고 소금 1작은술을 뿌려 10분간 절인다. "
            "절이지 않으면 무친 뒤에 물이 생겨 싱거워진다."
        )
        out.append("나온 물을 버리고 손으로 가볍게 짠다. 너무 세게 짜면 식감이 죽는다.")

    if roles["season"]:
        target = eul(amount(main)) if main else "재료를"
        out.append(
            f"볼에 {target} {sauce_name(roles)}과 함께 넣고 손으로 조물조물 무친다. "
            "젓가락보다 손이 낫다 — 양념이 고루 붙고 재료도 덜 부서진다."
        )

    rest = roles["slow"] + roles["normal"] + roles["late"]
    if rest:
        out.append(f"{eul(listing(rest))} 넣고 가볍게 섞는다. 여기서는 많이 주무르지 않는다.")

    out.append(
        "10분쯤 두었다가 간을 다시 본다. 무침은 시간이 지나며 간이 배어 "
        "처음보다 짜게 느껴지므로, 처음에는 조금 싱겁게 맞추는 편이 낫다."
    )
    return out


def steps_salad(roles, t):
    out = [
        "채소를 찬물에 10분간 담갔다 건져 물기를 완전히 턴다. "
        "차가워야 아삭하고, 물기가 남으면 드레싱이 묽어져 겉돈다."
    ]

    if roles["season"]:
        out.append("양념을 모두 섞어 드레싱을 만들고 냉장고에 넣어 차게 둔다.")

    every = (
        ([roles["main"]] if roles["main"] else [])
        + roles["slow"] + roles["normal"] + roles["late"]
    )
    if every:
        out.append(f"그릇에 {eul(listing(every))} 층이 지게 보기 좋게 담는다.")

    out.append("먹기 직전에 드레싱을 두른다. 미리 부어두면 채소가 숨이 죽어 물러진다.")
    return out


def steps_drink(roles, t):
    main = roles["main"]
    out = []

    if main:
        out.append(f"{eul(amount(main))} 한 입 크기로 썰어 믹서에 넣는다.")

    rest = roles["slow"] + roles["normal"] + roles["late"]
    if rest:
        out.append(f"{eul(listing(rest))} 함께 넣는다.")

    if roles["season"]:
        out.append("양념을 넣어 단맛을 맞춘다. 과일이 충분히 달면 넣지 않아도 된다.")

    out.append(
        "얼음 대여섯 개와 물(또는 우유) 100ml를 넣고 10초씩 끊어가며 30초간 간다. "
        "한 번에 오래 갈면 믹서 날에서 열이 나 맛이 떨어진다."
    )
    out.append("체에 한 번 걸러 씨와 굵은 섬유질을 받아낸다. 목넘김이 훨씬 부드러워진다.")
    out.append("잔에 따라 바로 낸다. 두면 층이 갈라지므로 마시기 직전에 만드는 편이 낫다.")
    return out


def steps_rice_bowl(roles, t):
    main = roles["main"]
    out = ["팬에 기름을 두르고 센 불에 달군다."]

    if main:
        out.append(f"{eul(amount(main))} 넣고 {t['short']}분간 볶는다.")

    hard = roles["slow"] + roles["normal"]
    if hard:
        out.append(f"{eul(listing(hard))} 넣고 {t['short']}분 더 볶는다.")

    if roles["season"]:
        out.append(
            f"{eul(sauce_name(roles))} 붓고 중불에서 {t['short']}분간 조리듯 익힌다. "
            "국물이 조금 남아야 밥에 비볐을 때 뻑뻑하지 않다."
        )

    if roles["late"]:
        out.append(f"{eul(listing(roles['late']))} 넣고 30초만 더 익힌다.")

    out.append("밥 2공기를 그릇에 담고 주걱으로 살짝 눌러 평평하게 만든다.")
    out.append("익힌 재료를 국물째 밥 위에 얹는다.")
    return out


# 태그 -> 만드는 함수. ai_api 가 요리의 tags 를 위에서부터 훑어 처음 걸리는
# 것을 쓴다. 그래서 '한식' · '든든한' 같은 성격 태그는 여기 없어도 된다.
BUILDERS = {
    "국물": steps_soup,
    "볶음": steps_stirfry,
    "면": steps_noodle_mix,
    "부침": steps_pancake,
    "튀김": steps_fry,
    "구이": steps_grill,
    "조림": steps_braise,
    "무침": steps_seasoned,
    "샐러드": steps_salad,
    "음료": steps_drink,
    "덮밥": steps_rice_bowl,
}

# 조리법 태그가 하나도 없을 때. 카탈로그 100종은 전부 위 태그를 갖고 있어서
# 여기로 오는 건 사용자가 직접 검색해 카탈로그에 없는 이름뿐이다.
DEFAULT_BUILDER = steps_stirfry


def prep_line(item: dict) -> str:
    """재료 하나의 손질 단계."""
    label = amount(item)
    how = PREP.get(item.get("name") or "", DEFAULT_PREP)
    return f"{eun(label)} {how}."


# 가루·알갱이로 된 양념. 국물에 풀거나 뿌리는 것이지 "둘러 넣는" 것이 아니다.
DRY_SEASONINGS = {"소금", "천일염", "고춧가루", "설탕", "후추", "참깨"}


def sauce_is_dry(roles: dict) -> bool:
    """양념이 마른 것 하나뿐인가."""
    season = roles["season"]
    return len(season) == 1 and (season[0].get("name") or "") in DRY_SEASONINGS


def sauce_name(roles: dict) -> str:
    """조리 단계에서 양념을 부르는 이름."""
    season = roles["season"]
    if len(season) == 1:
        return season[0].get("name") or "양념"
    return "양념장"


def sauce_step(roles: dict) -> list[str]:
    """양념을 한 그릇에 미리 섞는 단계."""
    season = roles["season"]
    if not season:
        return []

    names = " · ".join(amount(item) for item in season)

    if len(season) == 1:
        return [f"{eul(names)} 미리 계량해 둔다. 불 앞에서 재면 늦는다."]

    tip = ""
    if any((item.get("name") or "") == "고춧가루" for item in season):
        tip = " 고춧가루는 물 2큰술에 미리 개어두면 뭉치지 않고 색이 곱게 난다."

    return [f"{eul(names)} 한 그릇에 모아 고루 섞어 양념장을 만든다.{tip}"]


def garnish_step(roles: dict) -> list[str]:
    """고명 단계. 불을 끈 뒤에 올린다."""
    if not roles["garnish"]:
        return []

    names = " · ".join(amount(item) for item in roles["garnish"])
    return [f"마지막으로 {eul(names)} 올리거나 뿌려서 낸다. 열에 오래 두면 향이 날아간다."]


def build_steps(
    food_name: str, tags: list[str], ingredients: list[dict], minutes: int
) -> list[str]:
    """조리 순서 전체를 만든다."""
    roles = split_roles(ingredients)
    t = timings(minutes)

    order = (
        ([roles["main"]] if roles["main"] else [])
        + roles["slow"] + roles["normal"] + roles["late"]
    )

    return (
        [prep_line(item) for item in order]
        + sauce_step(roles)
        + pick_builder(food_name, tags)(roles, t)
        + garnish_step(roles)
    )


# 태그만으로는 갈리지 않는 것들

def steps_noodle_broth(roles, t):
    """국물국수. 칼국수 · 수제비 · 잔치국수."""
    main = roles["main"]
    out = [f"냄비에 물 {t['water'] + 400}ml를 붓고 센 불에 올려 육수를 낸다."]

    if main:
        out.append(
            f"물이 끓어오르면 {eul(amount(main))} 넣고 {t['short']}분간 끓인다. "
            "떠오르는 거품은 걷어낸다."
        )

    hard = roles["slow"] + roles["normal"]
    if hard:
        out.append(f"{eul(listing(hard))} 넣고 {t['short']}분 더 끓인다.")

    if roles["season"]:
        out.append(f"{eul(sauce_name(roles))} 넣어 국물 간을 맞춘다.")

    out.append(
        "면은 국물에 바로 넣지 말고 다른 냄비에 따로 삶는다. "
        "면에서 나온 전분이 국물을 걸쭉하고 텁텁하게 만들기 때문이다."
    )
    out.append(
        "끓는 물에 면을 넣고 봉지에 적힌 시간만큼 삶는다. "
        "끓어 넘치려 하면 찬물을 반 컵씩 부어 가라앉힌다."
    )
    out.append("삶은 면을 체에 건져 뜨거운 물에 한 번 헹궈 전분기를 씻어낸다.")

    if roles["late"]:
        out.append(f"국물에 {eul(listing(roles['late']))} 넣고 30초만 더 끓인다.")

    out.append("그릇에 면을 담고 뜨거운 국물을 부어 낸다. 국물은 면이 잠길 만큼 넉넉히 붓는다.")
    return out


def steps_noodle_cold(roles, t):
    """찬 국물국수. 냉면 · 콩국수 · 냉우동 · 소바."""
    out = [
        "국물은 미리 만들어 냉장고에서 차게 식힌다. 살얼음이 낄 만큼 차가워야 "
        "면을 말았을 때 미지근해지지 않는다.",
        "냄비에 물을 넉넉히 붓고 센 불로 팔팔 끓인다. 면 200g에 물 2L는 되어야 한다.",
        "면을 부채꼴로 펼쳐 넣고 봉지에 적힌 시간만큼 삶는다. "
        "찬 국수는 면이 퍼지면 되돌릴 수 없으니 30초 전부터 하나 건져 씹어 본다.",
        "삶은 면을 얼음물에 넣고 손으로 비벼 가며 헹군다. "
        "겉의 전분이 다 빠지고 면이 차가워질 때까지 물을 두세 번 갈아준다.",
        "면을 한 입 크기로 돌돌 말아 물기를 꽉 짜서 그릇에 담는다. "
        "물기가 남으면 국물이 밍밍해진다.",
    ]

    every = (
        ([roles["main"]] if roles["main"] else [])
        + roles["slow"] + roles["normal"] + roles["late"]
    )
    if every:
        out.append(f"{eul(listing(every))} 면 위에 색을 맞춰 보기 좋게 올린다.")

    if roles["season"]:
        out.append(f"{eul(sauce_name(roles))} 가운데에 얹는다.")

    out.append(
        "차게 식힌 국물을 부어 내거나, 따로 담아 곁들인다. "
        "얼음 두세 개를 띄우면 끝까지 시원하다."
    )
    return out


def steps_bibimbap(roles, t):
    """비빔밥."""
    out = []

    cook = ([roles["main"]] if roles["main"] else []) + roles["slow"]
    if cook:
        out.append(
            f"{eul(listing(cook))} 팬에 기름을 두르고 {t['short']}분간 따로 볶아 덜어 둔다. "
            "재료마다 익는 때가 달라서 한꺼번에 볶으면 어떤 건 타고 어떤 건 설익는다."
        )

    fresh = roles["normal"] + roles["late"]
    if fresh:
        out.append(
            f"{eun(listing(fresh))} 익히지 않고 그대로 둔다. "
            "아삭한 것이 섞여야 비볐을 때 질척하지 않다."
        )

    out.append("밥 2공기를 넓은 그릇에 담고 주걱으로 살짝 눌러 평평하게 만든다.")
    out.append(
        "준비한 재료를 밥 위에 부채꼴로 색을 갈라 올린다. "
        "섞어 담으면 비비기 전에 이미 한 가지 색이 된다."
    )

    if roles["season"]:
        out.append(f"가운데에 {eul(sauce_name(roles))} 올린다.")
    else:
        out.append("가운데에 고추장 1큰술과 참기름 1작은술을 올린다.")

    out.append(
        "먹기 직전에 숟가락으로 바닥부터 크게 뒤집듯 비빈다. "
        "짓이기면 밥알이 뭉개지므로 가볍게 여러 번 뒤집는 편이 낫다."
    )
    return out


def steps_cold_soup(roles, t):
    """냉국 · 물회. 끓이지 않고 찬 국물에 재료를 넣는다."""
    main = roles["main"]
    out = [
        "국물로 쓸 물 600ml를 미리 냉장고에 넣어 차게 준비한다. "
        "얼음을 나중에 많이 넣으면 간이 풀려 싱거워지므로, 물부터 차게 하는 편이 낫다."
    ]

    if main:
        out.append(
            f"{eul(amount(main))} 소금 1작은술에 5분간 절였다가 물기를 가볍게 짠다. "
            "절여야 국물에 넣어도 물러지지 않고 아삭함이 남는다."
        )

    if roles["season"]:
        out.append(
            f"차게 둔 물에 {eul(sauce_name(roles))} 넣고 잘 저어 녹인다. "
            "식초와 설탕을 쓰면 신맛을 먼저 맞추고 단맛으로 조절한다."
        )

    every = ([main] if main else []) + roles["slow"] + roles["normal"] + roles["late"]
    if every:
        out.append(f"그릇에 {eul(listing(every))} 담는다.")

    out.append("차게 만든 국물을 붓고 간을 본다. 차가우면 간이 약하게 느껴지니 평소보다 조금 세게 맞춘다.")
    out.append("먹기 직전에 얼음 서너 개를 띄운다. 미리 넣으면 녹아서 국물이 묽어진다.")
    return out


def steps_raw(roles, t):
    """육회 · 생회. 익히지 않는다."""
    main = roles["main"]
    out = [
        "고기와 도마 · 칼을 모두 차갑게 준비한다. 익히지 않는 요리라 "
        "온도가 올라가면 상하기 쉽고, 차가워야 칼질도 깔끔하다."
    ]

    if main:
        out.append(
            f"{eul(amount(main))} 키친타월로 감싸 핏물을 충분히 뺀다. "
            "핏물이 남으면 양념이 겉돌고 비린내가 난다."
        )
        out.append(f"{eul(amount(main))} 결 반대 방향으로 0.5cm 두께로 채 썬다.")

    rest = roles["slow"] + roles["normal"] + roles["late"]
    if rest:
        out.append(f"{eul(listing(rest))} 곁들일 수 있게 따로 채 썰어 둔다.")

    if roles["season"]:
        out.append(
            f"{eul(sauce_name(roles))} 넣고 손으로 가볍게 버무린다. "
            "오래 주무르면 고기에서 물이 나와 질척해지므로 열 번 안쪽으로 끝낸다."
        )

    out.append(
        "접시에 담고 바로 먹는다. 만들어 두면 물이 생기므로 낼 직전에 무치는 것이 좋다."
    )
    return out


# 이름에 든 낱말로 고르는 뼈대. 긴 것부터 본다 —
# '비빔국수' 를 '국수' 로 잡으면 국물국수가 되어 버린다.
NAME_BUILDERS = [
    ("비빔냉면", steps_noodle_mix),
    ("열무비빔국수", steps_noodle_mix),
    ("비빔국수", steps_noodle_mix),
    ("회냉면", steps_noodle_mix),
    ("막국수", steps_noodle_mix),
    ("비빔밥", steps_bibimbap),
    ("육회", steps_raw),
    ("물회", steps_cold_soup),
    ("냉국", steps_cold_soup),
]

# 차게 먹는 음식에 붙는 태그. 같은 '면' 이라도 이게 있으면 찬 국수다.
COLD_TAGS = {"시원한음식", "여름메뉴"}


def pick_builder(food_name: str, tags: list[str]):
    """이 요리의 조리 순서를 만들 함수를 고른다."""
    name = food_name or ""

    for keyword, builder in NAME_BUILDERS:
        if keyword in name:
            return builder

    for tag in tags or []:
        if tag == "면":
            # 같은 면이라도 찬 것과 뜨거운 것은 만드는 법이 아예 다르다
            cold = bool(COLD_TAGS & set(tags)) or "냉" in name or "콩국수" in name
            return steps_noodle_cold if cold else steps_noodle_broth

        if tag == "덮밥":
            # '제육덮밥' 은 볶아서 얹고, '전주비빔밥 · 무밥' 은 올려서 비빈다
            return steps_rice_bowl if name.endswith("덮밥") else steps_bibimbap

        if tag in BUILDERS:
            return BUILDERS[tag]

    return DEFAULT_BUILDER
