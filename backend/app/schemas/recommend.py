"""AI 추천 · 레시피 · 재료 가격 요청/응답 스키마 (STEP 10~19)."""

from typing import Literal

from pydantic import BaseModel, ConfigDict, Field, field_validator

from app.schemas.weather import WeatherResponse

# 추천을 어디서 요청했는지. 화면의 'AI 추천 기록'에 그대로 찍힌다.
RecommendSource = Literal[
    "날씨 추천",
    "시세 추천",
    "카테고리",
    "예산 추천",
    "레시피 검색",
    "메뉴 게임",
]

# 한 번에 받을 수 있는 후보 개수의 상한.
# 더 많이 받아도 화면에서 다 안 보고, AI 호출 시간만 길어진다.
MAX_LIMIT = 12


class RecommendRequest(BaseModel):
    """추천 요청."""

    source: RecommendSource = "카테고리"
    limit: int = Field(5, ge=1, le=MAX_LIMIT)

    # '한식' 처럼 카테고리 이름을 그대로 받는다.
    # 키(KR)가 아니라 이름을 쓰는 이유는 주소창에 그대로 실려 사람이 읽을 수 있어서다.
    category: str | None = Field(None, max_length=20)

    # 예산(원). 상한만 주거나 하한만 줄 수도 있다.
    budget_min: int | None = Field(None, ge=0, le=1_000_000)
    budget_max: int | None = Field(None, ge=0, le=1_000_000)

    # "8천원 이하 파스타" 같은 문장. 그대로 AI 에게 넘긴다.
    # SQL LIKE 로는 이런 문장을 해석할 수 없어서 검색을 AI 에 맡긴다.
    query: str | None = Field(None, max_length=150)

    # 이미 보여준 메뉴. '다른 메뉴 추천'을 눌렀을 때 겹치지 않게 한다.
    exclude: list[str] = Field(default_factory=list, max_length=30)

    # 요즘 값이 내린 재료를 반영할지.
    use_bargains: bool = False

    # 날씨를 반영할지. 대시보드 히어로만 True 로 보낸다.
    use_weather: bool = False
    lat: float | None = Field(None, ge=-90, le=90)
    lon: float | None = Field(None, ge=-180, le=180)

    @field_validator("category", "query")
    @classmethod
    def blank_to_none(cls, value: str | None) -> str | None:
        """공백만 들어온 값은 "안 보낸 것"으로 취급한다."""
        if value is None:
            return None

        value = value.strip()
        if not value:
            return None

        return value

    @field_validator("exclude")
    @classmethod
    def clean_names(cls, values: list[str]) -> list[str]:
        """빈 값과 중복을 걸러낸다."""
        cleaned = []

        for raw in values:
            name = raw.strip()
            if name and name not in cleaned:
                cleaned.append(name)

        return cleaned


class FoodItem(BaseModel):
    """추천 후보 한 건. 화면의 FoodCard 가 이 모양을 그대로 쓴다."""

    food_name: str
    category: str
    reason: str
    cook_time_minutes: int
    difficulty: str
    estimated_cost: int
    tags: list[str] = []

    # 사진이 없는 동안 쓰는 SVG 일러스트 종류 (stew · soup · noodle ...).
    # image_url 이 채워지면 화면이 그쪽을 먼저 쓴다.
    art: str | None = None
    image_url: str | None = None


class BargainItem(BaseModel):
    """요즘 값이 내린 재료 한 건."""

    name: str
    percent: float      # 일주일 전 대비 등락률. 내린 것만 담으므로 음수다
    price: int          # 가장 싼 시장의 조사가격
    unit: str           # 그 가격의 기준 단위 ("1kg", "10개")
    surveyed_at: str | None = None


class RecommendResponse(BaseModel):
    """추천 결과."""

    condition: str
    items: list[FoodItem]

    # use_weather=True 로 요청했을 때만 채워진다
    weather: WeatherResponse | None = None

    # use_bargains=True 로 요청했을 때만 채워진다.
    # 화면이 "지금 대파가 12% 쌉니다" 칩을 그리는 데 쓴다.
    bargains: list[BargainItem] = []


class PriceOffer(BaseModel):
    """조사처 한 곳의 가격."""

    shop: str
    price: int
    link: str

    # 가격의 기준 단위. 예: "1kg", "30개", "500g"
    unit: str = ""

    # 조사일자(YYYYMMDD). 주말·공휴일은 조사가 없어서 며칠 전 값일 수 있다.
    # 지난주 가격을 오늘 가격으로 오해시키면 안 되므로 화면에 같이 적는다.
    surveyed_at: str | None = None


class PriceTrend(BaseModel):
    """최근 가격이 어느 쪽으로 움직였는지."""

    percent: float      # +는 오름, -는 내림
    from_price: int     # 기간 처음 날의 전국 평균
    to_price: int       # 기간 마지막 날의 전국 평균
    from_day: str       # YYYYMMDD
    to_day: str


class PriceItem(BaseModel):
    """재료 하나와 그 재료를 조사한 곳들."""

    name: str
    offers: list[PriceOffer] = []

    # 최근 가격 흐름. 변동이 작거나 조사일이 하루뿐이면 None.
    trend: PriceTrend | None = None

    # ----- 이 레시피 때문에 내야 하는 돈 -----
    need_text: str | None = None   # "0.2개" 처럼 사람이 읽는 필요량
    low_cost: int | None = None    # 가장 싼 시장에서 살 때 내는 돈(원)
    high_cost: int | None = None   # 가장 비싼 시장에서 살 때 내는 돈(원)

    # 필요한 양보다 더 산 재료인가.
    min_unit: bool = False

    # 실제로 사게 되는 양. "1개" · "30구". min_unit 일 때 화면이
    # "0.2개 필요 -> 1개 구입" 으로 두 양을 나란히 적는다.
    buy_text: str | None = None

    # KAMIS 조사 품목인가.
    surveyed: bool = True


class RecipeResponse(BaseModel):
    """AI 가 만든 레시피 한 건 (아직 저장 전)."""

    model_config = ConfigDict(from_attributes=True)

    food_name: str
    recipe_name: str

    # meal_histories 의 컬럼 이름과 맞춰둔다. 확정할 때 그대로 옮겨 담기 위함이다.
    recipe_json: dict
    ingredients_json: list
    substitutes_json: list

    estimated_cost: int

    # 카탈로그에 있는 음식이면 채워진다. 직접 검색한 이름이면 None.
    category: str | None = None
    art: str | None = None
    image_url: str | None = None

    # 재료별 시장 조사가격. DB 에 저장하지 않는 조회 시점의 값이다.
    prices: list[PriceItem] = []
