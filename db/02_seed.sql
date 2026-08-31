-- =========================================================
-- 뭐먹지? — 개발용 시드 데이터 (Oracle 21c XE)
--
-- 언제 실행하나요?
--   01_schema.sql 로 테이블을 만든 직후, 화면을 띄워볼 데이터가 필요할 때.
--   실제 서비스 데이터가 아니라 개발/시연용입니다.
--
-- 실행 계정 : MEOKJI_APP   (SYS / SYSTEM 으로 실행 금지)
-- 실행 방법 : DBeaver에서 이 파일 전체 선택 후 Alt+X (Execute script)
--
-- ⚠️ 팀 공용 DB입니다(10.1.54.120 / XEPDB1 / MEOKJI_APP).
--    이 스크립트는 아래 시드 계정의 기존 데이터를 지우고 다시 넣습니다.
--    다른 사람의 계정과 게시글은 건드리지 않습니다.
--
-- 재실행 안전(idempotent)합니다. 몇 번을 돌려도 결과가 같습니다.
--
-- 넣는 것
--   users            1건  시드 계정
--   user_bans        2건  오이 · 양파
--   meal_histories   4건  김치찌개 · 들깨 수제비 · 닭갈비 · 계란밥
--   posts            3건  앞의 3건에서 공유 (계란밥은 일부러 미공유로 남긴다)
--   comments         3건
--   post_likes       2건
--
-- 왜 계란밥만 공유하지 않았나
--   posts.meal_history_id 에 uq_posts_meal(UNIQUE)이 걸려 있어 한 기록은
--   한 번만 공유할 수 있다. 글쓰기 화면에서 고를 수 있는 기록이 하나도 없으면
--   그 화면을 테스트할 수 없으므로 하나를 남겨둔다.
--
-- 재료에 오이/양파를 쓰지 않은 이유
--   이 계정의 Ban 재료다. 추천 화면에서 Ban 필터가 동작하는지 볼 때
--   시드 레시피에 섞여 있으면 헷갈린다.
-- =========================================================


-- =========================================================
-- 0. 시드 계정
--
-- 비밀번호는 bcrypt 해시로만 넣는다. DDL 주석대로 평문은 저장하지 않는다.
-- 아래 해시의 원문은 meokji1234! 이다. 개발용 계정이라 팀에 공유해도 되지만,
-- 이 계정을 운영에 그대로 쓰면 안 된다.
--
-- WHEN NOT MATCHED 만 두고 WHEN MATCHED 는 두지 않았다.
-- 이미 그 이메일로 쓰던 계정이 있으면 비밀번호를 덮어쓰지 않고 그대로 둔다.
-- 시드를 다시 돌렸다고 남이 쓰던 계정의 로그인이 깨지면 안 되기 때문이다.
--
-- id 는 지정하지 않는다. IDENTITY 가 채우게 두고, 아래에서는 email 로 찾아 쓴다.
-- (명시적 id 를 넣으면 IDENTITY 시퀀스가 따라오지 않아 나중에 PK 충돌이 난다)
-- =========================================================
MERGE INTO users u
USING (
    SELECT 'kmg8585@naver.com' AS email FROM dual
) s
ON (u.email = s.email)
WHEN NOT MATCHED THEN
    INSERT (email, password_hash, nickname)
    VALUES (
        s.email,
        '$2b$12$Em/RK7DIBRf6jS8Xud0wLuhcIMGNIpJ7NY25KOqAFfBfXZGppuixi',
        '잘살자'
    );


-- =========================================================
-- 1. 기존 시드 데이터 정리
--
-- 삭제 순서가 중요하다. posts.meal_history_id 는 RESTRICT(ON DELETE 절 없음)라
-- 게시글이 남아 있는 동안에는 meal_histories 를 지울 수 없다(ORA-02292).
--   comments / post_likes  ->  posts  ->  meal_histories  ->  user_bans
-- comments 와 post_likes 는 posts 에 CASCADE로 걸려 있어 생략해도 되지만,
-- 순서를 눈에 보이게 적어두는 편이 나중에 읽기 좋다.
-- =========================================================
DELETE FROM comments
WHERE  post_id IN (
           SELECT p.id FROM posts p
           JOIN   users u ON u.id = p.user_id
           WHERE  u.email = 'kmg8585@naver.com'
       );

