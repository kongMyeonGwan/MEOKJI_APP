# Oracle 과 자바 사이에서 어긋나는 두 가지

시각(`TIMESTAMP`)과 JSON(`CLOB`) 이야기다.

이 프로젝트에서 **가장 조용히 틀리는 축**에 속한다. 둘 다 에러가 안 난다.
값이 그냥 9시간 밀려 있거나, 화면에 따옴표 붙은 문자열이 나올 뿐이다.

한 줄 요약:

> 규칙을 하나로 못 박고, **그 규칙을 적용하는 자리를 한 곳으로 모았다.**
> 시각은 `util/Times` 와 `util/Kst`, JSON 은 `util/Json` 이다.

---

## 0. 파이썬은 컬럼 타입에 심었고, 스프링은 밖에 뒀다

파이썬 시절에는 `TypeDecorator` 라는 걸 썼다. **컬럼 타입 자체에 변환 규칙을
심어서** 모델에 한 줄만 쓰면 끝이었다.

```python
created_at: Mapped[datetime] = mapped_column(UtcTimestamp(), nullable=False)
#                                            ^^^^^^^^^^^^ 넣을 때·뺄 때 알아서 변환
```

MyBatis 에도 비슷한 `TypeHandler` 가 있다. **그런데 쓰지 않았다.**

| | 컬럼 타입에 심기 | 지금 방식 (밖에서 변환) |
|---|---|---|
| 코드 양 | 적다. 변환 코드가 안 보인다 | 조금 많다. 서비스에서 한 줄씩 부른다 |
| 읽을 때 | **어디서 변환되는지 안 보인다** | `Json.node(...)` 가 눈에 보인다 |
| 디버깅 | 값이 이상하면 ORM 내부까지 봐야 한다 | 변환하는 줄에 중단점을 걸면 된다 |

옮기는 동안 두 서버의 응답을 글자 단위로 비교해야 했다. 그때 **"어디서 값이
바뀌는가" 가 보이는 쪽**이 훨씬 편했다. 지금도 그대로 두고 있다.

정답이 있는 문제는 아니다. 표가 늘고 변환이 반복되면 `TypeHandler` 로 옮기는
게 맞을 수도 있다.

---

## 1. 시각 — 9시간이 조용히 어긋나는 문제

### 안 맞추면 무슨 일이 생기나

Oracle 의 `TIMESTAMP` 컬럼에는 **시간대 정보가 없다.** `2026-09-06 08:00` 이
UTC 인지 한국 시각인지 컬럼만 봐서는 알 수 없다.

그래서 넣는 쪽과 읽는 쪽이 다르게 생각하면 9시간이 어긋난다.

```
DDL 의 DEFAULT        SYS_EXTRACT_UTC(SYSTIMESTAMP)   -> UTC 로 들어간다
애플리케이션이 넣는 값   LocalDateTime.now()            -> JVM 시간대(한국)로 들어간다
```

**같은 표 안에 9시간 어긋난 행이 섞인다. 에러는 하나도 안 난다.**
버그를 발견하는 시점이 "왜 어제 쓴 글이 오늘 날짜로 뜨지?" 일 때다.

### 그래서 어떻게 했나

**규칙을 하나로 못 박았다: DB 에는 UTC 만 넣는다.**

| 구간 | 하는 일 | 어디 |
|---|---|---|
| 커넥션 | 세션 시간대를 UTC 로 고정 | `application.yml` 의 `connection-init-sql` |
| DDL 의 DEFAULT | DB 가 채우는 값도 UTC | `db/01_schema.sql` 의 `SYS_EXTRACT_UTC` |
| UPDATE 문 | 직접 `SYS_EXTRACT_UTC(SYSTIMESTAMP)` 를 쓴다 | 매퍼 XML |
| 응답으로 내보낼 때 | UTC 로 읽고 `Z` 를 붙인다 | `util/Times.toText()` |

```yaml
hikari:
  # 이걸 빼면 오라클 JDBC 가 JVM 기본 시간대(한국)를 세션 시간대로 잡는다.
  connection-init-sql: ALTER SESSION SET TIME_ZONE='+00:00'
```

**네 군데가 다 같은 약속을 지켜야 성립한다.** 하나만 빠져도 어긋난다.

DDL 의 `DEFAULT` 는 INSERT 때만 적용된다. 그래서 UPDATE 에서는 매퍼가 직접
써야 한다 — `UserMapper.xml` 의 `updated_at = SYS_EXTRACT_UTC(SYSTIMESTAMP)` 가 그것이다.

