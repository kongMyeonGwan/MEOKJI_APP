# 작업 내역 — 초보자 수준으로 코드 정리 (2026-09-30)

> 브랜치: `feature/kongmg`
> 목표: 학원에서 2주 배운 팀원이 읽고 이어서 만들 수 있는 수준으로 전체 코드를 낮춘다.

---

## 1. 프론트엔드 — ✅ 끝남 (커밋 `e66f953` 로 푸시 완료)

| 한 일 | 내용 |
|---|---|
| Context 6개 제거 | `contexts/` 폴더 삭제. 로그인 사용자 · 테마 · 알림은 `App.jsx` 의 `useState` 3개 → props 로 내려줌 |
| ProtectedRoute · Outlet 제거 | `App.jsx` 의 `loginOnly(<Page />)` 로 로그인 필요 화면 감쌈 |
| 문법 통일 | 화살표 함수, `async/await` + `try/catch/finally`, `map/filter/find`, 스프레드, 템플릿 문자열, 삼항 |
| 폼 검사 | 에러 state 하나 + `if` 로 순서대로 검사 후 `return` |
| 에러 처리 | `utils/apiError.js` 의 `getErrorMessage(err)` 하나로 통일 (axios 에러 그대로 읽음) |
| 로그인 · JWT | `utils/token.js` (localStorage). 토큰 저장은 `App.jsx` 의 `handleLogin` 한 곳에서만. 401 이면 `api/client.js` 가 로그아웃 → `/login` |
| 컴포넌트 분리 | 파일 하나에 컴포넌트 하나. `components/mypage/*`, `components/community/CommentItem`, `components/price/PriceRow` |
| 룰렛 | sin/cos SVG → CSS `conic-gradient` (**브라우저에서 모양 확인 필요**) |
| 포트 | `.env` · `.env.example` · `client.js` 기본값 8000 → **8080** (Spring) |
| 규칙 문서 | `frontend/CODE_STYLE.md` |

---

## 2. 백엔드 (Spring) — 🔧 거의 끝남 (**아직 커밋 안 함**)

### 바뀐 구조

```
controller/  →  service/XxxService (인터페이스)  →  service/XxxServiceImpl  →  dao/XxxDAO  →  resources/mapper/XxxMapper.xml
vo/XxxVO (테이블 한 행, Lombok @Data)      dto/ (요청·응답, Lombok @Data 클래스)
```

| 한 일 | 내용 |
|---|---|
| `mapper/XxxMapper.java` → `dao/XxxDAO.java` | XML 파일 이름은 그대로(`BanMapper.xml`), 안의 namespace 만 `com.meokji.api.dao.XxxDAO` |
| VO 이름 | `User` → `UserVO` 등 (테이블 이름 + VO). getter/setter → `@Data` |
| Service 분리 | 14개 전부 인터페이스 + `Impl` |
| `record` 제거 | DTO 42개, util 안의 record, DAO 안의 record 전부 일반 클래스로. `isAdmin` · `isMine` 은 JSON 이름 때문에 `Boolean` 으로 |
| 로그인 확인 | `@LoginUser` 커스텀 어노테이션 · 인자 리졸버 삭제 → 컨트롤러에서 `@RequestHeader("Authorization") String token` 받고 `authService.getLoginUser(token)` / `findLoginUser` / `getAdminUser` |
| Security | `spring-boot-starter-security` → `spring-security-crypto`(BCrypt 만). `SecurityConfig` 삭제, CORS · BCrypt 는 `config/WebConfig` |
| 설정값 | `config/AppProperties`(356줄) 삭제 → 각 클래스에서 `@Value("${app...}")` |
| JWT | `security/JwtProvider` → `util/JwtUtil` |
| 에러 처리 | `ApiExceptionHandler` 가 `ResponseEntityExceptionHandler` 상속 안 함. 에러 종류별 메서드 나열 |
| Health | `JdbcTemplate` → `dao/HealthDAO` + `HealthMapper.xml` (`SELECT 1 FROM DUAL`) |
| 게시판 · 공지 | 컨트롤러 완성, `ReviewServiceImpl` · `NoticeServiceImpl` 에 TODO + 501. `ReviewDAO`/`NoticeDAO` + XML 에 예시 SQL |
| 연속 기록일 | User · History 둘 다 쓰던 계산을 `util/Kst.countStreak()` 하나로 |
| 주석 | Javadoc HTML 태그(`<p>`, `{@code}`) 제거, 파이썬 이야기 제거 |
| README | `backend-spring/README.md` 새로 씀 (구조 · 흐름 · 새 기능 만드는 순서 · 게시판 담당 안내) |

