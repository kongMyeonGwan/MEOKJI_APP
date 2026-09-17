"""찜한 레시피 스키마."""

from pydantic import BaseModel, ConfigDict, Field, field_validator

from app.schemas.post import require_text


class LikeListResponse(BaseModel):
    """내 찜 목록 전체."""

    model_config = ConfigDict(from_attributes=True)

    # 찜한 음식 이름들. 최근에 찜한 것이 앞에 온다.
    names: list[str]

    count: int


class LikeToggleRequest(BaseModel):
    """찜하기 · 찜 해제 요청."""

    # DDL 이 VARCHAR2(150 CHAR) 라 그 이상은 받지 않는다.
    food_name: str = Field(..., min_length=1, max_length=150)

    @field_validator("food_name")
    @classmethod
    def validate_food_name(cls, food_name: str) -> str:
        return require_text(food_name, "음식 이름")


class LikeToggleResponse(BaseModel):
    """찜을 켜거나 끈 결과."""

    food_name: str

    # 누른 뒤 상태. True 면 찜한 것이다.
    liked: bool

    # 내 찜 전체 개수. 상단바 뱃지가 이 값을 쓴다.
    count: int
