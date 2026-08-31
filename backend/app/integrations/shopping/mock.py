"""개발용 가짜 가격 Provider.

외부 API 키 없이도 레시피 화면과 가격 표시 흐름 전체를 만들 수 있게 한다.
11번가 인증키나 공공데이터포털 승인을 기다리는 동안에도 프론트 작업이 멈추지 않는다.

값은 재료명에서 결정적으로(deterministic) 만든다. 랜덤을 쓰면 새로고침할 때마다
가격이 바뀌어서 화면 정렬이나 캐싱 버그를 오히려 못 잡는다.
같은 재료는 서버를 재시작해도 늘 같은 값이 나온다.

주의: 이 Provider는 빈 문자열을 뺀 모든 재료에 대해 값을 돌려준다.
"가격을 못 찾았을 때"의 화면은 실제 Provider를 붙인 뒤에 확인해야 한다.
"""

import hashlib

from app.integrations.shopping.base import PriceQuote

# 자주 쓰는 재료는 실제와 비슷한 값을 박아둔다.
# 시연이나 스크린샷에서 "감자 1kg 47,300원" 같은 값이 나오면 설득력이 떨어진다.
#   재료명: (최저가, 최고가, 단위)
_KNOWN: dict[str, tuple[int, int, str]] = {
    "감자": (2_900, 6_500, "1kg"),
    "양파": (2_400, 5_800, "1kg"),
    "대파": (2_100, 4_900, "1단"),
    "당근": (2_800, 5_200, "1kg"),
    "배추": (4_500, 9_800, "1포기"),
    "무": (2_200, 4_300, "1개"),
    "애호박": (1_800, 3_400, "1개"),
    "마늘": (7_900, 14_500, "1kg"),
    "달걀": (5_800, 9_200, "30구"),
    "두부": (1_500, 3_200, "300g"),
    "돼지고기": (11_800, 24_000, "1kg"),
    "소고기": (32_000, 68_000, "1kg"),
    "닭고기": (5_900, 12_400, "1kg"),
    "고등어": (4_200, 8_900, "1마리"),
    "간장": (3_400, 8_700, "500ml"),
    "고추장": (5_600, 12_900, "500g"),
    "된장": (4_800, 11_200, "500g"),
    "참기름": (7_200, 16_800, "320ml"),
    "설탕": (2_600, 4_900, "1kg"),
    "소금": (1_900, 4_100, "1kg"),
    "식용유": (4_700, 9_300, "900ml"),
    "쌀": (24_000, 42_000, "10kg"),
    "라면": (3_900, 6_800, "5개입"),
    "우유": (2_700, 4_200, "1L"),
}

# 사전에 없는 재료에 쓸 값의 범위. 식료품 한 품목의 현실적인 가격대에 맞춘다.
_FALLBACK_MIN = 1_500
_FALLBACK_MAX = 9_000

_FALLBACK_UNITS = ("1개", "100g", "300g", "500g", "1kg")


class MockPriceProvider:
    name = "mock"

    def fetch_quote(self, ingredient: str) -> PriceQuote | None:
        name = ingredient.strip()
        if not name:
            return None

        known = _KNOWN.get(name)
        if known is not None:
            low, high, unit = known
            sample_size = 12
        else:
            low, high, unit = self._derive(name)
            sample_size = 7

        return PriceQuote(
            ingredient=name,
            low=low,
            high=high,
            unit=unit,
            source=self.name,
            sample_size=sample_size,
        )

    def _derive(self, name: str) -> tuple[int, int, str]:
        """재료명 해시로 가격과 단위를 만든다.

        내장 hash()를 쓰지 않는 이유: 파이썬은 프로세스마다 문자열 해시에 무작위 시드를
        섞기 때문에 서버를 재시작하면 값이 바뀐다. md5는 언제 어디서 돌려도 같은 값이다.
        (보안 용도가 아니라 안정적인 숫자를 뽑기 위한 것이다.)
        """
        digest = hashlib.md5(name.encode("utf-8")).digest()

        span = _FALLBACK_MAX - _FALLBACK_MIN
        low = _FALLBACK_MIN + int.from_bytes(digest[:2], "big") % span
        # 최고가는 최저가의 1.4~2.6배. 실제 최저/최고 편차와 비슷한 폭이다.
        spread = 140 + digest[2] % 121
        high = low * spread // 100

        # 100원 단위로 떨어뜨려야 진짜 가격처럼 보인다.
        low = low // 100 * 100
        high = high // 100 * 100

        unit = _FALLBACK_UNITS[digest[3] % len(_FALLBACK_UNITS)]
        return low, high, unit