### `Times` 가 글자까지 맞추는 이유

`LocalDateTime` 을 Jackson 에 그냥 넘기면 `.955Z` 처럼 **뒤의 0 을 떼어낸다.**
파이썬(Pydantic)은 `.955000Z` 로 내보냈다.

```java
"created_at": "2026-09-11T21:29:42.955000Z"   // 파이썬
"created_at": "2026-09-11T21:29:42.955Z"      // 그냥 두면 자바
```

화면은 `new Date()` 로 읽어서 둘 다 똑같이 동작한다. 그런데 **두 서버의 응답을
기계적으로 비교하는 동안**에는 글자가 다르면 매번 사람이 눈으로 확인해야 한다.
그래서 `Times.toText()` 가 마이크로초 여섯 자리를 맞춘다.

규칙까지 같게 뒀다 — 마이크로초가 0 이면 소수점 자리를 아예 쓰지 않는다
(`2026-09-11T21:29:42Z`). Pydantic 의 `isoformat()` 이 그렇게 동작한다.

### `Kst` — 사용자가 보는 날짜는 한국 날짜다

DB 에는 UTC 만 있는데 화면의 "오늘" 은 한국 날짜다. 이 9시간을 안 맞추면
**경계에서 결과가 어긋난다.**

```
연속 방문일        새벽 0시~9시에 만든 기록이 전날 것으로 세어진다
인기 레시피 기간    밤 9시 이후에 누른 좋아요가 다음 날로 넘어간다
```

두 곳에서 같은 변환이 필요해서 `util/Kst` 에 모았다. 각자 계산하면 한쪽만
고치는 실수가 난다.

---

## 2. JSON — 레시피를 통째로 저장하기

레시피는 구조가 이렇게 생겼다.

```json
{
  "steps": ["재료를 씻는다", "물을 끓인다"],
  "tips": "면은 마지막에 넣으세요"
}
```

이걸 테이블 컬럼으로 쪼개려면 `recipe_steps` 표를 또 만들어야 한다.
확정한 순간의 레시피를 **스냅샷으로 남기는 것**이 목적이라 통째로 한 칸에 넣는다.

### 컬럼은 CLOB + CHECK 다

```sql
recipe_json  CLOB,
CONSTRAINT ck_histories_recipe_json CHECK (recipe_json IS JSON)
```

Oracle 21c 에 JSON 타입이 따로 있지만 쓰지 않았다. `CLOB + CHECK` 면 **형식이
깨진 값은 DB 가 막아주면서** 드라이버·ORM 호환 문제도 없다.

### 자바에서는 String 과 JsonNode 를 나눠 들었다

DTO 를 없애고 VO 하나로 합친 뒤로는 `history/vo/MealHistoryVO` 한 클래스 안에서 나눈다.

```
recipeJson          String     DB 와 주고받는 칸 (@JsonIgnore — 화면에는 안 나간다)
getRecipeJsonNode() JsonNode   화면과 주고받는 칸 (@JsonProperty("recipe_json"))
        ↑ 그 사이를 util/Json 이 잇는다
```

```java
// 화면으로 내보낼 때 (getter)
public JsonNode getRecipeJsonNode() {
    return Json.node(recipeJson);
}

// 화면이 보낸 값을 받을 때 (setter)
public void setRecipeJsonNode(JsonNode node) {
    this.recipeJson = Json.text(node);
}
```

### 왜 화면으로 나가는 칸이 String 이면 안 되나

화면으로 나가는 칸이 `String` 이면 JSON 안에 **따옴표 붙은 문자열**이 나간다.

```json
"recipe_json": "{\"steps\":[\"재료를 씻는다\"]}"     ← 이러면
"recipe_json": { "steps": ["재료를 씻는다"] }        ← 이래야 한다
```

앞쪽이면 React 가 `recipe_json.steps` 를 읽지 못한다. **에러는 안 난다.**
`undefined` 를 받아서 그대로 그린다 — 조리 순서가 빈 채로 화면이 뜬다.

### 깨진 값 하나 때문에 목록 전체가 죽지 않게

`Json.node()` 는 파싱에 실패하면 **null 을 돌려준다.**

```java
try {
    return MAPPER.readTree(text);
} catch (JacksonException exc) {
    return null;
}
```

