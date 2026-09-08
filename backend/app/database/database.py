"""DB 연결 준비 (Oracle 21c XE).

여기서 만드는 것은 세 가지다.

    engine         DB 로 가는 실제 연결. 앱 전체에서 하나만 만든다
    SessionLocal   요청 하나가 쓸 "작업 창구"를 찍어내는 공장
    Base           모든 테이블 모델(app/models/*.py)이 상속하는 부모 클래스

엔진과 세션의 차이
------------------
engine 은 "DB 로 가는 전화선"이고, Session 은 "지금 통화 중인 한 건"이다.
전화선은 비싸니까 앱 시작할 때 몇 개 만들어 두고 돌려쓰고(커넥션 풀),
통화는 요청마다 새로 걸었다 끊는다(get_db).
"""

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
    """DB 에 새로 연결될 때마다 그 연결의 시간대를 UTC 로 고정한다.

    @event.listens_for 는 "engine 에 connect 라는 일이 생기면 이 함수를 불러줘" 라는
    뜻이다. 우리가 직접 부르는 함수가 아니라 SQLAlchemy 가 알아서 불러준다.

    왜 필요한가
    -----------
    Oracle 은 시간대 정보가 없는 값을 받으면 "접속한 세션의 시간대"로 해석한다.
    그러면 한국(+09:00)에서 켠 서버와 UTC 로 맞춰둔 서버가 같은 값을 서로 다르게
    저장하게 된다. 여기서 UTC 로 못 박아두면 누가 어디서 켜도 결과가 같다.
    """
    cursor = dbapi_connection.cursor()
    try:
        cursor.execute("ALTER SESSION SET TIME_ZONE='+00:00'")
    finally:
        cursor.close()


# 세션(작업 창구)을 찍어내는 공장.
#   autoflush=False   내가 commit 할 때까지 DB 로 안 보낸다 (예상치 못한 SQL 방지)
#   autocommit=False  commit() 을 직접 불러야 저장된다
SessionLocal = sessionmaker(bind=engine, autoflush=False, autocommit=False)


class Base(DeclarativeBase):
    """모든 테이블 모델의 부모 클래스.

    app/models/*.py 의 클래스들이 이걸 상속하면
    SQLAlchemy 가 "이건 테이블이구나" 하고 알아본다.

    주의: Base.metadata.create_all() 은 부르지 않는다.
    테이블 설계의 원본은 db/01_schema.sql 이고, 파이썬은 그걸 따라갈 뿐이다.
    양쪽에서 만들면 어느 쪽이 진짜인지 알 수 없게 된다.
    """


def get_db():
    """요청 하나가 쓸 DB 세션을 만들어 주고, 끝나면 닫는다.

    라우터에서 Depends(get_db) 로 받아 쓴다.

    return 이 아니라 yield 인 이유
    ------------------------------
    yield 는 "여기까지 만든 걸 넘겨주고, 쓸 때까지 기다렸다가 이어서 실행" 이다.
    그래서 흐름이 이렇게 된다.

        db = SessionLocal()   세션을 연다
        yield db              라우터가 이 세션으로 일한다
        db.close()            라우터가 끝나면 여기로 돌아와서 닫는다

    finally 에 넣었으므로 라우터에서 에러가 나도 세션은 반드시 닫힌다.
    안 닫으면 커넥션 풀이 금방 바닥나서 서버가 멈춘다.
    """
    db = SessionLocal()
    try:
        yield db
    finally:
        db.close()
