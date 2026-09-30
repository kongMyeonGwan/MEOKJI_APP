# 뭐먹지? (MEOKJI)

> 오늘 뭐 먹을지 결정하지 못하는 사람을 위한 AI 음식 추천 서비스

날씨·취향·예산·Ban 재료를 반영해 AI가 음식과 레시피를 생성하고,
실제 쇼핑 API로 재료 최저가를 알려준다.

## 설계 원칙

- **AI**가 음식/레시피/재료/대체재료를 동적으로 생성한다 (음식 Master 테이블 없음)
- **날씨와 가격은 실제 API**에서 가져온다. AI가 추측하지 않는다
- **DB에는 사용자가 최종 확정한 것만** 저장한다 (검색·추천 후보는 저장 안 함)
- **테이블을 함부로 늘리지 않는다.**
  AI 추천 기록처럼 테이블이 필요해 보이는 것은 브라우저 localStorage 에 두고,
  카테고리처럼 사용자가 만들지 않는 목록은 서버 파일
  (`backend-spring/src/main/resources/data/catalog.json`)에서 내려준다

  > 테이블은 **10개**다.
  > `users` · `verification_codes` · `user_bans` · `meal_histories` ·
  > `recipe_reviews` · `review_likes` · `review_comments` · `notices` ·
  > `game_histories` · `recipe_likes`.
  >
  > 찜(`recipe_likes`)은 원래 localStorage 에 있었다. 기기를 바꾸면 안 따라오고,
  > 한 브라우저를 여러 사람이 쓰면 앞사람 찜이 보여서 표로 옮겼다.
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
| Backend | Java 17 + Spring Boot 4 |
| DB 접근 | MyBatis (JPA 미사용 — 아래 참고) |
| Database | **Oracle Database 21c XE** |
| Driver | ojdbc17 (Thin 모드, Instant Client 불필요) |
| 인증 | JWT Access Token (Refresh Token 미사용) |
| 비밀번호 | bcrypt |

> ### 서버는 스프링부트다 (2026-09-29)
>
> 원래 Python + FastAPI 였고, 학원이 스프링부트를 쓰기로 바뀌어 옮겼다.
> 옮기기를 끝내고 **파이썬은 걷어냈다.**
>
> 필요하면 git 에서 꺼내 볼 수 있다. 특히 조리 순서를 만드는 `steps.py` 와
> 게시판 구현은 옮기지 않았다.
>
> ```
> git show dd8e8e9:backend/app/integrations/steps.py        # 레시피 조리 순서
> git show e22853f:backend/app/services/post_service.py     # 돌아가던 게시판
> ```
>
> **MyBatis 를 고른 이유가 이 스키마에 있다.** 모든 조회에 `use_yn='Y'` 가 붙고,
> UNIQUE 가 제약조건이 아니라 함수 기반 인덱스이고, 중복 INSERT 는 `MERGE` 로
> 처리하고, 검색은 `LOWER(col) LIKE LOWER(?)` 다. JPA 로 우회하는 것보다 SQL 을
> 직접 쓰는 쪽이 맞다.
>
> 돌리는 법과 담당자용 안내는 [backend-spring/README.md](backend-spring/README.md),
> 옮기면서 걸린 것들은 [docs/PROGRESS.md](docs/PROGRESS.md) 에 있다.

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
cd backend-spring
copy src\main\resources\application-local.yml.example src\main\resources\application-local.yml
#  열어서 DB 비밀번호와 JWT 비밀키를 채운다 (git 에 올라가지 않는 파일)

