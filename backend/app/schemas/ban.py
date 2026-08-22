"""Ban 재료 요청/응답 스키마 (STEP 9)."""

from datetime import datetime
from typing import Literal

from pydantic import BaseModel, ConfigDict, Field, field_validator

# DDL의 ck_user_bans_type과 같은 값만 허용한다.
# Literal로 막아두면 잘못된 값이 DB까지 가지 않고 422로 끊긴다.
# (CHECK 제약에 걸려 ORA-02290 -> 500이 나는 것보다 낫다)
BanType = Literal["DISLIKE", "ALLERGY"]


class BanCreateRequest(BaseModel):
    # min_length=1: Oracle은 빈 문자열을 NULL로 취급하므로
    # ''를 그대로 넘기면 ORA-01400(NOT NULL 위반)으로 500이 난다.
    ingredient_name: str = Field(..., min_length=1, max_length=100)
    ban_type: BanType = "DISLIKE"

    @field_validator("ingredient_name")
    @classmethod
    def strip_name(cls, v: str) -> str:
        """공백만 입력한 재료명을 거른다.

        앞뒤 공백도 여기서 떼어낸다. '오이'와 '오이 '가 서로 다른 값으로
        저장되면 uq_user_bans가 중복을 못 막는다.
        """
        stripped = v.strip()
        if not stripped:
            raise ValueError("재료 이름을 입력해주세요")
        return stripped


class BanResponse(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: int
    ingredient_name: str
    ban_type: BanType
    created_at: datetime
    # user_id는 내려주지 않는다. 어차피 "내 Ban"만 조회되므로 불필요하다.
