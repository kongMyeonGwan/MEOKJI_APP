"""SQLAlchemy 엔진 / 세션 / Base 정의 (Oracle 21c XE)."""

from collections.abc import Generator

from sqlalchemy import create_engine, event
from sqlalchemy.orm import DeclarativeBase, Session, sessionmaker

from app.core.config import settings

engine = create_engine(
    settings.database_url,
    echo=settings.DB_ECHO,
    pool_pre_ping=True,      # 끊어진 커넥션을 재사용하지 않도록 사전 확인
    pool_size=settings.DB_POOL_SIZE,
    max_overflow=settings.DB_MAX_OVERFLOW,
    pool_recycle=1800,       # 유휴 커넥션 30분 후 재생성 (방화벽/XE 세션 정리 대비)
)
# 팀 공용 XE는 동시 세션 수에 제약이 있다.
# 팀원 수 x (pool_size + max_overflow) 가 한도를 넘지 않도록 작게 잡았다.

@event.listens_for(engine, "connect")
def _set_session_timezone(dbapi_conn, _record) -> None:
    """모든 커넥션의 세션 타임존을 UTC로 고정한다.

    python-oracledb는 바인딩 시 tzinfo를 버리고, Oracle은 그 값을
    세션 타임존으로 해석한다. 접속한 PC의 로케일에 따라 저장 시각이
    달라지는 것을 막기 위해 UTC로 못박는다.
    """
    cursor = dbapi_conn.cursor()
    try:
        cursor.execute("ALTER SESSION SET TIME_ZONE='+00:00'")
    finally:
        cursor.close()


SessionLocal = sessionmaker(bind=engine, autoflush=False, autocommit=False)


class Base(DeclarativeBase):
    """모든 모델의 부모 클래스. STEP 5부터 models/ 에서 상속받는다.

    스키마의 단일 소스는 db/01_schema.sql 이다.
    Base.metadata.create_all() 은 호출하지 않는다.
    """


def get_db() -> Generator[Session, None, None]:
    """FastAPI 의존성. 요청마다 세션을 열고 끝나면 닫는다."""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
