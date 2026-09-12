"""데모 데이터 채우기 — 회원 · 공지 · 식사 기록 · 후기(첨부 포함).

빈 DB 로는 화면을 볼 수가 없다. 게시판도 공지도 비어 있고, 글을 쓰려면
식사를 먼저 확정해야 해서 손으로 채우려면 한참 걸린다. 이 스크립트가
그 한 바퀴를 대신 돌아준다.

사용법 (backend 디렉터리에서):
    .venv\\Scripts\\activate
    python -m scripts.seed_demo

만드는 것
    1) 회원 2명   운영자(관리자) · 일반 회원
    2) 공지 4건   관리자가 쓴다. 공지에는 첨부파일이 없다
    3) 식사 기록 3건
    4) 후기 1건   첨부 없음
    5) 후기 1건   첨부 있음 (그림은 아래에서 직접 그린다)

왜 DB 에 직접 INSERT 하지 않나
------------------------------
서버를 거쳐야 검증기 · FK · UNIQUE 가 실제로 돌아간다. INSERT 로 넣으면
화면에서는 멀쩡해 보이는데 API 로는 만들 수 없는 데이터가 섞인다.
덤으로 "이 흐름이 지금도 도는가" 가 한 번 확인된다.

여러 번 돌려도 안전하다. 이미 있는 것은 건너뛴다.

지우려면
    scripts/seed_demo.py 의 ADMIN · MEMBER 이메일로 만든 계정을 지우면 된다.
    자식부터 지워야 FK 에 걸리지 않는다 — 순서는 tests/conftest.py 의
    CLEANUP_SQL 을 그대로 쓰면 된다.
"""

import math
import os
import struct
import sys
import time
import zlib
from pathlib import Path

from app.core.config import settings

# AI 는 mock 으로 고정한다. .env 가 gemini 로 켜져 있어도 데모 데이터를
# 만들자고 API 할당량과 돈을 쓸 이유가 없다. 레시피 모양은 어느 쪽이든 같다.
settings.AI_PROVIDER = "mock"

# 가격 조회도 막는다. DATA_GO_KR_SERVICE_KEY 가 비어 있으면
# GET /api/recipes 가 재료마다 KAMIS 를 부르다 500 으로 죽는다.
# 시세는 DB 에 저장되지도 않는 값이라 데모 데이터와 아무 상관이 없다.
from app.integrations import price_api  # noqa: E402
from app.services import bargain_service  # noqa: E402


async def _no_price_lookup(client, name):
    """kamis_offers 와 같은 모양으로 돌려준다 — (조사가격, 가격추이)."""
    return [], None


price_api.kamis_offers = _no_price_lookup

# 서버가 뜰 때 도는 시세 훑기(품목 101개)도 건너뛴다. 이미 훑은 것처럼 채워둔다.
bargain_service.bargain_cache = (time.time(), [])

from fastapi.testclient import TestClient  # noqa: E402
from sqlalchemy import text  # noqa: E402

from app.database.database import engine  # noqa: E402
from app.main import app  # noqa: E402

# db/02_seed.sql · docs/DB_SETUP.md 와 같은 개발용 비밀번호다.
# 다른 값을 쓰고 싶으면 환경변수로 덮어쓸 수 있다:
#     set SEED_PASSWORD=...   (Windows)   /   export SEED_PASSWORD=...
#
# 이 값이 저장소에 적혀 있어도 되는 이유는 아래 local_only() 가 로컬 밖에서는
# 아예 실행을 막기 때문이다. 그 가드가 없으면 이 줄은 그냥 공개된 비밀번호다.
PASSWORD = os.environ.get("SEED_PASSWORD") or "meokji1234!"


