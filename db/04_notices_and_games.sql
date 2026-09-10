-- =========================================================
-- NOTICES 분류 컬럼 + GAME_HISTORIES 게임 종류 확장 (v2.1 -> v2.2)
--
-- 언제 실행하나요?
--   이미 01_schema.sql 로 NOTICES · GAME_HISTORIES 가 만들어진 DB 에서
--   딱 한 번 실행합니다. 빈 스키마라면 이 파일이 필요 없습니다 —
--   01_schema.sql 에 이미 반영돼 있습니다.
--
-- 실행 계정 : MEOKJI_APP
-- 실행 방법 : 파일 전체 선택 후 Alt+X (DBeaver) / F5 (SQL Developer)
--             ※ Ctrl+Enter 는 한 문장만 실행합니다
--
-- 무엇이 바뀌나요?
--   1) NOTICES 에 CATEGORY 컬럼이 하나 늘어납니다.
--      공지 목록의 분류 배지('점검' · '안내' · '업데이트')를 그리는 값입니다.
--      화면(NoticeList.jsx)이 이미 이 값을 쓰고 있었는데, 공지를
--      파일(catalog.json)에서 DB 로 옮기면서 컬럼이 필요해졌습니다.
--      기존 행은 전부 '안내' 가 됩니다.
--
--   2) GAME_HISTORIES 의 게임 종류가 'A' 하나에서 'A' · 'B' · 'C' 로 늘어납니다.
--      화면의 게임이 셋이기 때문입니다.
--
--        A  음식 월드컵   (/game/worldcup)
--        B  취향 밸런스   (/game/balance)
--        C  랜덤 돌림판   (/roulette)
--
--   테이블 개수는 그대로 9개입니다.
--
-- 되돌리려면
--   ALTER TABLE notices DROP COLUMN category;
--   ALTER TABLE game_histories DROP CONSTRAINT ck_game_histories_type;
--   ALTER TABLE game_histories ADD  CONSTRAINT ck_game_histories_type
--       CHECK (game_type IN ('A'));
-- =========================================================


-- ---------------------------------------------------------
-- 0. 실행 전 확인
--    CATEGORY 가 이미 있으면 1, 아직이면 0 입니다.
--    1 이면 아래 1번을 건너뛰세요 (실행하면 ORA-01430).
-- ---------------------------------------------------------
SELECT COUNT(*) AS category_already_added
FROM   user_tab_columns
WHERE  table_name  = 'NOTICES'
AND    column_name = 'CATEGORY';


-- ---------------------------------------------------------
-- 1. NOTICES.CATEGORY 추가
--
--    DEFAULT 를 같이 주기 때문에 기존 행에도 '안내' 가 채워지고,
--    NOT NULL 을 붙여도 ORA-01758 이 나지 않는다.
--    (DEFAULT 없이 NOT NULL 만 붙이면 기존 행 때문에 실패한다)
--
--    한글이 들어가므로 VARCHAR2(20 CHAR) 다. CHAR 를 빼면 바이트 기준이
--    되어 한글이 6자까지밖에 안 들어간다.
--
--    CHECK 는 걸지 않는다. 분류는 운영하면서 늘어나는 값이고, 늘 때마다
--    DDL 을 고쳐야 하는 제약은 득보다 실이 크다.
-- ---------------------------------------------------------
ALTER TABLE notices ADD (
    category  VARCHAR2(20 CHAR)  DEFAULT '안내' NOT NULL
);

COMMENT ON COLUMN notices.category IS '목록의 분류 배지. 점검/안내/업데이트 등. 화면은 점검만 경고색';


-- ---------------------------------------------------------
-- 2. GAME_HISTORIES 게임 종류를 A · B · C 로
--
--    CHECK 는 수정이 안 된다. 지웠다가 다시 만든다.
--    기존 행은 전부 'A' 라 새 제약에도 걸리지 않는다.
-- ---------------------------------------------------------
ALTER TABLE game_histories DROP CONSTRAINT ck_game_histories_type;

ALTER TABLE game_histories ADD CONSTRAINT ck_game_histories_type
    CHECK (game_type IN ('A', 'B', 'C'));

COMMENT ON COLUMN game_histories.game_type IS '게임 종류. A=음식 월드컵 B=취향 밸런스 C=랜덤 돌림판';


COMMIT;


-- ---------------------------------------------------------
-- 3. 실행 결과 확인 (아래 두 개를 따로 실행해 보세요)
-- ---------------------------------------------------------
-- 1) CATEGORY 가 생겼는지 — 1줄이 나와야 합니다
-- SELECT column_name, data_type, data_length, nullable, data_default
-- FROM   user_tab_columns
-- WHERE  table_name = 'NOTICES' AND column_name = 'CATEGORY';
--
-- 2) 게임 종류 제약이 바뀌었는지 — IN ('A','B','C') 가 보여야 합니다
-- SELECT constraint_name, search_condition
-- FROM   user_constraints
-- WHERE  constraint_name = 'CK_GAME_HISTORIES_TYPE';
