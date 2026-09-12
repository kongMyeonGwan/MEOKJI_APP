# 뭐먹지? (MEOKJI)

> 오늘 뭐 먹을지 결정하지 못하는 사람을 위한 AI 음식 추천 서비스

날씨·취향·예산·Ban 재료를 반영해 AI가 음식과 레시피를 생성하고,
실제 쇼핑 API로 재료 최저가를 알려준다.

## 설계 원칙

- **AI**가 음식/레시피/재료/대체재료를 동적으로 생성한다 (음식 Master 테이블 없음)
- **날씨와 가격은 실제 API**에서 가져온다. AI가 추측하지 않는다
- **DB에는 사용자가 최종 확정한 것만** 저장한다 (검색·추천 후보는 저장 안 함)
- **테이블을 함부로 늘리지 않는다.**
  찜 목록 · AI 추천 기록처럼 테이블이 필요해 보이는 것은 브라우저
  localStorage 에 두고, 카테고리처럼 사용자가 만들지 않는 목록은
  서버 파일(`backend/app/data/catalog.json`)에서 내려준다

  > 테이블은 **9개**다.
  > `users` · `verification_codes` · `user_bans` · `meal_histories` ·
  > `recipe_reviews` · `review_likes` · `review_comments` · `notices` ·
  > `game_histories`.
  >
  > 공지(`notices`)와 미니게임 기록(`game_histories`)은 파일에 둘 수 없어서
  > 테이블이 됐다. 공지는 운영하면서 계속 새로 쓰는 값이라 파일에 두면
  > 공지 하나 올릴 때마다 서버를 다시 배포해야 하고, 게임 기록은 애초에
  > 사용자가 만드는 값이다.
- **외부 연동은 전부 `mock` 기본값**이다. API 키가 하나도 없어도
  회원가입부터 게시판까지 전 기능을 눌러볼 수 있다
- **삭제는 물리 삭제가 아니라 `USE_YN='N'` 갱신이다.** UI의 "삭제" · "회원 탈퇴"
  버튼은 `DELETE` 문을 실행하지 않는다. 그래서 모든 조회에 `WHERE use_yn='Y'`가
  붙어야 하고, UNIQUE는 제약조건이 아니라 함수 기반 인덱스로 건다
- 레시피 후기(`recipe_reviews`)는 **자기 `meal_histories`에서만** 쓸 수 있다.
  직접 만들어본 요리만 공유한다는 규칙을 FK로 강제한다

## 기술 스택

| 영역 | 기술 |
|---|---|
| Frontend | React 19 + Vite 6 (JavaScript, TypeScript 미사용) |
| Backend | Python 3.13 + FastAPI |
| ORM | SQLAlchemy 2.0 |
| Database | **Oracle Database 21c XE** |
| Driver | python-oracledb (Thin 모드, Instant Client 불필요) |
| 인증 | JWT Access Token (Refresh Token 미사용) |
| 비밀번호 | bcrypt |

---

## 빠른 시작

### 0. 사전 준비

- Python 3.11+
- Node.js 20+
- Oracle DB 접속 정보 (팀에 문의)

### 1. 클론

```bash
git clone https://github.com/kongMyeonGwan/MEOKJI_APP.git
cd MEOKJI_APP
```

### 2. Backend

```bash
cd backend
cp .env.example .env
#  .env를 열어 DB_PASSWORD 입력  (git에 올라가지 않는 파일)

python -m venv .venv
.venv\Scripts\activate          # macOS/Linux: source .venv/bin/activate
pip install -r requirements.txt

uvicorn app.main:app --reload --port 8000
```

확인: http://127.0.0.1:8000/api/health → `{"status":"ok","database":"ok"}`

### 3. Frontend

```bash
cd frontend
cp .env.example .env
npm install
npm run dev
```

확인: http://localhost:5173

### 4. DB 스키마

**팀 공용 서버(10.1.54.120)를 쓰는 경우** — 계정이 이미 있으므로 스키마만 실행

