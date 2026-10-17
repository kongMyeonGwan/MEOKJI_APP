-- =========================================================
-- RECIPE_REVIEWS 에 첨부파일 컬럼 추가 (v2 -> v2.1)
--
-- 언제 실행하나요?
--   이미 01_schema.sql 로 테이블이 만들어진 DB 에서 딱 한 번 실행합니다.
--   빈 스키마라면 이 파일이 필요 없습니다 — 01_schema.sql 에 이미 들어 있습니다.
--
-- 실행 계정 : MEOKJI_APP
-- 실행 방법 : 파일 전체 선택 후 Alt+X (DBeaver) / F5 (SQL Developer)
--
-- 무엇이 바뀌나요?
--   RECIPE_REVIEWS 에 첨부파일 컬럼 6개가 늘어납니다. 후기 하나당 파일 하나입니다.
--
--     FILE_YN        첨부 여부 ('Y'/'N', 기본 'N')
--     FILE_NAME      서버에 저장한 파일명 (겹치지 않게 바꿔 저장한 이름)
--     FILE_ORG_NAME  사용자가 올린 원래 파일명 (화면 표시 · 내려받기용)
--     FILE_PATH      저장 폴더 경로
--     FILE_EXT       확장자 (점 없이 'jpg')
--     FILE_SIZE      바이트
--
--   기존 행은 FILE_YN 이 'N' 이 되고 나머지는 NULL 입니다 = 첨부 없음.
--   테이블 개수는 그대로 7개입니다.
--
-- 되돌리려면 (아래를 순서대로)
--   ALTER TABLE recipe_reviews DROP CONSTRAINT ck_reviews_file_set;
--   ALTER TABLE recipe_reviews DROP CONSTRAINT ck_reviews_file_size;
--   ALTER TABLE recipe_reviews DROP CONSTRAINT ck_reviews_file_yn;
--   ALTER TABLE recipe_reviews DROP (file_yn, file_name, file_org_name,
--                                    file_path, file_ext, file_size);
-- =========================================================


-- ---------------------------------------------------------
-- 0. 실행 전 확인 — 이미 추가돼 있으면 6, 아직이면 0 입니다.
--    0 이 아니면 이 파일을 실행할 필요가 없습니다 (실행하면 ORA-01430).
-- ---------------------------------------------------------
SELECT COUNT(*) AS already_added
FROM   user_tab_columns
WHERE  table_name  = 'RECIPE_REVIEWS'
AND    column_name IN ('FILE_YN', 'FILE_NAME', 'FILE_ORG_NAME',
                       'FILE_PATH', 'FILE_EXT', 'FILE_SIZE');


-- ---------------------------------------------------------
-- 1. 컬럼 추가
--
--    FILE_YN 만 NOT NULL 이고 나머지는 NULL 을 허용한다.
--    첨부파일이 없는 후기가 정상이고, 이미 쌓인 행에 채워 넣을 값도 없다.
--
--    FILE_YN 에 DEFAULT 'N' 을 주면 기존 행에도 'N' 이 채워지므로
--    NOT NULL 을 같이 붙여도 ORA-01758 이 나지 않는다.
--    (DEFAULT 없이 NOT NULL 만 붙이면 기존 행 때문에 실패한다)
--
--    FILE_ORG_NAME 은 한글 파일명이 들어오므로 VARCHAR2(255 CHAR) 다.
--    CHAR 를 빼면 바이트 기준이 되어 한글이 85자까지밖에 안 들어간다.
-- ---------------------------------------------------------
ALTER TABLE recipe_reviews ADD (
    file_yn        CHAR(1)             DEFAULT 'N' NOT NULL,
    file_name      VARCHAR2(255 CHAR),
    file_org_name  VARCHAR2(255 CHAR),
    file_path      VARCHAR2(500 CHAR),
    file_ext       VARCHAR2(20 CHAR),
    file_size      NUMBER(19)
);


-- ---------------------------------------------------------
-- 2. 제약조건 추가
--
--    ck_reviews_file_set 이 핵심이다. file_yn 과 나머지 컬럼이 따로 노는 것을
--    막는다. 첨부를 지울 때 file_yn 만 'N' 으로 바꾸고 경로를 안 지우는 실수가
--    가장 흔한데, 그러면 화면에 깨진 링크가 뜬다.
--
--    file_size 는 'Y' 쪽 필수에서 뺐다. 크기를 못 구해도 파일 자체는 정상이다.
-- ---------------------------------------------------------
ALTER TABLE recipe_reviews ADD CONSTRAINT ck_reviews_file_yn
    CHECK (file_yn IN ('Y', 'N'));

ALTER TABLE recipe_reviews ADD CONSTRAINT ck_reviews_file_size
    CHECK (file_size IS NULL OR file_size >= 0);

ALTER TABLE recipe_reviews ADD CONSTRAINT ck_reviews_file_set
    CHECK (
        (file_yn = 'N' AND file_name IS NULL AND file_org_name IS NULL
                       AND file_path IS NULL AND file_ext  IS NULL AND file_size IS NULL)
     OR (file_yn = 'Y' AND file_name IS NOT NULL AND file_org_name IS NOT NULL
                       AND file_path IS NOT NULL AND file_ext  IS NOT NULL));


-- ---------------------------------------------------------
-- 3. 컬럼 설명
-- ---------------------------------------------------------
COMMENT ON COLUMN recipe_reviews.file_yn       IS '첨부파일 여부 Y/N. 목록에서 클립 아이콘 표시 판단용';
COMMENT ON COLUMN recipe_reviews.file_name     IS '서버 저장 파일명. 겹치지 않게 바꿔 저장한 이름(UUID 등)';
COMMENT ON COLUMN recipe_reviews.file_org_name IS '사용자가 올린 원래 파일명. 화면 표시·내려받기용';
COMMENT ON COLUMN recipe_reviews.file_path     IS '저장 폴더 경로. 파일명은 포함하지 않는다';
COMMENT ON COLUMN recipe_reviews.file_ext      IS '확장자(점 없이 jpg/png). 허용목록 검사·아이콘 표시용';
COMMENT ON COLUMN recipe_reviews.file_size     IS '파일 크기(바이트)';

COMMENT ON TABLE recipe_reviews IS
    '레시피 후기 게시판. 자기 MEAL_HISTORIES 에서만 작성 가능. 조회는 Guest 가능. 첨부파일은 FILE_* 컬럼에 1건 보관(별도 테이블 없음)';


COMMIT;


-- ---------------------------------------------------------
-- 4. 실행 결과 확인 (아래 두 개를 따로 실행해 보세요)
-- ---------------------------------------------------------
-- 1) 컬럼 6개가 생겼는지 — 6줄이 나와야 합니다
-- SELECT column_name, data_type, data_length, nullable, data_default
-- FROM   user_tab_columns
-- WHERE  table_name = 'RECIPE_REVIEWS'
-- AND    column_name LIKE 'FILE!_%' ESCAPE '!'
-- ORDER  BY column_id;
--
-- 2) 제약조건 3개가 붙었는지
-- SELECT constraint_name, search_condition
-- FROM   user_constraints
-- WHERE  table_name = 'RECIPE_REVIEWS'
-- AND    constraint_name LIKE 'CK!_REVIEWS!_FILE%' ESCAPE '!';