def local_only() -> None:
    """로컬이 아니면 아무것도 하지 않고 멈춘다.

    이 스크립트는 회원을 만들고 그중 하나를 **관리자로 올린다**. 비밀번호는
    저장소에 적혀 있는 개발용 값이라, 운영이나 공용 DB 에서 돌면 누구나
    README 만 읽고 관리자로 로그인할 수 있게 된다.

    기존 시드(db/02_seed.sql)는 일반 회원만 만들어서 이 위험이 없었다.
    관리자를 만드는 건 이 스크립트가 처음이라 가드도 여기서 시작한다.

    DB 를 보고 판단하지 않고 APP_ENV 로 판단한다. 로컬 DB 와 운영 DB 가
    겉보기에 다르지 않을 수 있고, "비어 있으면 로컬이겠지" 같은 추측은
    한 번 틀리면 돌이킬 수 없다.
    """
    if settings.APP_ENV != "local":
        raise SystemExit(
            f"APP_ENV 가 '{settings.APP_ENV}' 입니다. 이 스크립트는 로컬에서만 "
            "돌릴 수 있습니다.\n"
            "저장소에 적힌 개발용 비밀번호로 관리자 계정을 만들기 때문입니다.\n"
            "정말 필요하면 backend/.env 의 APP_ENV 를 local 로 두고 다시 실행하세요."
        )


# db/05_seed_notices.sql 이 공지 작성자로 찾는 바로 그 이메일이다.
#
# .local 같은 주소는 쓸 수 없다. 가입 API 의 EmailStr 이 예약 도메인이라며
# 422 로 막아서 그 주소로는 계정이 아예 만들어지지 않는다.
ADMIN = {
    "email": "admin@meokji.com",
    "nickname": "뭐먹지운영팀",
    "address_sido": "서울특별시",
    "address_sigungu": "강남구",
}

# 후기는 일반 회원이 쓰는 글이다. 한 계정으로 다 하면 "운영팀이 자기 후기를
# 올린" 데이터가 되어 화면이 실제와 달라진다.
MEMBER = {
    "email": "minji@example.com",
    "nickname": "민지의부엌",
    "gender": "FEMALE",
    "address_sido": "서울특별시",
    "address_sigungu": "마포구",
}

NOTICES = [
    {
        "title": "커뮤니티 이용 규칙 안내",
        "content": (
            "서로 존중하는 말투로 소통해주세요.\n"
            "레시피는 자세히 공유해주실수록 좋아요.\n"
            "링크 및 홍보성 글은 사전 안내 없이 삭제될 수 있습니다.\n"
            "신고는 관리자 확인 후 조치됩니다."
        ),
        "pinned": True,
    },
    {
        "title": "9월 15일(월) 새벽 서버 점검 안내 (02:00~04:00)",
        "content": (
            "추천 기능 개선을 위한 서버 점검이 예정되어 있습니다.\n"
            "점검 시간 동안에는 로그인과 레시피 추천을 이용하실 수 없습니다.\n"
            "작성 중인 글은 미리 저장해두시길 권해드립니다."
        ),
        "pinned": True,
    },
    {
        "title": "못 먹는 재료 등록 기능이 추가되었습니다",
        "content": (
            "마이페이지에서 알레르기 재료와 싫어하는 재료를 등록할 수 있습니다.\n"
            "알레르기로 등록한 재료는 추천에서 완전히 제외되고,\n"
            "싫어하는 재료는 가능한 한 대체재료로 바꿔 제안합니다."
        ),
        "pinned": False,
    },
    {
        "title": "가격 정보는 참고용입니다",
        "content": (
            "재료 가격은 KAMIS 소매 조사가격을 기준으로 보여드립니다.\n"
            "실제 결제 금액은 구입처와 판매자 사정에 따라 달라질 수 있습니다."
        ),
        "pinned": False,
    },
]

FOODS = ["김치찌개", "된장찌개", "크림 파스타"]

