"""Oracle 전용 컬럼 타입.

SQLAlchemy/드라이버 기본 동작이 Oracle에서 의도와 다르게 매핑되는 것들을
여기서 한 번 감싸고, 모든 모델이 이 타입을 사용한다.
"""

import json
from datetime import datetime, timezone

from sqlalchemy import DateTime, Text, TypeDecorator
from sqlalchemy.dialects.oracle import TIMESTAMP as OracleTIMESTAMP

# USE_YN (논리 삭제) 플래그. DDL의 ck_*_use_yn CHECK 제약과 값이 일치해야 한다.
# UI의 "삭제" / "회원 탈퇴"는 DELETE가 아니라 이 값을 'N'으로 바꾸는 것이다.
# 조회에는 반드시 use_yn == USE_YN_YES 조건을 붙인다.
USE_YN_YES = "Y"
USE_YN_NO = "N"


class UtcTimestamp(TypeDecorator):
    """항상 UTC로 저장하고, 읽을 때 tz-aware(UTC) datetime으로 돌려준다.

    왜 필요한가
    -----------
    python-oracledb는 타임존 정보를 **저장/조회 양방향에서 버린다.**

      - 저장: aware datetime 08:00+00:00 을 넘겨도 tzinfo가 잘려나가고,
              Oracle이 세션 타임존(+09:00)을 적용해 08:00+09:00 으로 저장한다.
              → 실제 시각이 9시간 어긋나는데 아무 에러도 나지 않는다.
      - 조회: TIMESTAMP WITH TIME ZONE 컬럼도 tzinfo=None 으로 돌아온다.
              → aware datetime과 비교하면 TypeError.

    그래서 컬럼 타입을 WITH TIME ZONE으로 둬도 얻는 게 없다.
    DB에는 UTC만 저장하고(DDL의 DEFAULT도 SYS_EXTRACT_UTC), 경계에서 변환한다.
    """

    impl = DateTime
    cache_ok = True

    def load_dialect_impl(self, dialect):
        # SQLAlchemy는 Oracle에서 DateTime을 DATE로 렌더링한다.
        # Oracle DATE는 초 단위까지만 저장해 OTP 만료(5분) 비교에 쓸 수 없다.
        if dialect.name == "oracle":
            return dialect.type_descriptor(OracleTIMESTAMP())
        return dialect.type_descriptor(DateTime())

    def process_bind_param(self, value: datetime | None, dialect) -> datetime | None:
        if value is None:
            return None
        if value.tzinfo is not None:
            value = value.astimezone(timezone.utc)
        # naive로 넘겨야 드라이버가 손대지 않는다. 이 시점의 값은 항상 UTC다.
        return value.replace(tzinfo=None)

    def process_result_value(self, value: datetime | None, dialect) -> datetime | None:
        if value is None:
            return None
        # DB에는 UTC만 들어있다는 전제 하에 tzinfo를 복원한다.
        return value.replace(tzinfo=timezone.utc)


class OracleJSON(TypeDecorator):
    """CLOB에 JSON 문자열을 저장하고, 파이썬에서는 dict/list로 다룬다.

    Oracle 21c에 네이티브 JSON 타입이 있지만 SQLAlchemy의 Oracle dialect가
    아직 지원하지 않는다(visit_JSON 미구현 -> CompileError).
    그래서 DDL은 CLOB + CHECK(... IS JSON)으로 만들고 여기서 직렬화를 담당한다.
    """

    impl = Text
    cache_ok = True

    def process_bind_param(self, value, dialect):
        if value is None:
            return None
        # ensure_ascii=False : 한글을 \uXXXX로 이스케이프하지 않는다.
        # 켜두면 CLOB 용량이 6배가 되고 DBeaver에서 육안 확인이 불가능해진다.
        return json.dumps(value, ensure_ascii=False)

    def process_result_value(self, value, dialect):
        if value is None:
            return None
        # 이미 파싱된 값이 그대로 올 때가 있다.
        # Session.get() 으로 엔티티를 두 번째로 읽을 때부터 재현되며
        # (select() 경로에서는 나지 않는다) json.loads(dict) -> TypeError 가 났다.
        # 문자열일 때만 파싱하고, 아니면 그대로 흘려보낸다.
        if not isinstance(value, (str, bytes, bytearray)):
            return value
        return json.loads(value)
