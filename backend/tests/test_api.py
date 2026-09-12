"""실제 Oracle 에 붙어서 도는 통합 검증.

DB 가 꺼져 있으면 이 파일 전체를 건너뛴다(skip).
"실패"가 아니라 "안 돌렸음"으로 나와야, 코드가 깨진 것과 DB 를 안 켠 것을
구분할 수 있다.

테스트가 만든 계정과 데이터는 conftest.py 가 앞뒤로 지운다.

순서가 있는 테스트다
--------------------
회원가입 -> 로그인 -> 확정 -> 글쓰기 순으로 앞의 결과를 뒤가 쓴다.
pytest 는 파일에 적힌 순서대로 실행하므로 그 순서를 지켜서 적었다.
값은 module 수준의 STATE 에 담아 다음 테스트로 넘긴다.
"""

import base64

import pytest

from tests.conftest import TEST_EMAILS, TEST_PASSWORD, database_is_up

# DB 가 없으면 이 파일의 모든 테스트를 건너뛴다
pytestmark = pytest.mark.skipif(
    not database_is_up(),
    reason="Oracle 에 연결할 수 없습니다. DB 를 켜고 backend/.env 를 확인하세요.",
)

OWNER_EMAIL, GUEST_EMAIL = TEST_EMAILS

# 앞 테스트의 결과를 뒤 테스트가 쓴다.
STATE = {}


def bearer(token: str) -> dict:
    return {"Authorization": f"Bearer {token}"}


def count_game_plays() -> int:
    """테스트 계정이 남긴 게임 기록 수.

    API 로는 셀 수 없어서 DB 를 직접 본다. GAME_HISTORIES 는 쌓기만 하는
    로그라 조회 API 가 없고(만들 이유도 없었고), PK 도 없다.
    """
    from sqlalchemy import text

    from app.database.database import engine

    with engine.connect() as connection:
        return connection.execute(
            text(
                "SELECT COUNT(*) FROM game_histories WHERE user_id = "
                "(SELECT id FROM users WHERE email = :email AND use_yn = 'Y')"
            ),
            {"email": OWNER_EMAIL},
        ).scalar()


def signup_and_login(client, email: str, nickname: str) -> str:
    """가입하고 바로 로그인해서 토큰을 돌려준다."""
    response = client.post(
        "/api/auth/register",
        json={"email": email, "password": TEST_PASSWORD, "nickname": nickname},
    )
    assert response.status_code == 201, response.text

    response = client.post(
        "/api/auth/login", json={"email": email, "password": TEST_PASSWORD}
    )
    assert response.status_code == 200, response.text

    return response.json()["access_token"]


# =========================================================
# 로그인 없이 되는 것들
# =========================================================


def test_헬스체크가_DB까지_확인한다(client):
    response = client.get("/api/health")

    assert response.status_code == 200
    assert response.json()["database"] == "ok"


def test_카탈로그를_한_번에_내려준다(client):
    response = client.get("/api/catalog")
    body = response.json()

    assert response.status_code == 200
    for key in (
        "categories",
        "roulette",
        "games",
        "balance_questions",
        "fridge_groups",
        "search_suggestions",
        "budget_presets",
    ):
        assert len(body[key]) > 0


def test_Guest도_날씨_추천을_받는다(client):
    response = client.post("/api/recommendations", json={"limit": 5, "use_weather": True})
    body = response.json()

    assert response.status_code == 200
    assert len(body["items"]) == 5
    # use_weather 를 켰을 때만 날씨가 함께 온다
    assert body["weather"]["pick_lead"]

    STATE["food_name"] = body["items"][0]["food_name"]


def test_예산_상한이_결과에_반영된다(client):
    response = client.post("/api/recommendations", json={"limit": 8, "budget_max": 5000})

    for item in response.json()["items"]:
        assert item["estimated_cost"] <= 5000


def test_레시피와_재료_가격이_한_번에_온다(client):
    response = client.get(f"/api/recipes/{STATE['food_name']}")
    body = response.json()

    assert response.status_code == 200
    assert len(body["recipe_json"]["steps"]) > 0
    assert len(body["ingredients_json"]) > 0

    # 재료마다 가격 줄이 하나씩 따라온다. 가격을 못 구한 재료도 줄은 남는다
    # (KAMIS 는 농수산물만 조사한다). 그래야 화면이 '가격 정보 없음' 을
    # 보여주고, 합계가 전체 재료비인 것처럼 읽히지 않는다.
    #
    # offers 개수는 조사된 시장 수에 따라 매일 달라지므로 세지 않는다.
    assert len(body["prices"]) == len(body["ingredients_json"])
    for row in body["prices"]:
        assert "offers" in row


def test_재료_가격만_따로도_조회된다(client):
    response = client.post("/api/prices", json={"ingredients": ["두부", "대파"]})

    assert response.status_code == 200
    assert len(response.json()) == 2


# =========================================================
# 회원가입 · 로그인 · 내 정보
# =========================================================


