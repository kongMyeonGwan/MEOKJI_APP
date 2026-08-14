-- =========================================================
-- 뭐먹지? — 테이블 전체 삭제 스크립트 (Oracle 21c XE)
--
-- 언제 실행하나요?
--   스키마를 처음부터 다시 만들 때만 실행합니다.
--   테이블이 하나도 없는 상태라면 실행할 필요 없이 01_schema.sql 로 바로 가세요.
--
-- ⚠️ 경고: 팀 공용 DB입니다(10.1.54.120 / XEPDB1 / MEOKJI_APP).
--          이 스크립트는 팀원 전체의 데이터를 삭제합니다.
--          실행 전 반드시 팀에 공지하세요.
--
-- 실행 계정: MEOKJI_APP   (SYS / SYSTEM 으로 실행 금지)
-- 실행 방법: DBeaver에서 이 파일 전체 선택 후 Alt+X (Execute script)
--
-- Oracle 21c에는 DROP TABLE IF EXISTS 가 없으므로(23ai부터 지원)
-- user_tables 를 조회해 "존재하는 것만" 지우는 PL/SQL 블록으로 작성했습니다.
-- 이름이 일치하는 우리 7개 테이블만 대상으로 하며, 다른 테이블은 건드리지 않습니다.
-- =========================================================

BEGIN
    FOR t IN (
        SELECT table_name
        FROM   user_tables
        WHERE  table_name IN (
                   'COMMENTS',
                   'POST_LIKES',
                   'POSTS',
                   'MEAL_HISTORIES',
                   'USER_BANS',
                   'VERIFICATION_CODES',
                   'USERS'
               )
    ) LOOP
        -- CASCADE CONSTRAINTS : 참조 중인 FK를 함께 제거 (삭제 순서 신경 안 써도 됨)
        -- PURGE               : 휴지통(Recycle Bin)을 거치지 않고 즉시 삭제
        EXECUTE IMMEDIATE 'DROP TABLE ' || t.table_name || ' CASCADE CONSTRAINTS PURGE';
        DBMS_OUTPUT.PUT_LINE('dropped: ' || t.table_name);
    END LOOP;
END;
/

-- 삭제 결과 확인 (0건이어야 정상)
SELECT COUNT(*) AS remaining_tables
FROM   user_tables
WHERE  table_name IN ('COMMENTS','POST_LIKES','POSTS','MEAL_HISTORIES',
                      'USER_BANS','VERIFICATION_CODES','USERS');
