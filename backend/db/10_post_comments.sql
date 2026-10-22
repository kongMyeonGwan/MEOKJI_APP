-- =========================================================
-- REVIEW_COMMENTS -> POST_COMMENTS : 댓글 표를 게시판 둘이 같이 쓴다 (v2.6 -> v2.7)
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
--   표 개수는 그대로입니다. 이름과 칸이 바뀝니다.
--
--     REVIEW_COMMENTS  ->  POST_COMMENTS
--     REVIEW_ID        ->  POST_ID
--     (없음)           ->  BOARD_TYPE  'R' = 레시피리뷰, 'F' = 자유게시판
--
--   레시피리뷰 댓글과 자유게시판 댓글을 한 표에 같이 담습니다. 칸 구성이
--   똑같아서(글번호 · 쓴사람 · 내용 · USE_YN · 시각) 표를 둘로 나눌 이유가
--   없었습니다. 게시판이 셋째로 늘어도 BOARD_TYPE 에 글자 하나만 더하면 됩니다.
--
--   대신 RECIPE_REVIEWS 로 가던 FK 를 뗍니다. POST_ID 한 칸이 두 표를 가리키게
--   되는데, 오라클이든 어디든 한 컬럼이 두 부모를 참조하는 FK 는 만들 수 없습니다.
--   "진짜 있는 글인지" 는 서비스 계층이 봅니다 (FreePostServiceImpl 의
--   findAlivePost 처럼 글을 먼저 찾고 404 를 던지는 식).
--
--   USERS 로 가는 FK 는 그대로 둡니다. 그쪽은 부모가 하나뿐입니다.
--
-- 되돌리려면
--   ALTER TABLE post_comments DROP CONSTRAINT ck_post_comments_board;
--   ALTER TABLE post_comments DROP COLUMN board_type;
--   ALTER TABLE post_comments RENAME COLUMN post_id TO review_id;
--   ALTER TABLE post_comments RENAME TO review_comments;
--   (FK 와 인덱스 이름도 같이 되돌려야 합니다. 아래 5·6번 참고)
-- =========================================================


-- ---------------------------------------------------------
-- 0. 실행 전 확인
--    이미 바꿨으면 1, 아직이면 0 입니다. 1 이면 실행할 필요가 없습니다.
-- ---------------------------------------------------------
SELECT COUNT(*) AS already_done
FROM   user_tables
WHERE  table_name = 'POST_COMMENTS';


-- ---------------------------------------------------------
-- 1. 혹시 FREE_POST_COMMENTS 를 먼저 만들었다면 옮겨 담고 버린다
--
--    자유게시판 댓글을 따로 만들었다가 한 표로 합치기로 바꿨다. 그 표를
--    이미 만들어 쓴 DB 가 있을 수 있어서, 있으면 내용을 옮기고 지운다.
--    없으면 이 블록은 아무 일도 하지 않는다.
-- ---------------------------------------------------------
DECLARE
    cnt NUMBER;
BEGIN
    SELECT COUNT(*) INTO cnt FROM user_tables WHERE table_name = 'FREE_POST_COMMENTS';

    IF cnt > 0 THEN
        -- 아래 2~4번이 끝난 뒤에 넣어야 하므로 여기서는 표만 남겨둔다.
        DBMS_OUTPUT.PUT_LINE('FREE_POST_COMMENTS 가 있습니다. 7번에서 옮겨 담습니다.');
    END IF;
END;
/


-- ---------------------------------------------------------
-- 2. 표 이름 바꾸기
-- ---------------------------------------------------------
ALTER TABLE review_comments RENAME TO post_comments;


-- ---------------------------------------------------------
-- 3. 컬럼 이름 바꾸기
-- ---------------------------------------------------------
ALTER TABLE post_comments RENAME COLUMN review_id TO post_id;


-- ---------------------------------------------------------
-- 4. RECIPE_REVIEWS 로 가던 FK 를 뗀다
--
--    POST_ID 가 이제 두 표(RECIPE_REVIEWS · FREE_POSTS)를 가리키므로
--    한쪽만 참조하는 FK 는 자유게시판 댓글을 넣는 순간 ORA-02291 을 낸다.
-- ---------------------------------------------------------
ALTER TABLE post_comments DROP CONSTRAINT fk_review_comments_rev;


-- ---------------------------------------------------------
-- 5. BOARD_TYPE 추가
--
--    DEFAULT 'R' 이라 이미 쌓인 댓글은 전부 레시피리뷰 댓글이 된다.
--    실제로도 그렇다 — 자유게시판 댓글은 아직 이 표에 없다.
--    DEFAULT 를 같이 주기 때문에 NOT NULL 을 붙여도 ORA-01758 이 나지 않는다.
-- ---------------------------------------------------------
ALTER TABLE post_comments ADD (
    board_type  CHAR(1)  DEFAULT 'R' NOT NULL
);