### 찾아서 고친 버그
- **회원가입 시 성별 · 주소를 비우면 500 에러였음.** `application.yml` 의 `jdbc-type-for-null: NULL` 에 따옴표가 없어서 YAML 이 "값 없음" 으로 읽고 설정이 무시됐음 → `'NULL'` 로 수정.

### 테스트 결과 (팀 DB, 메일 · 날씨 mock)
- **94개 중 91개 통과.** 실패 3개는 정답지의 회원 id(197)와 이 DB 의 id(262)가 달라서 생기는 것뿐 (다른 칸은 확인해보니 같음).
- 리팩터링 전 기준선과 같은 결과.

---

## 3. 남은 일 (여기서부터 이어서)

### 끝난 것 (2026-10-01)

- [x] **백엔드 커밋 & 푸시** — `b247cba` (약 140개 파일)
- [x] **루트 `README.md` 구조 설명** — `mapper/` → `dao/`, `security/` 줄 삭제,
      `config/` 는 `WebConfig` 만, `service/` 에 "인터페이스 + Impl 14쌍", 개수도 실제와 맞춤
      (dao 9 · vo 10 · util 16 · 매퍼 XML 9)
- [x] **`docs/BOARD_SPEC.md` 옛 이름** — 6~8줄 · 118줄 · 217줄 · 229~230줄 전부 수정
- [x] **테스트 컴파일 복구** — `StubFixtureTest.java` 가 지워진
      `com.meokji.api.security.LoginUser` 를 import 해서 `gradlew compileTestJava` 가
      깨져 있었다 (주석에서만 쓰는 미사용 import 였음). 지금은 `compileJava` ·
      `compileTestJava` 둘 다 통과한다.

### 자바 폴더를 기능별로 다시 나눔 (2026-10-01)

계층별(`controller/` · `service/` · `dao/` · `vo/` · `dto/`)에서
**기능별 + 그 아래 계층**으로 바꿨다. 기능 12개 —
`auth` · `user` · `ban` · `recommend` · `recipe` · `history` · `like` · `game` ·
`review` · `notice` · `catalog` · `health`.

- 자바 101개 파일 이동 · package 선언과 import 전부 교체
- 매퍼 XML 9개의 `namespace` 도 새 경로로 (예: `com.meokji.api.ban.dao.BanDAO`)
- 서비스가 없던 `catalog` · `health` 에 인터페이스 + Impl 을 만들어 계층을 맞췄다.
  컨트롤러가 직접 DAO · util 을 부르던 코드를 서비스로 내렸다
- `common/` · `config/` · `util/` 은 기능을 안 가리므로 그대로 위에 둔다
- `application.yml` 의 `type-aliases-package: com.meokji.api` 는 하위 패키지를 다
  훑어서 그대로 두면 된다. 클래스 이름이 겹치는 것은 없다(확인함)

게시판 담당자 구역이 `review/` · `notice/` 폴더 통째로 분리됐다.

### DTO 를 없애고 VO 로 합침 (2026-10-01)

"DTO 와 VO 는 결국 같은 자바 파일인데 왜 나누나" 라는 지적에 따라 `dto` 폴더를
전부 없앴다. **자바 113개, DTO 42개가 사라졌다.**

합친 방식은 기능마다 다르다.

