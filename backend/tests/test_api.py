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


def test_공지는_고정글이_위로_온다(client):
    response = client.get("/api/catalog/notices")

    assert response.status_code == 200
    assert response.json()[0]["pinned"] is True


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


def test_제목_본문으로_검색된다(client):
    response = client.get("/api/posts", params={"keyword": "테스트 후기"})

    assert response.status_code == 200
    assert len(response.json()) == 1


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
