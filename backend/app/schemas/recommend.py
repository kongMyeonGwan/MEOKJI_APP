"""AI 추천 · 레시피 · 재료 가격 요청/응답 스키마 (STEP 10~19).

여기 있는 것들은 전부 **DB 에 저장하지 않는 값**이다.
추천 후보와 가격은 조회 시점에만 존재하고, 사용자가 "이걸로 먹을래요"를 눌러
확정한 것만 MEAL_HISTORIES 로 넘어간다 (설계 원칙 §13).
"""

from typing import Literal

from pydantic import BaseModel, ConfigDict, Field, field_validator

from app.schemas.weather import WeatherResponse

# 추천을 어디서 요청했는지. 화면의 'AI 추천 기록'에 그대로 찍힌다.
RecommendSource = Literal[
    "날씨 추천",
    "카테고리",
    "예산 추천",
    "레시피 검색",
    "냉장고 재료",
    "메뉴 게임",
]

# 한 번에 받을 수 있는 후보 개수의 상한.
# 더 많이 받아도 화면에서 다 안 보고, AI 호출 시간만 길어진다.
MAX_LIMIT = 12


class RecommendRequest(BaseModel):
    """추천 요청.

    모든 항목이 선택이다. 아무것도 안 보내면 "오늘 무난한 메뉴"가 나온다.
    Guest 도 부를 수 있고, 로그인했으면 서버가 Ban 재료를 알아서 붙인다.
    """

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

    # 냉장고에 있는 재료
    ingredients: list[str] = Field(default_factory=list, max_length=30)

    # 이미 보여준 메뉴. '다른 메뉴 추천'을 눌렀을 때 겹치지 않게 한다.
    exclude: list[str] = Field(default_factory=list, max_length=30)

    # 날씨를 반영할지. 대시보드 히어로만 True 로 보낸다.
    #
    # 좌표를 안 보내도 된다. 로그인했으면 서버가 회원가입 때 고른 주소의
    # 좌표를 쓰고, 그것도 없으면 기본 좌표(종로2가)로 답한다
    # (services/ai_service.pick_coords).
    use_weather: bool = False
    lat: float | None = Field(None, ge=-90, le=90)
    lon: float | None = Field(None, ge=-180, le=180)

    @field_validator("category", "query")
    @classmethod
    def blank_to_none(cls, value: str | None) -> str | None:
        """공백만 들어온 값은 "안 보낸 것"으로 취급한다.

        ?category= 처럼 빈 값이 붙어 오는 경우가 있는데, 그대로 두면
        "이름이 빈 문자열인 분류"를 찾다가 결과가 0건이 된다.
        """
        if value is None:
            return None

        value = value.strip()
        if not value:
            return None

        return value

    @field_validator("ingredients", "exclude")
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

    # 냉장고 찾기의 "몇 개 가지고 있어요" 계산에 쓴다
    ingredients: list[str] = []

    # 사진이 없는 동안 쓰는 SVG 일러스트 종류 (stew · soup · noodle ...).
    # image_url 이 채워지면 화면이 그쪽을 먼저 쓴다.
    art: str | None = None
    image_url: str | None = None


class RecommendResponse(BaseModel):
    """추천 결과.

    condition 은 "서울특별시 · 맑음 28° · 못 먹는 재료 3건 제외" 처럼
    무슨 조건으로 골랐는지 한 줄로 적은 문장이다.
    화면의 'AI 추천 기록'이 이 문장을 그대로 저장한다.
    """

    condition: str
    items: list[FoodItem]

    # use_weather=True 로 요청했을 때만 채워진다
    weather: WeatherResponse | None = None


class PriceOffer(BaseModel):
    """조사처 한 곳의 가격.

    shop 은 쇼핑몰이 아니라 시장명이다. 예: "서울 가락시장"
    """

    shop: str
    price: int
    link: str

    # 가격의 기준 단위. 예: "1kg", "30개", "500g"
    #
    # 이걸 안 적으면 쌀 20kg 값과 깻잎 100g 값이 한 표에 나란히 서서
    # 쌀이 터무니없이 비싸 보인다. KAMIS 는 품목마다 단위가 다르다.
    unit: str = ""

    # 조사일자(YYYYMMDD). 주말·공휴일은 조사가 없어서 며칠 전 값일 수 있다.
    # 지난주 가격을 오늘 가격으로 오해시키면 안 되므로 화면에 같이 적는다.
    surveyed_at: str | None = None


class PriceItem(BaseModel):
    """재료 하나와 그 재료를 조사한 곳들."""

    name: str
    offers: list[PriceOffer] = []

    # ----- 필요한 만큼으로 환산한 값 -----
    #
    # offers 의 price 는 판매 단위 가격이다(고춧가루 1kg 25,200원).
    # 레시피가 요구하는 건 1큰술이라, 그대로 더하면 재료비가 백 배 넘게
    # 부풀려진다. 그래서 "필요한 만큼이 얼마인지" 를 따로 담는다.
    #
    # 환산할 수 없으면(판매 단위가 '1마리' 인데 레시피는 'g' 으로 적은 경우)
    # 셋 다 None 으로 두고, 화면이 합계에서 뺀다.
    need_text: str | None = None   # "1큰술" 처럼 사람이 읽는 필요량
    low_cost: int | None = None    # 가장 싼 시장 기준 필요량 값(원)
    high_cost: int | None = None   # 가장 비싼 시장 기준 필요량 값(원)


class PriceRequest(BaseModel):
    """가격 조회 요청. 재료 이름만 보내면 된다."""

    ingredients: list[str] = Field(..., min_length=1, max_length=30)


class RecipeResponse(BaseModel):
    """AI 가 만든 레시피 한 건 (아직 저장 전).

    사용자가 '이걸로 먹을래요'를 누르면 이 값이 그대로
    POST /api/histories 로 넘어가 MEAL_HISTORIES 에 스냅샷으로 저장된다.
    """

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
