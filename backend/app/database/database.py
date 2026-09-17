"""DB 연결 준비 (Oracle 21c XE)."""

from sqlalchemy import create_engine, event
from sqlalchemy.orm import DeclarativeBase, sessionmaker

from app.core.config import settings

engine = create_engine(
    settings.database_url,
    echo=settings.DB_ECHO,       # True 면 실행되는 SQL 이 콘솔에 다 찍힌다 (.env 에서 켠다)
    pool_pre_ping=True,          # 꺼내 쓰기 전에 살아있는 연결인지 확인한다
    pool_size=settings.DB_POOL_SIZE,
    max_overflow=settings.DB_MAX_OVERFLOW,
    pool_recycle=1800,           # 30분 놀고 있던 연결은 버리고 새로 만든다
)
# 팀이 같이 쓰는 XE 는 동시 접속 수에 제한이 있다.
# 팀원 수 x (pool_size + max_overflow) 가 그 한도를 넘지 않게 작게 잡았다.


@event.listens_for(engine, "connect")
def set_session_timezone(dbapi_connection, connection_record) -> None:
    """DB 에 새로 연결될 때마다 그 연결의 시간대를 UTC 로 고정한다."""
    cursor = dbapi_connection.cursor()
    try:
        cursor.execute("ALTER SESSION SET TIME_ZONE='+00:00'")
    finally:
        cursor.close()


# 세션(작업 창구)을 찍어내는 공장.
SessionLocal = sessionmaker(bind=engine, autoflush=False, autocommit=False)


class Base(DeclarativeBase):
    """모든 테이블 모델의 부모 클래스."""


def get_db():
    """요청 하나가 쓸 DB 세션을 만들어 주고, 끝나면 닫는다."""
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
