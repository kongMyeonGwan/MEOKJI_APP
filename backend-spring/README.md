# backend-spring — 뭐먹지? API (Spring Boot)

파이썬(FastAPI)으로 돌던 서버를 스프링부트로 옮기는 중이다.
**`backend/`(FastAPI)는 아직 살아 있다.** 옮기는 동안 두 서버를 같이 띄워놓고
응답을 비교하고, 프론트는 한 줄만 바꿔 갈아탄다.

```
backend/          FastAPI   :8000   ← 지금 돌아가는 서버 (건드리지 않는다)
backend-spring/   Spring    :8080   ← 여기
frontend/         React     :5173   ← 손대지 않는다
db/                                 ← 스키마 그대로. 바꿀 것이 없다
```

---

## 빠른 시작

### 0. 사전 준비

| 필요한 것 | 확인 |
|---|---|
| JDK 17 이상 | `java -version` |
| Oracle 접속 정보 | 팀에 문의 (또는 개인 PC 에 XE 설치 — `docs/DB_SETUP.md`) |

**Gradle 은 안 깔아도 된다.** `gradlew` 가 알아서 받아 쓴다.

### 1. 설정 파일 만들기

```
copy src\main\resources\application-local.yml.example src\main\resources\application-local.yml
```

열어서 두 칸을 채운다.

```yaml
spring:
  datasource:
    password: 팀에_문의
app:
  jwt:
    secret-key: 32자_이상의_랜덤_문자열
```

`JWT_SECRET_KEY` 는 **32바이트(영문 32자) 이상**이어야 한다. 짧으면 서버가 뜨다가
`WeakKeyException` 으로 죽는다. `backend/.env` 를 쓰는 사람은 거기 있는 값을 그대로
옮기면 된다 — 같은 키를 쓰면 **두 서버의 토큰이 서로 통한다.**

`application-local.yml` 은 `.gitignore` 에 있어 커밋되지 않는다.

### 2. 띄우기

```
gradlew bootRun
```

확인: <http://localhost:8080/api/health> → `{"status":"ok", ..., "database":"ok"}`

### 3. 테스트

```
gradlew test
```

**실제 Oracle 에 붙는다.** 그리고 `seed_demo` 가 만든 계정
(`minji@example.com` / `meokji1234!`)이 있어야 한다.

```
cd ..\backend
.venv\Scripts\activate
python -m scripts.seed_demo
```

---

## 지금 어디까지 왔나

전체 27개를 **다 옮겼다.** 게시판 · 공지 20개만 담당자 몫이라 501 스텁이다.

| 그룹 | 개수 | 상태 |
|---|:--:|---|
| health | 1 | ✅ |
| auth — 가입 · 로그인 · 토큰 확인 | 3 | ✅ |
| auth — 비밀번호 재설정 | 3 | ✅ 메일은 기본이 콘솔 출력(mock) |
| users — 내 정보 · 수정 · 비밀번호 · 알림 · 탈퇴 | 5 | ✅ |
| bans | 3 | ✅ |
| histories — 목록 · 요약 · 확정 · 삭제 | 4 | ✅ |
| catalog — 고정 목록 · 지역 | 2 | ✅ |
| games — 플레이 기록 | 1 | ✅ |
| likes — 찜 목록 · 토글 · 비우기 | 3 | ✅ |
| 추천 `POST /api/recommendations` | 1 | ✅ 날씨 · 떨이 재료 반영 |
| 레시피 `GET /api/recipes/{이름}` | 1 | ✅ KAMIS 실시세 포함 |
| **posts 15 · notices 5** | 20 | **🔧 501 스텁 — 담당자 작업 대상** |

레시피의 조리 순서를 만드는 코드(steps.py 1,004줄)는 옮기지 않았다.
recipes.json 에 순서가 이미 들어 있고, 그 파일을 만들 때 쓴 코드라 조회에는
필요하지 않다.

### 비밀번호 재설정을 쓰려면

기본값(`app.mail.provider=mock`)에서는 **인증번호가 콘솔에 찍힌다.** 메일 서버
없이 화면을 끝까지 볼 수 있다.

```
===================== [MOCK EMAIL] =====================
 받는 사람 : minji@example.com
 인증번호  : 481027
 유효시간  : 5분
```