DELETE FROM post_likes
WHERE  post_id IN (
           SELECT p.id FROM posts p
           JOIN   users u ON u.id = p.user_id
           WHERE  u.email = 'kmg8585@naver.com'
       );

DELETE FROM posts
WHERE  user_id = (SELECT id FROM users WHERE email = 'kmg8585@naver.com');

DELETE FROM meal_histories
WHERE  user_id = (SELECT id FROM users WHERE email = 'kmg8585@naver.com');

DELETE FROM user_bans
WHERE  user_id = (SELECT id FROM users WHERE email = 'kmg8585@naver.com');


-- =========================================================
-- 2. USER_BANS
-- =========================================================
INSERT INTO user_bans (user_id, ingredient_name, ban_type)
SELECT id, '오이', 'DISLIKE' FROM users WHERE email = 'kmg8585@naver.com';

INSERT INTO user_bans (user_id, ingredient_name, ban_type)
SELECT id, '양파', 'DISLIKE' FROM users WHERE email = 'kmg8585@naver.com';


-- =========================================================
-- 3. MEAL_HISTORIES
--
-- 시각은 전부 UTC다 (TIMESTAMP '...' 리터럴을 그대로 넣는다).
-- 컬럼이 TIMESTAMP(6) 이고 DDL DEFAULT도 SYS_EXTRACT_UTC 이므로
-- 여기서도 UTC 기준 값을 넣어야 앱에서 읽은 시각이 어긋나지 않는다.
-- =========================================================
INSERT INTO meal_histories
    (user_id, food_name, recipe_name, recipe_json, ingredients_json,
     substitutes_json, estimated_cost, selected_at)
SELECT
    id, '김치찌개', '돼지고기 김치찌개',
    '{"cook_time_minutes":25,"servings":2,"difficulty":"쉬움","steps":['
        || '"돼지고기를 한입 크기로 썰어 팬에 볶는다.",'
        || '"잘 익은 김치를 넣고 3분간 함께 볶는다.",'
        || '"물 500ml를 붓고 센 불에서 끓인다.",'
        || '"두부와 대파를 넣고 5분 더 끓인 뒤 간을 맞춘다."]}',
    '[{"name":"돼지고기 앞다리살","quantity":200,"unit":"g"},'
        || '{"name":"신김치","quantity":300,"unit":"g"},'
        || '{"name":"두부","quantity":1,"unit":"모"},'
        || '{"name":"대파","quantity":1,"unit":"대"}]',
    '[{"original":"돼지고기 앞다리살","substitute":"참치캔",'
        || '"reason":"더 간편하게 만들 수 있습니다"}]',
    7800, TIMESTAMP '2026-08-16 09:20:00'
FROM users WHERE email = 'kmg8585@naver.com';

INSERT INTO meal_histories
    (user_id, food_name, recipe_name, recipe_json, ingredients_json,
     substitutes_json, estimated_cost, selected_at)
SELECT
    id, '들깨 수제비', '고소한 들깨 수제비',
    '{"cook_time_minutes":30,"servings":2,"difficulty":"쉬움","steps":['
        || '"밀가루에 물을 조금씩 넣어가며 반죽하고 30분 숙성한다.",'
        || '"멸치 육수를 낸다.",'
        || '"반죽을 얇게 떼어 넣고 끓인다.",'
        || '"들깨가루를 풀고 소금으로 간한다."]}',
    '[{"name":"밀가루","quantity":200,"unit":"g"},'
        || '{"name":"들깨가루","quantity":4,"unit":"큰술"},'
        || '{"name":"애호박","quantity":0.5,"unit":"개"}]',
    NULL,
    6200, TIMESTAMP '2026-08-15 11:05:00'
FROM users WHERE email = 'kmg8585@naver.com';

INSERT INTO meal_histories
    (user_id, food_name, recipe_name, recipe_json, ingredients_json,
     substitutes_json, estimated_cost, selected_at)
