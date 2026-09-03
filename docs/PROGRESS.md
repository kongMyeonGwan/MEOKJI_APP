# 진행 상황 및 작업 기록

최종 갱신: 2026-09-04

---

## 전체 로드맵 (STEP 1~25)

| STEP | 내용 | 상태 |
|:--:|---|:--:|
| 1 | 전체 설계 (7테이블 ERD, API 목록, Flow) | ✅ |
| 2 | Oracle DDL 확정 | ✅ |
| 3 | FastAPI 기본 구조 · DB 연결 · CORS · Health Check | ✅ |
| 4 | React 기본 구조 · Router · Header · Layout | ✅ |
| 5 | 회원가입 | ✅ |
| 6 | JWT 로그인 · 로그아웃 · Protected API | ✅ |
| 7 | 로그인 실패 20회 → 계정 Lock | ✅ |
| 8 | Email OTP · 비밀번호 재설정 | ✅ |
| 9 | Ban CRUD (DISLIKE / ALLERGY) | ✅ |
| 10 | AI API 연동 · Structured Output | ⬜ **← 다음 작업** |
| 11 | 직접 음식 검색 → Recipe 생성 | ⬜ |
| 12 | Weather API | ⬜ |
| 13 | 날씨 + AI 음식 추천 | ⬜ |
| 14 | Ban을 추천/레시피에 연결 | ⬜ |
| 15 | 레시피 확정 → meal_histories INSERT | ⬜ |
| 16 | History 조회 | ⬜ |
| 17 | Shopping API 실제 연동 (네이버 · 쿠팡 · 알리) | ⬜ |
| 18 | 재료별 **최저가/최고가** 계산 | ⬜ |
| 19 | Mock Shopping Provider | ⬜ |
| 20~23 | 게시판 · 수정삭제 · 좋아요 · 댓글 | ⬜ |
| 24~25 | UI 개선 · 통합 테스트 | ⬜ |

---

## 🚨 다음에 할 일 (우선순위 순)

### 1. STEP 10 — AI API 연동 · Structured Output

- 추천 결과를 자유 텍스트로 받으면 파싱이 계속 깨진다. **Structured Output**으로
  스키마를 고정해서 받을 것
- `services/ban_service.list_bans()`로 Ban 목록을 읽어 프롬프트에 넣는다.
  `ALLERGY`는 "반드시 제외", `DISLIKE`는 "가능하면 피하고 대체재료 제시"로
  강도를 구분한다 (프론트 MyPage 문구와 이미 맞춰져 있다)
- AI 호출은 느리다. `api/client.js`의 timeout이 30초로 잡혀 있는 것도 이 때문이다
- 키는 `.env`로 빼고 `.env.example`에 항목만 적을 것

---

## 추가 확정된 요구사항 (2026-08-17)

프론트 화면은 하드코딩 데이터로 이미 만들어뒀다. 백엔드에서 채울 부분만 적는다.

### 1. 좋아요 받은 레시피 — 기간 검색 (대시보드)

- 기본값: **오늘 포함 최근 30일**
- 사용자가 시작일~종료일을 직접 고르면 그 기간에 좋아요를 받은 목록만 표시
- 프론트: `components/common/DateRangeFilter.jsx`, 화면은 `HomePage`

**서버에서 할 일**
```sql
-- post_likes.created_at 기준. 양 끝 포함
WHERE pl.created_at >= :start AND pl.created_at < :end + 1
```
- `created_at`은 **UTC로 저장**돼 있다. 화면에서 고른 날짜는 KST 기준이므로
  경계에서 9시간이 어긋난다. 서버에서 KST -> UTC 변환 후 비교할 것
- 정렬은 좋아요 수 내림차순. 기간이 넓으면 페이징 필요

### 2. 예산 안에서 레시피 추천

- 사용자가 금액(예: 8000원)을 입력하면 **그 금액 이내로 만들 수 있는 레시피만** 추천
- 프론트: `components/recommendation/BudgetFilter.jsx`

**서버에서 할 일**
- AI 프롬프트에 예산 상한을 넣어 **생성 단계에서** 제한한다.
  생성 후 걸러내면 후보가 0건이 되는 경우가 잦다
- 예산을 못 맞추면 "가장 가까운 금액" 대안을 함께 내려줄 것

### 3. 최저가 / 최고가 비교

- 재료 하나당 **네이버쇼핑 · 쿠팡 · 알리익스프레스** 세 곳의 가격을 모은다
- 가장 싼 곳과 가장 비싼 곳을 나란히 보여주고, 합계도 구간으로 표시
- 프론트: `components/price/PriceTable.jsx` (`getPriceSummary()`로 합계 계산)

