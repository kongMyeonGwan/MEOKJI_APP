# backend — 뭐먹지? API (Spring Boot)

React 화면(`frontend/`)이 부르는 API 서버다. 주소는 `http://localhost:8090`.

```
frontend/         React     :5173
backend/          Spring    :8090   ← 여기
db/                                 ← Oracle 테이블 만드는 SQL
```

---

## 빠른 시작

### 0. 사전 준비

| 필요한 것 | 확인 |
|---|---|
| JDK 17 이상 | `java -version` |
| Oracle 접속 정보 | 팀장에게 받는다 (개인 PC 에 설치하려면 `docs/DB_SETUP.md`) |

Gradle 은 안 깔아도 된다. `gradlew` 가 알아서 받아 쓴다.

### 1. 설정 파일 만들기

```
copy src\main\resources\application-local.yml.example src\main\resources\application-local.yml
```

열어서 두 칸을 채운다. 이 파일은 `.gitignore` 에 있어서 커밋되지 않는다.

```yaml
spring:
  datasource:
    password: 팀장에게_받은_값
app:
  jwt:
    secret-key: 32자_이상의_아무_문자열
```

`secret-key` 는 **영문 32자 이상**이어야 한다. 짧아도 서버는 뜨지만 로그인할 때 500 이 난다.

### 2. 서버 켜기

```
gradlew bootRun
```

확인: <http://localhost:8090/api/health> → `{"status":"ok", ..., "database":"ok"}`

### 3. 데모 데이터 채우기

```
gradlew seedDemo
```

빈 DB 로는 화면을 볼 수 없어서 회원 · 공지 · 식사 기록을 채워준다. 여러 번 돌려도 안전하다(이미 있으면 건너뛴다).

```
로그인    admin@meokji.com (관리자) / minji@example.com
비밀번호  meokji1234!
```

---

## 폴더 구조

**기능(화면)별로 먼저 나누고, 그 아래를 계층으로 나눈다.**
한 기능을 고칠 때 폴더 하나만 열면 되고, 팀원끼리 담당 구역이 겹치지 않는다.

```
src/main/java/com/meokji/api/
├── ban/                  못 먹는 재료 (기능 하나의 본보기)
│   ├── controller/       주소(URL)를 받는 곳               BanController
│   ├── service/          실제 일을 하는 곳 (인터페이스)      BanService
│   │                     + 그 구현                         BanServiceImpl
│   ├── dao/              DB 에 SQL 을 보내는 곳 (인터페이스)  BanDAO
│   └── vo/               값을 담는 상자                     UserBanVO
│
├── auth/                 로그인 · 회원가입 · 비밀번호 재설정
├── user/                 내 정보 · 알림 · 탈퇴
├── recommend/            음식 추천 (날씨 · 예산 · 검색)
├── recipe/               레시피 + 재료 가격
├── history/              식사 기록
├── like/                 찜
├── game/                 미니게임
├── review/               커뮤니티 게시판       <- 담당자 구역
├── notice/               공지사항             <- 담당자 구역
├── catalog/              화면이 쓰는 고정 목록
├── health/               서버 · DB 상태
│
│   아래 셋은 기능을 가리지 않고 같이 쓴다
├── common/       에러 처리 · 페이지 모양 같은 공통          ApiException, ApiExceptionHandler
├── config/       서버 설정 (CORS, 비밀번호 암호화)          WebConfig
└── util/         여러 곳에서 쓰는 도구                      JwtUtil, Kst, 날씨 · 시세 API

src/main/resources/
├── mapper/       실제 SQL (DAO 하나에 XML 하나)            BanMapper.xml
├── data/         음식 목록 · 레시피 · 지역 같은 고정 데이터 (JSON)
└── application.yml   설정
```

### 요청이 흘러가는 순서 (예: 못 먹는 재료 추가)

```
화면  POST /api/bans  { "ingredient_name": "오이" }
  ↓
BanController.add()          로그인 확인 → 서비스 부르기
  ↓
BanServiceImpl.add()         값 검사 · UserBanVO 만들기 → DAO 부르기
  ↓
BanDAO.insert(ban)           (인터페이스. 실제 SQL 은 XML 에)
  ↓
BanMapper.xml  <insert id="insert">  INSERT INTO user_bans ...
```

