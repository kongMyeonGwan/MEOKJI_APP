"""테스트 공통 준비물.

여기 있는 함수들은 pytest 가 알아서 찾아 쓴다. 직접 import 하지 않는다.

DB 가 필요한 테스트와 아닌 테스트
---------------------------------
    test_logic.py    DB 없이 도는 순수 계산 검증 (레벨 · 가격 · 카탈로그 추천)
    test_api.py      실제 Oracle 에 붙어서 도는 통합 검증

DB 가 꺼져 있으면 test_api.py 는 통째로 건너뛴다(skip).
"실패"가 아니라 "안 돌렸음"으로 나와야, 코드가 깨진 것과 DB 를 안 켠 것을
구분할 수 있다.

실행
----
    cd backend
    .venv\\Scripts\\activate
    pytest                  전부
    pytest tests/test_logic.py   DB 없이 되는 것만
"""

import time

import pytest
from sqlalchemy import text
from sqlalchemy.exc import SQLAlchemyError

from app.core.config import settings
from app.integrations import price_api
from app.services import bargain_service

# 테스트가 공공데이터포털을 실제로 때리지 않게 막는다.
#
# 때리게 두면 (1) 느려지고 (2) 일일 할당량을 깎고 (3) 조사일 데이터가
# 바뀌면 어제 통과하던 테스트가 오늘 깨진다. 외부 상태에 기대는 테스트는
# 코드가 깨진 건지 남의 서버가 바뀐 건지 구분해주지 못한다.
#
# 바깥으로 나가는 조회 자체를 막는다.
#
# 코드표를 비우는 방법도 써봤는데, 그러면 이름 맞추기 테스트가 같이
# 죽는다(찾을 표가 없으니 전부 None). 데이터가 아니라 네트워크를
# 막아야 둘 다 산다.
#
# 빈 배열은 실제로도 나오는 값이다 — KAMIS 에 없는 가공식품이 그렇다.
# 그래서 '가격 없는 재료' 경로가 그대로 확인된다.
settings.PRICE_PROVIDERS = "kamis"


async def _no_lookup(client, name):
    # kamis_offers 와 같은 모양으로 돌려준다 — (조사가격, 가격추이)
    return [], None


price_api.kamis_offers = _no_lookup

# 서버가 뜰 때 도는 시세 훑기도 건너뛴다.
#
# TestClient 를 만들 때마다 lifespan 이 돌면서 품목 101개를 훑는다.
# 위에서 조회를 막아뒀으니 네트워크는 안 타지만, 그래도 테스트마다
# 101번을 헛도는 건 낭비다. 이미 훑어둔 것처럼 비워서 채워둔다.
bargain_service.bargain_cache = (time.time(), [])

# 이 이메일로 만든 데이터는 테스트가 끝날 때 전부 지운다.
# 실제 사용자와 겹치지 않도록 도메인을 example.com 으로 둔다.
TEST_EMAILS = (
    "pytest_owner@example.com",
    "pytest_guest@example.com",
)

TEST_PASSWORD = "pytest1234!"

