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

### 아직 남은 것

1. **브라우저로 직접 확인**: 로그인 → 홈 → 룰렛 모양 → 레시피 → 찜 → 이걸로 먹을래요
   → History → 마이페이지 닉네임 변경 → 로그아웃
2. **팀 DB 에서 `gradlew test` 한 번 더** — 컴파일이 깨져 있던 동안은 못 돌렸다.
   기준선은 94개 중 91개 통과 (실패 3개는 정답지 회원 id 차이).
3. (선택) `docs/STUDY_AUTH.md` · `docs/PROGRESS.md` 의 `@LoginUser` · `AppProperties`
   설명 — 지금 코드에는 없는 것이다. 다만 파이썬→자바 대조 학습 기록이라 그대로 둬도 된다.
   (`STUDY_AUTH.md` 는 현재 설계처럼 읽힐 수 있으니 머리말에 "옛 방식" 한 줄만 달아두면 충분)
4. 이 PC 의 임시 폴더(Claude 스크래치패드 `.../scratchpad/be`)는 지워도 됨 — 전부 프로젝트로 복사했음

---

## 4. 테스트 돌리는 법

```
cd backend-spring
set EMAIL_PROVIDER=mock        ← 꼭! smtp 면 테스트가 진짜 메일을 보내려고 한다
set WEATHER_PROVIDER=mock      ← 실제 날씨면 기온 비교 테스트가 틀린다
(DB 접속 정보 환경변수: DB_HOST, DB_PASSWORD, JWT_SECRET_KEY 등)
gradlew seedDemo               ← 처음 한 번 (데모 계정 만들기)
gradlew test
```

- DB 접속 정보는 이 PC 의 `backend/.env` 에 있었음 (git 에 안 올라감).
- 팀 DB `10.1.54.120` 은 **학원 내부망이라 집에서는 안 될 수 있음.** 집에서는 컴파일만 확인: `gradlew compileJava compileTestJava`

---

## 5. 다른 PC 에서 이어 하려면

백엔드 변경은 **커밋 · 푸시 끝났다** (`b247cba`, `feature/kongmg`). 그래서:

1. `git fetch` → `git checkout feature/kongmg` → `git pull`
2. 이 문서(`docs/WORKLOG.md`)의 **3. 남은 일** 부터 이어서
3. 집에서는 팀 DB(`10.1.54.120`)가 안 붙을 수 있으니 컴파일만 확인:
   `gradlew compileJava compileTestJava`