**처음 보는 사람은 Ban 파일 5개부터 읽는다.** 가장 짧고 기본 모양이 다 들어 있다.
`BanController` → `BanService` → `BanServiceImpl` → `BanDAO` → `resources/mapper/BanMapper.xml`

### 이름 규칙이 세 가지다

| 어디 | 모양 | 예 |
|---|---|---|
| DB 컬럼 | snake_case | `ingredient_name` |
| 자바 필드 | camelCase | `ingredientName` |
| JSON (화면) | snake_case | `ingredient_name` |

DB ↔ 자바는 MyBatis 가, 자바 ↔ JSON 은 Jackson 이 **알아서 바꿔준다**(`application.yml` 설정). 그래서 자바에서는 camelCase 만 쓰면 된다.

---

## 자주 쓰는 모양

### 로그인한 사람 찾기 (컨트롤러)

```java
@GetMapping
public List<BanResponse> getList(@RequestHeader(value = "Authorization", required = false) String token) {
    UserVO loginUser = authService.getLoginUser(token);   // 로그인 안 했으면 여기서 401 에러
    return banService.getList(loginUser);
}
```

| 부르는 것 | 로그인 안 했으면 |
|---|---|
| `authService.getLoginUser(token)` | 401 에러 |
| `authService.findLoginUser(token)` | `null` (로그인 없이도 되는 화면) |
| `authService.getAdminUser(token)` | 401, 관리자가 아니면 403 |

### 에러 내기 (서비스)

```java
throw ApiException.notFound("등록되지 않은 재료입니다.");   // 404
throw ApiException.conflict("이미 등록한 재료입니다.");     // 409
```

`ApiExceptionHandler` 가 받아서 `{"detail": "등록되지 않은 재료입니다."}` 로 응답한다. 화면은 이 `detail` 을 그대로 보여준다.

### 입력값 검사 (DTO)

```java
@NotBlank(message = "재료 이름을 입력해주세요")
private String ingredientName;
```

컨트롤러에서 `@Valid @RequestBody` 로 받으면, 검사에 걸릴 때 컨트롤러까지 오지 않고 422 에러가 나간다.

### 설정값 쓰기

```java
@Value("${app.jwt.expire-minutes}")
private int expireMinutes;
```

---

## 새 기능 만드는 순서

0. 어느 기능인지 정한다. 새 기능이면 `<기능>/` 폴더부터 만든다
1. `<기능>/vo/` 에 테이블 한 행을 담을 VO 가 있는지 본다 (없으면 만든다. 필드 + `@Data`)
2. `<기능>/dao/XxxDAO.java` 에 메서드를 만든다
3. `resources/mapper/XxxMapper.xml` 에 **id 가 메서드 이름과 같은** SQL 을 쓴다
   (XML 의 `namespace` 는 DAO 의 전체 이름과 똑같아야 한다. 예: `com.meokji.api.ban.dao.BanDAO`)
4. `<기능>/service/XxxService.java`(인터페이스)에 메서드를 적고, `XxxServiceImpl` 에서 구현한다
5. 그 VO 에 화면이 보낼 칸과 검증(`@NotBlank` 등)을 얹는다.
   **화면에 나가면 안 되는 칸에는 `@JsonIgnore` 를 꼭 붙인다** — 안 붙이면
   그 값이 그대로 응답에 실려 나간다 (`ResponseShapeTest` 가 이걸 검사한다)
6. `<기능>/controller/` 에 주소를 만든다

---

## 게시판 · 공지 담당자에게

컨트롤러 · 서비스 인터페이스 · DTO 는 **다 만들어져 있다.** 지금은 `ServiceImpl` 이 501(미구현) 에러를 던진다. 이것만 채우면 된다.

