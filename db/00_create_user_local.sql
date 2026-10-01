-- =========================================================
-- 뭐먹지? — 로컬 개발용 Oracle 계정 생성 스크립트
--
-- 언제 쓰나요?
--   집이나 개인 PC에 Oracle 21c XE를 새로 설치했을 때 딱 한 번 실행합니다.
--   팀 공용 서버(10.1.54.120)에는 이미 계정이 있으므로 실행하지 마세요.
--
-- 무엇을 하나요?
--   meokji_app 이라는 DB 계정을 만들고, 테이블을 만들 수 있는 권한을 줍니다.
--   계정을 만드는 일은 관리자만 할 수 있어서 SYS(관리자)로 접속해 실행합니다.
--
-- 처음이라면? 아래 【방법 A】를 위에서부터 그대로 따라 하면 됩니다.
-- =========================================================


-- =========================================================
-- 실행 전 준비 — 딱 한 가지만 고치면 됩니다
--
--   아래 "4. 계정 생성" 부분의
--       <<<비밀번호를_여기에_입력>>>
--   를 본인이 쓸 비밀번호로 바꿔주세요.
--       예)  IDENTIFIED BY "meokji1234"
--
--   · 큰따옴표(")는 지우지 말고 그대로 두세요.
--   · & 문자는 쓰지 마세요 (SQL*Plus가 특수문자로 해석합니다).
--   · 여기 적은 값을 backend-spring 의 application-local.yml
--     (spring.datasource.password) 에도 똑같이 넣어야 합니다.
--   · 이 파일은 git에 올라갑니다. 실행이 끝나면 비밀번호를 다시 지워두세요.
-- =========================================================


-- =========================================================
-- 【방법 A】 명령 프롬프트(cmd)로 실행하기   ★ 추천 — 따로 설치할 게 없습니다
-- ---------------------------------------------------------
--
-- ── 0단계. Oracle이 켜져 있는지 확인 ─────────────────────
--
--   시작 메뉴에서 "cmd" 를 검색해 명령 프롬프트를 열고 입력합니다.
--
--       sc query OracleServiceXE
--
--   STATE 가 RUNNING 이면 정상입니다. 1단계로 넘어가세요.
--   STOPPED 이면 cmd를 "관리자 권한으로 실행"한 뒤 아래를 입력합니다.
--
--       net start OracleServiceXE
--
--
-- ── 1단계. 관리자(SYSDBA)로 DB에 접속 ───────────────────
--
--       sqlplus / as sysdba
--
--   "SQL>" 프롬프트가 뜨면 접속 성공입니다. 비밀번호는 묻지 않습니다.
--   (Oracle을 설치한 Windows 계정은 자동으로 DB 관리자로 인정됩니다.
--    이걸 OS 인증이라고 합니다. SYS 비밀번호를 몰라도 됩니다.)
--
--   ▸ "'sqlplus'은(는) 내부 또는 외부 명령이 아닙니다" 라고 나오면?
--       PATH에 등록이 안 된 것뿐입니다. 전체 경로로 실행하세요.
--       (경로 중간의 사용자이름 부분은 본인 PC에 맞게 바꿉니다)
--
--       "C:\app\사용자이름\product\21c\dbhomeXE\bin\sqlplus.exe" / as sysdba
--
--   ▸ ORA-01031: insufficient privileges 가 나오면?
--       cmd를 "관리자 권한으로 실행"한 뒤 다시 시도하세요.
--
--
-- ── 2단계. XEPDB1이 열려 있는지 확인 ────────────────────
--
--   SQL> 프롬프트에 아래를 세미콜론까지 그대로 입력하고 Enter.
--
--       SELECT name, open_mode FROM v$pdbs;
--
--   결과에서 XEPDB1 줄의 OPEN_MODE 값을 봅니다.
--
--       READ WRITE  ->  정상입니다. 3단계로 넘어가세요.
--       MOUNTED     ->  아직 안 열린 상태입니다. 아래 두 줄을 실행하세요.
--
--           ALTER PLUGGABLE DATABASE XEPDB1 OPEN;
--           ALTER PLUGGABLE DATABASE XEPDB1 SAVE STATE;
--
--       (SAVE STATE 를 해두면 PC를 껐다 켜도 다음부터 자동으로 열립니다.
--        XE는 재부팅하면 MOUNTED 로 남는 경우가 많습니다.)
--
--
-- ── 3단계. 이 파일 실행 ─────────────────────────────────
--
--   SQL> 프롬프트에서 @ 뒤에 이 파일의 전체 경로를 붙여 입력합니다.
--
--       @D:\workspace\Python_Camp\projectTest\meokji\db\00_create_user_local.sql
--
--   · 경로를 모르겠으면 탐색기에서 이 파일을 Shift+우클릭 ->
--     "경로로 복사"를 누르면 클립보드에 복사됩니다.
--   · 복사한 경로에 따옴표가 같이 붙어 있으면 그대로 두면 됩니다.
--     경로에 공백이 있을 때는 반드시 따옴표가 필요합니다.
--
--       @"C:\내 폴더\meokji\db\00_create_user_local.sql"
--
--
-- ── 4단계. 성공했는지 확인 ──────────────────────────────
--
--   이 파일 맨 아래 "6. 확인" 쿼리 결과가 아래처럼 나오면 완료입니다.
--
--       ACCOUNT_STATUS      : OPEN
--       DEFAULT_TABLESPACE  : USERS
--       PRIVILEGE           : CREATE SEQUENCE
--                             CREATE SESSION
--                             CREATE TABLE      (3줄)
--
--   확인했으면  exit  를 입력해 빠져나옵니다.
--   이어서 이 파일 맨 아래 "다음 단계"로 넘어가세요.
--
--
-- ── 에러가 났을 때 ──────────────────────────────────────
--
--   ORA-65096  invalid common user name
--       -> XEPDB1이 아니라 CDB$ROOT에서 실행됐습니다.
--          아래 3번의 ALTER SESSION 문이 지워지지 않았는지 확인하세요.
--   ORA-01109  database not open
--       -> 2단계의 XEPDB1 OPEN 을 먼저 실행하세요.
--   ORA-01920  user name 'MEOKJI_APP' conflicts with another user
--       -> 계정이 이미 있습니다. 처음부터 다시 만들려면 아래 실행 후 재시도.
--          DROP USER meokji_app CASCADE;
--   ORA-00922  missing or invalid option
--       -> 비밀번호 자리의 큰따옴표(")를 실수로 지웠을 가능성이 큽니다.
--   SP2-0310  unable to open file
--       -> @ 뒤 파일 경로 오타입니다. 공백이 있으면 따옴표로 감싸세요.
-- =========================================================


-- =========================================================
-- 【방법 B】 DBeaver로 실행하기
--
--   1) 새 접속 만들기 (Oracle)
--        Host     : localhost
--        Port     : 1521
--        Database : XEPDB1
--        타입     : Service Name   <- SID 아님. 틀리면 ORA-12505
--        Username : sys
--        Password : Oracle 설치할 때 정한 SYS 비밀번호
--        Role     : SYSDBA         <- 반드시 선택
--
--   2) 이 파일을 열고  Ctrl+A  ->  Alt+X  (Execute script)
--        Ctrl+Enter 는 커서가 놓인 한 문장만 실행되므로 쓰지 마세요.
-- =========================================================