def test_주소까지_넣어_가입한다(client):
    response = client.post(
        "/api/auth/register",
        json={
            "email": OWNER_EMAIL,
            "password": TEST_PASSWORD,
            "nickname": "테스트글쓴이",
            "gender": "OTHER",
            "address_sido": "서울특별시",
            "address_sigungu": "종로구",
        },
    )

    assert response.status_code == 201, response.text
    assert response.json()["address_sido"] == "서울특별시"


def test_로그인하면_토큰과_사용자가_함께_온다(client):
    response = client.post(
        "/api/auth/login", json={"email": OWNER_EMAIL, "password": TEST_PASSWORD}
    )
    body = response.json()

    assert response.status_code == 200
    assert body["user"]["email"] == OWNER_EMAIL

    STATE["owner"] = bearer(body["access_token"])


def test_처음_가입하면_1레벨이다(client):
    response = client.get("/api/users/me", headers=STATE["owner"])
    body = response.json()

    assert response.status_code == 200
    assert body["level"] == 1
    assert body["exp"] == 0
    assert body["title"] == "요리 입문"
    assert body["cook_count"] == 0


def test_닉네임만_고쳐도_주소는_그대로다(client):
    # PATCH 라서 보낸 항목만 바뀐다. PUT 이면 주소가 지워진다.
    response = client.patch(
        "/api/users/me", headers=STATE["owner"], json={"nickname": "테스트글쓴이2"}
    )
    body = response.json()

    assert response.status_code == 200
    assert body["nickname"] == "테스트글쓴이2"
    assert body["address_sido"] == "서울특별시"


# =========================================================
# Ban 이 추천에 반영되는가
# =========================================================


def test_알레르기_재료를_등록하면_추천에서_빠진다(client):
    response = client.post(
        "/api/bans",
        headers=STATE["owner"],
        json={"ingredient_name": "돼지고기", "ban_type": "ALLERGY"},
    )
    assert response.status_code == 201, response.text
    STATE["ban_id"] = response.json()["id"]

    response = client.post(
        "/api/recommendations", headers=STATE["owner"], json={"limit": 12}
    )
    body = response.json()

    names = []
    for item in body["items"]:
        names.append(item["food_name"])

    assert "김치찌개" not in names
    # 무슨 조건으로 골랐는지도 함께 알려준다
    assert "못 먹는 재료" in body["condition"]


# =========================================================
# 식사 확정 (여기서부터 DB 에 남는다)
# =========================================================


def test_레시피를_확정하면_기록에_남는다(client):
    recipe = client.get("/api/recipes/된장찌개", headers=STATE["owner"]).json()

    response = client.post(
        "/api/histories",
        headers=STATE["owner"],
        json={
            "food_name": recipe["food_name"],
            "recipe_name": recipe["recipe_name"],
            "recipe_json": recipe["recipe_json"],
            "ingredients_json": recipe["ingredients_json"],
            "substitutes_json": recipe["substitutes_json"],
            "estimated_cost": recipe["estimated_cost"],
        },
    )

    assert response.status_code == 201, response.text
    STATE["history_id"] = response.json()["id"]


def test_확정하면_요리_경험치가_오른다(client):
    response = client.get("/api/users/me", headers=STATE["owner"])
    body = response.json()

    assert body["cook_count"] == 1
    assert body["exp"] == 50  # EXP_PER_COOK


def test_기록_요약에_연속_방문일이_들어있다(client):
    response = client.get("/api/histories/stats", headers=STATE["owner"])
    body = response.json()

    assert body["count"] == 1
    assert body["streak_days"] == 1


def test_기록_목록은_페이지_정보와_함께_온다(client):
    body = client.get("/api/histories", headers=STATE["owner"]).json()

    # 기록 배열이 아니라 {items, total, page, size, total_pages} 가 온다
    assert body["items"][0]["food_name"] == "된장찌개"
    assert body["total"] == 1
    assert body["page"] == 1
    assert body["total_pages"] == 1


def test_기록_범위를_벗어난_페이지는_빈_목록이다(client):
    body = client.get(
        "/api/histories", headers=STATE["owner"], params={"page": 9999}
    ).json()

    assert body["items"] == []
    assert body["total"] == 1          # 전체 건수는 그대로 알려준다


def test_기록_한_페이지_크기에는_상한이_있다(client):
    headers = STATE["owner"]

    assert client.get("/api/histories", headers=headers, params={"size": 51}).status_code == 422
    assert client.get("/api/histories", headers=headers, params={"size": 50}).status_code == 200
    assert client.get("/api/histories", headers=headers, params={"page": 0}).status_code == 422


def test_아직_공유_안_한_기록만_고를_수_있다(client):
    """글쓰기 화면이 쓰는 필터. 아직 후기를 안 썼으므로 이 기록이 잡혀야 한다.

    화면에서 거르지 않고 서버에 맡긴 이유는 페이징 때문이다. 전체를 받아
    화면에서 걸렀다면 1페이지 20건이 전부 공유된 기록일 때 고를 것이 없다.
    """
    headers = STATE["owner"]

    not_shared = client.get(
        "/api/histories", headers=headers, params={"shared": "false"}
    ).json()
    shared = client.get(
        "/api/histories", headers=headers, params={"shared": "true"}
    ).json()

    assert not_shared["total"] == 1
    assert not_shared["items"][0]["shared_post_id"] is None
    assert shared["total"] == 0

    # 거르지 않으면 둘을 합친 만큼이다
    everything = client.get("/api/histories", headers=headers).json()
    assert everything["total"] == not_shared["total"] + shared["total"]


