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

### 3. 데모 데이터 채우기

```
gradlew seedDemo
```

빈 DB 로는 화면을 볼 수가 없다. 글을 쓰려면 식사를 먼저 확정해야 해서 손으로
채우려면 한참 걸린다. 이게 그 한 바퀴를 대신 돌아준다 — 회원 2명(관리자 · 일반) ·
공지 4건 · 식사 기록 3건 · 후기 2건(첨부 1건).

**여러 번 돌려도 안전하다.** 이미 있는 것은 건너뛴다. 게시판과 공지는 아직
501 이라 그 단계만 건너뛰고, 담당자가 구현하면 다시 돌릴 때 채워진다.

```
로그인    admin@meokji.com (관리자) / minji@example.com
비밀번호  meokji1234!
```

`app.env` 가 `local` 이 아니면 **아무것도 하지 않고 멈춘다.** 저장소에 적힌
비밀번호로 관리자 계정을 만들기 때문이다.

> 파이썬 쪽 `python -m scripts.seed_demo` 와 같은 자리다. 둘 중 아무거나 써도
> 같은 데이터가 만들어진다 — 다만 파이썬 쪽은 메뉴 목록에 `크림 파스타` 가
> 남아 있어 **빈 DB 에서는 3단계에서 멈춘다.** STEP 41 에서 레시피 100종을 다시
> 정의할 때 빠진 메뉴라 `GET /api/recipes` 가 404 를 낸다. 스프링 쪽은
> `비빔국수` 로 바꿔뒀다.

### 4. 테스트

```
gradlew test
```

**실제 Oracle 에 붙는다.** 그리고 위 `seedDemo` 가 만든 계정
(`minji@example.com` / `meokji1234!`)이 있어야 한다.

`seedDemo` 는 `gradlew test` 에서 빠진다(`@Tag("seed")`). 평소 테스트를 돌릴
때마다 회원과 후기를 새로 만들 이유가 없다.

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
| 만들 메서드 목록 · 지켜야 할 규칙 | `service/PostService.java` · `service/NoticeService.java` 주석 |
| 주소 · 로그인 필요 여부 | `controller/PostController.java` · `controller/NoticeController.java` |
| 요청/응답 모양 | `dto/post/` 9개 · `dto/notice/` 4개 (다 정의돼 있다) |
| 표 한 행 | `vo/RecipeReview.java` · `vo/ReviewComment` · `vo/ReviewLike` · `vo/Notice.java` |
| 매퍼 본보기 | `mapper/UserMapper.java` + `resources/mapper/UserMapper.xml` |
| 페이징 본보기 | `resources/mapper/HistoryMapper.xml` 의 `<sql id="mineWithShared">` |
| **어떻게 동작해야 하는가** | **`docs/BOARD_SPEC.md`** — 규칙 · 정렬 · 권한 · 첨부파일 처리 |
| 돌아가던 구현 (파이썬) | `git show e22853f:backend/app/services/post_service.py` |
| 왜 이렇게 넘겼는지 | `docs/PROGRESS.md` 의 「게시판 · 공지를 담당자에게 넘김」 |

### 시작하는 순서

0. **`docs/BOARD_SPEC.md` 를 먼저 읽는다.** DTO 만 봐서는 알 수 없는 것들이 있다 —
   남의 글도 403 이 아니라 404 인 이유, 좋아요를 껐다 켤 때 새로 INSERT 하지 않는
   이유, 인기 레시피의 날짜가 한국 날짜인 이유
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

**다시 뜰 수는 없다.** 이걸 떠온 스크립트(`capture_fixtures.py`)는 scratchpad 에만
있다가 사라졌다. 그래서 이 파일들은 **고정된 과거 스냅샷**으로 본다 — 스프링 응답이
정답지와 달라지면 스프링을 고치는 것이지 정답지를 고치는 게 아니다.

일부러 응답 모양을 바꿔야 한다면(프론트와 함께) 정답지도 손으로 고치고, 왜 바꿨는지를
`docs/PROGRESS.md` 에 남긴다.

### 데이터 파일은 테스트가 지킨다

`resources/data/*.json` 다섯 개는 파이썬에서 내보낸 표다. **`backend/` 를 지우면
이 파일이 원본이 된다.** 그때부터는 손으로 고치게 되는데, 116개 · 107개짜리 표를
손으로 고치면 한두 개는 틀리고 **틀려도 티가 안 난다** — 좌표가 어긋나면 다른 동네
날씨가 나오고, 분량이 어긋나면 가격이 조용히 달라진다.

`DataFileTest` 가 그걸 잡는다. 개수 · 빠진 칸 · 좌표 범위와, **카탈로그와 레시피의
메뉴가 정확히 같은지**까지 본다. 데이터를 고쳤으면 이걸 돌린다.

