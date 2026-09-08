"""Oracle 전용 컬럼 타입 두 가지.

SQLAlchemy 의 기본 동작이 Oracle 에서는 우리 뜻과 다르게 나오는 게 두 군데 있다.
여기서 한 번 감싸두고, 모든 모델이 이 타입을 가져다 쓴다.

    UtcTimestamp   날짜/시간 컬럼. 항상 UTC 로 저장하고 UTC 로 읽는다
    OracleJSON     JSON 컬럼. 파이썬에서는 dict/list 로, DB 에는 문자열로

왜 이런 게 필요한지, 안 만들면 무슨 일이 생기는지는 아래 문서에 정리해 뒀다.
    docs/STUDY_COLUMN_TYPES.md

이 파일을 다 이해하지 못해도 나머지 코드를 읽는 데는 지장이 없다.
모델에서 `created_at: Mapped[datetime] = mapped_column(UtcTimestamp(), ...)` 처럼
쓰기만 하면, 시간대 변환은 이 파일이 알아서 해준다.
"""

import json
from datetime import datetime, timezone

from sqlalchemy import DateTime, Text, TypeDecorator
from sqlalchemy.dialects.oracle import TIMESTAMP as OracleTIMESTAMP

# 논리 삭제 플래그.
#
# 이 프로젝트는 DELETE 로 행을 지우지 않는다.
# 화면의 "삭제" · "회원 탈퇴" 는 전부 use_yn 을 'N' 으로 바꾸는 UPDATE 다.
# 행이 그대로 남아 있으므로, 조회할 때는 반드시 use_yn == USE_YN_YES 를 붙여야 한다.
# 이걸 빠뜨리면 지운 글이 목록에 다시 나타난다.
USE_YN_YES = "Y"
USE_YN_NO = "N"


class UtcTimestamp(TypeDecorator):
    """날짜/시간을 항상 UTC 로 주고받는 컬럼 타입.

    파이썬 -> DB : 어떤 시간대로 들어와도 UTC 로 바꿔서 저장한다
    DB -> 파이썬 : UTC 라는 표시를 붙여서 돌려준다

    TypeDecorator 는 "기존 타입을 감싸서 넣고 뺄 때 한 번씩 손보는" 도구다.
    아래 세 메서드는 우리가 부르는 게 아니라 SQLAlchemy 가 알아서 불러준다.
    자세한 배경은 docs/STUDY_COLUMN_TYPES.md 참고.
    """

    impl = DateTime   # 기본은 DateTime 을 흉내낸다
    cache_ok = True   # SQLAlchemy 가 이 타입이 쓰인 쿼리를 캐시해도 된다는 표시

    def load_dialect_impl(self, dialect):
        """DB 종류에 따라 실제로 어떤 컬럼 타입을 쓸지 정한다.

        Oracle 에서 그냥 DateTime 을 쓰면 DATE 컬럼이 되는데,
        Oracle 의 DATE 는 초 단위까지만 저장한다.
        OTP 만료(5분) 비교처럼 밀리초가 필요한 곳에서 문제가 되므로
        TIMESTAMP 로 바꿔준다.
        """
        if dialect.name == "oracle":
            return dialect.type_descriptor(OracleTIMESTAMP())
        return dialect.type_descriptor(DateTime())

    def process_bind_param(self, value, dialect):
        """저장하기 직전에 값을 손본다 (파이썬 -> DB)."""
        if value is None:
            return None

        # 시간대가 붙어 있으면 UTC 로 환산한다.
        # 예: 17:00+09:00 (한국) -> 08:00+00:00 (UTC)
        if value.tzinfo is not None:
            value = value.astimezone(timezone.utc)

        # 시간대 표시를 떼고 넘긴다.
        # 붙여서 넘기면 드라이버가 제멋대로 해석하는데, 이 시점의 값은 이미 UTC 다.
        return value.replace(tzinfo=None)

    def process_result_value(self, value, dialect):
        """읽어온 값을 손본다 (DB -> 파이썬)."""
        if value is None:
            return None

        # DB 에는 UTC 만 들어있다는 전제로, 떨어져 나간 시간대 표시를 다시 붙인다.
        # 이게 없으면 datetime.now(timezone.utc) 와 비교할 때 TypeError 가 난다.
        return value.replace(tzinfo=timezone.utc)


class OracleJSON(TypeDecorator):
    """JSON 을 다루는 컬럼 타입.

    파이썬 -> DB : dict/list 를 JSON 문자열로 바꿔서 저장한다
    DB -> 파이썬 : JSON 문자열을 dict/list 로 되돌려준다

    덕분에 모델을 쓰는 쪽에서는 그냥 파이썬 딕셔너리처럼 다루면 된다.

        history.recipe_json["steps"][0]

    Oracle 21c 에 JSON 타입이 있긴 하지만 SQLAlchemy 가 아직 지원하지 않는다.
    그래서 DB 쪽은 CLOB(긴 문자열) 로 만들고 변환은 여기서 담당한다.
    """

    impl = Text
    cache_ok = True

    def process_bind_param(self, value, dialect):
        """저장하기 직전 (파이썬 dict -> JSON 문자열)."""
        if value is None:
            return None

        # ensure_ascii=False : 한글을 감자 같은 코드로 바꾸지 않는다.
        # True 로 두면 용량이 6배가 되고, DBeaver 에서 눈으로 확인할 수가 없다.
        return json.dumps(value, ensure_ascii=False)

    def process_result_value(self, value, dialect):
        """읽어온 직후 (JSON 문자열 -> 파이썬 dict)."""
        if value is None:
            return None

        # 가끔 이미 dict 로 변환된 값이 그대로 올 때가 있다.
        # (같은 행을 Session.get() 으로 두 번째 읽을 때 재현된다.)
        # 그때 json.loads(dict) 를 부르면 TypeError 가 나므로,
        # 문자열일 때만 변환하고 아니면 그대로 넘긴다.
        if not isinstance(value, (str, bytes, bytearray)):
            return value

        return json.loads(value)
