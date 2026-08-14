# 뭐먹지? (MEOKJI)

> 오늘 뭐 먹을지 결정하지 못하는 사람을 위한 AI 음식 추천 서비스

날씨·취향·예산·Ban 재료를 반영해 AI가 음식과 레시피를 생성하고,
실제 쇼핑 API로 재료 최저가를 알려준다.

## 설계 원칙

- **AI**가 음식/레시피/재료/대체재료를 동적으로 생성한다 (음식 Master 테이블 없음)
- **날씨와 가격은 실제 API**에서 가져온다. AI가 추측하지 않는다
- **DB에는 사용자가 최종 확정한 것만** 저장한다 (검색·추천 후보는 저장 안 함)
- 테이블은 **7개**로 고정한다

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
| 2 | `db/01_schema.sql` | meokji_app | 7개 테이블 생성 |

**개인 PC에 Oracle을 새로 설치한 경우** — 계정부터 만들어야 함

| 순서 | 파일 | 실행 계정 | 비고 |
|:--:|---|---|---|
| 1 | `db/00_create_user_local.sql` | **SYS AS SYSDBA** | 파일 안의 비밀번호 자리를 먼저 수정 |
| 2 | `db/01_schema.sql` | meokji_app | 7개 테이블 생성 |

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
│   ├── 00_drop_all.sql
│   └── 01_schema.sql
├── backend/
│   ├── requirements.txt
│   ├── .env.example
│   └── app/
│       ├── main.py            FastAPI 진입점
│       ├── core/              config.py(설정) · security.py(bcrypt)
│       ├── database/          엔진 · 세션 · Base
│       ├── models/            SQLAlchemy 모델 · Oracle 전용 타입
│       ├── schemas/           Pydantic 요청/응답
│       ├── services/          비즈니스 로직
│       ├── routers/           API 엔드포인트
│       └── integrations/      ai · weather · shopping · email (교체 가능)
└── frontend/
    ├── package.json
    ├── .env.example
    └── src/
        ├── api/               axios 클라이언트 + API별 함수
        ├── components/        공통 · 기능별 컴포넌트
        ├── pages/             라우팅 대상 화면
        ├── layouts/           Header + Outlet
        └── utils/             에러 파서 등
```

---

## 환경변수

`.env`는 git에 올라가지 않는다. `.env.example`을 복사해서 쓴다.

### backend/.env

```env
DB_HOST=10.1.54.120     # 네트워크가 바뀌면 이 줄만 수정
DB_PORT=1521
DB_SERVICE=XEPDB1
DB_USER=meokji_app
DB_PASSWORD=            # 팀에 문의
JWT_SECRET_KEY=         # 아무 랜덤 문자열
```

이후 STEP에서 `ANTHROPIC_API_KEY`(AI), `OPENWEATHER_API_KEY`(날씨),
`NAVER_CLIENT_ID/SECRET`(쇼핑), `SMTP_*`(이메일)가 추가로 필요하다.

### frontend/.env

```env
VITE_API_BASE_URL=http://localhost:8000
```

`VITE_` 접두사가 붙은 값은 **브라우저에 그대로 노출된다.** API 키를 넣지 말 것.

---

## 진행 상황

[docs/PROGRESS.md](docs/PROGRESS.md) 참고.
설계 결정 이유와 Oracle 관련 함정이 정리되어 있다.