**응답 형태** — 프론트가 이 모양을 기대한다
```json
[
  { "name": "돼지고기 앞다리살 200g",
    "offers": [
      { "shop": "쿠팡", "price": 3980, "link": "..." },
      { "shop": "네이버쇼핑", "price": 4350, "link": "..." }
    ] }
]
```
- `.env`의 `PRICE_PROVIDERS`에 켜둔 몰만 조회한다 (쉼표 구분)
- 알리는 해외배송이라 배송비·기간 차이가 크다. 화면에 주의 문구를 넣어뒀다
- 세 곳을 순차 호출하면 느리다. `httpx.AsyncClient`로 **병렬 호출**할 것

---

## 화면 개편 (2026-08-24) — 대시보드 시안 반영

시안(`현재UI.png`)에 맞춰 프론트를 다시 짰다. **화면은 전부 mocks로 동작한다.**
여기 적는 건 서버가 채워야 할 부분이다.

### 새로 생긴 화면

| 경로 | 화면 | 서버가 할 일 |
|---|---|---|
| `/game`, `/game/worldcup`, `/game/balance` | 메뉴 게임 3종 | 후보 목록만 내려주면 된다. 진행은 화면에서 한다 |
| `/search?q=` | 레시피 검색 | 검색어를 **그대로 AI에 넘긴다**. "8천원 이하 파스타" 같은 문장 해석이 목적이라 SQL LIKE로는 안 된다 |
| `/fridge` | 냉장고 재료로 찾기 | 고른 재료를 프롬프트에 넣어 만들 수 있는 메뉴만 생성. 만들어놓고 거르면 0건이 되기 쉽다 |
| `/ai-history` | AI 추천 기록 | 언제·어떤 조건으로·무엇을 받았는지. 레시피 본문은 저장하지 않는다(§13 원칙 유지) |
| `/settings` | 설정 | 없음. 테마·알림은 브라우저 localStorage에만 둔다 |
| `/liked` | 찜한 레시피 | 찜 테이블이 필요하다. 지금은 `food_name` 배열을 localStorage에 둔다 |
| `/popular` | 인기 레시피 전체 | 기존 "좋아요 받은 레시피"의 이름을 바꾼 것 |

`/liked`가 **찜한 레시피**(내가 담은 것)로 바뀌었고, 남들이 많이 누른 목록은
`/popular`로 갈라졌다. 대시보드 오른쪽 순위가 후자다.

### 서버에서 채울 값

- **요리 레벨** (`MOCK_PROFILE`) — `GET /users/me` 응답에 `level` `title` `exp`
  `exp_next` `streak_days` `recommend_count` `cook_count` 를 함께 싣는다.
  퍼센트로 계산해서 내리지 말 것. 화면마다 필요한 표현이 다르다
- **알림** (`MOCK_NOTIFICATIONS`) — 읽음 여부(`read`)가 상단바 배지 숫자가 된다
- **인기 레시피 기간 집계** — 지금은 화면에서 거른다. 기간 안에 **새로 눌린**
  좋아요만 세야 한다. 누적으로 세면 오래된 글이 영원히 1위다
- **음식별 주재료** (`MOCK_FOOD_INGREDIENTS`) — 냉장고 찾기 매칭용

### 다크 모드

`<html data-theme="dark">` 한 곳만 바뀌고 색은 전부 `styles/index.css`의
CSS 변수에서 갈린다. **컴포넌트 CSS에 색상 리터럴(`#fff` 등)을 새로 적지 말 것.**
적는 순간 그 자리만 라이트 모드로 남는다. 흰 배경 위에 얹는 하트 버튼처럼
의도적으로 고정한 곳에는 주석을 달아뒀다.

첫 페인트 전에 테마를 정하려고 `index.html`에 인라인 스크립트를 하나 뒀다.
React가 뜬 뒤에 칠하면 다크 모드 사용자에게 흰 화면이 한 프레임 번쩍인다.

---

## 나중에 보완할 것 (MVP에는 없어도 되지만 기억해둘 것)

지금 일부러 넣지 않은 것들이다. 급하지 않지만 실제 서비스로 간다면 필요하다.

| 항목 | 내용 |
|---|---|
| 인증번호 재발송 제한 | 지금은 누르는 만큼 계속 발송된다. "1분에 1회" 같은 제한 필요 |
| 발신 전용 계정 | 개인 Gmail은 스팸 처리 위험 + 하루 500통 제한. 도메인 메일이나 SES/SendGrid로 교체 |
| 자동 테스트 | `pytest`가 설치만 되어 있고 테스트가 하나도 없다. 인증 흐름부터 묶는 것이 좋다 |
| 로그인 시도 시간차 | 없는 이메일은 `verify_password`를 건너뛰어 응답이 빠르다. 응답 시간으로 계정 유무가 샌다 |