| 어떤 DTO 였나 | 어떻게 했나 | 예 |
|---|---|---|
| 표 한 행을 다르게 보여주던 응답 | 그 VO 로 **합쳤다** | `BanResponse` → `UserBanVO` |
| 그 표에 값을 넣는 요청 | 그 VO 에 검증을 **얹었다** | `NoticeCreateRequest` → `NoticeVO` |
| 표가 없는 모양 (여러 행을 센 값, 계산 결과) | `vo/` 로 옮기고 이름만 `...VO` 로 | `LikeListResponse` → `LikeListVO` |

**한 번 잘못 합쳤다가 테스트가 잡았다.** 마이페이지 응답(`ProfileResponse`)을
`UserVO` 에 넣었더니 로그인 응답에도 `latitude` · `cook_count: 0` 이 따라 나가서
정답지 테스트 3개가 깨졌다 (`AuthFixtureTest` 2개 · `UserBanFixtureTest` 1개).
요리 횟수 · 연속 기록일은 **세어서** 만드는 값이라 표와 칸이 다르다 — 위 표의
셋째 줄에 해당한다. `ProfileVO` 로 되돌리니 100개 전부 통과.

합친 VO 에서 지켜야 할 것 두 가지.

1. **나가면 안 되는 칸에 `@JsonIgnore`.** `UserVO` 를 그냥 내보내면
   `password_hash` · `login_fail_count` · `is_locked` 가 실려 나간다.
   컴파일러가 안 잡아준다.
2. **DB 모양과 화면 모양이 다르면 getter 로 바꾼다.** `pinned_yn('Y'/'N')`
   → `pinned(true/false)`, `created_at(LocalDateTime)` → 글자,
   `recipe_json(CLOB 글자)` → JSON 객체.

`src/test/java/com/meokji/api/ResponseShapeTest.java` 를 새로 만들어 이 둘을
검사한다. **DB 없이 도는 테스트다** — 실제로 JSON 을 만들어서 칸 이름을 본다.
6개 전부 통과.

합치지 못한 것도 있다. 로그인 · 회원가입 · 내 정보 수정은 **필수 칸이 서로 달라서**
한 클래스로 못 합친다 (`RegisterVO` 는 닉네임이 필수인데 `LoginVO` 는 아니다).
합치면 검증 그룹(`@Validated(OnRegister.class)`)을 써야 해서 오히려 어려워진다.
그래서 `auth/vo/` · `user/vo/` 에 따로 두되 이름만 `...VO` 로 맞췄다.

### 화면부터 서버까지 실제로 태워봄 (2026-10-01)

로컬 Oracle + `gradlew bootRun`(8080) + `npm run dev`(5173) 를 같이 띄우고
헤드리스 Chrome 으로 화면까지 확인했다. **전 구간 통과.**

| 확인한 것 | 결과 |
|---|---|
| CORS 프리플라이트 (5173 → 8080) | `Allow-Headers` 에 `authorization` 있음 |
| 로그인 · `/api/auth/me` | `user` 8칸, `password_hash` 안 나감 |
| `/api/users/me` · `PATCH` | `ProfileVO` 11칸, `cook_count` 실제 값 |
| Ban · 찜 · 게임 · History · 추천 · 레시피 · 카탈로그 | 전부 200/201/204 |
| `/api/reviews` · `/api/notices` | **501** (게스트도 401 아님) |
| 화면 | 홈 · 마이페이지 · History · 커뮤니티 정상 렌더 |

홈의 추천이 API 응답(냉우동 2,100원)과 일치했고, 마이페이지에 "요리 3회" 가
떴다 — `ProfileVO` 를 분리하지 않았다면 0회로 떴을 자리다.

**클릭이 필요한 것은 아직 못 봤다.** History 행 펼치기, "이걸로 먹을래요",
룰렛 돌리기, 로그아웃은 자동화 도구가 없어 못 눌렀다. 그 데이터를 내려주는
API 는 전부 확인했으니, 사람이 한 번 훑으면 된다.

### 잡것 정리 (2026-10-01)