DDL 의 CHECK 가 막아주므로 정상적으로는 일어나지 않는다. 다만 제약이 걸리기
전에 들어간 옛 행이 있을 수 있다. **그 한 건 때문에 목록 전체가 500 이 되는
것보다 그 칸만 비는 편이 낫다.**

---

## 3. 직접 확인해보기

**실험 1 — 커넥션 시간대를 빼본다**

`application.yml` 의 `connection-init-sql` 줄을 주석 처리하고 서버 재시작.
찜을 하나 하고 DBeaver 에서 본다.

```sql
SELECT food_name, created_at FROM recipe_likes ORDER BY id DESC FETCH FIRST 3 ROWS ONLY;
```

- 예상: 방금 넣은 행의 시각이 어떻게 찍힐까?
- **핵심**: 에러가 안 난다. 9시간 밀린 값이 조용히 들어간다.
- 확인 후 반드시 되돌리고, **잘못 들어간 행은 지운다.**

**실험 2 — `Times.toText()` 를 안 쓰면**

`HistoryService` 에서 `Times.toText(...)` 대신 `LocalDateTime` 을 그대로 넘겨보고
`GET /api/histories` 응답을 본다.

- 예상: `created_at` 이 어떤 모양으로 나올까?
- 확인: `src/test/resources/fixtures/histories_list.json` 과 글자가 같은가?
  `gradlew test` 를 돌리면 어느 테스트가 깨지는가?

**실험 3 — JSON 이 DB 에 어떻게 들어있나**

```sql
SELECT recipe_json FROM meal_histories WHERE ROWNUM = 1;
```

자바에서는 `JsonNode` 로 다뤘는데 DB 에는 문자열로 들어있는 것을 확인한다.

**실험 4 — CHECK 제약을 건드려본다**

```sql
UPDATE meal_histories SET recipe_json = '{망가진' WHERE id = (SELECT MIN(id) FROM meal_histories);
```

- 예상: 이 UPDATE 가 될까?
- **핵심**: `ORA-02290` 으로 막힌다. 형식 검증을 DB 가 하고 있다.

**실험 5 — `Kst` 를 안 쓰면**

`UserService` 의 연속 방문일 계산에서 `Kst.toDate(...)` 대신
`LocalDateTime.toLocalDate()` 를 써본다. 새벽 0시~9시 사이에 만든 기록이 있어야
차이가 보인다.

- **핵심**: 하루 중 9시간 동안만 틀린다. 낮에 테스트하면 멀쩡해 보인다.

---

## 4. 스스로 답해보기

- `connection-init-sql` 을 빼면 무슨 일이 생기나? 왜 에러가 안 나나?
- DDL 의 `DEFAULT SYS_EXTRACT_UTC(...)` 가 있는데 UPDATE 문에서 또 써야 하는 이유는?
- `Times.toText()` 가 마이크로초 여섯 자리를 맞추는 이유는? 화면에는 영향이 없는데도?
- `Kst` 를 안 쓰고 UTC 날짜로 자르면 어떤 기능이 어떻게 틀리나?
- 화면으로 나가는 칸이 `String` 이면 화면에서 무슨 일이 벌어지나? 에러는 나나?
- `Json.node()` 가 예외를 삼키고 null 을 주는 이유는? 어떤 경우에 그게 나쁜가?
- MyBatis 의 `TypeHandler` 로 옮기면 무엇이 좋아지고 무엇을 잃나?
- 논리 삭제(`use_yn='N'`)를 쓰면 조회 코드에서 뭘 절대 빠뜨리면 안 되나?

---

## 참고

| 무엇 | 어디 |
|---|---|
| UTC 규칙 · 오라클 함정 | `docs/PROGRESS.md` 의 「오라클 관련 함정」 |
| 시각 변환 | `backend/.../util/Times.java` · `util/Kst.java` |
| JSON 변환 | `backend/.../util/Json.java` |
| 커넥션 설정 | `backend/src/main/resources/application.yml` |
| 스키마 | `db/01_schema.sql` |

> 이 문서는 원래 파이썬 SQLAlchemy 의 `TypeDecorator`(`UtcTimestamp` ·
> `OracleJSON`) 를 설명하던 글이었다. 스프링으로 옮기면서 **같은 두 문제를 다른
> 자리에서 풀게 되어** 다시 썼다. 파이썬 쪽 구현이 궁금하면 git 에서 꺼내 볼 수 있다.
>
> ```
> git show <backend/ 를 지우기 직전 커밋>:backend/app/models/types.py
> ```