# 자식부터 지워야 FK 에 걸리지 않는다.
# 회원 탈퇴(use_yn='N')와 달리 테스트 뒷정리는 진짜 DELETE 다.
# 논리 삭제만 하면 테스트를 돌릴 때마다 행이 쌓이고,
# uq_users_email 이 use_yn='Y' 만 보므로 재가입 테스트도 계속 통과해버린다.
CLEANUP_SQL = (
    "DELETE FROM review_comments WHERE review_id IN "
    "(SELECT id FROM recipe_reviews WHERE user_id = :uid)",
    "DELETE FROM review_likes WHERE review_id IN "
    "(SELECT id FROM recipe_reviews WHERE user_id = :uid)",
    "DELETE FROM review_comments WHERE user_id = :uid",
    "DELETE FROM review_likes WHERE user_id = :uid",
    "DELETE FROM recipe_reviews WHERE user_id = :uid",
    "DELETE FROM meal_histories WHERE user_id = :uid",
    # 게임 기록에는 PK 가 없다. user_id 로만 지운다 — 테스트 계정의 판은
    # 어차피 전부 이번 실행에서 만든 것이라 그걸로 충분하다.
    "DELETE FROM game_histories WHERE user_id = :uid",
    # 공지도 지운다. 쓰기 API 가 생기면서 테스트가 공지를 만들게 됐다.
    #
    # user_id 로 좁히는 것이 중요하다. 조건 없이 지우면 시드해둔 진짜 공지가
    # 같이 날아간다. 테스트 계정이 쓴 것만 골라 지운다.
    #
    # 이 줄이 없으면 아래 users DELETE 가 ORA-02292 로 막힌다.
    # fk_notices_user 에는 ON DELETE CASCADE 가 없다 — 운영자 계정이 사라진다고
    # 공지가 같이 사라지면 안 되기 때문이다(models/notice.py 참고).
    "DELETE FROM notices WHERE user_id = :uid",
    "DELETE FROM recipe_likes WHERE user_id = :uid",
    "DELETE FROM user_bans WHERE user_id = :uid",
    "DELETE FROM verification_codes WHERE user_id = :uid",
    "DELETE FROM users WHERE id = :uid",
)


def database_is_up() -> bool:
    """DB 에 붙을 수 있는지 확인한다."""
    from app.database.database import engine

    try:
        with engine.connect() as connection:
            # Oracle 은 FROM 절 없는 SELECT 를 허용하지 않는다 (DUAL 필수)
            connection.execute(text("SELECT 1 FROM DUAL"))
        return True
    except SQLAlchemyError:
        return False


def remove_test_users() -> None:
    """테스트가 만든 계정과 그 하위 데이터를 물리 삭제한다."""
    from app.database.database import engine

    with engine.begin() as connection:
        rows = connection.execute(
            text("SELECT id FROM users WHERE email IN (:a, :b)"),
            {"a": TEST_EMAILS[0], "b": TEST_EMAILS[1]},
        )
        user_ids = list(rows.scalars().all())

        for user_id in user_ids:
            for statement in CLEANUP_SQL:
                connection.execute(text(statement), {"uid": user_id})


@pytest.fixture(scope="session", autouse=True)
def use_catalog_provider():
    """테스트가 도는 동안에는 AI 를 catalog 로 고정한다.

    .env 가 gemini/claude 로 켜져 있어도 테스트는 바깥을 부르지 않는다.
    이유가 셋 있다.
      - 돌릴 때마다 API 할당량과 돈이 나간다
      - 남의 서버가 붐비면(503) 우리 코드는 멀쩡한데 테스트가 빨개진다
      - AI 는 같은 입력에 다른 답을 준다. "이 조건이면 이 결과" 를 검증할 수 없다

    진짜 AI 응답이 우리 스키마에 맞는지는 테스트가 아니라 실제로 한 번
    호출해서 확인할 일이다.
    """
    from app.core.config import settings

    original = settings.AI_PROVIDER
    settings.AI_PROVIDER = "catalog"
    yield
    settings.AI_PROVIDER = original


@pytest.fixture(scope="session")
def client():
    """FastAPI 를 서버로 띄우지 않고 직접 호출하는 테스트용 클라이언트.

    uvicorn 을 따로 켤 필요가 없다. 포트를 안 쓰므로 다른 사람이 8000 번을
    쓰고 있어도 테스트가 돌아간다.
    """
    from fastapi.testclient import TestClient

    from app.main import app

    with TestClient(app) as test_client:
        yield test_client


@pytest.fixture(scope="session", autouse=True)
def clean_database():
    """테스트 전후로 테스트 계정을 지운다.

    autouse=True 라 테스트가 직접 요청하지 않아도 자동으로 실행된다.
    앞에서도 지우는 이유는, 지난번 테스트가 중간에 죽어 데이터가 남아 있으면
    회원가입부터 409 로 실패하기 때문이다.
    """
    if not database_is_up():
        yield
        return

    remove_test_users()
    yield
    remove_test_users()