# =========================================================
# 게시판
# =========================================================


def test_확정한_기록으로_글을_쓴다(client):
    response = client.post(
        "/api/posts",
        headers=STATE["owner"],
        json={
            "meal_history_id": STATE["history_id"],
            "title": "테스트 후기",
            "content": "본문입니다.",
        },
    )
    body = response.json()

    assert response.status_code == 201, response.text
    # 레시피는 요청에 없었다. 서버가 원본 기록에서 JOIN 해 온 것이다.
    assert body["recipe_json"] is not None

    STATE["post_id"] = body["id"]


def test_같은_기록으로_두_번_쓸_수_없다(client):
    response = client.post(
        "/api/posts",
        headers=STATE["owner"],
        json={
            "meal_history_id": STATE["history_id"],
            "title": "중복",
            "content": "중복",
        },
    )

    assert response.status_code == 409


def test_공유한_기록은_지울_수_없다(client):
    # FK 는 진짜 DELETE 만 막는다. use_yn='N' 은 서비스 계층이 막아야 한다.
    response = client.delete(
        f"/api/histories/{STATE['history_id']}", headers=STATE["owner"]
    )

    assert response.status_code == 409


def test_내_글의_제목과_본문을_고친다(client):
    response = client.put(
        f"/api/posts/{STATE['post_id']}",
        headers=STATE["owner"],
        json={"title": "테스트 후기(수정)", "content": "고친 본문"},
    )

    assert response.status_code == 200
    assert response.json()["title"] == "테스트 후기(수정)"


def test_조회수는_별도_요청으로_오른다(client):
    # GET 상세에서 올리면 새로고침할 때마다 숫자가 바뀐다
    response = client.post(f"/api/posts/{STATE['post_id']}/view")

    assert response.status_code == 200
    assert response.json()["view_count"] == 1


def test_좋아요는_몇_번_눌러도_한_개다(client):
    post_id = STATE["post_id"]

    client.put(f"/api/posts/{post_id}/like", headers=STATE["owner"])
    response = client.put(f"/api/posts/{post_id}/like", headers=STATE["owner"])

    assert response.json()["like_count"] == 1


def test_껐다_켜도_ORA_00001이_나지_않는다(client):
    # 취소는 use_yn='N' 이라, 평범한 UNIQUE 였다면 재클릭이 막힌다
    post_id = STATE["post_id"]

    response = client.delete(f"/api/posts/{post_id}/like", headers=STATE["owner"])
    assert response.json()["like_count"] == 0

    response = client.put(f"/api/posts/{post_id}/like", headers=STATE["owner"])
    assert response.json()["like_count"] == 1


def test_댓글을_쓰고_고친다(client):
    post_id = STATE["post_id"]

    response = client.post(
        f"/api/posts/{post_id}/comments",
        headers=STATE["owner"],
        json={"content": "첫 댓글"},
    )
    assert response.status_code == 201
    STATE["comment_id"] = response.json()["id"]

    response = client.put(
        f"/api/posts/{post_id}/comments/{STATE['comment_id']}",
        headers=STATE["owner"],
        json={"content": "고친 댓글"},
    )
    assert response.status_code == 200
    assert response.json()["content"] == "고친 댓글"


def test_상세에_내_글_표시가_붙는다(client):
    response = client.get(f"/api/posts/{STATE['post_id']}", headers=STATE["owner"])
    body = response.json()

    assert body["is_mine"] is True
    assert body["liked"] is True
    assert body["comments"][0]["is_mine"] is True


def test_Guest에게는_내_글_표시가_없다(client):
    response = client.get(f"/api/posts/{STATE['post_id']}")
    body = response.json()

    assert body["is_mine"] is False
    assert body["liked"] is False


# ---------------------------------------------------------
# 첨부파일 (후기 하나당 파일 하나)
#
# 글쓰기(JSON)와 따로 주고받는다. 순서가 있는 테스트라
# 마지막에 사진을 떼면서 디스크에 남는 것도 같이 치운다.
# ---------------------------------------------------------

# 1x1 짜리 진짜 PNG. 확장자만 png 인 가짜를 쓰면 나중에 이미지 검사를
# 붙였을 때 이 테스트부터 깨진다.
PNG_1X1 = base64.b64decode(
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8"
    "z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="
)


def test_내_글에_사진을_붙인다(client):
    response = client.post(
        f"/api/posts/{STATE['post_id']}/file",
        headers=STATE["owner"],
        files={"file": ("김치찌개.png", PNG_1X1, "image/png")},
    )
    body = response.json()

    assert response.status_code == 200, response.text

    # 디스크에는 UUID 로 저장되지만, 화면에는 올린 이름 그대로 나가야 한다
    assert body["file"]["name"] == "김치찌개.png"
    assert body["file"]["ext"] == "png"
    assert body["file"]["size"] == len(PNG_1X1)
    assert body["file"]["url"] == f"/api/posts/{STATE['post_id']}/file"