POSTS = [
    {
        "food": "김치찌개",
        "title": "묵은지 있으면 무조건 이거 끓이세요",
        "content": (
            "냉장고에 반년 묵은 김치가 굴러다녀서 끓여봤는데 결과가 좋았습니다.\n\n"
            "돼지고기를 먼저 기름 나올 때까지 볶고 김치를 넣어 같이 볶다가 물을 부으면\n"
            "국물 색이 확실히 진해집니다. 처음부터 다 넣고 끓이면 이 맛이 안 나요.\n\n"
            "두부는 마지막 5분에 넣는 게 좋습니다. 오래 끓이면 부서집니다.\n"
            "재료비는 8천원이 안 나왔는데 두 끼를 먹었습니다."
        ),
        "attach": False,
    },
    {
        "food": "크림 파스타",
        "title": "생크림 없이 우유로 만든 크림파스타 (사진 있음)",
        "content": (
            "생크림을 사러 나가기 귀찮아서 우유로 해봤습니다. 생각보다 됩니다.\n\n"
            "우유는 끓이면 분리되니까 약불로만 데우고, 면수를 두 국자 정도 섞어주면\n"
            "농도가 잡힙니다. 면수의 전분이 생크림 역할을 대신해줍니다.\n\n"
            "치즈는 불을 끄고 넣어야 덩어리지지 않아요.\n"
            "접시에 담고 후추를 갈아 올린 게 아래 사진입니다."
        ),
        "attach": True,
    },
]

ATTACH_NAME = "크림파스타.png"


# ==========================================================
# 첨부용 그림
#
# 이미지 파일을 저장소에 같이 두지 않고 여기서 그린다. 첨부 기능을 확인하는
# 데 필요한 건 "진짜 열리는 이미지" 하나뿐인데, 그걸 위해 바이너리를 커밋하면
# diff 에도 안 잡히는 파일이 하나 늘어난다.
#
# Pillow 를 쓰지 않는다. 이 스크립트 하나 때문에 의존성을 늘릴 이유가 없어서
# zlib + struct 로 PNG 를 직접 만든다.
# ==========================================================


def draw_pasta_png(width: int = 640, height: int = 420) -> bytes:
    """접시에 담긴 파스타 그림 한 장을 PNG 바이트로 만든다."""
    center_x, center_y = width / 2, height / 2 + 10

    rows = bytearray()

    for y in range(height):
        rows.append(0)  # PNG 필터 타입 0 (None)

        for x in range(width):
            # 세로로 눕힌 타원이 되게 y 쪽을 눌러준다
            dx, dy = x - center_x, (y - center_y) * 1.35
            distance = math.hypot(dx, dy)

            if distance < 96:
                # 면 가닥. 각도에 따라 밝기를 흔들어 감아 올린 결을 만든다
                wave = math.sin(math.atan2(dy, dx) * 9 + distance * 0.16)
                tone = 236 + int(14 * wave) - (22 if distance > 88 else 0)
                pixel = (min(255, tone), min(255, tone - 28), max(0, tone - 118))
            elif distance < 150:
                pixel = (252, 250, 246)  # 접시
            elif distance < 158:
                pixel = (226, 216, 202)  # 접시 테두리
            else:
                # 배경. 위에서 아래로 따뜻하게 어두워진다
                t = y / height
                pixel = (250 - int(28 * t), 243 - int(34 * t), 232 - int(40 * t))

            rows.extend(bytes(pixel))

    def chunk(tag: bytes, data: bytes) -> bytes:
        crc = zlib.crc32(tag + data) & 0xFFFFFFFF
        return struct.pack(">I", len(data)) + tag + data + struct.pack(">I", crc)

    return (
        b"\x89PNG\r\n\x1a\n"
        # 8비트 트루컬러(color type 2)
        + chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 2, 0, 0, 0))
        + chunk(b"IDAT", zlib.compress(bytes(rows), 9))
        + chunk(b"IEND", b"")
    )


# ==========================================================
# 도우미
# ==========================================================


def step(number: str, title: str) -> None:
    print(f"\n[{number}] {title}")


def ok(message: str) -> None:
    print(f"  OK  {message}")


def skip(message: str) -> None:
    print(f"  --  {message}")