---

## 🚨 지금 깨져 있는 것 (2026-09-04 머지 직후)

스키마 v2에서 테이블 3개의 이름이 바뀌었는데, 팀원이 그 전에 붙여둔
게시판 · History API는 옛 이름을 참조한다. **import는 되지만 조회하면
`ORA-00942: 테이블 또는 뷰가 존재하지 않습니다`가 난다.**

| 옛 이름 | 새 이름 |
|---|---|
| `posts` | `recipe_reviews` |
| `post_likes` | `review_likes` |
| `comments` | `review_comments` |
| `posts.post_id` | `recipe_reviews.review_id` |

고쳐야 하는 파일

- `models/post.py` · `models/post_like.py` · `models/comment.py` — `__tablename__`과 컬럼
- `models/meal_history.py` — `use_yn` 컬럼 추가
- `services/post_service.py` (271줄) — 조회에 `use_yn='Y'` 필터, 삭제를 논리 삭제로
- `services/history_service.py` — 같은 처리
- `routers/posts.py` · `schemas/post.py` — 필드명
- `frontend/src/api/posts.js` · `PostWritePage.jsx` · `PostDetailPage.jsx` · `CommunityPage.jsx`

또한 v2는 `food_name` / `recipe_json` / `ingredients_json`을 `recipe_reviews`에
두지 않고 `meal_histories`와 JOIN해서 읽는다. 팀원 버전은 값을 복사해 두는
설계였고 "Guest가 남의 MEAL_HISTORIES를 JOIN하지 않게 한다"는 이유를 달아뒀다.
**어느 쪽으로 갈지 팀에서 정한 뒤** 위 파일들을 고쳐야 한다.

`db/02_seed.sql`(팀원 작성)도 옛 스키마 기준이라 그대로 실행되지 않는다.

---

## 스키마 v2 (2026-09-04) — 논리 삭제 · 후기 게시판 FK

### 무엇이 바뀌었나

| # | 변경 | 이유 |
|:--:|---|---|
| 1 | 6개 테이블에 `USE_YN CHAR(1)` 추가 | 삭제 · 탈퇴를 물리 삭제 대신 `'N'` 갱신으로 |
| 2 | `UNIQUE` 제약 4개 → 함수 기반 UNIQUE 인덱스 | `'N'` 행이 자리를 차지해 재등록이 막히는 것을 방지 |
| 3 | `POSTS` → `RECIPE_REVIEWS` | "레시피 추천" 게시판이라는 성격을 이름에 반영 |
| 4 | `RECIPE_REVIEWS`에 `MEAL_HISTORY_ID` 합성 FK 추가 | 직접 만들어본 요리만 후기를 쓸 수 있게 DB가 강제 |
| 5 | `POST_LIKES` → `REVIEW_LIKES`, `COMMENTS` → `REVIEW_COMMENTS` | `post_id` → `review_id` |
| 6 | `USERS`에 `GENDER` + 주소 4개 컬럼 추가 | 대시보드 첫 화면의 날씨 조회 (STEP 12) |

`VERIFICATION_CODES`에는 `USE_YN`을 붙이지 않았다. OTP는 사용자가 삭제하는
데이터가 아니고 `used_at` / `expires_at`으로 이미 생명주기를 관리한다.

### 왜 UNIQUE를 인덱스로 바꿨나

논리 삭제에서 가장 먼저 터지는 곳이다. `uq_post_likes(user_id, post_id)`가
평범한 UNIQUE면, 좋아요를 취소(`'N'`)한 뒤 다시 누르는 순간 `ORA-00001`이 난다.
행이 그대로 남아 있기 때문이다. 같은 문제가 Ban 재등록 · 탈퇴 이메일 재가입에도 있다.

```sql
CREATE UNIQUE INDEX uq_review_likes ON review_likes (
    CASE WHEN use_yn = 'Y' THEN user_id   END,
    CASE WHEN use_yn = 'Y' THEN review_id END
);
```

`'N'` 행은 모든 키 컬럼이 `NULL`이 되어 인덱스에 들어가지 않는다.
살아있는 행끼리만 유일성을 본다.

**대신 이 인덱스는 `WHERE user_id = :v` 조회에 쓰이지 않는다.** 선두가 표현식이라
옵티마이저가 매칭하지 못한다. 그래서 조회용 일반 인덱스를 `(col, use_yn)`으로
따로 만들었다. `idx_users_email` · `idx_user_bans_user` · `idx_review_likes_review`가 그것이다.

### 왜 합성 FK인가

```sql
CONSTRAINT fk_reviews_meal FOREIGN KEY (meal_history_id, user_id)
                           REFERENCES meal_histories (id, user_id)
```