```
gradlew test --tests "com.meokji.api.DataFileTest"
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
DB 에는 UTC 만 들어간다. 시각을 응답에 넣을 때는 `util/Times.toText()` 를 쓴다 —
파이썬이 내던 `2026-09-11T21:29:42.955000Z` 와 글자까지 맞춘다.

한국 날짜가 필요하면 `util/Kst` 를 쓴다. 연속 방문일과 인기 레시피 기간 검색이
그 9시간 때문에 경계에서 어긋난다.

---

## 폴더 구조

**계층별로 나눠져 있다.** 폴더 이름이 곧 그 파일의 역할이다 — 어디에 무엇을
만들지 헤매지 않게 하려는 것이다.

```
backend-spring/
├── build.gradle                    Boot 4.0.8 · Java 17 · MyBatis · ojdbc17 · jjwt · mail
├── gradlew(.bat)                   Gradle 을 안 깔아도 빌드된다
└── src/
    ├── main/
    │   ├── java/com/meokji/api/
    │   │   ├── MeokjiApiApplication.java   루트에 있어야 컴포넌트 스캔이 된다
    │   │   │
    │   │   ├── controller/   12    HTTP 를 받는 자리. 로직을 두지 않는다
    │   │   ├── service/      14    업무 규칙과 트랜잭션 경계
    │   │   ├── mapper/        6    SQL 을 부르는 인터페이스 (+ Post · Notice 는 담당자 몫)
    │   │   ├── dto/          42    요청 · 응답 모양. 기능별로 한 겹 더 나눠져 있다
    │   │   │                       auth 7 · post 9 · user 5 · notice 4 · recommend 4
    │   │   │                       history 3 · like 3 · recipe 3 · ban 2 · game 1 · weather 1
    │   │   ├── vo/            9    표 한 행. User · MealHistory · RecipeReview · Notice ...
    │   │   │
    │   │   ├── util/         15    계산 · 바깥 API 호출 · 고정 목록 읽기
    │   │   │                       KamisClient · FoodScorer · RecipeBook · Times · Kst ...
    │   │   ├── common/        4    ApiException · 오류 핸들러 · Page · MessageResponse
    │   │   ├── config/        3    AppProperties · SecurityConfig · WebConfig
    │   │   └── security/      3    JwtProvider · @LoginUser · 인자 리졸버
    │   └── resources/
    │       ├── application.yml
    │       ├── application-local.yml.example
    │       ├── data/               파이썬에서 내보낸 표 5개 (아래 설명)
    │       └── mapper/             User · History · Ban · Like · Game · VerificationCode
    └── test/
        ├── java/com/meokji/api/    테스트 94건 + 데모 데이터(SeedDemoTest)
        └── resources/fixtures/     파이썬 응답 정답지 38개
```

### 흐름은 예외 없이 세 층이다

```
controller  ->  service  ->  mapper  ->  DB
```

컨트롤러가 매퍼를 직접 부르지 않는다. 하는 일이 한 줄짜리라도 서비스를 둔다 —
어떤 컨트롤러는 거치고 어떤 것은 건너뛰면, 새로 들어온 사람이 규칙을 코드마다
다르게 배운다.

예외는 둘뿐이고 둘 다 DB 를 안 쓴다. `HealthController` 는 DB 가 살아있는지만
보고, `CatalogController` 는 JSON 파일을 읽어 그대로 내보낸다.

### 주입은 `@Autowired` 필드 주입이다

```java
@RestController
public class BanController {

    @Autowired
    private BanService banService;
```

`ApiException` 만 생성자를 쓴다. 빈이 아니라 예외 값 객체라 생성자가 값을 받는
것이지 주입이 아니다.

`JwtProvider` 는 주입받은 설정으로 서명키를 만들어야 해서 `@PostConstruct` 를 쓴다.
**생성자에서는 주입받은 필드가 아직 null 이다** — 주입이 끝난 뒤 스프링이
`@PostConstruct` 를 불러준다.

### DAO 가 아니라 Mapper 다

MyBatis 의 용어를 그대로 쓴다. 애노테이션이 `@Mapper` 이고, XML 루트 태그가
`<mapper>` 이고, 설정 키가 `mapper-locations` 다. 인터페이스만 선언하면
**구현은 MyBatis 가 만들어 준다** — 내가 구현을 짜는 DAO 와 그 점이 다르다.

### 패키지를 옮길 때 조심할 것

매퍼 XML 의 `namespace` 와 `resultType` 은 **문자열로 클래스를 찾는다.** 자바만
옮기고 XML 을 안 고치면 컴파일은 지나가고 서버가 뜨다가 죽는다.

```xml
<mapper namespace="com.meokji.api.mapper.UserMapper">
<select resultType="com.meokji.api.vo.User">
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
