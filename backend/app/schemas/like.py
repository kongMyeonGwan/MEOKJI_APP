"""찜한 레시피 스키마.

목록은 음식 이름만 내려준다
---------------------------
화면(LikesContext)이 들고 있어야 하는 것은 "이 음식이 찜 목록에 있는가" 뿐이다.
하트를 칠할지, 개수를 몇으로 보일지가 전부 이 판단에서 나온다.

찜한 시각이나 행 id 는 화면이 쓰지 않는다. 내려주면 화면이 안 쓰는 값을
들고 다니게 되고, 나중에 그 값을 믿고 쓰는 코드가 생긴다.
"""

from pydantic import BaseModel, ConfigDict, Field, field_validator

from app.schemas.post import require_text


class LikeListResponse(BaseModel):
    """내 찜 목록 전체.

    배열이 아니라 한 겹 감쌌다. 페이징 때문이 아니라 count 를 같이 주기
    위해서다 — 상단바가 하트 옆에 개수를 그리는데, 화면이 배열 길이를 세면
    나중에 목록을 잘라 보낼 때 숫자가 조용히 틀려진다.
    """

    model_config = ConfigDict(from_attributes=True)

    # 찜한 음식 이름들. 최근에 찜한 것이 앞에 온다.
    names: list[str]

    count: int


class LikeToggleRequest(BaseModel):
    """찜하기 · 찜 해제 요청.

    켜고 끄는 것을 한 엔드포인트로 받는다. 화면의 동작이 하트 하나를 누르는
    것뿐이라, PUT 과 DELETE 로 나누면 화면이 "지금 켜져 있나"를 먼저 알아야
    부를 주소를 정할 수 있다. 그 판단을 서버가 하는 편이 낫다.
    """

    # DDL 이 VARCHAR2(150 CHAR) 라 그 이상은 받지 않는다.
    food_name: str = Field(..., min_length=1, max_length=150)

    @field_validator("food_name")
    @classmethod
    def validate_food_name(cls, food_name: str) -> str:
        return require_text(food_name, "음식 이름")


class LikeToggleResponse(BaseModel):
    """찜을 켜거나 끈 결과.

    게시글 좋아요(LikeResponse)와 같은 모양이다 — 바뀐 상태와 전체 개수를
    함께 준다. 화면이 개수를 직접 세지 않게 하려는 것이다.
    """

    food_name: str

    # 누른 뒤 상태. True 면 찜한 것이다.
    liked: bool

    # 내 찜 전체 개수. 상단바 뱃지가 이 값을 쓴다.
    count: int
