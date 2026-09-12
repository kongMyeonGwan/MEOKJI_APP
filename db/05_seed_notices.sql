-- =========================================================
-- NOTICES 초기 공지 5건
--
-- 언제 실행하나요?
--   공지 테이블이 비어 있을 때 한 번. 없어도 서비스는 돌아가고,
--   화면에 "등록된 공지가 없습니다" 가 뜰 뿐입니다.
--
-- 실행 계정 : MEOKJI_APP
-- 실행 방법 : 파일 전체 선택 후 Alt+X (DBeaver) / F5 (SQL Developer)
--
-- 어디서 온 내용인가요?
--   backend/app/data/catalog.json 에 하드코딩돼 있던 공지입니다.
--   공지를 파일에서 DB 로 옮기면서 화면에 보이던 내용을 그대로 옮겼습니다.
--
-- 작성자(USER_ID)는 누구로 들어가나요?
--   아래 v_admin_email 에 적은 계정으로 넣습니다. 그 계정이 없으면 가장 먼저
--   가입한 회원으로 대신 넣고, 회원이 하나도 없으면 아무것도 넣지 않고
--   끝냅니다 (fk_notices_user 때문에 없는 회원으로는 INSERT 할 수 없습니다).
--
--   ※ 회원이 하나도 없는 빈 DB 에서는 이 파일이 아무 일도 하지 않습니다.
--     가입을 한 번 한 뒤에 다시 실행하세요.
--
--   USERS.USER_LEVEL (1=관리자) 이 06_add_user_level.sql 로 생겼습니다.
--   이메일 대신 등급으로 작성자를 찾고 싶으면 아래 SELECT 를
--   WHERE user_level = 1 AND use_yn = 'Y' 로 바꾸면 됩니다.
--
-- 두 번 실행하면?
--   제목이 같은 공지가 이미 있으면 건너뜁니다. 안전하게 다시 돌려도 됩니다.
-- =========================================================

SET SERVEROUTPUT ON

DECLARE
    -- 공지 작성자로 쓸 계정. 팀 상황에 맞게 바꾸세요.
    --
    -- .local 같은 주소는 쓰지 마세요. 가입 API 의 EmailStr 이 예약 도메인이라며
    -- 422 로 막아서 그 주소로는 계정이 아예 만들어지지 않습니다 — 여기 적어둬도
    -- 영원히 못 찾고 '가장 먼저 가입한 회원' 으로 넘어갑니다.
    v_admin_email  VARCHAR2(255) := 'admin@meokji.com';

    v_admin        NUMBER;
    v_exists       NUMBER;
BEGIN
    -- 1) 작성자 정하기
    BEGIN
        SELECT id INTO v_admin
        FROM   users
        WHERE  email = v_admin_email AND use_yn = 'Y';
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            BEGIN
                -- 관리자 계정이 없으면 가장 먼저 가입한 회원으로 대신한다
                SELECT id INTO v_admin
                FROM   (SELECT id FROM users WHERE use_yn = 'Y' ORDER BY id)
                WHERE  ROWNUM = 1;
            EXCEPTION
                WHEN NO_DATA_FOUND THEN
                    DBMS_OUTPUT.PUT_LINE('회원이 하나도 없어 공지를 넣지 않았습니다.');
                    RETURN;
            END;
    END;

    -- 2) 이미 넣어둔 적이 있으면 건너뛴다
    SELECT COUNT(*) INTO v_exists FROM notices WHERE use_yn = 'Y';

    IF v_exists > 0 THEN
        DBMS_OUTPUT.PUT_LINE('공지가 이미 ' || v_exists || '건 있어 건너뜁니다.');
        RETURN;
    END IF;

    -- 3) 공지 넣기
    INSERT INTO notices (user_id, title, content, view_count,
                         pinned_yn, created_at, updated_at)
    VALUES (v_admin,
            '가격 정보는 참고용입니다',
            '재료 가격은 네이버쇼핑·쿠팡·알리익스프레스에서 수집한 값입니다.' || CHR(10) || '실제 결제 금액은 배송비와 판매자 사정에 따라 달라질 수 있습니다.' || CHR(10) || '특히 해외배송 상품은 도착까지 시간이 오래 걸릴 수 있으니 확인해주세요.',
            264,
            'N',
            TIMESTAMP '2026-08-05 07:15:00',
            TIMESTAMP '2026-08-05 07:15:00');

    INSERT INTO notices (user_id, title, content, view_count,
                         pinned_yn, created_at, updated_at)
    VALUES (v_admin,
            '이달의 레시피 공유 이벤트 (~8/31)',
            '직접 만든 요리 사진과 레시피를 커뮤니티에 올려주세요.' || CHR(10) || '좋아요를 가장 많이 받은 세 분께 소정의 상품을 드립니다.' || CHR(10) || '당첨자는 9월 첫째 주에 공지로 안내드립니다.',
            731,
            'N',
            TIMESTAMP '2026-08-12 02:00:00',
            TIMESTAMP '2026-08-12 02:00:00');

    INSERT INTO notices (user_id, title, content, view_count,
                         pinned_yn, created_at, updated_at)
    VALUES (v_admin,
            '못 먹는 재료 등록 기능이 추가되었습니다',
            '마이페이지에서 알레르기 재료와 싫어하는 재료를 등록할 수 있습니다.' || CHR(10) || '알레르기로 등록한 재료는 추천에서 완전히 제외되고,' || CHR(10) || '싫어하는 재료는 가능한 한 대체재료로 바꿔 제안합니다.',
            486,
            'N',
            TIMESTAMP '2026-08-20 05:30:00',
            TIMESTAMP '2026-08-20 05:30:00');

    INSERT INTO notices (user_id, title, content, view_count,
                         pinned_yn, created_at, updated_at)
    VALUES (v_admin,
            '커뮤니티 이용 규칙 안내',
            '서로 존중하는 말투로 소통해주세요.' || CHR(10) || '레시피는 자세히 공유해주실수록 좋아요.' || CHR(10) || '링크 및 홍보성 글은 사전 안내 없이 삭제될 수 있습니다.' || CHR(10) || '신고는 관리자 확인 후 조치됩니다.',
            2317,
            'Y',
            TIMESTAMP '2026-08-01 00:00:00',
            TIMESTAMP '2026-08-01 00:00:00');

    INSERT INTO notices (user_id, title, content, view_count,
                         pinned_yn, created_at, updated_at)
    VALUES (v_admin,
            '8월 25일(월) 새벽 서버 점검 안내 (02:00~04:00)',
            '추천 기능 개선을 위한 서버 점검이 예정되어 있습니다.' || CHR(10) || '점검 시간 동안에는 로그인과 레시피 추천을 이용하실 수 없습니다.' || CHR(10) || '작성 중인 글은 미리 저장해두시길 권해드립니다.',
            1042,
            'Y',
            TIMESTAMP '2026-08-22 09:00:00',
            TIMESTAMP '2026-08-22 09:00:00');

    DBMS_OUTPUT.PUT_LINE('공지 5건을 넣었습니다. (작성자 id=' || v_admin || ')');
END;
/

COMMIT;


-- 확인
-- SELECT id, pinned_yn, title, view_count, created_at
-- FROM   notices WHERE use_yn = 'Y'
-- ORDER  BY pinned_yn DESC, created_at DESC;