`meal_history_id` 하나만 참조하면 "존재하는 히스토리"인 것만 보장되고,
**남의 히스토리를 자기 후기로 공유하는 것**은 막지 못한다. 쌍으로 걸면
글쓴이와 히스토리 주인이 다를 때 DB가 `ORA-02291`로 거부한다.
Oracle은 PK/UNIQUE 조합만 참조 대상으로 허용하므로
`meal_histories`에 `uq_meal_id_user UNIQUE (id, user_id)`를 함께 넣었다.

### FK로 못 막는 것 — 서비스 계층의 책임

`ON DELETE CASCADE`와 참조 차단은 **진짜 `DELETE`에만 작동한다.**
`UPDATE use_yn='N'`은 DB 입장에서 그냥 값 수정이라 아무 반응도 없다. 그래서:

- **회원 탈퇴** — `users`만 `'N'`으로 바꾸면 그 사람의 후기 · 댓글 · 좋아요는
  `'Y'`로 남아 화면에 계속 보인다. 하위 테이블까지 연쇄 UPDATE 해야 한다. **(미구현)**
- **"후기가 있으면 히스토리 삭제 금지"** — 논리 삭제는 FK가 못 막는다.
  `'N'`으로 바꾸기 전에 살아있는 후기가 있는지 검사해야 한다. **(미구현)**

### 실측으로 확인한 것 (25건 중 24건 통과, 1건은 테스트 쪽 오류)

| 확인한 동작 | 결과 |
|---|---|
| 남의 히스토리로 후기 작성 | `ORA-02291` 거부 ✅ |
| 같은 식사로 후기 2번 | `ORA-00001` 거부 ✅ |
| 후기 삭제(`'N'`) 후 같은 식사로 재작성 | 성공 ✅ |
| 좋아요 취소 후 재클릭 | 성공 ✅ |
| Ban 해제 후 같은 재료 재등록 | 성공 ✅ |
| 탈퇴 후 같은 이메일 재가입 | 성공 ✅ |
| 재가입 계정이 살아있는 동안 탈퇴 계정 복구 | `ORA-00001` 거부 (정상) ✅ |
| 후기가 달린 히스토리 `DELETE` | `ORA-02292` 거부 ✅ |
| 후기가 달린 **회원** `DELETE` | **통과됨.** 연쇄 삭제가 `recipe_reviews`를 먼저 지워서 막히지 않는다. `DELETE FROM users`를 손으로 실행하면 그 회원의 모든 데이터가 사라진다 ⚠️ |

### 적용한 코드

- `db/01_schema.sql` — 새로 만드는 DB용 (단일 소스)
- `db/02_migrate_v2.sql` — 구버전 DB용 1회성 마이그레이션
- `models/types.py` — `USE_YN_YES` / `USE_YN_NO` 상수
- `models/user.py` · `models/user_ban.py` — `use_yn` + 신규 컬럼
- `services/ban_service.py` — `delete_ban`이 물리 삭제 → `use_yn='N'`, 목록 조회에 필터
- `services/auth_service.py` · `services/password_reset_service.py` · `core/deps.py` — 사용자 조회에 `use_yn='Y'` 필터

### 아직 안 한 것

- 프론트엔드 `mocks/index.js`의 `MOCK_POSTS`는 아직 v1 구조(`food_name` 직접 보유)
- `recipe_reviews` · `review_likes` · `review_comments` ORM 모델 · 라우터 (STEP 20~23)
- 회원 탈퇴 API 자체가 없다

---

## 완료된 작업 상세

### STEP 1 — 설계
- 7테이블 확정: `users` `verification_codes` `user_bans` `meal_histories` `posts` `post_likes` `comments`
  (2026-09-04 v2에서 뒤 3개가 `recipe_reviews` `review_likes` `review_comments`로 바뀌었다)
- REST API 30여 개 정의
- Guest/User 권한표 확정

### STEP 2 — Oracle DDL
- `db/01_schema.sql` — 테이블 7 · PK 7 · FK 8 · UNIQUE 4 · CHECK 11 · 인덱스 6
  (v2에서 UNIQUE 4개가 함수 기반 인덱스로 바뀌고 CHECK · 인덱스가 늘었다. 아래 v2 항목 참고)
- `db/00_drop_all.sql` — Oracle에 `DROP TABLE IF EXISTS`가 없어 PL/SQL 블록으로 작성

### STEP 3 — FastAPI 기본 구조
- `main.py` · `core/config.py` · `database/database.py` · `routers/health.py`
- `GET /api/health` — 앱 + DB 상태를 한 번에 확인. DB 장애 시 503

### STEP 4 — React 기본 구조
- Vite 6 + React 19 + react-router-dom 7
- `api/client.js` — axios 인스턴스 + JWT 인터셉터(준비됨) + 에러 정규화
- Header · MainLayout · HomePage(Dashboard 골격) · 스텁 페이지 6개

