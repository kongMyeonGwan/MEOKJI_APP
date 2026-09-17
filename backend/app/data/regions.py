"""시·도 / 시·군·구 목록과 그 중심 좌표."""

import json
from pathlib import Path

REGIONS_PATH = Path(__file__).resolve().parent / "regions.json"

with REGIONS_PATH.open(encoding="utf-8") as regions_file:
    REGIONS: list[dict] = json.load(regions_file)


def list_regions() -> list[dict]:
    """화면에 내려줄 목록."""
    result = []

    for sido in REGIONS:
        names = []
        for sigungu in sido["sigungu"]:
            names.append(sigungu["name"])

        result.append({"name": sido["name"], "sigungu": names})

    return result


def find_coords(sido: str | None, sigungu: str | None) -> tuple[float, float] | None:
    """주소를 좌표로 바꾼다."""
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
