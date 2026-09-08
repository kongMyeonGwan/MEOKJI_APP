# 커스텀 컬럼 타입은 왜 필요한가

`backend/app/models/types.py` 에 있는 `UtcTimestamp` 와 `OracleJSON` 이야기다.

이 파일은 프로젝트에서 **가장 어려운 축**에 속한다.
당장 이해가 안 돼도 다른 코드를 읽는 데는 지장이 없으니, 나중에 봐도 된다.

한 줄 요약:

> Oracle 과 파이썬 사이에서 **시간대**와 **JSON**이 자꾸 어긋난다.
> 매번 손으로 고치는 대신, 컬럼 타입 자체에 변환 규칙을 심어뒀다.

---

## 1. TypeDecorator 가 뭔가

"기존 타입을 감싸서, **넣을 때와 뺄 때 한 번씩 손보는**" 도구다.

```
   [파이썬 값]
        |
        |  process_bind_param()   <- 저장하기 직전에 손본다
        v
   [ DB 에 저장 ]
        |
        |  process_result_value() <- 읽어온 직후에 손본다
        v
   [파이썬 값]
```

두 메서드는 **우리가 부르는 게 아니다.** SQLAlchemy 가 알아서 불러준다.
모델에 이렇게 한 줄만 써두면 끝이다.

```python
created_at: Mapped[datetime] = mapped_column(UtcTimestamp(), nullable=False)
```

---

## 2. UtcTimestamp — 시간이 9시간 어긋나는 문제

### 안 만들면 무슨 일이 생기나

`python-oracledb`(Oracle 드라이버)는 **시간대 정보를 버린다.**

**저장할 때**

```python
# 파이썬에서 이렇게 넘겼는데
datetime(2026, 9, 6, 8, 0, tzinfo=timezone.utc)   # 08:00 UTC

# 드라이버가 +00:00 을 떼고 08:00 만 Oracle 에 넘긴다
# Oracle 은 "세션 시간대(한국이면 +09:00)로 해석"해서 저장한다
# 결과: 08:00+09:00 = 실제로는 UTC 23:00
```

**9시간이 어긋나는데 에러가 하나도 안 난다.** 이게 제일 나쁘다.
버그를 발견하는 시점이 "왜 어제 쓴 글이 오늘 날짜로 뜨지?" 일 때다.

**읽을 때**

```python
record.expires_at                      # tzinfo=None 으로 온다
datetime.now(timezone.utc)             # tzinfo=UTC

record.expires_at <= datetime.now(timezone.utc)
# TypeError: can't compare offset-naive and offset-aware datetimes
```

OTP 만료 검사(`password_reset_service.py`)가 여기서 터진다.

### 그래서 어떻게 했나

**규칙을 하나로 못 박았다: DB 에는 UTC 만 넣는다.**

| 구간 | 하는 일 |
|---|---|
| `database.py` 의 `set_session_timezone` | 연결될 때마다 세션 시간대를 UTC 로 고정 |
| DDL (`db/01_schema.sql`) 의 DEFAULT | `SYS_EXTRACT_UTC(SYSTIMESTAMP)` — DB 가 채우는 값도 UTC |
| `process_bind_param` | 파이썬 값을 UTC 로 환산하고 시간대 표시를 뗀다 |
| `process_result_value` | 읽은 값에 UTC 표시를 다시 붙인다 |

네 군데가 다 같은 약속을 지켜야 성립한다. 하나만 빠져도 어긋난다.

### `load_dialect_impl` 은 왜 있나

SQLAlchemy 는 Oracle 에서 `DateTime` 을 `DATE` 컬럼으로 만든다.
그런데 **Oracle 의 DATE 는 초 단위까지만** 저장한다.

OTP 만료는 5분이라 초 단위로도 되긴 하지만,
같은 초에 발급/검증이 일어나면 비교가 애매해진다.
그래서 Oracle 일 때만 `TIMESTAMP` 로 바꿔치기한다.

---

## 3. OracleJSON — 레시피를 통째로 저장하기

AI 가 만든 레시피는 구조가 이렇게 생겼다.

```json
{
  "steps": ["재료를 씻는다", "물을 끓인다"],
  "tips": "면은 마지막에 넣으세요"
}
```

이걸 테이블 컬럼으로 쪼개려면 `recipe_steps` 테이블을 또 만들어야 한다.
MVP 단계에서는 **JSON 통째로 한 칸에** 넣는 편이 훨씬 빠르다.

### 문제

Oracle 21c 에 JSON 타입이 있긴 한데, **SQLAlchemy 의 Oracle 지원이 아직 못 따라온다.**
`sqlalchemy.JSON` 을 쓰면 이렇게 터진다.

```
CompileError: (in table 'meal_histories', column 'recipe_json'):
Compiler can't render element of type JSON
```

### 해결

- DB 쪽: `CLOB` (긴 문자열) + `CHECK (recipe_json IS JSON)` 제약
- 파이썬 쪽: `OracleJSON` 이 `json.dumps` / `json.loads` 를 대신해준다

덕분에 코드에서는 그냥 딕셔너리처럼 쓴다.

```python
history.recipe_json["steps"][0]     # 변환 코드가 한 줄도 없다
```

### 두 가지 함정

**`ensure_ascii=False`**

기본값(`True`)이면 한글이 `감자` 같은 코드로 저장된다.
용량이 6배가 되고, DBeaver 로 열어봐도 사람이 읽을 수가 없다.

**이미 dict 인 값이 오는 경우**

```python
if not isinstance(value, (str, bytes, bytearray)):
    return value
```

같은 행을 `Session.get()` 으로 **두 번째** 읽을 때,
이미 변환된 dict 가 그대로 들어와서 `json.loads(dict)` → `TypeError` 가 났다.
(`select()` 로 읽는 경로에서는 재현되지 않는다.)
그래서 문자열일 때만 변환하도록 방어선을 뒀다.

---

## 4. 직접 확인해보기

**실험 1 — 시간대를 정말 지키는지**

```python
# backend 에서: python
from app.database.database import SessionLocal
from app.models.user import User

db = SessionLocal()
user = db.query(User).first()
print(user.created_at)          # tzinfo=UTC 가 붙어 있는지 확인
print(user.created_at.tzinfo)   # datetime.timezone.utc
```

**실험 2 — 일부러 깨뜨려보기**

`types.py` 의 `process_result_value` 에서 `.replace(tzinfo=timezone.utc)` 를 지우고
비밀번호 재설정을 해본다. `TypeError` 가 나는 걸 눈으로 봐야 기억에 남는다.
(확인한 뒤에는 반드시 되돌린다.)

**실험 3 — JSON 이 어떻게 저장돼 있는지**

DBeaver 에서 직접 본다.

```sql
SELECT recipe_json FROM meal_histories WHERE ROWNUM = 1;
```

파이썬에서는 dict 였는데 DB 에는 문자열로 들어있는 걸 확인한다.

---

## 5. 스스로 답해보기

- `UtcTimestamp` 를 안 쓰고 그냥 `DateTime` 을 쓰면 무슨 일이 생기나?
- `process_bind_param` 에서 `tzinfo` 를 떼는 이유는?
- Oracle 의 `DATE` 대신 `TIMESTAMP` 를 쓰는 이유는?
- `ensure_ascii=False` 를 빼면 DB 에 뭐가 저장되나?
- 논리 삭제(`use_yn='N'`)를 쓰면 조회 코드에서 뭘 절대 빠뜨리면 안 되나?