진짜로 보내려면 `application-local.yml` 에 두 가지를 넣는다.

```yaml
spring:
  mail:
    host: smtp.gmail.com
    username: 보내는_계정
    password: 앱_비밀번호
app:
  mail:
    provider: smtp
```

`spring.mail.host` 를 비워두면 스프링이 메일 빈을 아예 만들지 않는다. 그래도
서버는 정상으로 뜬다 — 설정이 없으면 콘솔 출력으로 되돌아간다.

### 떨이 재료(`use_bargains`)를 쓰려면

대시보드의 「지금 싼 재료로」 카드만 이걸 켜서 부른다. 켜면 KAMIS 품목
**101개**(코드가 겹치는 이름을 줄인 수)를 훑어 일주일 전과 견주고, 5% 넘게
내린 것 8개를 골라 응답의 `bargains` 에 싣는다.

훑기는 1분 가까이 걸려서 **서버가 뜰 때 미리 한 번 돌린다.**

```
gradlew bootRun               떨이 재료를 안 훑는다 (기본값)
set WARM_BARGAINS=true        훑는다. 첫 화면에서 칩이 바로 보인다
gradlew bootRun
```

기본이 꺼짐인 것은 테스트 때문이다 — 켜두면 `gradlew test` 를 돌릴 때마다 바깥
API 를 101번 두드린다. 결과는 `data/bargains.json` 에 날짜와 함께 남아서, 서버를
다시 켜도 그날 안에는 다시 훑지 않는다(`.gitignore` 에 있다).

**고른 메뉴에는 영향을 주지 않는다.** 원래는 이 목록을 AI 프롬프트에 넣어 "이
재료를 쓰는 메뉴" 를 고르게 했는데, STEP 43 에서 AI 호출을 걷어내며 그 길이
끊겼다. 파이썬도 지금 같은 상태다 — `catalog_recommend_foods` 가
`context["bargains"]` 를 읽지 않는다. 화면은 이 값으로 칩만 그린다.

---

## 게시판 · 공지 담당자에게

서버 쪽이 비어 있고, 주소 · 요청/응답 모양 · 권한은 **다 맞춰져 있다.**
메서드 안만 채우면 된다.

### 어디를 보면 되나

| 무엇 | 어디 |
|---|---|
| 만들 메서드 목록 · 지켜야 할 규칙 | `post/PostService.java` · `notice/NoticeService.java` 주석 |
| 주소 · 로그인 필요 여부 | `post/PostController.java` · `notice/NoticeController.java` |
| 요청/응답 모양 | `post/dto/` 9개 · `notice/dto/` 4개 (다 정의돼 있다) |
| 표 한 행 | `post/RecipeReview.java` · `ReviewComment` · `ReviewLike` · `notice/Notice.java` |
| 매퍼 본보기 | `user/UserMapper.java` + `resources/mapper/UserMapper.xml` |
| 페이징 본보기 | `history/HistoryMapper.xml` 의 `<sql id="mineWithShared">` |
| 왜 이렇게 넘겼는지 | `docs/PROGRESS.md` 의 「게시판 · 공지를 담당자에게 넘김」 |

### 시작하는 순서

1. `PostMapper` 인터페이스와 `resources/mapper/PostMapper.xml` 을 새로 만든다
2. `PostService` 에 메서드를 채운다
3. `PostController` 의 `throw todo();` 를 서비스 호출로 바꾼다
4. 화면(`frontend/src/pages/CommunityPage.jsx` 등)에서 `SAMPLE_*` 대신
   `api/posts.js` 를 부르게 바꾸고 `frontend/src/data/sampleBoard.js` 를 지운다

### 확인은 이걸로

`src/test/java/.../StubFixtureTest.java` 가 **주소 20개가 파이썬과 같은지** 검사한다.
구현하면서 주소를 바꿨다면 이 테스트도 같이 고쳐야 한다 — 프론트가 그 주소를 쓴다.

---

## 옮기는 사람이 알아야 할 것

### 이름 규칙이 세 층이다

```
DB      snake_case   FOOD_NAME     스키마는 그대로 쓴다
자바    카멜          foodName      mybatis: map-underscore-to-camel-case
JSON    snake_case   food_name     jackson: property-naming-strategy
```