SELECT
    id, '닭갈비', '매콤 춘천식 닭갈비',
    '{"cook_time_minutes":35,"servings":3,"difficulty":"보통","steps":['
        || '"고추장 3 : 간장 1 : 설탕 1 : 다진마늘 1 로 양념을 만든다.",'
        || '"닭다리살을 한입 크기로 썰어 양념에 30분 재운다.",'
        || '"달군 팬에 고구마부터 넣고 5분 볶는다.",'
        || '"재운 닭을 넣고 익을 때까지 볶다가 떡사리와 깻잎을 넣는다."]}',
    '[{"name":"닭다리살","quantity":600,"unit":"g"},'
        || '{"name":"고구마","quantity":1,"unit":"개"},'
        || '{"name":"떡사리","quantity":150,"unit":"g"},'
        || '{"name":"깻잎","quantity":10,"unit":"장"}]',
    '[{"original":"닭다리살","substitute":"닭가슴살",'
        || '"reason":"담백하게 즐기고 싶을 때 좋습니다"}]',
    11400, TIMESTAMP '2026-08-11 10:40:00'
FROM users WHERE email = 'kmg8585@naver.com';

-- 이 한 건은 일부러 공유하지 않는다. 글쓰기 화면에서 고를 대상이 필요하다.
INSERT INTO meal_histories
    (user_id, food_name, recipe_name, recipe_json, ingredients_json,
     substitutes_json, estimated_cost, selected_at)
SELECT
    id, '계란밥', '간장 버터 계란밥',
    '{"cook_time_minutes":10,"servings":1,"difficulty":"쉬움","steps":['
        || '"밥을 그릇에 담고 버터 한 조각을 올린다.",'
        || '"계란을 반숙으로 부친다.",'
        || '"밥 위에 계란을 올리고 간장과 참기름을 두른다."]}',
    '[{"name":"밥","quantity":1,"unit":"공기"},'
        || '{"name":"계란","quantity":2,"unit":"개"},'
        || '{"name":"버터","quantity":10,"unit":"g"}]',
    NULL,
    2300, TIMESTAMP '2026-08-24 00:15:00'
FROM users WHERE email = 'kmg8585@naver.com';


-- =========================================================
-- 4. POSTS
--
-- food_name / recipe_json / ingredients_json 은 meal_histories 에서 그대로
-- 복사한다. 참조가 아니라 복사다 — 공유 후 글을 고쳐도 원본 스냅샷은 변하지 않고,
-- Guest가 게시글을 볼 때 남의 개인 기록을 JOIN해서 읽지 않아도 된다.
-- 그래서 아래 INSERT ... SELECT 가 meal_histories 에서 값을 그대로 가져온다.
-- =========================================================
INSERT INTO posts
    (user_id, meal_history_id, title, content,
     food_name, recipe_json, ingredients_json, view_count, created_at, updated_at)
SELECT
    h.user_id, h.id,
    '쌀쌀한 날엔 역시 김치찌개',
    '오늘 추천받은 대로 김치찌개 끓였는데 진짜 맛있었어요.' || CHR(10)
        || '돼지고기 대신 참치캔 넣어도 괜찮다고 나와서 그렇게 했는데 국물이 더 깔끔하더라고요.'
        || CHR(10) || CHR(10)
        || '재료비도 만원 안 넘어서 부담 없었습니다. 추천해요!',
    h.food_name, h.recipe_json, h.ingredients_json,
    128, TIMESTAMP '2026-08-16 10:30:00', TIMESTAMP '2026-08-16 10:30:00'
FROM   meal_histories h
JOIN   users u ON u.id = h.user_id
WHERE  u.email = 'kmg8585@naver.com' AND h.food_name = '김치찌개';

INSERT INTO posts
    (user_id, meal_history_id, title, content,
     food_name, recipe_json, ingredients_json, view_count, created_at, updated_at)
SELECT
    h.user_id, h.id,
    '수제비 반죽 실패담...',
    '물을 한 번에 다 부어버려서 반죽이 완전 질어졌어요.' || CHR(10)
        || '조금씩 넣으라는 이유가 있었네요. 다음엔 성공하겠습니다.',
    h.food_name, h.recipe_json, h.ingredients_json,
    64, TIMESTAMP '2026-08-15 12:10:00', TIMESTAMP '2026-08-15 12:10:00'
FROM   meal_histories h
JOIN   users u ON u.id = h.user_id
WHERE  u.email = 'kmg8585@naver.com' AND h.food_name = '들깨 수제비';

INSERT INTO posts
    (user_id, meal_history_id, title, content,
     food_name, recipe_json, ingredients_json, view_count, created_at, updated_at)
