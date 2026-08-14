-- =========================================================
-- 뭐먹지? — 로컬 개발용 Oracle 계정 생성 스크립트
--
-- 언제 쓰나요?
--   집이나 개인 PC에 Oracle 21c XE를 새로 설치했을 때 한 번만 실행합니다.
--   팀 공용 서버(10.1.54.120)에는 이미 계정이 있으므로 실행하지 마세요.
--
-- 실행 계정 : SYS AS SYSDBA
-- 실행 방법 : DBeaver에서 Ctrl+A -> Alt+X (Execute script)
--
-- ⚠️ 실행 전에 아래 <<<비밀번호를_여기에_입력>>> 부분을 바꾸세요.
--    그리고 backend/.env 의 DB_PASSWORD 에 같은 값을 넣어야 합니다.
--    이 파일은 git에 커밋되므로 실제 비밀번호를 적어두지 마세요.
-- =========================================================


-- ---------------------------------------------------------
-- 1. PDB 상태 확인
--    XE는 재부팅하면 PDB가 MOUNTED 상태로 남는 경우가 많습니다.
--    open_mode 가 READ WRITE 여야 정상입니다.
-- ---------------------------------------------------------
SELECT name, open_mode FROM v$pdbs;


-- ---------------------------------------------------------
-- 2. PDB가 MOUNTED 라면 열어줍니다 (이미 READ WRITE 면 건너뛰세요)
--    SAVE STATE 를 해두면 다음 재부팅 때 자동으로 열립니다.
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
--   1) DBeaver에서 새 접속을 만듭니다
--        Host     : localhost
--        Port     : 1521
--        Database : XEPDB1
--        타입     : Service Name   <- SID 아님. 틀리면 ORA-12505
--        Username : meokji_app
--        Password : 위에서 정한 값
--
--   2) 그 접속으로 01_schema.sql 을 실행합니다 (Ctrl+A -> Alt+X)
--
--   3) backend/.env 를 수정합니다
--        DB_HOST=localhost
--        DB_PASSWORD=위에서 정한 값
--
--   4) uvicorn app.main:app --reload --port 8000
--      http://127.0.0.1:8000/api/health 가 "database":"ok" 를 반환하면 완료
-- =========================================================