| 순서 | 파일 | 실행 계정 | 비고 |
|:--:|---|---|---|
| 1 | `db/00_drop_all.sql` | meokji_app | 기존 테이블이 있을 때만 |
| 2 | `db/01_schema.sql` | meokji_app | 9개 테이블 생성 |

이미 구버전(v1) 테이블이 만들어져 있고 회원 데이터를 살리고 싶다면,
위 두 개 대신 **`db/02_migrate_v2.sql`을 한 번만** 실행한다.

이미 v2 테이블이 있는데 후기 첨부파일 컬럼(`FILE_*`)만 없다면
**`db/03_add_review_attachments.sql`을 한 번만** 실행한다. 파일 맨 위의
확인 쿼리가 `0`을 주면 실행 대상이고, `6`이면 이미 적용된 것이다.

**개인 PC에 Oracle을 새로 설치한 경우** — 계정부터 만들어야 함

| 순서 | 파일 | 실행 계정 | 비고 |
|:--:|---|---|---|
| 1 | `db/00_create_user_local.sql` | **SYS AS SYSDBA** | 파일 안의 비밀번호 자리를 먼저 수정 |
| 2 | `db/01_schema.sql` | meokji_app | 9개 테이블 생성 |

그리고 `backend/.env`의 `DB_HOST`를 `localhost`로 바꾼다.

**DBeaver 접속 설정 주의**

| 항목 | 값 |
|---|---|
| Database | `XEPDB1` |
| 타입 | **`Service Name`** (기본값 `SID` 아님 — 틀리면 `ORA-12505`) |

**실행 주의**: `Ctrl+A` → **`Alt+X`** (Execute script).
`Ctrl+Enter`는 한 문장만 실행한다.

---

## 폴더 구조

```
MEOKJI_APP/
├── db/                        Oracle DDL
│   ├── 00_create_user_local.sql   로컬 Oracle 계정 생성 (SYS로 실행)
│   ├── 00_drop_all.sql
│   ├── 01_schema.sql              9개 테이블 (단일 소스)
│   ├── 02_migrate_v2.sql          v1 -> v2 마이그레이션 (1회성)
│   ├── 03_add_review_attachments.sql  후기 첨부파일 컬럼 추가 (1회성)
│   ├── 04_notices_and_games.sql   공지 분류 컬럼 · 게임 종류 A~C (1회성)
│   ├── 05_seed_notices.sql        초기 공지 5건 (선택)
│   └── 06_add_user_level.sql      회원 권한 등급 1=관리자 2=일반 (1회성)
├── backend/
│   ├── requirements.txt
│   ├── pytest.ini
│   ├── .env.example
│   ├── tests/                 자동 테스트
│   │   ├── test_logic.py          DB 없이 도는 계산 검증
│   │   └── test_api.py            실제 Oracle 에 붙는 통합 검증
│   ├── scripts/               손으로 한 번씩 돌리는 것들
│   │   ├── check_email.py         .env 의 메일 설정으로 실제 발송 시도
│   │   └── seed_demo.py           빈 DB 에 데모 데이터 채우기
│   └── app/
│       ├── main.py            FastAPI 진입점
│       ├── core/              config(설정) · security(bcrypt/JWT) · deps(로그인) · kst(시간대)
│       ├── database/          엔진 · 세션 · Base
│       ├── models/            SQLAlchemy 모델 · Oracle 전용 타입
│       ├── schemas/           Pydantic 요청/응답
│       ├── services/          비즈니스 로직
│       ├── routers/           API 엔드포인트
│       ├── data/              테이블이 필요 없는 고정 목록
│       │   └── catalog.json       카테고리 · 룰렛 · 게임 · 질문 · mock 음식 39종
│       └── integrations/      외부 서비스 연동 (파일 하나 = 서비스 하나)
│           ├── ai_api.py          AI 추천·레시피 (mock / gemini / claude)
│           ├── weather_api.py     날씨 (mock / openweather)
│           ├── email_sender.py    메일 (mock / smtp)
│           └── price_api.py       재료 가격 (kamis)
└── frontend/
    ├── package.json
    ├── .env.example
    └── src/
        ├── api/               axios 클라이언트 + API별 함수
        ├── components/        공통 · 기능별 컴포넌트
        ├── contexts/          앱 전체가 공유하는 상태
        │                      (로그인 · 프로필/알림 · 찜 · AI 기록 · 카탈로그 · 설정)
        ├── pages/             라우팅 대상 화면
        ├── layouts/           사이드바 + 상단바 + Outlet
        └── utils/             token(로그인 토큰) · apiError · format
```