- `catalog.json` 의 게임 설명 3개에 역슬래시가 **글자로** 박혀 있어 화면에
  그대로 보였다. 공백으로 바꿨다 (줄바꿈은 화면 폭에 맞춰 접힌다).
  다른 데이터 파일 4개에는 같은 흔적이 없다. 테스트 정답지도 같이 고쳤다.
- 옛 이름 `shared_post_id` → `recipe_review_id` (`sharedPostId` →
  `recipeReviewId`). 표 이름이 `posts` 에서 `recipe_reviews` 로 바뀔 때 남은
  것이다. **DB 컬럼이 아니라 `HistoryMapper.xml` 에서 `r.id` 에 붙이던
  별칭**이라 DDL 은 필요 없었다. 고친 곳은 VO · XML · 테스트 2개 ·
  정답지 2개 · 프론트 `HistoryPage`.
- `docs/PROGRESS.md` · `BOARD_SPEC` 의 `post_service.py` · `/api/posts` 는
  **그대로 뒀다.** 파이썬 시절의 사실 기록이고, `git show` 명령에 쓰는 실제
  경로다. 이름을 바꾸면 그 명령이 안 먹는다.
- `StubFixtureTest` 의 안 쓰는 import 삭제. `backend-spring/bin/`(IDE 빌드
  산출물 1.8M, git 제외)도 지웠다 — 패키지를 옮기기 전의 `.class` 가 남아
  있어 IDE 를 헷갈리게 한다. 빌드하면 다시 생긴다.

### 아직 남은 것

1. **브라우저로 직접 클릭해 보기** — 위에서 못 누른 것들.
   로그인 → 홈 → 룰렛 → 레시피 → 찜 → 이걸로 먹을래요 → History 펼치기
   → 마이페이지 닉네임 변경 → 로그아웃
2. **`develop` 병합** — `feature/kongmg` 가 35개 앞서 있다. 팀원이 `develop` 를
   받으면 기능별 패키지도 VO 통합도 없는 코드를 본다.
3. (선택) `RecipeReviewVO` 에 `@JsonIgnore` 붙이기 — `ReviewCommentVO` ·
   `NoticeVO` 와 달리 아직 하나도 없어서 15칸이 전부 요청으로 들어온다.
   `docs/BOARD_SPEC.md` 3절에 적어뒀다. 담당자가 쓸 칸을 정할 때 같이 하면 된다.
4. (선택) `docs/PROGRESS.md` 의 `@LoginUser` · `AppProperties` 설명 — 지금 코드에
   없는 것이다. 파이썬→자바 대조 학습 기록이라 그대로 둬도 된다.
   (`STUDY_AUTH.md` 에는 머리말에 "옛 구조 기준" 경고를 달아뒀다)

---

## 4. 테스트 돌리는 법

```
cd backend
set EMAIL_PROVIDER=mock        ← 꼭! smtp 면 테스트가 진짜 메일을 보내려고 한다
set WEATHER_PROVIDER=mock      ← 실제 날씨면 기온 비교 테스트가 틀린다
(DB 접속 정보 환경변수: DB_HOST, DB_PASSWORD, JWT_SECRET_KEY 등)
gradlew seedDemo               ← 처음 한 번 (데모 계정 만들기)
gradlew test
```

- DB 접속 정보는 이 PC 의 옛 파이썬 `backend/.env` 에 있었음. 지금은 `backend/src/main/resources/application-local.yml` 이다 (둘 다 git 에 안 올라감).
- 팀 DB `10.1.54.120` 은 **학원 내부망이라 집에서는 안 될 수 있음.** 집에서는 컴파일만 확인: `gradlew compileJava compileTestJava`

---

## 5. 다른 PC 에서 이어 하려면

백엔드 변경은 **커밋 · 푸시 끝났다** (`b247cba`, `feature/kongmg`). 그래서:

1. `git fetch` → `git checkout feature/kongmg` → `git pull`
2. 이 문서(`docs/WORKLOG.md`)의 **3. 남은 일** 부터 이어서
3. 집에서는 팀 DB(`10.1.54.120`)가 안 붙을 수 있으니 컴파일만 확인:
   `gradlew compileJava compileTestJava`
