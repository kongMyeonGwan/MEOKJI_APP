-- =========================================================
-- FREE_POSTS : 자유게시판 (v2.5 -> v2.6)
--
-- 언제 실행하나요?
--   01_schema.sql 로 스키마가 만들어진 DB 에서 딱 한 번 실행합니다.
--   빈 스키마라면 이 파일이 필요 없습니다 — 01_schema.sql 에 이미 들어 있습니다.
--
-- 실행 계정 : MEOKJI_APP
-- 실행 방법 : 파일 전체 선택 후 Alt+X (DBeaver) / F5 (SQL Developer)
--             ※ Ctrl+Enter 는 한 문장만 실행합니다
--
-- 무엇이 바뀌나요?
--   테이블이 하나 늘어납니다. 10개 -> 11개.
--   시퀀스도 하나 늘어납니다 (FREE_SEQ). PK 를 IDENTITY 가 아니라 이 시퀀스로 채웁니다.
--
--   RECIPE_REVIEWS(레시피 후기) 와 거의 같은 모양입니다. 제목 · 본문 ·
--   첨부파일 1건 · 조회수 · USE_YN · 작성/수정 시각까지 그대로 가져왔습니다.
--
--   딱 하나가 다릅니다 — 부모 테이블이 없습니다.
--
--     RECIPE_REVIEWS  MEAL_HISTORIES 의 PK 를 MEAL_HISTORY_ID 로 받아야 한다.
--                     "내가 직접 만들어 본 요리" 만 글을 쓸 수 있다는 규칙이
--                     FK(fk_reviews_meal)로 박혀 있어서, 글쓰기 화면이
--                     내 식사 기록 목록에서 하나를 고르는 것으로 시작한다.
--
--     FREE_POSTS      그런 조건이 없다. 로그인만 했으면 아무 주제나 쓴다.
--                     그래서 MEAL_HISTORY_ID 가 없고, 합성 FK 도 없고,
--                     "같은 식사로 두 번 쓰지 못하게" 막던 UNIQUE 인덱스
--                     (uq_reviews_meal) 도 없다. 한 사람이 몇 개를 써도 된다.
--
--   남는 FK 는 USER_ID 하나입니다. 글쓴이가 누구인지는 있어야 하니까요.
--
--   첨부파일도 후기와 똑같이 이 테이블의 FILE_* 컬럼 6개로 받습니다.
--   글 하나당 파일 하나이고, 파일 전용 테이블은 만들지 않습니다.
--
-- 되돌리려면
--   DROP TABLE free_posts PURGE;
--   DROP SEQUENCE free_seq;
-- =========================================================


-- ---------------------------------------------------------
-- 0. 실행 전 확인
--    이미 있으면 1, 아직이면 0 입니다. 1 이면 실행할 필요가 없습니다.
-- ---------------------------------------------------------
SELECT COUNT(*) AS already_created
FROM   user_tables
WHERE  table_name = 'FREE_POSTS';


-- ---------------------------------------------------------
-- 1. 테이블
--
--    왜 이름이 POSTS 가 아니라 FREE_POSTS 인가
--    ----------------------------------------
--    v1 에 POSTS 라는 표가 있었고(지금은 RECIPE_REVIEWS 로 바뀌었다),
--    아직 그 표가 남아 있는 팀원 DB 가 있을 수 있다. 같은 이름을 다시 쓰면
--    이 POSTS 가 옛 표인지 새 표인지 이름만 보고 알 수 없게 된다.
--
--    TITLE 은 VARCHAR2, CONTENT 는 CLOB
--    ---------------------------------
--    RECIPE_REVIEWS 와 같은 기준이다. 제목은 길이가 뻔하고 목록 조회에서
--    매번 읽어야 하므로 VARCHAR2(200 CHAR) 가 맞고, 본문은 사진 설명이든
--    장문이든 들어올 수 있어 4000바이트 제한이 없는 CLOB 로 둔다.
--    목록 응답에는 본문 앞 200자만 잘라서 내려보낸다.
--
--    CHAR 를 꼭 붙인다
--    ----------------
--    VARCHAR2(200) 이라고만 쓰면 바이트 기준이라 한글이 66자까지만 들어간다.
--
--    PK 는 IDENTITY 가 아니라 시퀀스(FREE_SEQ)
--    ---------------------------------------
--    JPA(FreePostEntity)의 @SequenceGenerator 가 FREE_SEQ.NEXTVAL 로 id 를
--    먼저 받아 INSERT 한다. allocationSize = 1 과 맞추려고 INCREMENT BY 1 이다.
--    손으로 넣을 때는 INSERT INTO free_posts (id, ...) VALUES (free_seq.NEXTVAL, ...)
-- ---------------------------------------------------------
CREATE SEQUENCE free_seq START WITH 1 INCREMENT BY 1 NOCACHE;