**가운데만 카멜이다.** 프론트가 `food_name` · `has_file` 을 그대로 읽고 있어서,
나가는 JSON 을 카멜로 바꾸면 **화면이 빈 값으로 뜨면서 에러도 안 난다.**
React 가 없는 필드를 읽어 `undefined` 를 받고 그대로 그리기 때문이다.

### 오류는 `{"detail": ...}` 하나로만 나간다

프론트(`frontend/src/utils/apiError.js`)가 읽는 방식이 딱 두 가지다.

```
detail 이 문자열이면   그대로 사용자에게 보여준다
detail 이 배열이면     입력칸별 오류. 각 항목의 loc 마지막 값이 칸 이름, msg 가 문구
```

`ApiException.conflict("이미 사용 중인 이메일입니다.")` 처럼 던지면
`ApiExceptionHandler` 가 알아서 그 모양으로 바꿔준다. **`ResponseEntity` 로 오류를
직접 만들지 않는다.**

### 로그인 필요 여부는 컨트롤러가 정한다

```java
@LoginUser User me                      // 없으면 401
@LoginUser(required = false) User me    // 게스트도 가능. 없으면 null
@LoginUser(admin = true) User me        // user_level=1 아니면 403
```

`SecurityConfig` 에 주소 목록을 적지 않는다. 라우터를 추가할 때 두 곳을 고쳐야 하고,
한쪽만 고치면 조용히 열린다.

### 응답 정답지 38개

`src/test/resources/fixtures/*.json` 이 **파이썬 서버의 실제 응답**이다.
옮긴 엔드포인트가 같은 JSON 을 내는지 여기에 대고 확인한다.
오류 본문(401 · 404 · 409 · 422 · 501)도 같이 떠 있다 — 프론트가 `detail` 을 읽어
화면에 뿌리므로 오류 모양도 계약이다.

다시 뜨고 싶으면 (파이썬 쪽에서):

```
cd ..\backend
python <스크립트>   # scratchpad 의 capture_fixtures.py 참고
```

---

## 오라클에서 걸리는 것

옮기다가 막히면 여기를 먼저 본다. 더 자세한 것은 `docs/PROGRESS.md` 의
「오라클 관련 함정」 절에 있다.

| 하려는 것 | 오라클에서는 |
|---|---|
| `LIMIT 10` | `OFFSET ? ROWS FETCH NEXT ? ROWS ONLY` (12c+) |
| 상위 N건 | `ORDER BY` 를 안쪽에 두고 감싼다 — `ROWNUM` 은 **정렬 전에** 매겨진다 |
| `ILIKE` | `LOWER(col) LIKE LOWER(?)` |
| `ON CONFLICT` | `MERGE` 또는 `DuplicateKeyException` 캐치 |
| `SELECT 1` | `SELECT 1 FROM DUAL` — FROM 절이 필수다 |
| 빈 문자열 저장 | **`''` 는 NULL 이다.** NOT NULL 컬럼에 넣으면 ORA-01400 → `@NotBlank` 로 먼저 막는다 |
| `DROP TABLE IF EXISTS` | 21c 에는 없다 (23ai부터) |

**시간대**는 커넥션에서 못박아 뒀다 (`application.yml` 의 `connection-init-sql`).
DB 에는 UTC 만 들어간다. 시각을 응답에 넣을 때는 `common/Times.toText()` 를 쓴다 —
파이썬이 내던 `2026-09-11T21:29:42.955000Z` 와 글자까지 맞춘다.

한국 날짜가 필요하면 `common/Kst` 를 쓴다. 연속 방문일과 인기 레시피 기간 검색이
그 9시간 때문에 경계에서 어긋난다.

---

## 폴더 구조

