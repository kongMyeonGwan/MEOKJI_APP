# 진행 상황 및 작업 기록

최종 갱신: 2026-08-14

---

## 전체 로드맵 (STEP 1~25)

| STEP | 내용 | 상태 |
|:--:|---|:--:|
| 1 | 전체 설계 (7테이블 ERD, API 목록, Flow) | ✅ |
| 2 | Oracle DDL 확정 | ⚠️ **재실행 필요** |
| 3 | FastAPI 기본 구조 · DB 연결 · CORS · Health Check | ✅ |
| 4 | React 기본 구조 · Router · Header · Layout | ✅ |
| 5 | 회원가입 | 🔶 코드 완료, DDL 재실행 후 재검증 필요 |
| 6 | JWT 로그인 · 로그아웃 · Protected API | ⬜ |
| 7 | 로그인 실패 20회 → 계정 Lock | ⬜ |
| 8 | Email OTP · 비밀번호 재설정 | ⬜ |
| 9 | Ban CRUD (DISLIKE / ALLERGY) | ⬜ |
| 10 | AI API 연동 · Structured Output | ⬜ |
| 11 | 직접 음식 검색 → Recipe 생성 | ⬜ |
| 12 | Weather API | ⬜ |
| 13 | 날씨 + AI 음식 추천 | ⬜ |
| 14 | Ban을 추천/레시피에 연결 | ⬜ |
| 15 | 레시피 확정 → meal_histories INSERT | ⬜ |
| 16 | History 조회 | ⬜ |
| 17 | Shopping API 실제 연동 | ⬜ |
| 18 | 재료별 최저가 계산 | ⬜ |
| 19 | Mock Shopping Provider | ⬜ |
| 20~23 | 게시판 · 수정삭제 · 좋아요 · 댓글 | ⬜ |
| 24~25 | UI 개선 · 통합 테스트 | ⬜ |

---

## 🚨 다음에 할 일 (우선순위 순)

### 1. DDL 재실행 (필수, 다른 작업의 선행조건)

날짜/시간 정책을 UTC로 바꿨는데 DB는 아직 구 스키마다.

```
DBeaver → MEOKJI_APP 접속
1. db/00_drop_all.sql   Ctrl+A → Alt+X
2. db/01_schema.sql     Ctrl+A → Alt+X
```

확인:
```sql
SELECT column_name, data_type, data_default FROM user_tab_columns
WHERE table_name='USERS' AND column_name='CREATED_AT';
-- 기대: TIMESTAMP(6) / SYS_EXTRACT_UTC(SYSTIMESTAMP)
```

### 2. STEP 5 재검증

DDL 재실행 후 회원가입을 다시 돌려 `created_at`이 UTC로 저장되는지 확인.

```bash
curl -X POST http://localhost:8000/api/auth/register \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"a@b.com\",\"password\":\"myPassword123!\",\"nickname\":\"테스터\"}"
```

### 3. STEP 6 — JWT 로그인

- `core/security.py`에 `create_access_token()` / `decode_token()` 추가
- `core/deps.py`에 `get_current_user()` / `get_optional_user()` 의존성
- `POST /api/auth/login`
- 프론트 `AuthContext` + `ProtectedRoute` + Header 로그인 상태 전환
- `api/client.js`의 `setAuthToken()`은 이미 준비돼 있음

---

## 완료된 작업 상세

### STEP 1 — 설계
- 7테이블 확정: `users` `verification_codes` `user_bans` `meal_histories` `posts` `post_likes` `comments`
- REST API 30여 개 정의
- Guest/User 권한표 확정

### STEP 2 — Oracle DDL
- `db/01_schema.sql` (220줄) — 테이블 7 · PK 7 · FK 8 · UNIQUE 4 · CHECK 11 · 인덱스 6
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

---

## 설계 결정 기록

### 왜 PostgreSQL에서 Oracle로 바꿨나
프로젝트 방침 변경. 전환 시점에 SQLAlchemy 모델을 하나도 안 만든 상태여서
비용이 거의 없었다. 영향 파일은 5개뿐이었고 프론트엔드는 수정 0건.

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

---

## 팀 공용 DB 주의사항

`10.1.54.120:1521/XEPDB1`을 팀원 전체가 공유한다.

- 한 명이 DDL을 실행하면 **전원에게 반영**된다. `00_drop_all.sql` 실행 전 공지할 것
- `email`/`nickname`이 UNIQUE라 테스트 계정이 충돌한다.
  `test-{본인이름}@example.com` 식으로 구분해서 쓸 것
- XE는 동시 세션 수에 제약이 있어 `DB_POOL_SIZE=5`로 작게 잡아뒀다

### 사외에서 작업할 때

`10.1.54.120`은 **사내망 주소**라 집에서는 접속이 안 된다. 선택지:

| 방법 | 비고 |
|---|---|
| VPN | 사내망에 붙을 수 있으면 그대로 사용 |
| 로컬 Oracle XE 설치 | `.env`의 `DB_HOST=localhost`로 변경 후 `01_schema.sql` 실행 |
| 프론트엔드만 작업 | DB 없이 가능. `/api/health`가 503이어도 화면은 뜬다 |

`DB_HOST` 한 줄만 바꾸면 되도록 설계해뒀다.