gradlew bootRun
```

**Gradle 은 안 깔아도 된다.** `gradlew` 가 알아서 받아 쓴다. JDK 17 이상만 있으면 된다.

확인: <http://localhost:8080/api/health> → `{"status":"ok", ..., "database":"ok"}`

JWT 비밀키는 **32바이트(영문 32자) 이상**이어야 한다. 짧으면 서버가 뜨다가
`WeakKeyException` 으로 죽는다.

더 자세한 것은 `backend-spring/README.md` 에 있다.

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
| 2 | `db/01_schema.sql` | meokji_app | 10개 테이블 생성 |

이미 구버전(v1) 테이블이 만들어져 있고 회원 데이터를 살리고 싶다면,
위 두 개 대신 **`db/02_migrate_v2.sql`을 한 번만** 실행한다.

이미 v2 테이블이 있는데 후기 첨부파일 컬럼(`FILE_*`)만 없다면
**`db/03_add_review_attachments.sql`을 한 번만** 실행한다. 파일 맨 위의
확인 쿼리가 `0`을 주면 실행 대상이고, `6`이면 이미 적용된 것이다.

그 뒤에 붙은 것이 셋 더 있다. 이미 돌아가는 DB 라면 **번호 순서대로**
한 번씩 실행한다 (새로 만드는 DB 는 `01_schema.sql` 에 다 들어 있으니 불필요).

| 파일 | 하는 일 | 안 돌리면 |
|---|---|---|
| `db/06_add_user_level.sql` | `USERS.USER_LEVEL` 추가 (1=관리자 2=일반) | 로그인·내정보에서 `ORA-00904` |
| `db/07_drop_notice_category.sql` | 04 가 넣은 `NOTICES.CATEGORY` 제거 | 스키마만 어긋남 (동작은 함) |
| `db/08_add_recipe_likes.sql` | `RECIPE_LIKES` 표 생성 | 찜하기에서 `ORA-00942` |

`06` 은 기존 회원을 전부 `user_level=2`(일반)로 만든다. 관리자로 쓸 계정은
`UPDATE users SET user_level = 1 WHERE email = '...';` 로 따로 올려야 한다.

**개인 PC에 Oracle을 새로 설치한 경우** — 계정부터 만들어야 함

| 순서 | 파일 | 실행 계정 | 비고 |
|:--:|---|---|---|
| 1 | `db/00_create_user_local.sql` | **SYS AS SYSDBA** | 파일 안의 비밀번호 자리를 먼저 수정 |
| 2 | `db/01_schema.sql` | meokji_app | 10개 테이블 생성 |

그리고 `backend-spring/src/main/resources/application-local.yml` 의 접속 정보를
로컬에 맞춘다(기본값이 이미 `localhost`라 비밀번호만 채우면 된다).

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
├── backend-spring/            Spring Boot 서버
│   ├── build.gradle           Boot 4.0.8 · Java 17 · MyBatis · ojdbc17 · jjwt · mail
│   ├── gradlew(.bat)          Gradle 을 안 깔아도 빌드된다
│   └── src/
│       ├── main/java/com/meokji/api/
│       │   ├── controller/    HTTP 를 받는 자리 (12)
│       │   ├── service/       업무 규칙과 트랜잭션 경계 (14)
│       │   ├── mapper/        SQL 을 부르는 인터페이스 (6)
│       │   ├── dto/           요청 · 응답 모양. 기능별로 한 겹 더 (42)
│       │   ├── vo/            표 한 행 (9)
│       │   ├── util/          계산 · 바깥 API · 고정 목록 읽기 (15)
│       │   ├── common/        ApiException · 오류 핸들러 · Page
│       │   ├── config/        AppProperties · SecurityConfig · WebConfig
│       │   └── security/      JwtProvider · @LoginUser · 인자 리졸버
│       ├── main/resources/
│       │   ├── application.yml
│       │   ├── data/          테이블이 필요 없는 고정 목록 5개
│       │   │   ├── catalog.json        음식 100종 · 카테고리 · 룰렛 · 게임
│       │   │   ├── recipes.json        손으로 쓴 레시피 100종
│       │   │   ├── regions.json        시·도 17개와 시·군·구 좌표
│       │   │   ├── ingredient-rules.json  분량 환산 · 대체재료
│       │   │   └── kamis-items.json    재료 이름 -> KAMIS 품목코드 116개
│       │   └── mapper/        매퍼 XML 6개
│       └── test/
│           ├── java/          테스트 94건 + 데모 데이터(SeedDemoTest)
│           └── resources/fixtures/   파이썬 응답 정답지 38개
└── frontend/
    ├── package.json
    ├── .env.example
    └── src/
        ├── api/               axios 클라이언트 + API별 함수
        ├── components/        공통 · 기능별 컴포넌트
        ├── pages/             라우팅 대상 화면. 역할별 폴더로 나뉜다
        │                      (home · auth · recommend · game · community · mypage)
        ├── layouts/           사이드바 + 상단바 + 본문(children)
        └── utils/             token(로그인 토큰·사용자) · aiLog(AI 기록) · settings(테마·알림)
                               · apiError · format
```


---

## API 목록

아래는 어떤 기능이 어디에 있는지 찾기 위한 지도다.

로그인 칸의 ● 는 로그인 필수, ▲ 는 관리자(`users.user_level = 1`) 전용이다.

**✗ 가 붙은 20개는 아직 서버에 없다(501).** 후기 게시판(`reviews`)과
공지(`notices`)는 팀원이 직접 구현할 자리라 주소와 DTO 만 남기고 메서드
안을 비워뒀다. 화면은 `frontend/src/data/sampleBoard.js` 의 가데이터를
그리고 있어서 그대로 볼 수 있다.