def signup(client: TestClient, profile: dict) -> dict:
    """가입하고 로그인해서 Authorization 헤더를 돌려준다.

    이미 있는 계정이면 409 가 오는데, 그때는 로그인만 한다.
    스크립트를 다시 돌렸다고 실패로 끝낼 이유가 없다.
    """
    response = client.post("/api/auth/register", json={**profile, "password": PASSWORD})

    if response.status_code == 201:
        ok(f"가입 {profile['email']} ({profile['nickname']})")
    elif response.status_code == 409:
        skip(f"이미 있는 계정 {profile['email']} — 로그인만 한다")
    else:
        raise SystemExit(f"가입 실패 {response.status_code}: {response.text}")

    response = client.post(
        "/api/auth/login", json={"email": profile["email"], "password": PASSWORD}
    )
    if response.status_code != 200:
        raise SystemExit(f"로그인 실패 {response.status_code}: {response.text}")

    return {"Authorization": f"Bearer {response.json()['access_token']}"}


def promote(email: str) -> None:
    """관리자로 올린다. DB 를 직접 고치는 유일한 대목이다.

    아무나 자기를 관리자로 올릴 수 있으면 등급이 의미가 없어서, 관리자를
    만드는 화면도 API 도 일부러 두지 않았다(db/06_add_user_level.sql).
    """
    with engine.begin() as connection:
        connection.execute(
            text("UPDATE users SET user_level = 1 WHERE email = :email"),
            {"email": email},
        )


# ==========================================================
# 본문
# ==========================================================


def seed_notices(client: TestClient, headers: dict) -> None:
    """2) 공지. 관리자만 쓸 수 있다.

    공지에는 첨부파일이 없다. NOTICES 표에 파일 컬럼 자체가 없고 붙일 API 도
    없다 — 첨부는 후기(RECIPE_REVIEWS)에만 있다.
    """
    # 목록 API 는 배열이 아니라 {items, total, ...} 로 온다 (STEP 31).
    # 시드가 넣는 공지는 몇 건뿐이라 1페이지만 봐도 충분하다.
    existing = {row["title"] for row in client.get("/api/notices").json()["items"]}

    for notice in NOTICES:
        if notice["title"] in existing:
            skip(f"이미 있음 — {notice['title']}")
            continue

        response = client.post("/api/notices", headers=headers, json=notice)
        if response.status_code != 201:
            raise SystemExit(f"공지 실패 {response.status_code}: {response.text}")

        body = response.json()
        ok(f"{'고정 ' if body['pinned'] else ''}{body['title']}")


def seed_histories(client: TestClient, headers: dict) -> dict:
    """3) 식사 기록. 화면과 같은 순서로 간다.

    레시피를 먼저 받아서(GET /api/recipes) 그 값을 그대로 확정한다
    (POST /api/histories). 확정한 순간의 레시피가 스냅샷으로 남는다.
    """
    histories = {
        row["food_name"]: row
        for row in client.get("/api/histories", headers=headers).json()["items"]
    }

    for food in FOODS:
        if food in histories:
            skip(f"이미 있음 — {food}")
            continue

        recipe = client.get(f"/api/recipes/{food}", headers=headers)
        if recipe.status_code != 200:
            raise SystemExit(f"레시피 실패 {recipe.status_code}: {recipe.text}")

        recipe = recipe.json()

        response = client.post(
            "/api/histories",
            headers=headers,
            json={
                "food_name": recipe["food_name"],
                "recipe_name": recipe["recipe_name"],
                "recipe_json": recipe["recipe_json"],
                "ingredients_json": recipe["ingredients_json"],
                "substitutes_json": recipe["substitutes_json"],
                "estimated_cost": recipe["estimated_cost"],
            },
        )
        if response.status_code != 201:
            raise SystemExit(f"확정 실패 {response.status_code}: {response.text}")

        saved = response.json()
        histories[food] = saved
        ok(f"{saved['food_name']} — {saved['recipe_name']} ({saved['estimated_cost']:,}원)")

    return histories