SELECT
    h.user_id, h.id,
    '닭갈비 양념 비율 공유합니다',
    '고추장 3 : 간장 1 : 설탕 1 : 다진마늘 1 비율이 제일 무난했어요.' || CHR(10)
        || '매운 거 좋아하시면 고춧가루 한 스푼 추가하세요.' || CHR(10) || CHR(10)
        || '고구마를 먼저 볶아두는 게 포인트입니다.',
    h.food_name, h.recipe_json, h.ingredients_json,
    302, TIMESTAMP '2026-08-11 11:00:00', TIMESTAMP '2026-08-11 11:00:00'
FROM   meal_histories h
JOIN   users u ON u.id = h.user_id
WHERE  u.email = 'kmg8585@naver.com' AND h.food_name = '닭갈비';


-- =========================================================
-- 5. COMMENTS
--
-- 시드 계정이 하나뿐이라 자기 글에 자기가 댓글을 단 모양이 된다.
-- 화면 확인용으로는 충분하다.
-- =========================================================
INSERT INTO comments (post_id, user_id, content, created_at, updated_at)
SELECT p.id, p.user_id, '참치캔 조합 저도 해봐야겠네요!',
       TIMESTAMP '2026-08-16 11:02:00', TIMESTAMP '2026-08-16 11:02:00'
FROM   posts p JOIN users u ON u.id = p.user_id
WHERE  u.email = 'kmg8585@naver.com' AND p.food_name = '김치찌개';

INSERT INTO comments (post_id, user_id, content, created_at, updated_at)
SELECT p.id, p.user_id, '국물 깔끔한 거 인정합니다',
       TIMESTAMP '2026-08-17 01:20:00', TIMESTAMP '2026-08-17 01:20:00'
FROM   posts p JOIN users u ON u.id = p.user_id
WHERE  u.email = 'kmg8585@naver.com' AND p.food_name = '김치찌개';

INSERT INTO comments (post_id, user_id, content, created_at, updated_at)
SELECT p.id, p.user_id, '이 비율 그대로 했는데 성공했어요',
       TIMESTAMP '2026-08-12 05:30:00', TIMESTAMP '2026-08-12 05:30:00'
FROM   posts p JOIN users u ON u.id = p.user_id
WHERE  u.email = 'kmg8585@naver.com' AND p.food_name = '닭갈비';


-- =========================================================
-- 6. POST_LIKES
--
-- 자기 게시글에도 좋아요를 누를 수 있다(스키마 주석 참고).
-- 중복은 uq_post_likes 가 막는다.
-- =========================================================
INSERT INTO post_likes (user_id, post_id)
SELECT p.user_id, p.id
FROM   posts p JOIN users u ON u.id = p.user_id
WHERE  u.email = 'kmg8585@naver.com' AND p.food_name IN ('김치찌개', '닭갈비');

COMMIT;


-- =========================================================
-- 실행 결과 확인용 쿼리 (아래를 따로 실행해 보세요)
-- =========================================================
-- 1) 건수 확인 — 각각 4 / 3 / 3 / 2 건이어야 정상
-- SELECT
--     (SELECT COUNT(*) FROM meal_histories) AS histories,
--     (SELECT COUNT(*) FROM posts)          AS posts,
--     (SELECT COUNT(*) FROM comments)       AS comments,
--     (SELECT COUNT(*) FROM post_likes)     AS likes
-- FROM dual;
--
-- 2) 미공유 기록 확인 — '계란밥' 한 건만 나와야 정상
--    (글쓰기 화면의 선택 목록이 이 쿼리와 같은 결과를 쓴다)
-- SELECT h.id, h.food_name, h.recipe_name
-- FROM   meal_histories h
-- LEFT   JOIN posts p ON p.meal_history_id = h.id
-- WHERE  p.id IS NULL;
--
-- 3) 게시글 목록 (좋아요/댓글 수 포함)
-- SELECT p.id, p.title, u.nickname,
--        (SELECT COUNT(*) FROM post_likes l WHERE l.post_id = p.id) AS likes,
--        (SELECT COUNT(*) FROM comments  c WHERE c.post_id = p.id) AS comments
-- FROM   posts p JOIN users u ON u.id = p.user_id
-- ORDER  BY p.created_at DESC;