어떻게 동작해야 하는지는 [docs/BOARD_SPEC.md](docs/BOARD_SPEC.md) 에 있다.

**✗ 가 붙은 20개는 아직 서버에 없다(501).** 게시판(`posts`)과
공지(`notices`)는 팀원이 직접 구현할 자리라 주소와 스키마만 남기고 함수
안을 비워뒀다. 화면은 `frontend/src/data/sampleBoard.js` 의 가데이터를
그리고 있어서 그대로 볼 수 있다. 자세한 사정은
[docs/PROGRESS.md](docs/PROGRESS.md) 의 「게시판 · 공지를 담당자에게 넘김」 참고.

| 그룹 | 엔드포인트 | 로그인 | 설명 |
|---|---|:--:|---|
| health | `GET /api/health` | | 서버 + DB 상태 |
| auth | `POST /api/auth/register` | | 회원가입 (성별·주소 선택) |
| | `POST /api/auth/login` | | Access Token 발급 |
| | `GET /api/auth/me` | ● | 토큰 유효성 확인용 (가볍다) |
| | `POST /api/auth/password-reset/{send,verify,change}` | | 이메일 OTP 재설정 (없는 이메일 404 · 재발송 60초 제한) |
| users | `GET /api/users/me` | ● | 내 정보 + 요리 횟수 · 연속 방문일 |
| | `PATCH /api/users/me` | ● | 닉네임 · 성별 · 주소 수정 |
| | `POST /api/users/me/password` | ● | 비밀번호 변경 |
| | `GET /api/users/me/notifications` | ● | 내 글의 댓글 · 좋아요 |
| | `DELETE /api/users/me` | ● | 회원 탈퇴 (`use_yn='N'`) |
| bans | `GET·POST /api/bans`, `DELETE /api/bans/{id}` | ● | 못 먹는 재료 |
| 추천 | `POST /api/recommendations` | | 날씨·예산·검색어 조건 추천 |
| | `GET /api/recipes/{음식이름}` | | 레시피 생성 + 재료 가격 |
| histories | `GET /api/histories` | ● | 내 기록 · 페이징 (`?page=1&size=20&shared=false`) |
| | `GET /api/histories/stats` | ● | 건수 · 누적 비용 · 연속 방문일 (전체 기준) |
| | `POST /api/histories` | ● | 식사 확정 (이걸로 먹을래요) |
| | `DELETE /api/histories/{id}` | ● | 기록 삭제 (공유했으면 409) |
| reviews | `GET /api/reviews` | | ✗ 목록 · 검색 · 페이징 (`?page=1&size=20`) |
| | `GET /api/reviews/popular` | | ✗ 기간 내 좋아요 순위 |
| | `GET /api/reviews/{id}` | | ✗ 상세 + 댓글 |
| | `POST·PUT·DELETE /api/reviews[/{id}]` | ● | ✗ 글쓰기 · 수정 · 삭제 |
| | `POST /api/reviews/{id}/view` | | ✗ 조회수 +1 |
| | `PUT·DELETE /api/reviews/{id}/like` | ● | ✗ 좋아요 켜기 · 끄기 |
| | `POST·PUT·DELETE /api/reviews/{id}/comments[/{cid}]` | ● | ✗ 댓글 |
| | `POST·DELETE /api/reviews/{id}/file` | ● | ✗ 사진 붙이기 · 교체 · 떼기 |
| | `GET /api/reviews/{id}/file` | | ✗ 사진 내려받기 (원본 파일명으로) |
| catalog | `GET /api/catalog` | | 카테고리 · 룰렛 · 게임 · 질문 · 검색어 · 예산 |
| | `GET /api/catalog/regions` | | 시·도 · 시·군·구 (주소 드롭다운) |
| notices | `GET /api/notices` | | ✗ 공지사항 (고정 먼저 · 최신순, `?page=1&size=20`) |
| | `POST /api/notices/{id}/view` | | ✗ 조회수 +1 (펼칠 때) |
| | `POST·PUT·DELETE /api/notices[/{id}]` | ▲ | ✗ 공지 작성 · 수정 · 삭제 |
| games | `POST /api/games/{key}/plays` | ● | 게임 시작 기록 (worldcup·balance·roulette) |
| likes | `GET /api/likes` | ● | 내 찜 목록 |
| | `PUT /api/likes` | ● | 찜하기 · 해제 (누를 때마다 뒤집힘) |
| | `DELETE /api/likes` | ● | 찜 전체 비우기 |