-- ---------------------------------------------------------
-- 1. PDB 상태 확인
--    open_mode 가 READ WRITE 여야 정상입니다.
-- ---------------------------------------------------------
SELECT name, open_mode FROM v$pdbs;


-- ---------------------------------------------------------
-- 2. PDB가 MOUNTED 라면 열어줍니다 (이미 READ WRITE 면 건너뜁니다)
--    필요할 때만 아래 두 줄의 앞 "--" 를 지우고 실행하세요.
-- ---------------------------------------------------------
-- ALTER PLUGGABLE DATABASE XEPDB1 OPEN;
-- ALTER PLUGGABLE DATABASE XEPDB1 SAVE STATE;


-- ---------------------------------------------------------
-- 3. XEPDB1 컨테이너로 전환
--    CDB$ROOT 에 계정을 만들면 이름이 C## 로 시작해야 하고
--    애플리케이션 테이블이 엉뚱한 곳에 생깁니다. 반드시 PDB에서 만듭니다.
-- ---------------------------------------------------------
ALTER SESSION SET CONTAINER = XEPDB1;


-- ---------------------------------------------------------
-- 4. 계정 생성
--    ↓↓↓ 아래 비밀번호를 실제 값으로 바꾼 뒤 실행하세요 ↓↓↓
-- ---------------------------------------------------------
CREATE USER meokji_app
    IDENTIFIED BY "<<<비밀번호를_여기에_입력>>>"
    DEFAULT TABLESPACE USERS
    TEMPORARY TABLESPACE TEMP
    QUOTA UNLIMITED ON USERS;


-- ---------------------------------------------------------
-- 5. 권한 부여 — 애플리케이션에 필요한 최소한만
--    DBA / SYSDBA 는 부여하지 않습니다.
--    CREATE SEQUENCE 는 Identity 컬럼이 내부 시퀀스를 만들기 때문에 필요합니다.
-- ---------------------------------------------------------
GRANT CREATE SESSION  TO meokji_app;
GRANT CREATE TABLE    TO meokji_app;
GRANT CREATE SEQUENCE TO meokji_app;


-- ---------------------------------------------------------
-- 6. 확인
-- ---------------------------------------------------------
SELECT username, account_status, default_tablespace
FROM   dba_users
WHERE  username = 'MEOKJI_APP';

SELECT privilege FROM dba_sys_privs WHERE grantee = 'MEOKJI_APP' ORDER BY privilege;


-- =========================================================
-- 다음 단계
--
--   1) 방금 만든 meokji_app 계정으로 접속합니다.
--      SQL> 에서  exit  로 나온 뒤, cmd에 아래를 입력하세요.
--
--        sqlplus meokji_app/여기에_정한_비번@localhost:1521/XEPDB1
--
--      ※ XEPDB1 은 SID가 아니라 Service Name 이라서 슬래시(/)로 씁니다.
--         콜론(:)으로 쓰면 ORA-12505 가 납니다.
--      ※ 비밀번호에 특수문자가 있으면 전체를 따옴표로 감싸세요.
--         sqlplus "meokji_app/비번@localhost:1521/XEPDB1"
--
--   2) 그 상태에서 스키마(테이블 7개)를 만듭니다.
--
--        @D:\workspace\Python_Camp\projectTest\meokji\db\01_schema.sql
--
--      확인:  SELECT table_name FROM user_tables;   -> 7줄이 나오면 성공
--
--   3) backend-spring 의 설정 파일을 만듭니다.
--
--        cd backend-spring
--        copy src\main\resources\application-local.yml.example src\main\resources\application-local.yml
--
--      열어서 두 칸을 채웁니다.
--        spring.datasource.password : 위에서 정한 값
--        app.jwt.secret-key         : 영문 32자 이상의 아무 문자열
--
--   4) 백엔드를 실행하고 확인합니다.
--
--        gradlew bootRun
--
--      브라우저에서 http://localhost:8080/api/health 를 열어
--      "database":"ok" 가 나오면 전부 완료입니다.
-- =========================================================