### STEP 5 — 회원가입
**Backend**
- `models/types.py` — `UtcTimestamp`, `OracleJSON`
- `models/user.py` · `core/security.py` · `schemas/auth.py`
  · `services/auth_service.py` · `routers/auth.py`
- `POST /api/auth/register` → 201 / 409(중복) / 422(검증)

**Frontend**
- `pages/RegisterPage.jsx` + CSS — 폼, 클라이언트 검증, 필드별 에러 표시
- `api/auth.js` · `utils/apiError.js`

**검증 결과** (2026-08-17, 로컬 Oracle 21c XE)
- `POST /api/auth/register` → 201, 한글 닉네임 정상 저장
- `created_at` 이 UTC로 저장됨 (KST 01:41 요청 → DB 16:41). `UtcTimestamp` 정상 동작
- `password_hash` = `$2b$12$...` 60자. 평문 아님

### STEP 6 — JWT 로그인

**Backend**
- `core/security.py` — `create_access_token()` / `decode_token()`
- `core/deps.py` (신규) — `get_current_user()` / `get_optional_user()`
- `schemas/auth.py` — `LoginRequest` · `TokenResponse`
- `services/auth_service.py` — `login_user()`
- `POST /api/auth/login` → 200 / 401 / 423
- `GET /api/auth/me` → 200 / 401

**Frontend**
- `contexts/AuthContext.jsx` (신규) — 토큰 보관 · 새로고침 복원 · 자동 로그아웃
- `components/common/ProtectedRoute.jsx` (신규)
- `pages/LoginPage.jsx` + CSS
- `Header.jsx` — 로그인 상태에 따라 메뉴 전환
- `api/client.js` — 401 발생 시 로그아웃시키는 콜백 훅 추가

**검증 결과** (2026-08-17, HTTP 직접 호출)

| 케이스 | 결과 |
|---|---|
| 올바른 비밀번호 | 200 + 토큰 (`exp - iat = 1800`초) |
| 토큰으로 `/me` | 200 |
| 비밀번호 틀림 | 401 |
| 없는 이메일 | 401 (**문구 동일**) |
| 토큰 없음 / 위조 | 401 |

### STEP 7 — 계정 잠금

- `services/auth_service.py`의 `login_user()`만 수정. 프론트는 수정 없음
  (서버가 문자열 detail로 내려주고 `utils/apiError.js`가 그대로 화면에 넘긴다)
- 실패 시 `login_fail_count += 1` → 20회 도달 시 `is_locked=1` · `locked_at` 기록
- 성공 시 카운터 0으로 리셋 (실패가 없었으면 UPDATE를 보내지 않는다)
- 이미 잠긴 계정은 비밀번호 검증 전에 423으로 끊는다. 안 그러면 잠긴 뒤에도
  카운트가 계속 올라간다

**검증 결과** (2026-08-17)

| 케이스 | 결과 |
|---|---|
| 1~19회차 | 401 + `(남은 시도 N회)` |
| 20회차 | 423, `is_locked=1` · `locked_at` 기록 |
| 잠긴 뒤 올바른 비밀번호 | 423, 카운트 증가 없음 |
| 2회 실패 후 성공 | `login_fail_count` 2 → 0 |

### STEP 8 — Email OTP 비밀번호 재설정

**Backend**
- `models/verification_code.py` (신규)
- `integrations/email/` (신규) — `base.py`(Protocol) · `mock.py` · `__init__.py`(팩토리)
- `core/security.py` — `generate_otp_code()` · `hash_otp_code()` · `verify_otp_code()`
- `services/password_reset_service.py` (신규)
- `POST /api/auth/password-reset/send` · `/verify` · `/change`

**Frontend**
- `pages/PasswordResetPage.jsx` + CSS (신규) — 3단계를 한 화면에서 전환
- `LoginPage.jsx` 에 "비밀번호를 잊으셨나요?" 링크

**검증 결과** (2026-08-17)

| 케이스 | 결과 |
|---|---|
| 없는 이메일로 send | 200 (가입 여부 노출 안 함) |
| 틀린 코드 | 400 + `(남은 시도 N회)`, `attempt_count` 증가 |
| 맞는 코드 | 200, `used_at` 기록 |
| 같은 코드로 재검증 | 400 (일회성 유지) |
| change 성공 | 200, 코드 행 삭제됨 |
| 같은 코드로 재변경 | 400 |
| 잠긴 계정 → 재설정 후 로그인 | **200** (`is_locked` 0, `locked_at` NULL) |
| 5회 틀림 | 429 (재발급 안내) |

**실제 SMTP 발송** (2026-08-17 추가)