**로그인 없이 되는 것** — 추천 · 레시피 · 가격 · 날씨 · 카탈로그.
(게시판 · 공지 조회도 로그인 없이 되게 만들어두었으나 아직 서버가 501 이다.)
로그인하면 못 먹는 재료(Ban)가 추천과 레시피에 자동으로 반영된다.

---

## 데모 데이터 채우기

빈 DB 로는 화면을 볼 수가 없다. 게시판도 공지도 비어 있고, 글을 쓰려면 식사를
먼저 확정해야 해서 손으로 채우려면 한참 걸린다.

```bash
cd backend-spring
gradlew seedDemo
```

회원 2명(운영자 · 일반) · 공지 4건 · 식사 기록 3건 · 후기 2건(첨부 1건)을
**실제 API 를 거쳐** 만든다(MockMvc). DB 에 직접 INSERT 하지 않으므로 검증기 · FK ·
UNIQUE 가 그대로 돌고, API 로는 만들 수 없는 데이터가 섞이지 않는다.

**지금은 회원과 식사 기록만 만들어진다.** 공지와 후기는 서버가 501 이라
그 단계를 건너뛰고 "아직 서버에 없다" 고 찍는다. 담당자가 구현하면 조건이
저절로 풀려서 공지 4건 · 후기 2건까지 다시 채워진다 - 이 코드는 고칠
필요가 없다.

여러 번 돌려도 안전하다 — 이미 있는 것은 건너뛴다.

| 계정 | 등급 |
|---|---|
| `admin@meokji.com` | 관리자 (`user_level = 1`) — 공지를 쓸 수 있다 |
| `minji@example.com` | 일반 회원 |

비밀번호는 `db/02_seed.sql` 의 개발용 계정과 같다. 다른 값을 쓰려면 실행 전에
`SEED_PASSWORD` 환경변수를 지정한다.

> **`app.env` 가 `local` 일 때만 돈다.** 저장소에 적힌 개발용 비밀번호로 **관리자
> 계정**을 만들기 때문이다. 운영이나 공용 DB 에서 돌면 README 를 읽은 누구나
> 관리자로 로그인할 수 있게 된다. 다른 값이면 아무것도 하지 않고 멈춘다.

---

## 테스트

```bash
cd backend-spring
gradlew test
```

**94건이다.** 실제 Oracle 에 붙고, 위 `seedDemo` 가 만든 계정
(`minji@example.com`)이 있어야 한다.

대부분이 **파이썬 서버의 실제 응답과 글자 단위로 대조**하는 방식이다
(`src/test/resources/fixtures/` 38개). 게시판 · 공지는 아직 501 이라
주소 20개가 제대로 서 있는지만 확인한다.

데이터 파일(`resources/data/*.json`)이 성한지 보는 검사도 여기 들어 있다 —
개수 · 빠진 칸 · 좌표가 한국 안인지, 카탈로그와 레시피의 메뉴가 같은지.

프론트는 빌드로 확인한다.

```bash
cd frontend
npm run build      # 못 찾는 import 가 있으면 여기서 걸린다
```

테스트는 데모 계정을 그대로 쓰고 새 계정을 만들지 않는다. 비밀번호 재설정
테스트만 `minji@example.com` 의 비밀번호를 건드리는데, **같은 값으로 되돌려
놓아서** 다른 테스트가 깨지지 않는다.

---

## 환경변수

사람마다 다른 값은 git 에 올라가지 않는다. `.example` 파일을 복사해서 쓴다.

### backend-spring/src/main/resources/application-local.yml

```yaml
spring:
  datasource:
    password: 팀에_문의          # 네트워크가 바뀌면 url 도 여기서 덮어쓴다

app:
  jwt:
    secret-key: 32자_이상의_랜덤_문자열
  price:
    service-key: 공공데이터포털에서_발급     # 없으면 레시피의 가격만 비어 있다
```

접속 주소 · 포트 같은 기본값은 `application.yml` 에 있고, 환경변수로도 덮어쓸 수
있다(`DB_HOST` · `DB_PORT` · `DB_SERVICE` · `DB_USER` ...).

### frontend/.env

```env
VITE_API_BASE_URL=http://localhost:8080
```

### 외부 연동 켜기 (전부 선택)

키가 없어도 전 기능이 동작한다. 진짜 서비스를 붙이려면 아래 값만 바꾸면 되고,
**코드는 한 줄도 고치지 않는다.**

| 하고 싶은 것 | `application-local.yml` 에 넣을 값 |
|---|---|
| 실제 날씨 | `app.weather.provider: openweather` + `app.weather.api-key` |
| 실제 재료 시세 | `app.price.service-key` (`providers` 는 기본이 `kamis`) |
| 실제 인증 메일 발송 | `app.mail.provider: smtp` + `spring.mail.*` |
| 뜰 때 떨이 재료 미리 훑기 | 환경변수 `WARM_BARGAINS=true` |

