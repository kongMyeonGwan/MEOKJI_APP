"""시·도 / 시·군·구 목록과 그 중심 좌표.

무엇에 쓰나
-----------
회원가입·정보수정에서 주소를 **드롭다운으로** 고르게 하고,
고른 주소를 **좌표로 바꿔** users.latitude / longitude 에 저장한다.
그 좌표로 대시보드 날씨를 조회한다.

왜 좌표를 미리 구워뒀나
-----------------------
"서울특별시 강남구" 를 좌표로 바꾸는 건 바깥 API(Nominatim) 일이고 2~3초 걸린다.
가입할 때마다 부르면 회원가입이 그만큼 느려지고, 그 API 가 죽으면 가입이 막힌다.

그런데 시·군·구는 목록이 고정이고 좌표도 변하지 않는다.
그래서 regions.json 을 만들 때 **한 번만** 변환해서 넣어뒀고,
서비스가 돌 때는 그냥 표에서 찾기만 한다.

    scratchpad/build_regions.py  ->  regions.json  (일회성, 저장소에 커밋됨)

주소를 자유 입력이 아니라 목록에서 고르게 한 이유도 여기에 있다.
"서울시" / "서울특별시" / "서울" 이 뒤섞이면 어느 것도 좌표로 못 바꾼다.
"""

import json
from pathlib import Path

REGIONS_PATH = Path(__file__).resolve().parent / "regions.json"

with REGIONS_PATH.open(encoding="utf-8") as regions_file:
    REGIONS: list[dict] = json.load(regions_file)


def list_regions() -> list[dict]:
    """화면에 내려줄 목록. 좌표는 빼고 이름만 준다.

    좌표는 서버가 쓰는 값이라 화면에 보낼 이유가 없다.
    250개가 넘는 항목마다 숫자 두 개씩 붙으면 응답만 커진다.
    """
    result = []

    for sido in REGIONS:
        names = []
        for sigungu in sido["sigungu"]:
            names.append(sigungu["name"])

        result.append({"name": sido["name"], "sigungu": names})

    return result


def find_coords(sido: str | None, sigungu: str | None) -> tuple[float, float] | None:
    """주소를 좌표로 바꾼다. 목록에 없으면 None.

    시·군·구까지 고르면 그 좌표를, 시·도만 고르면 시·도 중심 좌표를 준다.
    세종특별자치시처럼 시·군·구가 없는 곳도 시·도 좌표로 답이 나온다.
    """
    if not sido:
        return None

    for row in REGIONS:
        if row["name"] != sido:
            continue

        if sigungu:
            for child in row["sigungu"]:
                if child["name"] == sigungu:
                    return child["lat"], child["lon"]

        # 시·군·구를 안 골랐거나 목록에 없으면 시·도 중심으로 떨어뜨린다.
        # 좌표가 아예 없는 것보다 시청 근처 날씨라도 보여주는 편이 낫다.
        if "lat" in row:
            return row["lat"], row["lon"]

        return None

    return None