Mock만으로는 부족해서 실제 발송까지 붙였다.

- `integrations/email/smtp.py` (신규) — 표준 `smtplib`. 포트로 STARTTLS(587) / SSL(465) 자동 판별
- `integrations/email/__init__.py` — `smtp` 분기 추가
- `scripts/check_email.py` (신규) — 전체 흐름을 돌리지 않고 SMTP 접속만 점검
- `.env.example` — 항목별 설명 주석

발신 계정은 Gmail 하나(`EMAIL_PROVIDER=smtp`). 받는 사람은 DB의 `user.email`이라
회원이 어느 메일 서비스를 쓰든 그 주소로 간다.

| 확인 | 결과 |
|---|---|
| Gmail → Gmail | 발송 성공 |
| Gmail → 네이버 | 발송 성공 (타 서비스 수신 확인) |

**Gmail 설정 시 주의**
- Google 계정 비밀번호로는 SMTP 로그인이 안 된다. 2단계 인증을 켜고
  https://myaccount.google.com/apppasswords 에서 앱 비밀번호 16자리를 발급받아야 한다
- `.env`는 서버가 시작할 때 한 번만 읽는다. `--reload`는 코드만 감지하므로
  `.env`를 고쳤으면 **서버를 재시작**해야 한다
- 개인 Gmail에서 자동 발송한 메일이라 수신측 스팸함으로 갈 수 있다

### STEP 9 — Ban CRUD

**Backend**
- `models/user_ban.py` (신규) — `user_bans` 매핑
- `schemas/ban.py` (신규) — `BanCreateRequest` · `BanResponse`
- `services/ban_service.py` (신규) — `list_bans()` · `create_ban()` · `delete_ban()`
- `routers/bans.py` (신규) — 세 엔드포인트 전부 `get_current_user` 필요
- `main.py` — 라우터 등록

| 메서드 | 경로 | 응답 |
|---|---|---|
| GET | `/api/bans` | 200 (등록 순) |
| POST | `/api/bans` | 201 / 409(중복) / 422(검증) |
| DELETE | `/api/bans/{id}` | 204 / 404 |

**Frontend**
- `api/bans.js` (신규)
- `pages/MyPage.jsx` — `MOCK_BANS` 제거하고 실제 API 연결.
  로딩 · 추가 중 · 삭제 중 상태를 각각 표시한다

**검증 결과** (2026-08-23, 로컬 Oracle 21c XE · 25건 전부 통과)

| 케이스 | 결과 |
|---|---|
| 토큰 없음 / 위조 토큰 | 401 |
| 추가 · 목록 · 삭제 | 201 / 200 / 204 |
| `ban_type` 생략 | `DISLIKE`로 저장 |
| 같은 재료 재등록 | 409 |
| `' 오이 '`로 재등록 | 409 (strip 후 비교되므로 뚫리지 않음) |
| 빈 문자열 · 공백만 · 101자 · `ban_type='HATE'` | 422 |
| 한글 100자 | 201 (`VARCHAR2(100 CHAR)` 정상) |
| 남의 Ban 조회 / 삭제 | 목록에 안 보임 / 404 |
| 다른 사용자가 같은 이름 등록 | 201 (uq는 user_id와 묶여 있다) |
| 삭제한 id 재삭제 · 없는 id | 404 |
| `DELETE /api/bans/0` | 422 (`Path(ge=1)`) |
| 사용자 삭제 | `user_bans` 행도 CASCADE 삭제 |
| `created_at` | UTC 저장 (KST 04:47 → DB 19:47) |

---

## 설계 결정 기록

### 왜 CSS를 styles/ 아래로 몰았나 (2026-08-23)
JSX와 CSS가 한 폴더에 섞여 있어 `pages/`를 열면 파일 수가 두 배로 보였다.
`src/styles/` 아래에 **소스 폴더 구조를 그대로 미러링**해서 옮겼다.

```
src/pages/LoginPage.jsx           -> import '../styles/pages/LoginPage.css'
src/components/common/TopBar.jsx  -> import '../../styles/components/common/TopBar.css'
```

`src/styles/index.css`는 전역이라 그대로 뒀다.
CSS 파일을 새로 만들 때도 이 규칙을 따를 것. 클래스명 규칙(BEM 풍의
`.mypage__ban-name`)은 바뀐 게 없다.

### 왜 Ban 추가는 사전 조회 없이 IntegrityError만 잡나
회원가입(`register_user`)은 이메일/닉네임 **두 개**의 UNIQUE가 있어서, 어느 쪽이
중복인지 알려주려고 사전 `SELECT`를 한 번 한다. Ban은 `uq_user_bans` 하나뿐이라
`IntegrityError`가 곧 "재료명 중복"이다. 사전 조회를 넣어봐야
SELECT-INSERT 사이의 경합은 못 막으면서 쿼리만 늘어난다.