def seed_posts(client: TestClient, headers: dict, histories: dict) -> None:
    """4·5) 후기. 하나는 첨부 없이, 하나는 첨부를 붙여서."""
    for item in POSTS:
        history = histories[item["food"]]

        # 한 기록에는 후기를 한 번만 쓸 수 있다(uq_reviews_meal).
        # 이미 썼으면 shared_post_id 가 채워져 있으니 그 글을 이어서 쓴다.
        post_id = history.get("shared_post_id")

        if post_id:
            skip(f"이미 공유한 기록 — #{post_id} {item['title']}")
        else:
            response = client.post(
                "/api/posts",
                headers=headers,
                json={
                    "meal_history_id": history["id"],
                    "title": item["title"],
                    "content": item["content"],
                },
            )
            if response.status_code != 201:
                raise SystemExit(f"글쓰기 실패 {response.status_code}: {response.text}")

            post_id = response.json()["id"]
            ok(f"#{post_id} {item['title']}")

        if not item["attach"]:
            skip("   첨부 없음")
            continue

        # 이미 붙어 있으면 다시 올리지 않는다. 다시 올려도 교체라 문제는
        # 없지만, 돌릴 때마다 같은 그림이 디스크에 하나씩 쌓인다.
        if client.get(f"/api/posts/{post_id}").json().get("file"):
            skip("   첨부 이미 있음")
            continue

        # 글쓰기(JSON)와 파일 업로드가 따로인 이유는 post_service 상단 참고.
        # 업로드가 실패해도 남는 건 '첨부 없는 멀쩡한 글' 이고, 첨부는 원래
        # 선택 사항이라 그게 정상 상태다.
        response = client.post(
            f"/api/posts/{post_id}/file",
            headers=headers,
            files={"file": (ATTACH_NAME, draw_pasta_png(), "image/png")},
        )
        if response.status_code != 200:
            raise SystemExit(f"첨부 실패 {response.status_code}: {response.text}")

        attached = response.json()["file"]
        ok(f"   첨부 {attached['name']} ({attached['size_text']}) -> {attached['url']}")


def main() -> None:
    local_only()

    with TestClient(app) as client:
        step("1", "회원 생성")
        admin = signup(client, ADMIN)
        promote(ADMIN["email"])
        ok(f"{ADMIN['email']} 를 관리자(user_level=1)로 올림")

        member = signup(client, MEMBER)

        # 등급을 바꿨으니 토큰을 다시 받는다. 토큰 안에 등급이 들어 있는 건
        # 아니지만(요청마다 DB 를 본다), is_admin 이 실린 user 는 로그인
        # 응답에 담겨오므로 화면이 쓰려면 새로 받아야 한다.
        admin = signup(client, ADMIN)
        ok(f"is_admin = {client.get('/api/auth/me', headers=admin).json()['is_admin']}")

        step("2", "공지사항 작성 (관리자 · 첨부 없음)")
        seed_notices(client, admin)

        step("3", "히스토리에 식사 기록 넣기 (회원)")
        histories = seed_histories(client, member)

        step("4·5", "레시피 후기 작성 (첨부 없는 글 · 첨부 있는 글)")
        seed_posts(client, member, histories)

        print("\n[결과]")
        notices = client.get("/api/notices").json()["items"]
        page = client.get("/api/posts").json()
        stats = client.get("/api/histories/stats", headers=member).json()

        print(f"  공지      {len(notices)} 건 (고정 {sum(n['pinned'] for n in notices)})")
        print(f"  게시글    {page['total']} 건 (첨부 {sum(p['has_file'] for p in page['items'])})")
        print(f"  식사 기록 {stats['count']} 건 · 누적 {stats['total_cost']:,}원")
        print(f"\n  로그인    {ADMIN['email']} (관리자) / {MEMBER['email']}")


if __name__ == "__main__":
    # python -m scripts.seed_demo 로 부르면 backend 가 이미 경로에 있다.
    # 파일을 직접 실행했을 때를 위해 한 번 더 넣어둔다.
    sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
    main()
