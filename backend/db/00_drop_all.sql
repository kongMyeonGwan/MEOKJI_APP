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
-- 이름이 일치하는 우리 테이블만 대상으로 하며, 다른 테이블은 건드리지 않습니다.
--
-- 목록에 v1 이름(POSTS / POST_LIKES / COMMENTS)도 함께 넣어 두었습니다.
-- 아직 마이그레이션하지 않은 DB에서도 이 스크립트 하나로 정리되게 하기 위함입니다.
--
-- ! 표를 새로 만들면 아래 목록에도 이름을 넣어야 합니다. 빠뜨리면 그 표만
--   남은 채로 01_schema.sql 을 돌리게 되고, CREATE TABLE 이 ORA-00955 로 막힙니다.
--   목록은 아래 두 군데(PL/SQL 블록 · 확인 쿼리)에 있습니다.
--
-- ! 직접 만든 시퀀스(FREE_SEQ)는 표를 지워도 남습니다. IDENTITY 가 속으로 만든
--   시퀀스(ISEQ$$_...)와 달리 표에 딸려 있지 않아서입니다. 그래서 따로 지웁니다.
--   안 지우면 01_schema.sql 의 CREATE SEQUENCE 가 ORA-00955 로 막힙니다.
-- =========================================================

BEGIN
    FOR t IN (
        SELECT table_name
        FROM   user_tables
        WHERE  table_name IN (
                   -- v2 (현재)
                   'POST_COMMENTS',
                   'REVIEW_COMMENTS',   -- 옛 이름 (10_post_comments.sql 전)
                   'REVIEW_LIKES',
                   'RECIPE_REVIEWS',
                   'RECIPE_LIKES',
                   'FREE_POST_COMMENTS',  -- 잠깐 있었던 표. 지금은 POST_COMMENTS 로 합쳤다
                   'FREE_POSTS',
                   'NOTICES',
                   'GAME_HISTORIES',
                   -- v1 (구버전 — 남아 있을 수 있어 함께 정리)
                   'COMMENTS',
                   'POST_LIKES',
                   'POSTS',
                   -- 공통
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

-- 직접 만든 시퀀스 삭제 (표를 지워도 남기 때문에 따로 지운다)
BEGIN
    FOR s IN (
        SELECT sequence_name
        FROM   user_sequences
        WHERE  sequence_name IN ('FREE_SEQ')
    ) LOOP
        EXECUTE IMMEDIATE 'DROP SEQUENCE ' || s.sequence_name;
        DBMS_OUTPUT.PUT_LINE('dropped sequence: ' || s.sequence_name);
    END LOOP;
END;
/

-- 삭제 결과 확인 (0건이어야 정상)
SELECT COUNT(*) AS remaining_tables
FROM   user_tables
WHERE  table_name IN ('POST_COMMENTS','REVIEW_COMMENTS','REVIEW_LIKES','RECIPE_REVIEWS',
                      'FREE_POST_COMMENTS','FREE_POSTS',
                      'RECIPE_LIKES','NOTICES','GAME_HISTORIES',
                      'COMMENTS','POST_LIKES','POSTS','MEAL_HISTORIES',
                      'USER_BANS','VERIFICATION_CODES','USERS');

-- 시퀀스 삭제 결과 확인 (0건이어야 정상)
SELECT COUNT(*) AS remaining_sequences
FROM   user_sequences
WHERE  sequence_name IN ('FREE_SEQ');