### 왜 남의 Ban을 지우려 하면 403이 아니라 404인가
403은 "권한은 없지만 그 리소스는 존재한다"는 뜻이 된다. id를 1씩 올려가며
403/404를 세면 남의 Ban이 몇 개인지 알 수 있다. 없는 id와 남의 id를 같은 404로
묶어서 그 차이를 없앴다.

### 왜 재료명을 스키마에서 `strip()` 하나
`'오이'`와 `'오이 '`는 다른 문자열이라 `uq_user_bans`가 중복으로 보지 않는다.
DB에 닿기 전에 잘라내야 UNIQUE 제약이 의도대로 동작한다. 회원가입 닉네임과 같은 이유다.

### 왜 PostgreSQL에서 Oracle로 바꿨나
프로젝트 방침 변경. 전환 시점에 SQLAlchemy 모델을 하나도 안 만든 상태여서
비용이 거의 없었다. 영향 파일은 5개뿐이었고 프론트엔드는 수정 0건.

### 왜 change 단계에서 코드 행을 지우나
`used_at`이 찍힌 코드는 `verify`를 다시 통과하지 못하지만, `change`는
"`used_at`이 있는가"를 조건으로 삼기 때문에 같은 코드로 몇 번이든 호출할 수 있다.
비밀번호를 바꾼 뒤 행을 삭제해서 그 구멍을 막는다.

### 왜 `verification_codes.used_at` 컬럼을 뒀나
`verify-code`와 `change`가 별도 엔드포인트라, 인증 성공 사실을 어딘가 남겨야
`change` 단계에서 "검증된 요청"인지 판별할 수 있다. 없으면 같은 코드로
비밀번호를 여러 번 바꿀 수 있다(OTP 일회성 위반).

### 왜 JSON을 CLOB에 넣나
Oracle 21c에 네이티브 `JSON` 타입이 있지만 **SQLAlchemy 2.0.52의 Oracle
dialect가 지원하지 않는다** (`visit_JSON` 미구현 → `CompileError`).
`CLOB` + `CHECK (... IS JSON)`이 유일하게 성립하는 선택지.
레시피 JSON은 통째로 읽고 쓰는 스냅샷이라 성능 손실도 무의미하다.

### 왜 passlib 대신 bcrypt를 직접 쓰나
passlib 1.7.4는 2020년 이후 릴리스가 없어 bcrypt 5.x와 호환되지 않는다.
`AttributeError: module 'bcrypt' has no attribute '__about__'`.

### 왜 로그인 실패 시 남은 횟수를 알려주나
사용성을 위해 그렇게 정했다 (2026-08-17).

단, 이건 트레이드오프다. `(남은 시도 N회)` 문구는 **존재하는 계정에만** 붙으므로,
없는 이메일과 응답을 비교하면 가입 여부를 알아낼 수 있다.
"없는 이메일과 틀린 비밀번호를 구분하지 않는다"는 로그인 정책이 이 부분에서는
사실상 무력해진다.

계정 열거를 막는 쪽으로 바꾸려면 `login_user()`의 401 메시지를
`_INVALID_CREDENTIALS` 상수로 되돌리면 된다. 그 한 줄이 전부다.

### 왜 좋아요/댓글 수를 컬럼으로 안 두나
MVP에서는 `COUNT()` 집계로 충분하다. 비정규화 컬럼은 동기화 버그 위험이
성능 이득보다 크다.

---

## ⚠️ Oracle 관련 함정 (실측으로 확인한 것들)

앞으로 코드 쓸 때 계속 걸리는 것들이라 기록해둔다.

### 1. `VARCHAR2(n)`은 바이트 단위다
```sql
VARCHAR2(50)        -- 한글 16자에서 잘림 (DB가 BYTE 시맨틱)
VARCHAR2(50 CHAR)   -- 한글 50자 정상        <- 반드시 이렇게
```
이 DB의 `NLS_LENGTH_SEMANTICS`는 `BYTE`다. SQLAlchemy는 자동으로 `CHAR`를
붙여주지만, 수기 DDL에서는 직접 써야 한다.

### 2. 타임존이 조용히 어긋난다 (가장 위험)
python-oracledb는 tzinfo를 **저장·조회 양방향에서 버린다.**
```
바인딩: 08:00+00:00 (UTC)  ->  DB 저장: 08:00 +09:00   ← 9시간 오차, 에러 없음
조회  : DB의 +09:00 값      ->  파이썬: tzinfo=None
```
**대응**: DB에는 UTC만 저장한다.
- DDL: `TIMESTAMP(6) DEFAULT SYS_EXTRACT_UTC(SYSTIMESTAMP)`
- 모델: `models/types.py`의 `UtcTimestamp` 타입 사용 (경계에서 변환)
- 커넥션: 접속 시 `ALTER SESSION SET TIME_ZONE='+00:00'` (database.py)