ALTER TABLE post_comments ADD CONSTRAINT ck_post_comments_board
    CHECK (board_type IN ('R', 'F'));


-- ---------------------------------------------------------
-- 6. 제약조건 · 인덱스 이름 맞추기
--
--    이름에 review 가 남아 있으면 표 이름과 어긋나서, 나중에 제약조건 목록을
--    봤을 때 어느 표 것인지 헷갈린다.
--
--    인덱스는 이름만 바꾸지 않고 다시 만든다. 선두 컬럼에 BOARD_TYPE 이
--    들어가야 "자유게시판 3번 글의 댓글" 조회가 인덱스를 그대로 탄다.
-- ---------------------------------------------------------
ALTER TABLE post_comments RENAME CONSTRAINT pk_review_comments       TO pk_post_comments;
ALTER TABLE post_comments RENAME CONSTRAINT fk_review_comments_user  TO fk_post_comments_user;
ALTER TABLE post_comments RENAME CONSTRAINT ck_review_comments_useyn TO ck_post_comments_useyn;

-- PK 가 쓰는 인덱스는 제약조건 이름을 바꿔도 따라오지 않는다. 따로 바꾼다.
ALTER INDEX pk_review_comments RENAME TO pk_post_comments;

DROP INDEX idx_review_comments_rev;

CREATE INDEX idx_post_comments_post
    ON post_comments (board_type, post_id, use_yn, created_at);


-- ---------------------------------------------------------
-- 7. FREE_POST_COMMENTS 가 있으면 옮겨 담고 버린다
--
--    없으면 두 문장 다 ORA-00942 가 납니다. 0번 확인에서 그 표가 없었다면
--    이 블록은 통째로 건너뛰세요.
-- ---------------------------------------------------------
DECLARE
    cnt NUMBER;
BEGIN
    SELECT COUNT(*) INTO cnt FROM user_tables WHERE table_name = 'FREE_POST_COMMENTS';

    IF cnt > 0 THEN
        EXECUTE IMMEDIATE '
            INSERT INTO post_comments (board_type, post_id, user_id, content, use_yn, created_at, updated_at)
            SELECT ''F'', post_id, user_id, content, use_yn, created_at, updated_at
              FROM free_post_comments';

        EXECUTE IMMEDIATE 'DROP TABLE free_post_comments PURGE';
        DBMS_OUTPUT.PUT_LINE('FREE_POST_COMMENTS 를 옮겨 담고 지웠습니다.');
    ELSE
        DBMS_OUTPUT.PUT_LINE('FREE_POST_COMMENTS 는 없습니다. 건너뜁니다.');
    END IF;
END;
/


-- ---------------------------------------------------------
-- 8. 설명
-- ---------------------------------------------------------
COMMENT ON TABLE  post_comments IS
    '댓글. 레시피리뷰(R)와 자유게시판(F)이 같이 쓴다. 대댓글 미지원. 삭제는 use_yn=N';
COMMENT ON COLUMN post_comments.board_type IS
    '어느 게시판의 댓글인지. R=RECIPE_REVIEWS, F=FREE_POSTS. POST_ID 와 짝으로 봐야 한다';
COMMENT ON COLUMN post_comments.post_id IS
    '댓글이 달린 글 번호. BOARD_TYPE 에 따라 가리키는 표가 달라서 FK 를 걸지 않는다';


COMMIT;


-- ---------------------------------------------------------
-- 9. 실행 결과 확인
-- ---------------------------------------------------------
-- 1) 표 이름이 바뀌었는지 — POST_COMMENTS 1줄, REVIEW_COMMENTS 0줄
-- SELECT table_name FROM user_tables
-- WHERE  table_name IN ('POST_COMMENTS', 'REVIEW_COMMENTS', 'FREE_POST_COMMENTS');
--
-- 2) 칸 확인 — BOARD_TYPE 이 있고 REVIEW_ID 가 없어야 합니다
-- SELECT column_name, data_type, nullable, data_default
-- FROM   user_tab_columns WHERE table_name = 'POST_COMMENTS' ORDER BY column_id;
--
-- 3) FK 는 USERS 하나만 남아야 합니다
-- SELECT c.constraint_name, p.table_name AS parent_table
-- FROM   user_constraints c
-- JOIN   user_constraints p ON p.constraint_name = c.r_constraint_name
-- WHERE  c.table_name = 'POST_COMMENTS' AND c.constraint_type = 'R';
--
-- 4) 게시판별 댓글 수
-- SELECT board_type, COUNT(*) FROM post_comments GROUP BY board_type;

CREATE INDEX idx_post_comments_post ON post_comments (board_type, post_id, use_yn, created_at);