CREATE TABLE free_posts (
    id             NUMBER(19)          NOT NULL,                                 -- PK. FREE_SEQ 시퀀스로 채운다 (IDENTITY 아님)
    user_id        NUMBER(19)          NOT NULL,                                 -- 글쓴이. 남는 FK 는 이것 하나다 (부모 식사기록이 없다)
    title          VARCHAR2(200 CHAR)  NOT NULL,                                 -- 글 제목
    content        CLOB                NOT NULL,                                 -- 본문. 목록 응답에는 앞 200자만 잘라서 내려보낸다
    file_yn        CHAR(1)             DEFAULT 'N' NOT NULL,                     -- 'Y' = 첨부 있음. 목록에서 클립 아이콘을 붙일지 판단하는 값
    file_name      VARCHAR2(255 CHAR),                                           -- 서버에 저장한 이름. 겹치지 않게 UUID 로 바꿔 저장한다
    file_org_name  VARCHAR2(255 CHAR),                                           -- 사용자가 올린 원래 이름. 화면 표시·내려받기용 (한글이라 CHAR 기준)
    file_path      VARCHAR2(500 CHAR),                                           -- 저장 폴더. 'uploads/2026/10' 처럼 파일명은 빼고 적는다
    file_ext       VARCHAR2(20 CHAR),                                            -- 확장자 (점 없이 'jpg'). 허용 목록 검사·아이콘 표시용
    file_size      NUMBER(19),                                                   -- 바이트. 못 구해도 파일은 정상이라 file_yn='Y' 필수에서 뺐다
    view_count     NUMBER(10)          DEFAULT 0 NOT NULL,                       -- 조회수. 상세 조회 때 1 증가
    use_yn         CHAR(1)             DEFAULT 'Y' NOT NULL,                     -- 'N' = 삭제된 글. 목록·상세 조회에서 걸러야 한다
    created_at     TIMESTAMP(6) DEFAULT SYS_EXTRACT_UTC(SYSTIMESTAMP) NOT NULL,  -- 작성 시각 (UTC). 게시판 목록 정렬 기준
    updated_at     TIMESTAMP(6) DEFAULT SYS_EXTRACT_UTC(SYSTIMESTAMP) NOT NULL,  -- 마지막 수정 시각 (UTC)
    CONSTRAINT pk_free_posts            PRIMARY KEY (id),
    CONSTRAINT fk_free_posts_user       FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_free_posts_view_count CHECK (view_count >= 0),
    CONSTRAINT ck_free_posts_use_yn     CHECK (use_yn IN ('Y', 'N')),
    CONSTRAINT ck_free_posts_file_yn    CHECK (file_yn IN ('Y', 'N')),
    CONSTRAINT ck_free_posts_file_size  CHECK (file_size IS NULL OR file_size >= 0),
    CONSTRAINT ck_free_posts_file_set   CHECK (
        (file_yn = 'N' AND file_name IS NULL AND file_org_name IS NULL
                       AND file_path IS NULL AND file_ext  IS NULL AND file_size IS NULL)
     OR (file_yn = 'Y' AND file_name IS NOT NULL AND file_org_name IS NOT NULL
                       AND file_path IS NOT NULL AND file_ext  IS NOT NULL))
);
-- 첨부파일 컬럼 (글 하나당 파일 하나) — RECIPE_REVIEWS 와 동일하다
--   file_yn       'Y' 면 첨부 있음. file_path IS NOT NULL 로도 알 수 있지만,
--                 목록 조회에서 500자짜리 경로를 읽지 않고 CHAR(1) 하나만 보면 된다.
--   file_name     서버에 실제로 저장한 이름. 사용자가 올린 이름을 그대로 쓰면
--                 같은 이름끼리 덮어쓰고, '../../.env' 같은 경로가 섞여 들어오면
--                 서버 파일을 건드릴 수 있다. 그래서 UUID 로 바꿔 저장한다.
--   file_org_name 사용자가 올린 원래 이름. 화면 표시·내려받기용.
--   file_path     저장 폴더까지만. 파일명을 합쳐 두면 이름 바꿀 때 두 컬럼을 같이 고쳐야 한다.
--   file_ext      확장자. file_name 에서 잘라 쓸 수도 있지만 넣을 때 한 번 계산해 둔다.
--   file_size     바이트. '184 KB' 표시와 용량 제한 검사에 쓴다.
--
-- ck_free_posts_file_set 이 하는 일
--   file_yn='Y' 인데 경로가 비어 있는 행, 반대로 'N' 인데 파일 정보가 남아 있는
--   행을 DB 가 막는다. 첨부를 지울 때 file_yn 만 'N' 으로 바꾸고 나머지를
--   안 지우는 실수가 가장 흔한데, 그러면 화면에 깨진 링크가 뜬다.
--   file_size 는 'Y' 쪽 필수에서 뺐다. 크기를 못 구해도 파일은 정상이기 때문이다.
--
-- 파일 본체(바이너리)는 DB 에 넣지 않는다. 디스크에 저장하고 경로만 적는다.
-- BLOB 으로 넣으면 목록 조회마다 이미지가 딸려 나와 게시판이 급격히 느려지고,
-- 백업 용량이 사진 용량만큼 그대로 불어난다.
--
-- 파일을 여러 개 붙이려면 이 구조로는 안 된다. file_name2, file_name3 로 컬럼을
-- 늘리는 방식은 개수가 고정되고 조회가 지저분해지므로, 그때는 FREE_POST_FILES
-- 테이블을 따로 만들어 (post_id, 파일정보) 로 두는 것이 맞다.