| 무엇 | 어디 |
|---|---|
| **담당 폴더** | **`review/` 와 `notice/` 통째로.** 다른 기능 폴더는 건드릴 일이 없다 |
| **채울 곳** | `review/service/ReviewServiceImpl.java` · `notice/service/NoticeServiceImpl.java` (TODO 주석) |
| DAO · SQL (예시 몇 개 들어 있음) | `review/dao/ReviewDAO.java` + `mapper/ReviewMapper.xml`, `notice/dao/NoticeDAO.java` + `mapper/NoticeMapper.xml` |
| 주소 · 로그인 필요 여부 | `review/controller/ReviewController.java` · `notice/controller/NoticeController.java` (완성) |
| 요청 · 응답 모양 | `review/vo/` 4개 · `notice/vo/` 1개 (완성). `RecipeReviewVO` 하나가 표 한 행 · 요청 · 목록 · 상세 · 인기 목록을 겸한다 |
| 테이블 한 행 | `review/vo/RecipeReviewVO` · `ReviewCommentVO` · `ReviewLikeVO` · `notice/vo/NoticeVO` |
| 페이지 목록 본보기 | `HistoryServiceImpl.getList()` + `HistoryMapper.xml` 의 `findPageByUser` |
| **어떻게 동작해야 하는가** | **`docs/BOARD_SPEC.md`** — 정렬 · 권한 · 첨부파일 규칙 |

순서

1. `docs/BOARD_SPEC.md` 를 읽는다
2. DAO 에 메서드, XML 에 SQL 을 만든다
3. `ServiceImpl` 의 `throw ApiException.notImplemented(TODO);` 를 실제 코드로 바꾼다
4. 화면(`frontend/src/pages/community/`)에서 `SAMPLE_*` 가데이터 대신 `api/reviews.js` 를 부르게 바꾼다

---

## 건드리지 않아도 되는 곳

아래는 바깥 API 호출과 계산이 섞여 있어서 조금 어렵다. **고칠 일이 거의 없다.**

| 파일 | 하는 일 |
|---|---|
| `util/KamisClient` · `util/IngredientMatcher` · `util/UnitConvert` | 정부 시세 API 호출, 재료 이름 → 품목 코드, "1kg 12,000원" → "300g 3,600원" |
| `recipe/service/PriceServiceImpl` · `recommend/service/BargainServiceImpl` | 재료 가격 모으기, "요즘 싼 재료" 찾기 |
| `util/WeatherClient` | 날씨 API 호출 |
| `util/FoodScorer` · `util/RecipeBook` | 추천 점수 매기기, 레시피 파일 읽기 |

---

## 테스트

```
set EMAIL_PROVIDER=mock
set WEATHER_PROVIDER=mock
gradlew test
```

- **실제 Oracle 에 붙는다.** `gradlew seedDemo` 로 만든 계정이 있어야 한다.
- 메일과 날씨는 꼭 **mock** 으로 둔다. `smtp` 로 두면 테스트가 진짜 메일을 보내려고 하고, 실제 날씨로 돌면 기온을 비교하는 테스트가 틀린다.
- 응답을 미리 저장해둔 정답지(`src/test/resources/fixtures/`)와 비교한다. 회원 번호(`id`)는 DB 마다 달라서 그 칸만 다를 수 있다.

---

## Oracle 에서 조심할 것

| 증상 | 이유 |
|---|---|
| 빈 글자(`""`)를 넣었는데 NOT NULL 에러 | Oracle 은 빈 글자를 NULL 로 본다. DTO 에서 `@NotBlank` 로 먼저 막는다 |
| null 을 넣을 때 `ORA-17004` | `application.yml` 의 `jdbc-type-for-null: 'NULL'` 이 빠졌거나 **따옴표가 없을 때**. 따옴표가 없으면 YAML 이 "값 없음" 으로 읽는다 |
| `SELECT 1` 이 안 된다 | Oracle 은 `FROM` 이 꼭 필요하다. `SELECT 1 FROM DUAL` |
| 페이지 자르기 | `OFFSET #{offset} ROWS FETCH NEXT #{size} ROWS ONLY` (12c 이상) |
| 시각이 9시간 어긋남 | DB 에는 UTC 로 저장한다. 한국 날짜가 필요하면 `util/Kst` 를 쓴다 |