```
backend-spring/
├── build.gradle                    Boot 4.0.8 · Java 17 · MyBatis · ojdbc17 · jjwt · mail
├── gradlew(.bat)                   Gradle 을 안 깔아도 빌드된다
└── src/
    ├── main/
    │   ├── java/com/meokji/api/
    │   │   ├── common/             ApiException · 오류 핸들러 · Page · Times · Kst · Json
    │   │   ├── config/             AppProperties · SecurityConfig · WebConfig
    │   │   ├── security/           JwtProvider · @LoginUser · 인자 리졸버
    │   │   ├── health/             GET /api/health
    │   │   ├── auth/               가입 · 로그인 · 토큰 확인 · 비밀번호 재설정
    │   │   ├── user/               내 정보 · 알림 · 탈퇴
    │   │   ├── ban/                못 먹는 재료
    │   │   ├── history/            식사 기록
    │   │   ├── catalog/            고정 목록 · catalog.json 읽기
    │   │   ├── region/             regions.json 읽기 (주소 → 좌표)
    │   │   ├── like/               찜한 레시피
    │   │   ├── weather/            날씨 (mock / openweather) + 안내 문구
    │   │   ├── recommend/          음식 추천 (카탈로그 점수)
    │   │   ├── recipe/             레시피 (recipes.json) + 응답 DTO
    │   │   ├── price/              KAMIS 시세 · 재료 이름 매칭 · 단위 환산 · 떨이 재료
    │   │   ├── game/               미니게임 플레이 기록
    │   │   ├── post/               🔧 게시판 — 501 스텁
    │   │   └── notice/             🔧 공지 — 501 스텁
    │   └── resources/
    │       ├── application.yml
    │       ├── application-local.yml.example
    │       ├── data/               파이썬에서 내보낸 표 5개 (아래 설명)
    │       └── mapper/             User · History · Ban · Like · Game
    └── test/
        ├── java/com/meokji/api/    테스트 85건 (정답지 대조 + 재설정 · 떨이 재료)
        └── resources/fixtures/     파이썬 응답 정답지 38개
```

---

## `data/` 는 파이썬에서 내보낸 것이다

| 파일 | 무엇이 | 어디서 |
|---|---|---|
| `catalog.json` | 음식 100종 · 카테고리 · 룰렛 · 게임 | 그대로 복사 |
| `recipes.json` | 손으로 쓴 레시피 100종 | 그대로 복사 |
| `regions.json` | 시·도 17개와 시·군·구 좌표 | 그대로 복사 |
| `ingredient-rules.json` | 세는 단위 무게 52 · 단위별 무게 10 · 대체재료 12 · 2인분 분량 107 | `portion.py` · `ai_api.py` 에서 내보냄 |
| `kamis-items.json` | 품목코드 116 · 손질 표현 34 · 별칭 26 · 가공식품 27 | `ingredient_match.py` 에서 내보냄 |

**손으로 옮기지 않았다.** 52개 · 116개를 베끼면 한두 개는 틀리고, 틀려도 티가
안 난다 — 좌표가 어긋나면 다른 동네 날씨가 나오고, 분량이 어긋나면 가격이
조용히 달라진다.

고칠 일이 있으면 **파이썬 쪽을 고친 뒤 다시 내보낸다.** 각 파일 맨 위에 그렇게
적어뒀다. `backend/` 를 지울 때 이 파일들이 원본이 된다.

---

## 옮기다 걸린 것들 (다음 사람을 위해)

| 무엇 | 어떻게 됐나 |
|---|---|
| Boot 4 는 **Jackson 3** | 패키지가 `com.fasterxml.jackson` 이 아니라 `tools.jackson` 이다 |
| `WRITE_DATES_AS_TIMESTAMPS` | Jackson 3 에서 `DateTimeFeature` 로 옮겼다. `spring.jackson.serialization` 아래에 적으면 **서버가 뜨지 않는다** |
| XML 주석의 `--` | 매퍼 XML 주석에 `-----` 를 그으면 `SAXParseException` 으로 매퍼를 못 읽는다 |
| 405 가 500 으로 | `ResponseStatusException` 만 잡으면 `HttpRequestMethodNotSupportedException`(ServletException 계열)이 빠져나간다. `ResponseEntityExceptionHandler` 를 상속해야 한다 |
| 파이썬 `round()` | **짝수로 맞춤**이다. `round(2.5)` 가 2 다. `Math.round` 는 3 이라 가격이 1원씩 어긋난다 — `common/Numbers` 를 쓴다 |
| `grams` 의 정수/소수 | 파이썬은 `quantity` 가 정수면 `grams` 도 정수로 내보낸다 (`80` vs `625.0`) |