---

## API 목록

전체 목록과 요청/응답 모양은 서버를 켜고 <http://localhost:8000/docs> 에서
직접 눌러볼 수 있다. 아래는 어떤 기능이 어디에 있는지 찾기 위한 지도다.

로그인 칸의 ● 는 로그인 필수, ▲ 는 관리자(`users.user_level = 1`) 전용이다.

| 그룹 | 엔드포인트 | 로그인 | 설명 |
|---|---|:--:|---|
| health | `GET /api/health` | | 서버 + DB 상태 |
| auth | `POST /api/auth/register` | | 회원가입 (성별·주소 선택) |
| | `POST /api/auth/login` | | Access Token 발급 |
| | `GET /api/auth/me` | ● | 토큰 유효성 확인용 (가볍다) |
| | `POST /api/auth/password-reset/{send,verify,change}` | | 이메일 OTP 재설정 |
| users | `GET /api/users/me` | ● | 내 정보 + 요리 레벨 |
| | `PATCH /api/users/me` | ● | 닉네임 · 성별 · 주소 수정 |
| | `POST /api/users/me/password` | ● | 비밀번호 변경 |
| | `GET /api/users/me/notifications` | ● | 내 글의 댓글 · 좋아요 |
| | `DELETE /api/users/me` | ● | 회원 탈퇴 (`use_yn='N'`) |
| bans | `GET·POST /api/bans`, `DELETE /api/bans/{id}` | ● | 못 먹는 재료 |
| 추천 | `POST /api/recommendations` | | 날씨·예산·검색어·냉장고 조건 추천 |
| | `GET /api/recipes/{음식이름}` | | 레시피 생성 + 재료 가격 |
| | `POST /api/prices` | | 재료별 시장 조사가격 |
| histories | `GET /api/histories` | ● | 내 식사 기록 |
| | `GET /api/histories/stats` | ● | 건수 · 누적 비용 · 연속 방문일 |
| | `POST /api/histories` | ● | 식사 확정 (이걸로 먹을래요) |
| | `DELETE /api/histories/{id}` | ● | 기록 삭제 (공유했으면 409) |
| posts | `GET /api/posts` | | 목록 · 검색 · 페이징 (`?page=1&size=20`) |
| | `GET /api/posts/popular` | | 기간 내 좋아요 순위 |
| | `GET /api/posts/{id}` | | 상세 + 댓글 |
| | `POST·PUT·DELETE /api/posts[/{id}]` | ● | 글쓰기 · 수정 · 삭제 |
| | `POST /api/posts/{id}/view` | | 조회수 +1 |
| | `PUT·DELETE /api/posts/{id}/like` | ● | 좋아요 켜기 · 끄기 |
| | `POST·PUT·DELETE /api/posts/{id}/comments[/{cid}]` | ● | 댓글 |
| | `POST·DELETE /api/posts/{id}/file` | ● | 사진 붙이기 · 교체 · 떼기 |
| | `GET /api/posts/{id}/file` | | 사진 내려받기 (원본 파일명으로) |
| weather | `GET /api/weather` | | 현재 날씨 |
| catalog | `GET /api/catalog` | | 카테고리 · 룰렛 · 게임 · 질문 · 재료 칩 |
| | `GET /api/catalog/regions` | | 시·도 · 시·군·구 (주소 드롭다운) |
| notices | `GET /api/notices` | | 공지사항 (고정 먼저 · 최신순) |
| | `POST /api/notices/{id}/view` | | 조회수 +1 (펼칠 때) |
| | `POST·PUT·DELETE /api/notices[/{id}]` | ▲ | 공지 작성 · 수정 · 삭제 |
| games | `POST /api/games/{key}/plays` | ● | 게임 시작 기록 (worldcup·balance·roulette) |

