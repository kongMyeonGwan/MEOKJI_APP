-- =========================================================
-- USERS 에 권한 등급 컬럼 추가 (v2.2 -> v2.3)
--
-- 언제 실행하나요?
--   이미 01_schema.sql 로 USERS 가 만들어진 DB 에서 딱 한 번 실행합니다.
--   빈 스키마라면 이 파일이 필요 없습니다 — 01_schema.sql 에 이미 들어 있습니다.
--
-- 실행 계정 : MEOKJI_APP
-- 실행 방법 : 파일 전체 선택 후 Alt+X (DBeaver) / F5 (SQL Developer)
--             ※ Ctrl+Enter 는 한 문장만 실행합니다
--
-- 무엇이 바뀌나요?
--   USERS 에 USER_LEVEL 컬럼이 하나 늘어납니다.
--
--     1  관리자
--     2  일반 회원
--
--   기존 회원은 전부 2(일반 회원)가 됩니다. 관리자를 만들려면 아래 4번처럼
--   해당 계정만 1 로 올려주세요.
--
--   테이블 개수는 그대로 9개입니다.
--
-- 주의 — 화면의 '요리 레벨'과 다른 값입니다
--   마이페이지에 나오는 레벨(1 → 2 → 3 …)은 식사 기록 · 후기로 쌓인 경험치에서
--   매번 계산하는 값이고 DB 에 저장하지 않습니다. 이 컬럼과 아무 상관이 없습니다.
--   여기 숫자는 오직 관리자 여부만 가릅니다.
--
-- 되돌리려면
--   ALTER TABLE users DROP CONSTRAINT ck_users_user_level;
--   ALTER TABLE users DROP COLUMN user_level;
-- =========================================================


-- ---------------------------------------------------------
-- 0. 실행 전 확인
--    USER_LEVEL 이 이미 있으면 1, 아직이면 0 입니다.
--    1 이면 아래 1번을 건너뛰세요 (실행하면 ORA-01430).
-- ---------------------------------------------------------
SELECT COUNT(*) AS already_added
FROM   user_tab_columns
WHERE  table_name  = 'USERS'
AND    column_name = 'USER_LEVEL';


-- ---------------------------------------------------------
-- 1. USERS.USER_LEVEL 추가
--
--    DEFAULT 를 같이 주기 때문에 기존 행에도 2 가 채워지고,
--    NOT NULL 을 붙여도 ORA-01758 이 나지 않는다.
--    (DEFAULT 없이 NOT NULL 만 붙이면 기존 행 때문에 실패한다)
--
--    DEFAULT 가 2 인 이유
--      빠뜨렸을 때 일반 회원이 되는 쪽이 안전하다. DEFAULT 1 이면 이 컬럼을
--      모르는 INSERT 하나가 관리자를 만들어낸다.
--
--    CHECK 를 거는 이유
--      NOTICES.CATEGORY 에는 CHECK 를 걸지 않았다. 분류는 운영하면서 늘어나는
--      값이라 늘 때마다 DDL 을 고쳐야 하기 때문이다. 이 컬럼은 반대다 —
--      "관리자냐 아니냐" 두 갈래라 늘어날 여지가 없고, 오타로 0 이나 3 이
--      들어가면 관리자 판정이 조용히 어긋난다. DB 가 막아주는 편이 낫다.
-- ---------------------------------------------------------
ALTER TABLE users ADD (
    user_level  NUMBER(1)  DEFAULT 2 NOT NULL
);

ALTER TABLE users ADD CONSTRAINT ck_users_user_level
    CHECK (user_level IN (1, 2));

COMMENT ON COLUMN users.user_level IS '권한 등급. 1=관리자 2=일반 회원. 화면의 요리 레벨과 무관';


COMMIT;


-- ---------------------------------------------------------
-- 2. 실행 결과 확인
--    USER_LEVEL 이 생겼는지 — 1줄이 나와야 합니다
-- ---------------------------------------------------------
-- SELECT column_name, data_type, data_length, nullable, data_default
-- FROM   user_tab_columns
-- WHERE  table_name = 'USERS' AND column_name = 'USER_LEVEL';


-- ---------------------------------------------------------
-- 3. 관리자로 올리기 (필요할 때 이메일만 바꿔 실행하세요)
--
--    관리자를 만드는 화면은 없습니다. 아무나 자기를 관리자로 올릴 수 있는
--    API 를 두느니 DB 에서 직접 올리는 편이 낫습니다.
-- ---------------------------------------------------------
-- UPDATE users SET user_level = 1 WHERE email = 'admin@example.com' AND use_yn = 'Y';
-- COMMIT;
--
-- 지금 관리자가 누구인지 보려면
-- SELECT id, email, nickname FROM users WHERE user_level = 1 AND use_yn = 'Y';