def test_목록에_사진_있음_표시가_붙는다(client):
    # 목록은 파일 정보를 통째로 싣지 않는다. 붙었는지 여부만 내려준다.
    # 방금 쓴 글이라 최신순 1페이지에 있다.
    response = client.get("/api/posts")
    row = next(r for r in response.json()["items"] if r["id"] == STATE["post_id"])

    assert row["has_file"] is True


def test_사진은_로그인_없이_받을_수_있다(client):
    # 게시판 조회가 Guest 에게 열려 있으니 사진도 같이 열려야 한다
    response = client.get(f"/api/posts/{STATE['post_id']}/file")

    assert response.status_code == 200
    assert response.content == PNG_1X1
    assert response.headers["content-type"] == "image/png"

    # attachment 로 주면 <img> 에서 안 그려지고 내려받기 창이 뜬다
    assert response.headers["content-disposition"].startswith("inline")


def test_허용하지_않는_확장자는_막는다(client):
    response = client.post(
        f"/api/posts/{STATE['post_id']}/file",
        headers=STATE["owner"],
        files={"file": ("악성.exe", b"MZ-fake-exe", "application/octet-stream")},
    )

    assert response.status_code == 422


def test_사진을_떼면_상세에서도_사라진다(client):
    response = client.delete(
        f"/api/posts/{STATE['post_id']}/file", headers=STATE["owner"]
    )
    assert response.status_code == 204

    detail = client.get(f"/api/posts/{STATE['post_id']}").json()
    assert detail["file"] is None

    # 파일이 없으니 내려받기도 404 다
    assert client.get(f"/api/posts/{STATE['post_id']}/file").status_code == 404


def test_없는_첨부를_떼도_에러가_아니다(client):
    # 요청의 목적은 '첨부 없는 상태' 이고, 이미 그 상태다.
    # 좋아요 끄기(set_like)와 같은 규칙이다.
    response = client.delete(
        f"/api/posts/{STATE['post_id']}/file", headers=STATE["owner"]
    )

    assert response.status_code == 204


def test_제목_본문으로_검색된다(client):
    response = client.get("/api/posts", params={"keyword": "테스트 후기"})
    body = response.json()

    assert response.status_code == 200
    assert len(body["items"]) == 1

    # 전체 건수도 검색어를 반영해야 한다.
    # COUNT 에 검색 조건을 빼먹으면 결과는 1건인데 "12페이지" 라고 나온다.
    assert body["total"] == 1
    assert body["total_pages"] == 1