### 3. 빈 문자열이 NULL이다
`''`를 `NOT NULL` 컬럼에 넣으면 `ORA-01400`으로 500이 난다.
Pydantic 스키마에 `min_length=1`을 반드시 건다.

### 4. `SELECT 1`이 안 된다
Oracle은 FROM 절이 필수다. `SELECT 1 FROM DUAL`.

### 5. DBeaver가 빈 줄에서 SQL을 자른다
`CREATE TABLE` 안에 빈 줄이 있으면 문장이 쪼개져 `ORA-00900`이 난다.
DDL 파일은 이미 빈 줄을 제거해뒀다.
설정으로도 끌 수 있다: Preferences → Editors → SQL Editor → SQL Processing
→ "Blank line is statement delimiter" → Never

### 6. `ON CONFLICT` / `ILIKE`가 없다
- 중복 INSERT: `MERGE` 또는 `IntegrityError` 캐치 (STEP 22 좋아요에서 필요)
- 대소문자 무시 검색: `LOWER(col) LIKE LOWER(:kw)` (STEP 20 게시판 검색)

### 7. `DROP TABLE IF EXISTS`가 없다
21c에는 없다(23ai부터). `db/00_drop_all.sql`처럼 PL/SQL로 우회.

### 8. bcrypt는 72바이트가 상한이다
한글 비밀번호는 24자가 최대. `schemas/auth.py`에서 바이트 기준으로 검증한다.

### 9. 리스너가 설치 시점의 IP에 고정된다 (개인 PC 설치 시)
XE를 설치하면 `listener.ora`에 그 순간의 IP가 박힌다.
```
(ADDRESS = (PROTOCOL = TCP)(HOST = 192.168.219.101)(PORT = 1521))
```
이러면 `localhost` 접속이 거부된다 — `DPY-6005` / `WinError 10061`.
서비스는 Running이고 DBeaver가 IP로 붙으면 되기 때문에 원인을 찾기 어렵다.

**대응**: `<ORACLE_HOME>/network/admin/listener.ora`에서 `HOST`를 `0.0.0.0`으로
바꾸고 `OracleOraDB21Home1TNSListener` 서비스를 재시작한다.
IP를 직접 적으면 다른 네트워크(집↔학원)로 이동할 때마다 다시 깨진다.

확인: `Get-NetTCPConnection -State Listen -LocalPort 1521` 이 `0.0.0.0`을 보여야 한다.

---

## ⚠️ Windows 개발환경 함정

### 1. pip가 requirements.txt를 cp949로 읽는다
한글 Windows에서 pip는 BOM이 없으면 **OS 기본 인코딩(cp949)** 으로
requirements.txt를 읽는다. 파일에 한글 주석이 있으면 설치가 통째로 실패한다.
```
UnicodeDecodeError: 'cp949' codec can't decode byte 0x80 in position 10
```
**대응**: `requirements.txt`를 **UTF-8 with BOM**으로 저장한다. pip는 BOM을
먼저 검사하므로 pip 버전·OS 로케일과 무관하게 동작한다. (2026-08-17 적용 완료)

### 2. 기본 python이 3.11 미만일 수 있다
이 프로젝트는 Python 3.11+ 가 필요하다. `python --version`이 3.9 등이면
venv 생성 시 버전을 명시한다.
```
py -3.11 -m venv .venv
```

---

## 팀 공용 DB 주의사항

`10.1.54.120:1521/XEPDB1`을 팀원 전체가 공유한다.

- 한 명이 DDL을 실행하면 **전원에게 반영**된다. `00_drop_all.sql` 실행 전 공지할 것
- `email`/`nickname`이 유일해야 해서 테스트 계정이 충돌한다.
  `test-{본인이름}@example.com` 식으로 구분해서 쓸 것
- XE는 동시 세션 수에 제약이 있어 `DB_POOL_SIZE=5`로 작게 잡아뒀다

### 사외에서 작업할 때

`10.1.54.120`은 **사내망 주소**라 집에서는 접속이 안 된다. 선택지:

| 방법 | 비고 |
|---|---|
| VPN | 사내망에 붙을 수 있으면 그대로 사용 |
| 로컬 Oracle XE 설치 | `db/00_create_user_local.sql`(SYS로 실행) → `01_schema.sql` → `.env`의 `DB_HOST=localhost` |
| 프론트엔드만 작업 | DB 없이 가능. `/api/health`가 503이어도 화면은 뜬다 |

`DB_HOST` 한 줄만 바꾸면 되도록 설계해뒀다.