**로그인 없이 되는 것** — 추천 · 레시피 · 가격 · 게시판 조회 · 날씨 · 카탈로그.
로그인하면 못 먹는 재료(Ban)가 추천과 레시피에 자동으로 반영된다.

---

## 데모 데이터 채우기

빈 DB 로는 화면을 볼 수가 없다. 게시판도 공지도 비어 있고, 글을 쓰려면 식사를
먼저 확정해야 해서 손으로 채우려면 한참 걸린다.

```bash
cd backend
.venv\Scriptsctivate

python -m scripts.seed_demo
```

회원 2명(운영자 · 일반) · 공지 4건 · 식사 기록 3건 · 후기 2건(첨부 1건)을
**실제 API 를 거쳐** 만든다. DB 에 직접 INSERT 하지 않으므로 검증기 · FK ·
UNIQUE 가 그대로 돌고, API 로는 만들 수 없는 데이터가 섞이지 않는다.

여러 번 돌려도 안전하다 — 이미 있는 것은 건너뛴다.

| 계정 | 등급 |
|---|---|
| `admin@meokji.com` | 관리자 (`user_level = 1`) — 공지를 쓸 수 있다 |
| `minji@example.com` | 일반 회원 |

비밀번호는 `db/02_seed.sql` 의 개발용 계정과 같다. 다른 값을 쓰려면 실행 전에
`SEED_PASSWORD` 환경변수를 지정한다.

> **`APP_ENV=local` 에서만 돈다.** 저장소에 적힌 개발용 비밀번호로 **관리자
> 계정**을 만들기 때문이다. 운영이나 공용 DB 에서 돌면 README 를 읽은 누구나
> 관리자로 로그인할 수 있게 된다. 다른 값이면 아무것도 하지 않고 멈춘다.

---

## 테스트

```bash
cd backend
.venv\Scripts\activate

pytest                      # 전부 (197건)
pytest tests/test_logic.py  # DB 없이 되는 것만 (127건)
```

`tests/test_api.py` 는 실제 Oracle 에 붙는다. DB 가 꺼져 있으면 실패가 아니라
**건너뛴다(skip)**. 코드가 깨진 것과 DB 를 안 켠 것을 구분하기 위해서다.

테스트가 만드는 계정(`pytest_*@example.com`)과 그 하위 데이터는
`tests/conftest.py` 가 앞뒤로 물리 삭제한다.

---

## 환경변수

`.env`는 git에 올라가지 않는다. `.env.example`을 복사해서 쓴다.

### backend/.env

```env
DB_HOST=localhost    # 네트워크가 바뀌면 이 줄만 수정
DB_PORT=1521
DB_SERVICE=XEPDB1
DB_USER=meokji_app
DB_PASSWORD=            # 팀에 문의
JWT_SECRET_KEY=         # 아무 랜덤 문자열
```

### 외부 연동 켜기 (전부 선택)

키가 없어도 전 기능이 `mock` 으로 동작한다. 진짜 서비스를 붙이려면
아래 값만 바꾸면 되고, **코드는 한 줄도 고치지 않는다.**

| 하고 싶은 것 | `.env` 에 넣을 값 |
|---|---|
| AI 로 진짜 추천/레시피 생성 (무료) | `AI_PROVIDER=gemini` + `GEMINI_API_KEY=...` |
| AI 로 진짜 추천/레시피 생성 (유료) | `AI_PROVIDER=claude` + `ANTHROPIC_API_KEY=...` |
| 실제 날씨 | `WEATHER_PROVIDER=openweather` + `OPENWEATHER_API_KEY=...` |
| 실제 재료 최저가 | `PRICE_PROVIDERS=kamis` + `DATA_GO_KR_SERVICE_KEY=...` |
| 실제 인증 메일 발송 | `EMAIL_PROVIDER=smtp` + `SMTP_*` |

