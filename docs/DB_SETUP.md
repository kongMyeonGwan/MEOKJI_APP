# 로컬 DB 세팅 가이드

> **이 프로젝트는 팀원 각자가 자기 PC에 Oracle을 설치해서 씁니다.**
> 공용 DB 서버는 쓰지 않습니다. 그래서 아래 과정을 **팀원 모두가 각자 한 번씩** 해야 합니다.
>
> 처음 해보는 사람도 따라 할 수 있게 썼습니다. 위에서부터 순서대로 진행하세요.
> 중간에 막히면 맨 아래 [문제 해결](#문제-해결)을 보세요.

---

## 이 문서를 끝내면

- 내 PC에 Oracle DB가 돌아가고
- `meokji_app` 이라는 DB 계정이 생기고
- 테이블 11개가 만들어지고
- 백엔드가 그 DB에 붙어서 `{"status":"ok","database":"ok"}` 를 응답합니다

**소요 시간**: Oracle 설치 30분 + 나머지 15분 정도.

---

## 전체 흐름

```
[0] Oracle 21c XE 설치          <- 한 번만. 이미 설치했으면 건너뜀
        |
[1] Oracle이 켜져 있는지 확인    <- sc query OracleServiceXE
        |
[2] 관리자로 접속               <- sqlplus / as sysdba
        |
[3] 계정 만들기                 <- db/00_create_user_local.sql
        |
[4] 테이블 만들기               <- db/01_schema.sql
        |
[5] 스프링 설정 파일 만들기      <- application-local.yml
        |
[6] 백엔드 실행 + 확인          <- /api/health
```

### 용어 4개만 알고 가기

| 용어 | 뜻 |
|---|---|
| **XE** | Oracle 무료 버전(Express Edition). 우리가 쓰는 것 |
| **XEPDB1** | XE 안에 들어 있는 실제 데이터베이스 칸의 이름. **여기에** 테이블을 만듭니다 |
| **SYS** | DB 최고 관리자 계정. 계정을 만들 때만 잠깐 씁니다 |
| **meokji_app** | 우리가 만들 계정. 평소에는 이걸로만 접속합니다 |

---

## [0] Oracle 21c XE 설치

> 이미 설치되어 있다면 [1단계](#1-oracle이-켜져-있는지-확인)로 건너뛰세요.
> 확인 방법: `Win + R` → `cmd` → `sc query OracleServiceXE` 입력.
> "지정된 서비스가 설치된 서비스로 존재하지 않습니다" 가 나오면 아직 설치 안 된 것입니다.

1. 다운로드 (Oracle 계정 로그인 필요, 무료)
   <https://www.oracle.com/database/technologies/xe-downloads.html>
   → **Oracle Database 21c Express Edition for Windows x64**
2. 압축을 풀고 `setup.exe` 를 **우클릭 → 관리자 권한으로 실행**
3. 설치 중 딱 하나 입력할 게 있습니다 — **관리자(SYS) 비밀번호**
   - 이 프로젝트에서는 사실상 안 씁니다(3단계에서 OS 인증을 씁니다). 그래도 메모해두세요
   - 특수문자는 `#` `_` 정도만 쓰는 걸 권장합니다
4. 설치 경로는 기본값 그대로 → `C:\app\<Windows사용자명>\product\21c\`
5. 설치 완료 후 **PC를 한 번 재시작**하면 이후가 깔끔합니다

**설치 시 주의**

| 항목 | 내용 |
|---|---|
| Windows 사용자명 | **한글이거나 공백이 있으면 설치가 실패**합니다. 영문 계정으로 하세요 |
| 디스크 | 최소 10GB 여유 |
| 백신 | 설치 중 잠시 꺼두면 실패 확률이 줄어듭니다 |

---

## [1] Oracle이 켜져 있는지 확인

`Win + R` → `cmd` 입력 → Enter 로 **명령 프롬프트**를 엽니다.

```cmd
sc query OracleServiceXE
```

`STATE : 4  RUNNING` 이 보이면 정상입니다. → [2단계로](#2-관리자로-접속)

`STOPPED` 이면 **cmd를 관리자 권한으로 다시 열고**:

```cmd
net start OracleServiceXE
```

리스너(외부 접속 창구)도 같이 확인해두면 좋습니다.

```cmd
sc query OracleOraDB21Home1TNSListener
```

멈춰 있으면 `net start OracleOraDB21Home1TNSListener`.

> **참고**: XE는 PC를 켤 때마다 자동으로 시작됩니다. 노트북이 무거워서 꺼두고 싶다면
> `services.msc` 에서 `OracleServiceXE` 를 수동 시작으로 바꾸고, 개발할 때만 `net start` 하세요.

---

## [2] 관리자로 접속

계정을 만드는 일은 관리자(SYS)만 할 수 있습니다. cmd에 그대로 입력하세요.

```cmd
sqlplus / as sysdba
```

`SQL>` 프롬프트가 뜨면 성공입니다. **비밀번호를 묻지 않습니다.**

> **왜 비밀번호를 안 물어보나요?**
> Oracle을 설치한 Windows 계정은 자동으로 DB 관리자로 인정됩니다(OS 인증).
> 그래서 SYS 비밀번호를 잊어버렸어도 이 방법으로는 들어갈 수 있습니다.

### sqlplus 를 못 찾는다고 나오면

> `'sqlplus'은(는) 내부 또는 외부 명령, 실행할 수 있는 프로그램... 이 아닙니다.`

PATH에 등록이 안 된 것뿐이고, 설치는 정상입니다. 전체 경로로 실행하세요.

```cmd
"C:\app\사용자이름\product\21c\dbhomeXE\bin\sqlplus.exe" / as sysdba
```

`사용자이름` 은 본인 Windows 계정명입니다. 경로를 모르겠으면 찾아보세요:

```cmd
where /r C:\app sqlplus.exe
```

### 이어서 — XEPDB1이 열려 있는지 확인

`SQL>` 에서 아래를 **세미콜론까지** 입력합니다.

```sql
SELECT name, open_mode FROM v$pdbs;
```

`XEPDB1` 줄의 `OPEN_MODE` 를 보세요.

| 값 | 할 일 |
|---|---|
| `READ WRITE` | 정상. [3단계로](#3-계정-만들기) |
| `MOUNTED` | 아직 안 열림. 아래 두 줄 실행 |

```sql
ALTER PLUGGABLE DATABASE XEPDB1 OPEN;
ALTER PLUGGABLE DATABASE XEPDB1 SAVE STATE;
```

> XE는 PC를 재부팅하면 XEPDB1이 닫힌 채로 남는 경우가 많습니다.
> `SAVE STATE` 를 한 번 해두면 다음부터는 자동으로 열립니다. **꼭 해두세요.**

---

## [3] 계정 만들기

### 3-1. 먼저 파일에 비밀번호를 적습니다

[`db/00_create_user_local.sql`](../db/00_create_user_local.sql) 을 에디터로 열고, 아래 부분을 찾습니다.

```sql
CREATE USER meokji_app
    IDENTIFIED BY "<<<비밀번호를_여기에_입력>>>"
```

`<<<비밀번호를_여기에_입력>>>` 를 본인이 쓸 비밀번호로 바꿉니다.

```sql
    IDENTIFIED BY "meokji1234"
```

| 규칙 | 이유 |
|---|---|
| 큰따옴표 `"` 는 지우지 말 것 | 지우면 `ORA-00922` 에러 |
| `&` 는 쓰지 말 것 | SQL*Plus가 변수 기호로 해석함 |
| 이 값을 어딘가 메모 | 5단계에서 `application-local.yml` 에 똑같이 넣어야 함 |

> 로컬 전용 DB라 이 비밀번호가 외부에 노출될 일은 없지만,
> **이 파일은 git에 올라갑니다.** 실행이 끝나면 원래 자리표시자로 되돌려놓으세요.

### 3-2. 실행

아직 `SQL>` 프롬프트에 있죠? `@` 뒤에 파일 전체 경로를 붙여 실행합니다.

```sql
@D:\workspace\Python_Camp\projectTest\meokji\db\00_create_user_local.sql
```

> **경로 알아내는 법**: 탐색기에서 `00_create_user_local.sql` 을 **Shift + 우클릭 → "경로로 복사"**.
> 따옴표까지 같이 복사되는데 `@"C:\...\00_create_user_local.sql"` 형태로 그대로 쓰면 됩니다.
> 경로에 공백이 있으면 따옴표가 **반드시** 필요합니다.

### 3-3. 성공 확인

스크립트 마지막 확인 쿼리가 이렇게 나오면 완료입니다.

```
USERNAME      ACCOUNT_STATUS   DEFAULT_TABLESPACE
------------  ---------------  ------------------
MEOKJI_APP    OPEN             USERS

PRIVILEGE
------------------
CREATE SEQUENCE
CREATE SESSION
CREATE TABLE
```

권한이 3줄 다 나와야 합니다. 확인했으면 빠져나옵니다.

```sql
exit
```

---

## [4] 테이블 만들기

이번엔 **방금 만든 `meokji_app` 계정으로** 접속합니다. SYS가 아닙니다.

```cmd
sqlplus meokji_app/3-1에서_정한_비번@localhost:1521/XEPDB1
```

예시:

```cmd
sqlplus meokji_app/meokji1234@localhost:1521/XEPDB1
```

> **`/XEPDB1` 의 슬래시를 콜론으로 쓰지 마세요.**
> `XEPDB1` 은 SID가 아니라 서비스명(Service Name)이라 반드시 `/` 입니다.
> `:` 로 쓰면 `ORA-12505` 가 납니다. 이 프로젝트에서 제일 자주 나는 실수입니다.
>
> 비밀번호에 특수문자가 있으면 전체를 따옴표로 감싸세요:
> `sqlplus "meokji_app/비번@localhost:1521/XEPDB1"`

접속됐으면 스키마를 실행합니다.

```sql
@D:\workspace\Python_Camp\projectTest\meokji\db\01_schema.sql
```

### 확인 — 테이블 11개

```sql
SELECT table_name FROM user_tables ORDER BY table_name;
```

이 11개가 나와야 합니다.

```
FREE_POSTS
GAME_HISTORIES
MEAL_HISTORIES
NOTICES
RECIPE_LIKES
RECIPE_REVIEWS
REVIEW_COMMENTS
REVIEW_LIKES
USERS
USER_BANS
VERIFICATION_CODES
```

`exit` 로 나옵니다.

> **다시 만들고 싶을 때**: [`db/00_drop_all.sql`](../db/00_drop_all.sql) 을 먼저 실행한 뒤
> `01_schema.sql` 을 다시 실행하세요. → [처음부터 다시 하기](#처음부터-다시-하기)

---

## [4-1] 시드 데이터 넣기 (선택)

테이블만 만들면 화면이 전부 비어 있습니다. 개발용 데이터를 넣으려면
[`db/02_seed.sql`](../db/02_seed.sql) 을 실행하세요.

```sql
@D:\workspace\Python_Camp\projectTest\meokji\db\02_seed.sql
```

공지 5건은 따로입니다 — [`db/05_seed_notices.sql`](../db/05_seed_notices.sql) 을
이어서 실행하세요. 공지가 없으면 공지 화면이 빈 채로 뜹니다.

넣는 내용은 이렇습니다.

| 테이블 | 건수 | 내용 |
|---|---|---|
| `users` | 1 | 시드 계정 (`kmg8585@naver.com` / `meokji1234!`) |
| `user_bans` | 2 | 오이 · 양파 |
| `meal_histories` | 4 | 김치찌개 · 들깨 수제비 · 닭갈비 · 계란밥 |
| `recipe_reviews` | 3 | 앞의 3건에서 공유 |
| `review_comments` | 3 | |
| `review_likes` | 2 | |

**계란밥은 일부러 공유하지 않은 채로 둡니다.** `recipe_reviews.meal_history_id` 에
`uq_reviews_meal(UNIQUE)` 이 걸려 있어 한 기록은 한 번만 공유할 수 있는데,
공유 가능한 기록이 하나도 없으면 글쓰기 화면을 테스트할 수 없기 때문입니다.

> **재실행해도 안전합니다.** 시드 계정의 데이터만 지우고 다시 넣으므로
> 몇 번을 돌려도 결과가 같고, 다른 팀원의 계정·게시글은 건드리지 않습니다.
>
> 단, 그 이메일 계정이 **이미 있으면 비밀번호를 덮어쓰지 않습니다.**
> 쓰던 계정의 로그인이 시드 재실행으로 깨지지 않게 한 것이라,
> 위 표의 `meokji1234!` 는 계정을 새로 만들 때만 적용됩니다.

### 확인

```sql
-- 4 / 3 / 3 / 2 가 나와야 정상
SELECT
    (SELECT COUNT(*) FROM meal_histories)  AS histories,
    (SELECT COUNT(*) FROM recipe_reviews)  AS reviews,
    (SELECT COUNT(*) FROM review_comments) AS comments,
    (SELECT COUNT(*) FROM review_likes)    AS likes
FROM dual;
```

---

## [5] 스프링 설정 파일 만들기

`backend` 폴더에서 템플릿을 복사합니다.

```cmd
cd backend
copy src\main\resources\application-local.yml.example src\main\resources\application-local.yml
```

`application-local.yml` 을 열어 **두 칸**을 채웁니다.

```yaml
spring:
  datasource:
    password: 3-1에서_정한_비번
app:
  jwt:
    secret-key: 영문_32자_이상의_아무_문자열
```

| 항목 | 값 | 설명 |
|---|---|---|
| `spring.datasource.password` | 3-1에서 정한 값 | |
| `app.jwt.secret-key` | 영문 32자 이상 | **비우면 서버가 안 켜지고, 짧으면 로그인할 때 500 이 납니다** |

DB 주소(`localhost:1521/XEPDB1`)와 계정(`meokji_app`)은 `application.yml` 의 기본값이라 따로 적지 않아도 됩니다.

> `application-local.yml` 은 git에 올라가지 않습니다. 각자 자기 PC에서 만드는 파일입니다.
> 비밀번호에 `@` `:` `/` 가 들어 있어도 그대로 적으면 됩니다 —
> YAML 에서 따옴표로 감싸면(`password: "a@b"`) 그대로 전달됩니다.

---

## [6] 백엔드 실행 + 최종 확인

```cmd
cd backend
gradlew bootRun
```

브라우저에서 <http://localhost:8090/api/health> 를 엽니다.

```json
{"status":"ok","app":"뭐먹지? API","version":"0.1.0","env":"local","database":"ok"}
```

**`"database":"ok"` 가 나오면 DB 세팅 완료입니다.**

> Oracle 접속에 Instant Client 같은 별도 프로그램은 필요 없습니다.
> `ojdbc17` 을 Thin 모드로 쓰기 때문에 JDK 17 만 있으면 됩니다.
> Gradle 도 안 깔아도 됩니다 — `gradlew` 가 알아서 받아 씁니다.

> 비밀키가 32자보다 짧으면 서버는 뜨지만, 로그인할 때 `WeakKeyException` 으로 500 이 납니다.
> HS256 의 요구사항입니다.

### 데모 데이터 채우기 (선택)

빈 DB 로는 화면을 볼 수가 없습니다.

```cmd
gradlew seedDemo
```

회원 2명 · 식사 기록 3건을 **실제 API 를 거쳐** 만듭니다. 여러 번 돌려도
안전합니다. 로그인은 `minji@example.com` / `meokji1234!` 입니다.

---

## (선택) DBeaver로 붙어보기

테이블 안의 데이터를 눈으로 보고 싶다면 DBeaver 같은 GUI 툴이 편합니다.
필수는 아닙니다. sqlplus만으로도 다 됩니다.

**새 접속 → Oracle**

| 항목 | 값 |
|---|---|
| Host | `localhost` |
| Port | `1521` |
| Database | `XEPDB1` |
| 타입 | **`Service Name`** ← 기본값이 `SID` 입니다. 안 바꾸면 `ORA-12505` |
| Username | `meokji_app` |
| Password | 3-1에서 정한 값 |
| Role | (비워둠) |

**SQL 파일 실행 시 주의**

- `Ctrl+A` → **`Alt+X`** (Execute script) — 파일 전체 실행
- `Ctrl+Enter` 는 커서가 놓인 **한 문장만** 실행됩니다. 쓰지 마세요
- Preferences → Editors → SQL Editor → SQL Processing 에서
  **"Blank line is statement delimiter" 를 `Never`** 로
  (켜져 있으면 `CREATE TABLE` 이 중간에서 잘려 `ORA-00900`)

---

## 문제 해결

### 접속 단계

| 증상 | 원인 / 해결 |
|---|---|
| `'sqlplus'은(는) 내부 또는 외부 명령...` | PATH 미등록. 전체 경로로 실행 → [2단계](#sqlplus-를-못-찾는다고-나오면) |
| `ORA-01031: insufficient privileges` | cmd를 **관리자 권한으로 실행**한 뒤 재시도 |
| `ORA-12541: TNS:no listener` | 리스너가 꺼짐 → `net start OracleOraDB21Home1TNSListener` |
| `ORA-12505` / `ORA-12514` | `XEPDB1` 을 SID로 취급한 것. `@localhost:1521/XEPDB1` 처럼 **슬래시**로 |
| `ORA-01017: invalid username/password` | 비밀번호 오타. SQL 파일에 적은 값과 `application-local.yml` 의 `password` 가 같은지 확인 |
| `ORA-28000: account is locked` | `sqlplus / as sysdba` 접속 후 `ALTER SESSION SET CONTAINER=XEPDB1;` → `ALTER USER meokji_app ACCOUNT UNLOCK;` |

### 계정 생성 단계

| 증상 | 원인 / 해결 |
|---|---|
| `ORA-65096: invalid common user name` | XEPDB1이 아니라 최상위(CDB$ROOT)에서 실행됨. 스크립트의 `ALTER SESSION SET CONTAINER = XEPDB1;` 줄이 지워지지 않았는지 확인 |
| `ORA-01109: database not open` | XEPDB1이 MOUNTED 상태. [2단계](#이어서--xepdb1이-열려-있는지-확인)의 `OPEN` 먼저 |
| `ORA-01920: user name 'MEOKJI_APP' conflicts...` | 계정이 이미 있음. 그냥 [4단계](#4-테이블-만들기)로 가거나, 새로 만들려면 `DROP USER meokji_app CASCADE;` |
| `ORA-00922: missing or invalid option` | 비밀번호 자리의 큰따옴표 `"` 를 실수로 지움 |
| `SP2-0310: unable to open file` | `@` 뒤 경로 오타. 공백이 있으면 따옴표로 감싸기 |

### 스키마 / 백엔드 단계

| 증상 | 원인 / 해결 |
|---|---|
| `CREATE TABLE` 에서 `ORA-01031` | SYS로 `01_schema.sql` 을 실행함. **`meokji_app` 으로** 접속해서 실행해야 합니다 |
| `ORA-00955: name is already used` | 테이블이 이미 있음. `00_drop_all.sql` 먼저 → [처음부터 다시 하기](#처음부터-다시-하기) |
| `ORA-00900: invalid SQL statement` | DBeaver의 "Blank line is statement delimiter" 설정을 `Never` 로 |
| 서버 실행 시 `Could not resolve placeholder 'JWT_SECRET_KEY'` | `application-local.yml` 이 없거나 `app.jwt.secret-key` 가 비어 있음 |
| 로그인하면 500, 서버 로그에 `WeakKeyException` | `app.jwt.secret-key` 가 32자보다 짧음 |
| `/api/health` 가 `"database":"error"` | Oracle 이 꺼져 있거나 `application-local.yml` 의 `password` 가 다름. 환경변수 `DB_HOST` 가 `10.1.54.120` 으로 잡혀 있지 않은지도 확인 |
| 재부팅 후 갑자기 접속 안 됨 | XEPDB1이 닫힘. `SAVE STATE` 를 안 해둔 경우 → [2단계](#이어서--xepdb1이-열려-있는지-확인) |

### 그래도 안 될 때 — 상태 한 번에 확인

```cmd
sc query OracleServiceXE
sc query OracleOraDB21Home1TNSListener
sqlplus / as sysdba
```

```sql
SELECT name, open_mode FROM v$pdbs;
ALTER SESSION SET CONTAINER = XEPDB1;
SELECT username, account_status FROM dba_users WHERE username = 'MEOKJI_APP';
SELECT COUNT(*) FROM dba_tables WHERE owner = 'MEOKJI_APP';
```

기대값: PDB가 `READ WRITE`, 계정이 `OPEN`, 테이블 개수 `10`.

---

## 처음부터 다시 하기

### 테이블만 밀기 (데이터 초기화)

```cmd
sqlplus meokji_app/비번@localhost:1521/XEPDB1
```

```sql
@D:\...\meokji\db\00_drop_all.sql
@D:\...\meokji\db\01_schema.sql
```

> `00_drop_all.sql` 헤더에는 "팀 공용 DB이니 팀에 공지하라"는 경고가 남아 있는데,
> **로컬 전용으로 바뀌었으므로 내 PC 데이터만 지워집니다.** 공지할 필요 없습니다.

### 계정까지 밀기

```cmd
sqlplus / as sysdba
```

```sql
ALTER SESSION SET CONTAINER = XEPDB1;
DROP USER meokji_app CASCADE;
```

그 후 [3단계](#3-계정-만들기)부터 다시 진행합니다.

---

## 요약 — 명령어만

```cmd
:: [1] 서비스 확인
sc query OracleServiceXE

:: [2] 관리자 접속
sqlplus / as sysdba
```

```sql
SELECT name, open_mode FROM v$pdbs;          -- MOUNTED면 아래 두 줄
ALTER PLUGGABLE DATABASE XEPDB1 OPEN;
ALTER PLUGGABLE DATABASE XEPDB1 SAVE STATE;

@경로\db\00_create_user_local.sql             -- [3] 비밀번호 먼저 수정!
exit
```

```cmd
:: [4] 앱 계정으로 접속 후 스키마
sqlplus meokji_app/비번@localhost:1521/XEPDB1
```

```sql
@경로\db\01_schema.sql
SELECT table_name FROM user_tables;           -- 10개 확인
exit
```

```cmd
:: [5][6] 백엔드
cd backend
copy src\main\resources\application-local.yml.example src\main\resources\application-local.yml
:: application-local.yml 에서 datasource.password / app.jwt.secret-key 수정
gradlew bootRun
:: http://localhost:8090/api/health  ->  "database":"ok"

:: (선택) 데모 데이터
gradlew seedDemo
```

---

## 관련 문서

- [README.md](../README.md) — 프로젝트 전체 실행 방법
- [docs/PROGRESS.md](PROGRESS.md) — 진행 상황과 설계 결정 이유
- [db/00_create_user_local.sql](../db/00_create_user_local.sql) — 계정 생성 스크립트
- [db/01_schema.sql](../db/01_schema.sql) — 테이블 11개 DDL
- [db/00_drop_all.sql](../db/00_drop_all.sql) — 테이블 전체 삭제