def test_목록은_페이지_정보와_함께_온다(client):
    response = client.get("/api/posts")
    body = response.json()

    assert response.status_code == 200
    assert body["page"] == 1
    assert body["size"] == 20  # DEFAULT_PAGE_SIZE
    assert len(body["items"]) <= body["size"]

    # 45건을 20개씩 보면 3페이지. 올림이 맞는지 본다
    assert body["total_pages"] == -(-body["total"] // body["size"])


def test_한_페이지_크기를_줄이면_그만큼만_온다(client):
    # 위에서 글을 최소 한 건 써뒀으므로 1건은 반드시 있다
    response = client.get("/api/posts", params={"size": 1})
    body = response.json()

    assert len(body["items"]) == 1
    assert body["size"] == 1
    assert body["total_pages"] == body["total"]


def test_다음_페이지는_앞_페이지와_겹치지_않는다(client):
    # 정렬이 흔들리면 같은 글이 1페이지와 2페이지에 다 나온다.
    # created_at 이 같은 글이 있어도 id 로 순서가 고정되는지 보는 것이다.
    first = client.get("/api/posts", params={"size": 1, "page": 1}).json()
    second = client.get("/api/posts", params={"size": 1, "page": 2}).json()

    first_ids = {row["id"] for row in first["items"]}
    second_ids = {row["id"] for row in second["items"]}

    assert first_ids & second_ids == set()


def test_범위를_벗어난_페이지는_빈_목록이다(client):
    # 404 가 아니다. 화면이 total_pages 를 보고 1페이지로 되돌리면 되는 일이라
    # 에러 페이지로 바꿀 이유가 없다.
    response = client.get("/api/posts", params={"page": 9999})
    body = response.json()

    assert response.status_code == 200
    assert body["items"] == []
    assert body["total"] > 0  # 글이 없어진 게 아니라 그 페이지가 없는 것뿐이다


def test_페이지_번호는_1부터다(client):
    # 0 이나 음수를 받으면 OFFSET 이 음수가 된다
    assert client.get("/api/posts", params={"page": 0}).status_code == 422


def test_한_페이지_크기에는_상한이_있다(client):
    # 상한이 없으면 ?size=100000 한 번으로 글 전체를 끌어올 수 있다
    assert client.get("/api/posts", params={"size": 51}).status_code == 422
    assert client.get("/api/posts", params={"size": 50}).status_code == 200


def test_인기_레시피는_기간_안의_좋아요만_센다(client):
    response = client.get("/api/posts/popular")
    rows = response.json()

    assert len(rows) == 1
    assert rows[0]["id"] == STATE["post_id"]
    assert rows[0]["like_count"] == 1

    # 좋아요가 없던 기간을 고르면 0건이어야 한다
    response = client.get(
        "/api/posts/popular", params={"start": "2020-01-01", "end": "2020-01-02"}
    )
    assert response.json() == []


# =========================================================
# 알림 (두 번째 사람이 필요하다)
# =========================================================


def test_남이_댓글과_좋아요를_달면_알림이_생긴다(client):
    guest_token = signup_and_login(client, GUEST_EMAIL, "테스트구경꾼")
    STATE["guest"] = bearer(guest_token)

    post_id = STATE["post_id"]
    client.put(f"/api/posts/{post_id}/like", headers=STATE["guest"])
    client.post(
        f"/api/posts/{post_id}/comments",
        headers=STATE["guest"],
        json={"content": "저도 해봐야겠어요"},
    )

    response = client.get("/api/users/me/notifications", headers=STATE["owner"])
    items = response.json()

    assert len(items) == 2

    kinds = []
    for item in items:
        kinds.append(item["kind"])
    assert sorted(kinds) == ["COMMENT", "LIKE"]

    for item in items:
        assert item["link"] == f"/community/{post_id}"


def test_남의_글에는_사진을_못_붙인다(client):
    # 남의 글일 때만 403 을 주면 "그 번호는 존재한다"가 새어나간다
    response = client.post(
        f"/api/posts/{STATE['post_id']}/file",
        headers=STATE["guest"],
        files={"file": ("남의글.png", PNG_1X1, "image/png")},
    )

    assert response.status_code == 404


def test_내가_한_행동은_내_알림이_아니다(client):
    # 글쓴이 본인이 누른 좋아요·댓글은 위에서 이미 걸러졌다.
    # 구경꾼 쪽에는 알림이 하나도 없어야 한다.
    response = client.get("/api/users/me/notifications", headers=STATE["guest"])

    assert response.json() == []


def test_남의_글은_고치지도_지우지도_못한다(client):
    post_id = STATE["post_id"]

    response = client.put(
        f"/api/posts/{post_id}",
        headers=STATE["guest"],
        json={"title": "가로채기", "content": "가로채기"},
    )
    # 403 이 아니라 404 다. 403 을 주면 "그 번호는 존재한다"가 새어나간다.
    assert response.status_code == 404

    response = client.delete(f"/api/posts/{post_id}", headers=STATE["guest"])
    assert response.status_code == 404

    response = client.delete(
        f"/api/histories/{STATE['history_id']}", headers=STATE["guest"]
    )
    assert response.status_code == 404


def test_받은_좋아요도_경험치가_된다(client):
    response = client.get("/api/users/me", headers=STATE["owner"])
    body = response.json()

    # 요리 50 + 글 30 + 댓글 10(내가 쓴 것) + 받은 좋아요 5x2 = 총 100
    assert body["like_received"] == 2
    assert body["post_count"] == 1

    # exp 는 총합이 아니라 "지금 레벨에서 쌓은 양"이다.
    # 100 을 채우는 순간 2레벨이 되고 exp 는 0으로 돌아간다.
    assert body["level"] == 2
    assert body["exp"] == 0
    assert body["exp_next"] == 150


# =========================================================
# 공지사항 (NOTICES 표)
#
# 전에는 catalog.json 에서 내려줬다. 운영하면서 계속 새로 쓰는 값이라
# 파일에 두면 공지 하나 올릴 때마다 서버를 다시 배포해야 해서 DB 로 옮겼다.
# =========================================================


def test_공지는_로그인_없이_볼_수_있다(client):
    response = client.get("/api/notices")

    assert response.status_code == 200

    # 공지 배열이 아니라 페이지 정보와 함께 온다
    body = response.json()
    assert isinstance(body["items"], list)
    assert body["page"] == 1
    assert body["total_pages"] == (body["total"] + body["size"] - 1) // body["size"]


def test_공지는_고정이_먼저_최신순이다(client):
    rows = client.get("/api/notices").json()["items"]

    if not rows:
        pytest.skip("공지가 없다 (db/05_seed_notices.sql 미실행)")

    # 고정(pinned)이 전부 앞쪽에 몰려 있어야 한다
    pinned = [row["pinned"] for row in rows]
    assert pinned == sorted(pinned, reverse=True)

    # 같은 고정 여부 안에서는 최신순
    for group in (True, False):
        days = [row["created_at"] for row in rows if row["pinned"] is group]
        assert days == sorted(days, reverse=True)


def test_공지는_pinned_를_불리언으로_준다(client):
    # DB 는 'Y'/'N' 이지만 화면이 문자열을 비교하게 두면 오타가 날 자리가 생긴다
    rows = client.get("/api/notices").json()["items"]

    if not rows:
        pytest.skip("공지가 없다")

    for row in rows:
        assert isinstance(row["pinned"], bool)
        assert row["category"]  # 배지에 쓸 분류가 비어 있으면 안 된다


def test_공지를_펼치면_조회수가_오른다(client):
    rows = client.get("/api/notices").json()["items"]

    if not rows:
        pytest.skip("공지가 없다")

    notice = rows[0]
    response = client.post(f"/api/notices/{notice['id']}/view")

    assert response.status_code == 200
    assert response.json()["view_count"] == notice["view_count"] + 1


def test_없는_공지는_404다(client):
    assert client.post("/api/notices/99999999/view").status_code == 404


def test_공지_한_페이지_크기를_줄이면_그만큼만_온다(client):
    body = client.get("/api/notices", params={"size": 1}).json()

    if body["total"] == 0:
        pytest.skip("공지가 없다")

    assert len(body["items"]) == 1
    assert body["size"] == 1
    assert body["total_pages"] == body["total"]


def test_공지_다음_페이지는_앞_페이지와_겹치지_않는다(client):
    first = client.get("/api/notices", params={"size": 1, "page": 1}).json()

    if first["total"] < 2:
        pytest.skip("공지가 2건 미만이다")

    second = client.get("/api/notices", params={"size": 1, "page": 2}).json()

    # 정렬에 id 가 들어가 있어야 순서가 고정된다.
    # 안 들어가면 같은 공지가 두 페이지에 다 나오거나 어디에도 안 나온다.
    assert first["items"][0]["id"] != second["items"][0]["id"]


def test_공지_범위를_벗어난_페이지는_빈_목록이다(client):
    body = client.get("/api/notices", params={"page": 9999}).json()

    # 404 가 아니다. 화면이 total_pages 를 보고 되돌리면 된다.
    assert body["items"] == []
    assert body["page"] == 9999


def test_공지_한_페이지_크기에는_상한이_있다(client):
    assert client.get("/api/notices", params={"size": 51}).status_code == 422
    assert client.get("/api/notices", params={"size": 50}).status_code == 200
    assert client.get("/api/notices", params={"page": 0}).status_code == 422


# =========================================================
# 공지 쓰기 (관리자만)
#
# 오래 "쓰기 API 가 없다" 였다. USERS 에 관리자 구분이 없어서 "운영자만" 을
# 가릴 방법이 없었기 때문이다. USER_LEVEL(1=관리자)이 생기면서 풀렸다.
#
# 테스트 계정은 가입한 그대로라 일반 회원(2)이다. 관리자가 필요한 테스트는
# set_user_level() 로 잠깐 올렸다가, 마지막 테스트가 도로 내린다.
# =========================================================


def set_user_level(email: str, level: int) -> None:
    """테스트 계정의 권한 등급을 바꾼다.

    API 로는 할 수 없어서 DB 를 직접 고친다. 관리자를 만드는 화면도 API 도
    일부러 두지 않았다 — 아무나 자기를 관리자로 올릴 수 있으면 등급이 의미가
    없어서, 운영에서도 DB 에서 직접 올린다(db/06_add_user_level.sql).
    """
    from sqlalchemy import text

    from app.database.database import engine

    with engine.begin() as connection:
        connection.execute(
            text("UPDATE users SET user_level = :level WHERE email = :email"),
            {"level": level, "email": email},
        )


def test_로그인_없이는_공지를_못_쓴다(client):
    response = client.post(
        "/api/notices", json={"title": "몰래 쓴 공지", "content": "본문"}
    )

    # 403 이 아니라 401 이다. "권한이 없다" 가 아니라 "누구인지 모르겠다" 다.
    assert response.status_code == 401


def test_일반_회원은_공지를_못_쓴다(client):
    response = client.post(
        "/api/notices",
        headers=STATE["owner"],
        json={"title": "회원이 쓴 공지", "content": "본문"},
    )

    # 404 로 숨기지 않는다. 공지 목록은 Guest 에게도 통째로 열려 있어서
    # 숨길 것이 없고, 404 를 주면 화면이 "없는 공지" 라고 잘못 안내하게 된다.
    assert response.status_code == 403


def test_일반_회원은_is_admin_이_거짓이다(client):
    me = client.get("/api/auth/me", headers=STATE["owner"]).json()

    assert me["is_admin"] is False


def test_관리자는_공지를_쓴다(client):
    set_user_level(OWNER_EMAIL, 1)

    response = client.post(
        "/api/notices",
        headers=STATE["owner"],
        json={
            "title": "pytest 점검 공지",
            "content": "본문입니다",
            "category": "점검",
            "pinned": True,
        },
    )

    assert response.status_code == 201, response.text
    body = response.json()

    assert body["title"] == "pytest 점검 공지"
    assert body["category"] == "점검"
    assert body["pinned"] is True

    # 조회수는 요청으로 받지 않는다. 읽혀서 쌓이는 값이라 0 에서 시작한다.
    assert body["view_count"] == 0

    STATE["notice_id"] = body["id"]


def test_관리자는_is_admin_이_참이다(client):
    me = client.get("/api/auth/me", headers=STATE["owner"]).json()

    assert me["is_admin"] is True


def test_분류를_안_적으면_안내다(client):
    response = client.post(
        "/api/notices",
        headers=STATE["owner"],
        json={"title": "pytest 분류 생략", "content": "본문"},
    )

    assert response.status_code == 201
    assert response.json()["category"] == "안내"

    # 고정도 기본이 꺼짐이다. 쓸 때마다 맨 위에 붙으면 고정이 뜻을 잃는다.
    assert response.json()["pinned"] is False


def test_공백만_든_제목은_거른다(client):
    response = client.post(
        "/api/notices",
        headers=STATE["owner"],
        json={"title": "   ", "content": "본문"},
    )

    # min_length=1 은 통과해버린다. Oracle 이 빈 문자열을 NULL 로 보기 때문에
    # 그냥 두면 NOT NULL 위반으로 500 이 난다. 그 전에 422 로 끊는다.
    assert response.status_code == 422


def test_고정한_공지가_목록_맨_위에_온다(client):
    rows = client.get("/api/notices").json()["items"]

    assert rows[0]["id"] == STATE["notice_id"]


def test_공지를_고쳐도_조회수는_그대로다(client):
    notice_id = STATE["notice_id"]

    viewed = client.post(f"/api/notices/{notice_id}/view").json()["view_count"]
    assert viewed == 1

    response = client.put(
        f"/api/notices/{notice_id}",
        headers=STATE["owner"],
        json={
            "title": "pytest 고친 공지",
            "content": "고친 본문",
            "category": "안내",
            "pinned": False,
        },
    )

    assert response.status_code == 200, response.text
    body = response.json()

    assert body["title"] == "pytest 고친 공지"
    assert body["category"] == "안내"
    assert body["pinned"] is False

    # 오타 하나 고쳤다고 "몇 명이 읽었나" 가 0 으로 돌아가면 안 된다
    assert body["view_count"] == viewed


def test_없는_공지는_고칠_수도_지울_수도_없다(client):
    payload = {"title": "제목", "content": "본문"}

    assert (
        client.put("/api/notices/99999999", headers=STATE["owner"], json=payload).status_code
        == 404
    )
    assert (
        client.delete("/api/notices/99999999", headers=STATE["owner"]).status_code == 404
    )


def test_관리자는_공지를_지운다(client):
    notice_id = STATE["notice_id"]

    assert client.delete(f"/api/notices/{notice_id}", headers=STATE["owner"]).status_code == 204

    # 목록에서 사라진다
    ids = [row["id"] for row in client.get("/api/notices").json()["items"]]
    assert notice_id not in ids

    # 행은 남아 있지만(use_yn='N') 조회에서는 없는 것과 같다.
    # 두 번 지우면 404 여야 한다 — 여기가 200 이면 use_yn 조건이 빠진 것이다.
    assert client.delete(f"/api/notices/{notice_id}", headers=STATE["owner"]).status_code == 404
    assert client.post(f"/api/notices/{notice_id}/view").status_code == 404


def test_관리자_자격을_잃으면_다시_못_쓴다(client):
    # 토큰은 그대로인데 등급만 내린다. 이미 발급된 토큰으로 계속 쓸 수 있으면
    # 권한을 회수할 방법이 없다 — deps 가 요청마다 DB 를 다시 본다.
    set_user_level(OWNER_EMAIL, 2)

    response = client.post(
        "/api/notices",
        headers=STATE["owner"],
        json={"title": "강등 후 공지", "content": "본문"},
    )

    assert response.status_code == 403


# =========================================================
# 미니게임 기록 (GAME_HISTORIES 표)
#
# 끝난 판이 아니라 시작한 판을 센다. 세 게임 모두 도중에 나가는 일이 흔해서,
# 끝난 것만 세면 "이 게임을 얼마나 열어보는가" 를 알 수 없다.
# =========================================================


def test_게임_세_개를_각각_기록한다(client):
    for game_key, game_type in (
        ("worldcup", "A"),
        ("balance", "B"),
        ("roulette", "C"),
    ):
        response = client.post(
            f"/api/games/{game_key}/plays", headers=STATE["owner"]
        )
        body = response.json()

        assert response.status_code == 201, response.text
        assert body["game_key"] == game_key
        assert body["game_type"] == game_type


def test_같은_게임을_여러_번_해도_다_쌓인다(client):
    # 누적 로그다. UNIQUE 도 UPSERT 도 없다.
    before = count_game_plays()

    client.post("/api/games/roulette/plays", headers=STATE["owner"])
    client.post("/api/games/roulette/plays", headers=STATE["owner"])

    assert count_game_plays() == before + 2


def test_모르는_게임은_404다(client):
    response = client.post("/api/games/tetris/plays", headers=STATE["owner"])

    assert response.status_code == 404


def test_Guest는_게임을_기록할_수_없다(client):
    # USER_ID 가 NOT NULL 이라 남길 자리가 없다.
    # 게임 자체는 Guest 도 할 수 있고, 화면이 이 API 를 안 부를 뿐이다.
    response = client.post("/api/games/worldcup/plays")

    assert response.status_code == 401


# =========================================================
# 삭제 · 비밀번호 변경 · 탈퇴
# =========================================================


def test_지운_글은_404가_된다(client):
    post_id = STATE["post_id"]

    response = client.delete(
        f"/api/posts/{post_id}/comments/{STATE['comment_id']}", headers=STATE["owner"]
    )
    assert response.status_code == 204

    response = client.delete(f"/api/posts/{post_id}", headers=STATE["owner"])
    assert response.status_code == 204

    response = client.get(f"/api/posts/{post_id}")
    assert response.status_code == 404


def test_글을_지우면_기록도_지울_수_있다(client):
    response = client.delete(
        f"/api/histories/{STATE['history_id']}", headers=STATE["owner"]
    )

    assert response.status_code == 204


def test_현재_비밀번호가_틀리면_못_바꾼다(client):
    response = client.post(
        "/api/users/me/password",
        headers=STATE["owner"],
        json={"current_password": "wrong-password", "new_password": "newpass123!"},
    )

    assert response.status_code == 400


def test_비밀번호를_바꾸면_새_값으로_로그인된다(client):
    response = client.post(
        "/api/users/me/password",
        headers=STATE["owner"],
        json={"current_password": TEST_PASSWORD, "new_password": "newpass123!"},
    )
    assert response.status_code == 200

    response = client.post(
        "/api/auth/login", json={"email": OWNER_EMAIL, "password": "newpass123!"}
    )
    assert response.status_code == 200

    STATE["owner"] = bearer(response.json()["access_token"])


def test_탈퇴_직전에_살아있는_글을_하나_만들어_둔다(client):
    """연쇄 UPDATE 를 확인하려면 탈퇴 시점에 'Y' 인 글이 있어야 한다.

    앞의 테스트들이 만든 글과 기록은 이미 지웠다. 그 상태로 탈퇴하면
    지울 것이 없어서, 연쇄 UPDATE 가 도는지 안 도는지 구분되지 않는다.
    """
    recipe = client.get("/api/recipes/김치찌개", headers=STATE["owner"]).json()

    response = client.post(
        "/api/histories",
        headers=STATE["owner"],
        json={
            "food_name": recipe["food_name"],
            "recipe_name": recipe["recipe_name"],
            "recipe_json": recipe["recipe_json"],
            "ingredients_json": recipe["ingredients_json"],
            "substitutes_json": recipe["substitutes_json"],
            "estimated_cost": recipe["estimated_cost"],
        },
    )
    assert response.status_code == 201, response.text

    response = client.post(
        "/api/posts",
        headers=STATE["owner"],
        json={
            "meal_history_id": response.json()["id"],
            "title": "탈퇴 전에 남긴 글",
            "content": "이 글은 글쓴이가 탈퇴하면 게시판에서 사라져야 한다.",
        },
    )
    assert response.status_code == 201, response.text

    STATE["farewell_post_id"] = response.json()["id"]

    # 지금은 분명히 보인다. 이게 보여야 뒤의 "사라졌다" 가 뜻을 가진다.
    assert client.get(f"/api/posts/{STATE['farewell_post_id']}").status_code == 200


def test_탈퇴하면_토큰이_바로_막힌다(client):
    response = client.request(
        "DELETE",
        "/api/users/me",
        headers=STATE["owner"],
        json={"password": "newpass123!"},
    )
    assert response.status_code == 204

    # JWT 는 취소할 수 없지만, deps 가 요청마다 use_yn 을 확인한다
    response = client.get("/api/users/me", headers=STATE["owner"])
    assert response.status_code == 401


def test_탈퇴하면_내_글이_게시판에서_사라진다(client):
    """회원 탈퇴는 하위 데이터까지 연쇄로 use_yn='N' 이어야 한다.

    FK 의 ON DELETE CASCADE 는 진짜 DELETE 에만 작동한다. UPDATE use_yn='N'
    은 DB 입장에서 그냥 값 수정이라 아무 반응도 없어서, 서비스 계층이
    직접 하위 테이블을 훑어야 한다 (db/01_schema.sql 상단 [4]).

    이걸 빠뜨리면 탈퇴한 사람의 글과 댓글이 게시판에 그대로 남는다.
    """
    post_id = STATE["farewell_post_id"]

    assert client.get(f"/api/posts/{post_id}").status_code == 404

    ids = [row["id"] for row in client.get("/api/posts").json()["items"]]
    assert post_id not in ids


def test_탈퇴해도_내가_쓴_공지는_남는다(client):
    """공지는 연쇄 대상이 아니다. 일부러 뺀 것이지 빠뜨린 것이 아니다.

    공지는 개인의 글이 아니라 운영팀이 서비스 이름으로 낸 글이라, 쓴 사람이
    그만뒀다고 '서버 점검 안내' 가 같이 사라지면 곤란하다. DDL 도 같은
    판단이라 fk_notices_user 에만 ON DELETE CASCADE 가 없다.

    앞에서 이 계정이 관리자로 올라갔을 때 공지를 썼다.
    """
    titles = [row["title"] for row in client.get("/api/notices").json()["items"]]

    assert "pytest 분류 생략" in titles


def test_탈퇴한_이메일로_다시_가입할_수_있다(client):
    # uq_users_email 이 use_yn='Y' 인 행만 보기 때문에 가능하다
    response = client.post(
        "/api/auth/register",
        json={
            "email": OWNER_EMAIL,
            "password": TEST_PASSWORD,
            "nickname": "다시가입한사람",
        },
    )

    assert response.status_code == 201