후기 첨부파일은 키가 필요 없다. `UPLOAD_DIR`(기본 `uploads`) 아래에
저장하고 DB 에는 경로만 적는다. 폴더는 `.gitignore` 에 있어 커밋되지 않는다.
받아줄 확장자와 최대 크기는 `UPLOAD_ALLOWED_EXTS` · `UPLOAD_MAX_MB` 로 정한다.

`PRICE_PROVIDERS` 는 쉼표로 여러 개를 적을 수 있다. **앞에서부터 시도하다가
값이 나오면 거기서 멈춘다** — 여러 곳의 결과를 합치지 않는다. 곳마다 단위
기준이 달라서, 합치면 최저/최고가 시장 차이가 아니라 단위 차이가 된다.

값을 지어내던 mock 프로바이더는 없앴다. 진짜 조사가격과 화면에서 구분되지
않은 채 합계까지 계산돼서, 재료 절반이 가짜인 표가 나왔기 때문이다.
지금은 KAMIS 에 없는 재료면 '가격 정보 없음'으로 두고, 합계에도
"재료 8개 중 4개 기준"처럼 몇 개를 더한 값인지 밝힌다.

`kamis` 는 공공데이터포털의 [한국농수산식품유통공사 일별 도·소매 가격정보][kamis]
다. 무료이고 개발계정은 자동승인(일 10,000건)이다. 인증키는 마이페이지의
**Decoding** 값을 넣어야 한다 — Encoding 값을 넣으면 인증에 실패한다.

품목 124개를 `KAMIS_ITEM_CODES` 에 매핑해뒀다(참고문서 코드표 기준).
**농수산물만** 나오므로 두부·간장·라면 같은 가공식품까지 채우려면
`kamis,mock` 처럼 mock 을 뒤에 붙인다.

쇼핑몰 최저가가 아니라 **소매시장 조사가격**이라, 구매 링크가 없고 품목마다
단위가 다르다(쌀 20kg / 깻잎 100g). 그래서 표에 단위와 조사일자를 같이 적는다.

네이버 쇼핑 검색 API 는 2026-07-31 종료됐고 공식 대체가 없다. 쿠팡·11번가는
제휴 승인이 필요해서 지금은 쓰지 않는다.

[kamis]: https://www.data.go.kr/data/15156057/openapi.do

`GEMINI_MODEL` 도 쉼표로 여러 개를 적을 수 있는데 뜻이 다르다. 이쪽은
**앞에서부터 하나만** 쓰고, 그 모델이 붐비면(503) 다음 모델로 넘어간다.
무료 등급은 붐비는 일이 잦아서 예비 모델을 하나 적어두는 편이 좋다.

    GEMINI_MODEL=gemini-3.7-flash,gemini-3.5-flash

**어느 프로바이더를 쓰든 프롬프트와 응답 스키마는 같다.** 바꿔 끼웠을 때
추천의 성격까지 달라지면 비교가 불가능해지기 때문이다.

### frontend/.env

```env
VITE_API_BASE_URL=http://localhost:8000
```

`VITE_` 접두사가 붙은 값은 **브라우저에 그대로 노출된다.** API 키를 넣지 말 것.

---

## 문서

| 문서 | 내용 |
|---|---|
| [docs/PROGRESS.md](docs/PROGRESS.md) | 진행 상황 · 설계 결정 이유 · Oracle 관련 함정 |
| [docs/DB_SETUP.md](docs/DB_SETUP.md) | 로컬 Oracle 개발환경 구축 |
| [docs/STUDY_AUTH.md](docs/STUDY_AUTH.md) | 로그인과 JWT 는 어떻게 도는가 |
| [docs/STUDY_JS_SYNTAX.md](docs/STUDY_JS_SYNTAX.md) | 프론트엔드에 나오는 `?.` 와 `??` |
| [docs/STUDY_COLUMN_TYPES.md](docs/STUDY_COLUMN_TYPES.md) | 커스텀 컬럼 타입은 왜 필요한가 |

처음 합류했다면 `STUDY_` 로 시작하는 세 문서를 먼저 읽으면 소스가 훨씬 잘 읽힌다.