**AI 는 쓰지 않는다 (2026-09-26).** 음식 추천은 카탈로그 100종에 날씨·예산·Ban
점수를 매겨 고르고, 레시피는 미리 써둔 100종(`resources/data/recipes.json`)에서
꺼낸다. 요리도 레시피도 파일에서 나오므로 AI 를 부를 자리가 없다.

후기 첨부파일은 키가 필요 없다. 다만 **아직 서버가 501** 이라 담당자가 구현할
자리다 — 저장 규칙은 [docs/BOARD_SPEC.md](docs/BOARD_SPEC.md) 에 적어뒀다.

`app.price.providers` 에 지금 쓸 수 있는 값은 **`kamis` 하나뿐이다.** 모르는 값을
적으면 조용히 건너뛰지 않고 에러를 낸다 — 오타를 못 찾는 것보다 낫기 때문인데,
지금은 그 에러를 아무도 잡지 않아서 레시피 조회가 500 으로 끝난다.

자리는 쉼표로 여러 개를 받게 열어뒀다. 둘 이상이 되면 **앞에서부터 시도하다가
값이 나오면 거기서 멈춘다** — 여러 곳의 결과를 합치지 않는다. 곳마다 단위
기준이 달라서, 합치면 최저/최고가 시장 차이가 아니라 단위 차이가 된다.

값을 지어내던 mock 프로바이더는 없앴다. 진짜 조사가격과 화면에서 구분되지
않은 채 합계까지 계산돼서, 재료 절반이 가짜인 표가 나왔기 때문이다.
지금은 KAMIS 에 없는 재료면 '가격 정보 없음'으로 두고, 합계에도
"재료 8개 중 4개 기준"처럼 몇 개를 더한 값인지 밝힌다.

`kamis` 는 공공데이터포털의 [한국농수산식품유통공사 일별 도·소매 가격정보][kamis]
다. 무료이고 개발계정은 자동승인(일 10,000건)이다. 인증키는 마이페이지의
**Decoding** 값을 넣어야 한다 — Encoding 값을 넣으면 인증에 실패한다.

품목 116개를 `resources/data/kamis-items.json` 에 매핑해뒀다(참고문서 코드표 기준).
**농수산물만** 나오므로 두부·간장·설탕 같은 가공식품은 값이 없다. 레시피
100종의 재료로 세어보면 998회 중 638회(64%)에 시세가 붙고, 나머지는
'가격 정보 없음'으로 둔다. 이것을 메우려고 값을 지어내던 mock 은
위에 적은 이유로 없앴으니 `kamis,mock` 처럼 적지 말 것 — 500 이 난다.

쇼핑몰 최저가가 아니라 **소매시장 조사가격**이라, 구매 링크가 없고 품목마다
단위가 다르다(쌀 20kg / 깻잎 100g). 그래서 표에 단위와 조사일자를 같이 적는다.

네이버 쇼핑 검색 API 는 2026-07-31 종료됐고 공식 대체가 없다. 쿠팡·11번가는
제휴 승인이 필요해서 지금은 쓰지 않는다.

[kamis]: https://www.data.go.kr/data/15156057/openapi.do

`FOODS_SCHEMA` · `RECIPE_SCHEMA`(`ai_api.py`)는 AI 에게 "이 모양으로 답하라" 고
주던 값인데, AI 를 끈 지금은 **카탈로그가 맞춰야 할 응답 모양**으로 남겨뒀다.
화면이 기대하는 필드가 무엇인지는 이 둘을 보면 된다.

### frontend/.env

```env
VITE_API_BASE_URL=http://localhost:8080
```

`VITE_` 접두사가 붙은 값은 **브라우저에 그대로 노출된다.** API 키를 넣지 말 것.

---

## 문서

| 문서 | 내용 |
|---|---|
| [docs/PROGRESS.md](docs/PROGRESS.md) | 진행 상황 · 설계 결정 이유 · Oracle 관련 함정 |
| [docs/DB_SETUP.md](docs/DB_SETUP.md) | 로컬 Oracle 개발환경 구축 |
| [docs/STUDY_AUTH.md](docs/STUDY_AUTH.md) | 로그인과 JWT 는 어떻게 도는가 |
| [docs/STUDY_COLUMN_TYPES.md](docs/STUDY_COLUMN_TYPES.md) | 커스텀 컬럼 타입은 왜 필요한가 |

처음 합류했다면 `STUDY_` 로 시작하는 두 문서를 먼저 읽으면 소스가 훨씬 잘 읽힌다.