-- 목록 조회용 — WHERE use_yn='Y' ORDER BY created_at DESC 를 그대로 받는다
CREATE INDEX idx_free_posts_created ON free_posts (use_yn, created_at DESC);

-- "내가 쓴 글" 조회용
CREATE INDEX idx_free_posts_user ON free_posts (user_id, use_yn);

-- UNIQUE 인덱스는 없다. RECIPE_REVIEWS 의 uq_reviews_meal 은 "같은 식사로 후기를
-- 두 번 쓰지 못하게" 막는 것이었는데, 자유게시판에는 막을 기준이 없다.
-- 같은 사람이 같은 제목으로 몇 개를 써도 된다.

COMMENT ON TABLE  free_posts IS '자유게시판. 로그인한 회원이면 주제 제한 없이 작성. 조회는 Guest 가능. 첨부파일은 FILE_* 컬럼에 1건 보관(별도 테이블 없음)';
COMMENT ON COLUMN free_posts.user_id       IS '글쓴이. RECIPE_REVIEWS 와 달리 부모 식사기록이 없어 FK 는 이것 하나다';
COMMENT ON COLUMN free_posts.title         IS '글 제목';
COMMENT ON COLUMN free_posts.content       IS '본문. 목록 응답에는 앞 200자만 잘라서 내려보낸다';
COMMENT ON COLUMN free_posts.file_yn       IS '첨부파일 여부 Y/N. 목록에서 클립 아이콘 표시 판단용';
COMMENT ON COLUMN free_posts.file_name     IS '서버 저장 파일명. 겹치지 않게 바꿔 저장한 이름(UUID 등)';
COMMENT ON COLUMN free_posts.file_org_name IS '사용자가 올린 원래 파일명. 화면 표시·내려받기용';
COMMENT ON COLUMN free_posts.file_path     IS '저장 폴더 경로. 파일명은 포함하지 않는다';
COMMENT ON COLUMN free_posts.file_ext      IS '확장자(점 없이 jpg/png). 허용목록 검사·아이콘 표시용';
COMMENT ON COLUMN free_posts.file_size     IS '파일 크기(바이트)';
COMMENT ON COLUMN free_posts.view_count    IS '조회수. 상세 조회 때 1 증가';
COMMENT ON COLUMN free_posts.use_yn        IS 'N = 삭제된 글(논리 삭제). 목록·상세 조회에서 걸러야 한다';


COMMIT;


-- ---------------------------------------------------------
-- 2. 실행 결과 확인
-- ---------------------------------------------------------
-- 1) 표가 생겼는지 — 1줄이 나와야 합니다
-- SELECT table_name FROM user_tables WHERE table_name = 'FREE_POSTS';
--
-- 2) 컬럼이 다 붙었는지 — 13줄이 나와야 합니다
-- SELECT column_name, data_type, data_length, nullable, data_default
-- FROM   user_tab_columns WHERE table_name = 'FREE_POSTS' ORDER BY column_id;
--
-- 3) 제약조건 — PK 1 · FK 1 · CHECK 5 (+ NOT NULL 몫) 이 보여야 합니다
-- SELECT constraint_name, constraint_type, search_condition
-- FROM   user_constraints
-- WHERE  table_name = 'FREE_POSTS'
-- AND    constraint_name NOT LIKE 'SYS!_%' ESCAPE '!'
-- ORDER  BY constraint_type, constraint_name;
--
-- 4) 인덱스 — PK 포함 3줄이 나와야 합니다 (UNIQUE 는 PK 하나뿐)
-- SELECT index_name, uniqueness FROM user_indexes WHERE table_name = 'FREE_POSTS';
--
-- 5) 전체 표 개수 — 11 이 나와야 합니다
-- SELECT COUNT(*) FROM user_tables;
--
-- 6) 시퀀스 — FREE_SEQ 1줄, increment_by 1 이어야 합니다
-- SELECT sequence_name, increment_by, last_number FROM user_sequences WHERE sequence_name = 'FREE_SEQ';
