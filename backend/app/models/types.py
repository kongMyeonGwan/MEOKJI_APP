"""Oracle 전용 컬럼 타입 두 가지."""

import json
from datetime import datetime, timezone

from sqlalchemy import DateTime, Text, TypeDecorator
from sqlalchemy.dialects.oracle import TIMESTAMP as OracleTIMESTAMP

# 논리 삭제 플래그.
USE_YN_YES = "Y"
USE_YN_NO = "N"


class UtcTimestamp(TypeDecorator):
    """날짜/시간을 항상 UTC 로 주고받는 컬럼 타입."""

    impl = DateTime   # 기본은 DateTime 을 흉내낸다
    cache_ok = True   # SQLAlchemy 가 이 타입이 쓰인 쿼리를 캐시해도 된다는 표시

    def load_dialect_impl(self, dialect):
        """DB 종류에 따라 실제로 어떤 컬럼 타입을 쓸지 정한다."""
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
    """JSON 을 다루는 컬럼 타입."""

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
        if not isinstance(value, (str, bytes, bytearray)):
            return value

        return json.loads(value)
