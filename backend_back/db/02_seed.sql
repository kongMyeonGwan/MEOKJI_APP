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
-- [v1] --   users            1건  시드 계정
-- [v1] --   user_bans        2건  오이 · 양파
-- [v1] --   meal_histories   4건  김치찌개 · 들깨 수제비 · 닭갈비 · 계란밥
-- [v1] --   posts            3건  앞의 3건에서 공유 (계란밥은 일부러 미공유로 남긴다)
-- [v1] --   comments         3건
-- [v1] --   post_likes       2건
--   users            1건  시드 계정 (주소/좌표 포함 — 날씨 화면 확인용)
--   user_bans        2건  오이 · 양파
--   meal_histories   4건  김치찌개 · 들깨 수제비 · 닭갈비 · 계란밥
--   recipe_reviews   3건  앞의 3건에서 공유 (계란밥은 일부러 미공유로 남긴다)
--   review_comments  3건
--   review_likes     2건
--
-- 왜 계란밥만 공유하지 않았나
-- [v1] --   posts.meal_history_id 에 uq_posts_meal(UNIQUE)이 걸려 있어 한 기록은
-- [v1] --   한 번만 공유할 수 있다. 글쓰기 화면에서 고를 수 있는 기록이 하나도 없으면
-- [v1] --   그 화면을 테스트할 수 없으므로 하나를 남겨둔다.
--   recipe_reviews.meal_history_id 에 uq_reviews_meal 이 걸려 있어 한 기록은
--   한 번만 공유할 수 있다. 글쓰기 화면에서 고를 수 있는 기록이 하나도 없으면
--   그 화면을 테스트할 수 없으므로 하나를 남겨둔다.
--   v2에서 이 UNIQUE 는 함수 기반이다 — CASE WHEN use_yn='Y' THEN meal_history_id END.
--   후기를 지웠다가(use_yn='N') 같은 기록으로 다시 쓰는 것은 허용된다.
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
--
-- [v2 변경] 주소 4개 컬럼을 함께 넣는다.
--   v1 시드는 email / password_hash / nickname 3개만 넣었다. 그때는 users 에
--   주소 컬럼 자체가 없었기 때문이다.
--   v2에서 gender / address_sido / address_sigungu / latitude / longitude 가
--   생겼고, 대시보드 날씨가 latitude·longitude 로 API를 호출한다.
--   좌표가 NULL이면 .env 의 DEFAULT_WEATHER_LAT/LON(종로2가)으로 fallback 되어
--   "회원 주소 기반 날씨"가 동작하는지 시드로 확인할 수 없다.
--
--   ON 절은 여전히 email 만 본다. use_yn 을 조건에 넣지 않는 이유는,
--   탈퇴 처리된 동일 이메일 행이 남아 있을 때 그 행을 무시하고 새로 INSERT 하면
--   uq_users_email 이 살아있는 행끼리만 유일성을 보므로 통과해 버려서
--   같은 이메일이 화면에 둘로 보이기 때문이다. email 단독으로 본다.
--
--   gender 는 NULL 로 둔다. DDL 주석대로 NULL = "선택 안 함" 이 정상 상태이고,
--   프로필 화면에서 미선택 표시가 제대로 나오는지도 확인해야 한다.
--   값이 필요하면 'MALE' / 'FEMALE' / 'OTHER' 중 하나로 바꾸면 된다
--   (ck_users_gender 가 그 외의 값을 막는다).
-- =========================================================
-- [v1] MERGE INTO users u
-- [v1] USING (
-- [v1]     SELECT 'kmg8585@naver.com' AS email FROM dual
-- [v1] ) s
-- [v1] ON (u.email = s.email)
-- [v1] WHEN NOT MATCHED THEN
-- [v1]     INSERT (email, password_hash, nickname)
-- [v1]     VALUES (
-- [v1]         s.email,
-- [v1]         '$2b$12$Em/RK7DIBRf6jS8Xud0wLuhcIMGNIpJ7NY25KOqAFfBfXZGppuixi',
-- [v1]         '잘살자'
-- [v1]     );
MERGE INTO users u
USING (
    SELECT 'kmg8585@naver.com' AS email FROM dual
) s
ON (u.email = s.email)
WHEN NOT MATCHED THEN
    INSERT (email, password_hash, nickname,
            gender, address_sido, address_sigungu, latitude, longitude)
    VALUES (
        s.email,
        '$2b$12$Em/RK7DIBRf6jS8Xud0wLuhcIMGNIpJ7NY25KOqAFfBfXZGppuixi',
        '잘살자',
        NULL, '서울특별시', '강남구', 37.517200, 127.047300
    );

-- [v2 추가] 이미 있는 시드 계정의 주소 backfill
--
-- 위 MERGE 에는 WHEN MATCHED 가 없다(비밀번호를 덮어쓰지 않기 위해서다).
-- 그래서 v1 시드로 이미 만들어 둔 계정은 주소가 NULL 인 채로 남는다.
-- 아래 UPDATE 가 그 경우만 채운다.
--
-- WHERE 에 "IS NULL" 을 건 것이 핵심이다.
-- 사용자가 마이페이지에서 다른 주소로 바꿔 두었다면 시드를 다시 돌려도
-- 그 값을 되돌리지 않는다. 재실행 안전(idempotent)이 깨지지 않는다.
UPDATE users
SET    address_sido    = '서울특별시',
       address_sigungu = '강남구',
       latitude        = 37.517200,
       longitude       = 127.047300,
       updated_at      = SYS_EXTRACT_UTC(SYSTIMESTAMP)
WHERE  email = 'kmg8585@naver.com'
AND    latitude IS NULL
AND    longitude IS NULL;


-- =========================================================
-- 1. 기존 시드 데이터 정리
--
-- 삭제 순서가 중요하다. recipe_reviews.meal_history_id(fk_reviews_meal)는
-- ON DELETE 절이 없어 기본값 차단이라, 후기가 남아 있는 동안에는
-- meal_histories 를 지울 수 없다(ORA-02292).
--   review_comments / review_likes  ->  recipe_reviews  ->  meal_histories  ->  user_bans
-- review_comments 와 review_likes 는 recipe_reviews 에 CASCADE로 걸려 있어
-- 생략해도 되지만, 순서를 눈에 보이게 적어두는 편이 나중에 읽기 좋다.
--
-- [v2 참고] 여기는 물리 DELETE 가 맞다.
--   앱은 use_yn='N' 논리 삭제만 쓰지만, 이 스크립트는 "시드를 지우고 다시 넣는"
--   개발용 리셋이다. 논리 삭제로 지우면 재실행할 때마다 'N' 행이 계속 쌓이고,
--   uq_reviews_meal 은 살아있는 행만 보므로 중복도 안 막힌다.
-- =========================================================
-- [v1] DELETE FROM comments
-- [v1] WHERE  post_id IN (
-- [v1]            SELECT p.id FROM posts p
-- [v1]            JOIN   users u ON u.id = p.user_id
-- [v1]            WHERE  u.email = 'kmg8585@naver.com'
-- [v1]        );
DELETE FROM review_comments
WHERE  review_id IN (
           SELECT r.id FROM recipe_reviews r
           JOIN   users u ON u.id = r.user_id
           WHERE  u.email = 'kmg8585@naver.com'
       );

-- [v1] DELETE FROM post_likes
-- [v1] WHERE  post_id IN (
-- [v1]            SELECT p.id FROM posts p
-- [v1]            JOIN   users u ON u.id = p.user_id
-- [v1]            WHERE  u.email = 'kmg8585@naver.com'
-- [v1]        );
DELETE FROM review_likes
WHERE  review_id IN (
           SELECT r.id FROM recipe_reviews r
           JOIN   users u ON u.id = r.user_id
           WHERE  u.email = 'kmg8585@naver.com'
       );

-- [v1] DELETE FROM posts
-- [v1] WHERE  user_id = (SELECT id FROM users WHERE email = 'kmg8585@naver.com');
DELETE FROM recipe_reviews
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
-- 4. RECIPE_REVIEWS   (v1 이름: POSTS)
--
-- [v1] -- food_name / recipe_json / ingredients_json 은 meal_histories 에서 그대로
-- [v1] -- 복사한다. 참조가 아니라 복사다 — 공유 후 글을 고쳐도 원본 스냅샷은 변하지 않고,
-- [v1] -- Guest가 게시글을 볼 때 남의 개인 기록을 JOIN해서 읽지 않아도 된다.
-- [v1] -- 그래서 아래 INSERT ... SELECT 가 meal_histories 에서 값을 그대로 가져온다.
--
-- [v2 변경] 위 설명은 v1 설계였고 지금은 반대다.
--   recipe_reviews 에는 food_name / recipe_json / ingredients_json 컬럼이 없다.
--   meal_histories 가 이미 "확정 당시 스냅샷, 이후 UPDATE 없음" 이라 값이 변할 일이
--   없고, 복사해 두면 두 값이 어긋날 여지만 생기기 때문이다(01_schema.sql 5번 참고).
--   따라서 게시글을 읽을 때는 meal_history_id 로 JOIN 한다.
--
--   그래서 아래 INSERT 는 컬럼 3개를 뺐다. 이름만 바꿔서는 ORA-00904 가 난다.
--   남은 컬럼은 user_id, meal_history_id, title, content, view_count,
--   created_at, updated_at 이다. (use_yn 은 DEFAULT 'Y' 라 생략)
--
--   h.user_id 를 그대로 넣는 것이 중요하다. fk_reviews_meal 이
--   (meal_history_id, user_id) 합성 FK 라, 글쓴이와 기록 주인이 다르면
--   ORA-02291 로 막힌다. 남의 히스토리로 후기를 못 쓰게 하는 장치다.
-- =========================================================
-- [v1] INSERT INTO posts
-- [v1]     (user_id, meal_history_id, title, content,
-- [v1]      food_name, recipe_json, ingredients_json, view_count, created_at, updated_at)
INSERT INTO recipe_reviews
    (user_id, meal_history_id, title, content, view_count, created_at, updated_at)
SELECT
    h.user_id, h.id,
    '쌀쌀한 날엔 역시 김치찌개',
    '오늘 추천받은 대로 김치찌개 끓였는데 진짜 맛있었어요.' || CHR(10)
        || '돼지고기 대신 참치캔 넣어도 괜찮다고 나와서 그렇게 했는데 국물이 더 깔끔하더라고요.'
        || CHR(10) || CHR(10)
        || '재료비도 만원 안 넘어서 부담 없었습니다. 추천해요!',
-- [v1]     h.food_name, h.recipe_json, h.ingredients_json,
    128, TIMESTAMP '2026-08-16 10:30:00', TIMESTAMP '2026-08-16 10:30:00'
FROM   meal_histories h
JOIN   users u ON u.id = h.user_id
WHERE  u.email = 'kmg8585@naver.com' AND h.food_name = '김치찌개';

-- [v1] INSERT INTO posts
-- [v1]     (user_id, meal_history_id, title, content,
-- [v1]      food_name, recipe_json, ingredients_json, view_count, created_at, updated_at)
INSERT INTO recipe_reviews
    (user_id, meal_history_id, title, content, view_count, created_at, updated_at)
SELECT
    h.user_id, h.id,
    '수제비 반죽 실패담...',
    '물을 한 번에 다 부어버려서 반죽이 완전 질어졌어요.' || CHR(10)
        || '조금씩 넣으라는 이유가 있었네요. 다음엔 성공하겠습니다.',
-- [v1]     h.food_name, h.recipe_json, h.ingredients_json,
    64, TIMESTAMP '2026-08-15 12:10:00', TIMESTAMP '2026-08-15 12:10:00'
FROM   meal_histories h
JOIN   users u ON u.id = h.user_id
WHERE  u.email = 'kmg8585@naver.com' AND h.food_name = '들깨 수제비';

-- [v1] INSERT INTO posts
-- [v1]     (user_id, meal_history_id, title, content,
-- [v1]      food_name, recipe_json, ingredients_json, view_count, created_at, updated_at)
INSERT INTO recipe_reviews
    (user_id, meal_history_id, title, content, view_count, created_at, updated_at)
SELECT
    h.user_id, h.id,
    '닭갈비 양념 비율 공유합니다',
    '고추장 3 : 간장 1 : 설탕 1 : 다진마늘 1 비율이 제일 무난했어요.' || CHR(10)
        || '매운 거 좋아하시면 고춧가루 한 스푼 추가하세요.' || CHR(10) || CHR(10)
        || '고구마를 먼저 볶아두는 게 포인트입니다.',
-- [v1]     h.food_name, h.recipe_json, h.ingredients_json,
    302, TIMESTAMP '2026-08-11 11:00:00', TIMESTAMP '2026-08-11 11:00:00'
FROM   meal_histories h
JOIN   users u ON u.id = h.user_id
WHERE  u.email = 'kmg8585@naver.com' AND h.food_name = '닭갈비';


-- =========================================================
-- 5. REVIEW_COMMENTS   (v1 이름: COMMENTS)
--
-- 시드 계정이 하나뿐이라 자기 글에 자기가 댓글을 단 모양이 된다.
-- 화면 확인용으로는 충분하다.
--
-- [v2 변경] 두 가지가 바뀌었다.
--   1) FK 컬럼명이 post_id -> review_id 다.
--   2) 어느 글에 달지 고르는 조건을 p.food_name 으로 쓸 수 없다.
--      recipe_reviews 에 food_name 컬럼이 없어졌기 때문이다(4번 참고).
--      meal_histories 를 한 단계 더 JOIN 해서 h.food_name 으로 찾는다.
-- =========================================================
-- [v1] INSERT INTO comments (post_id, user_id, content, created_at, updated_at)
-- [v1] SELECT p.id, p.user_id, '참치캔 조합 저도 해봐야겠네요!',
-- [v1]        TIMESTAMP '2026-08-16 11:02:00', TIMESTAMP '2026-08-16 11:02:00'
-- [v1] FROM   posts p JOIN users u ON u.id = p.user_id
-- [v1] WHERE  u.email = 'kmg8585@naver.com' AND p.food_name = '김치찌개';
INSERT INTO review_comments (review_id, user_id, content, created_at, updated_at)
SELECT r.id, r.user_id, '참치캔 조합 저도 해봐야겠네요!',
       TIMESTAMP '2026-08-16 11:02:00', TIMESTAMP '2026-08-16 11:02:00'
FROM   recipe_reviews r
JOIN   meal_histories h ON h.id = r.meal_history_id
JOIN   users u          ON u.id = r.user_id
WHERE  u.email = 'kmg8585@naver.com' AND h.food_name = '김치찌개';

-- [v1] INSERT INTO comments (post_id, user_id, content, created_at, updated_at)
-- [v1] SELECT p.id, p.user_id, '국물 깔끔한 거 인정합니다',
-- [v1]        TIMESTAMP '2026-08-17 01:20:00', TIMESTAMP '2026-08-17 01:20:00'
-- [v1] FROM   posts p JOIN users u ON u.id = p.user_id
-- [v1] WHERE  u.email = 'kmg8585@naver.com' AND p.food_name = '김치찌개';
INSERT INTO review_comments (review_id, user_id, content, created_at, updated_at)
SELECT r.id, r.user_id, '국물 깔끔한 거 인정합니다',
       TIMESTAMP '2026-08-17 01:20:00', TIMESTAMP '2026-08-17 01:20:00'
FROM   recipe_reviews r
JOIN   meal_histories h ON h.id = r.meal_history_id
JOIN   users u          ON u.id = r.user_id
WHERE  u.email = 'kmg8585@naver.com' AND h.food_name = '김치찌개';

-- [v1] INSERT INTO comments (post_id, user_id, content, created_at, updated_at)
-- [v1] SELECT p.id, p.user_id, '이 비율 그대로 했는데 성공했어요',
-- [v1]        TIMESTAMP '2026-08-12 05:30:00', TIMESTAMP '2026-08-12 05:30:00'
-- [v1] FROM   posts p JOIN users u ON u.id = p.user_id
-- [v1] WHERE  u.email = 'kmg8585@naver.com' AND p.food_name = '닭갈비';
INSERT INTO review_comments (review_id, user_id, content, created_at, updated_at)
SELECT r.id, r.user_id, '이 비율 그대로 했는데 성공했어요',
       TIMESTAMP '2026-08-12 05:30:00', TIMESTAMP '2026-08-12 05:30:00'
FROM   recipe_reviews r
JOIN   meal_histories h ON h.id = r.meal_history_id
JOIN   users u          ON u.id = r.user_id
WHERE  u.email = 'kmg8585@naver.com' AND h.food_name = '닭갈비';


-- =========================================================
-- 6. REVIEW_LIKES   (v1 이름: POST_LIKES)
--
-- 자기 후기에도 좋아요를 누를 수 있다(스키마 주석 참고).
--
-- [v2 변경] 테이블명 · FK 컬럼명(post_id -> review_id) · 조회 조건 모두 바뀌었다.
--   중복을 막는 인덱스도 uq_post_likes 가 아니라 uq_review_likes 이고,
--   함수 기반이라 (user_id, review_id) 조합은 use_yn='Y' 인 행끼리만 본다.
--   좋아요 취소('N') 후 재클릭이 ORA-00001 로 막히지 않는 이유가 이것이다.
-- =========================================================
-- [v1] INSERT INTO post_likes (user_id, post_id)
-- [v1] SELECT p.user_id, p.id
-- [v1] FROM   posts p JOIN users u ON u.id = p.user_id
-- [v1] WHERE  u.email = 'kmg8585@naver.com' AND p.food_name IN ('김치찌개', '닭갈비');
INSERT INTO review_likes (user_id, review_id)
SELECT r.user_id, r.id
FROM   recipe_reviews r
JOIN   meal_histories h ON h.id = r.meal_history_id
JOIN   users u          ON u.id = r.user_id
WHERE  u.email = 'kmg8585@naver.com' AND h.food_name IN ('김치찌개', '닭갈비');

COMMIT;


-- =========================================================
-- 실행 결과 확인용 쿼리 (아래를 따로 실행해 보세요)
-- =========================================================
-- [v2 변경] 세 쿼리 모두 테이블명을 바꾸고 use_yn='Y' 필터를 추가했다.
--   v1 스키마에는 use_yn 이 없어서 필터가 필요 없었다.
--   v2에서는 이 필터를 빼면 논리 삭제한 행까지 세어, 화면에 보이는 숫자와
--   여기서 확인한 숫자가 달라진다(01_schema.sql 상단 [1] 참고).
--   시드 직후에는 'N' 행이 없어 결과가 같지만, 화면에서 몇 건 지운 뒤
--   다시 세어 보면 필터 없는 쿼리가 틀린 값을 준다.
--
-- 1) 건수 확인 — 각각 4 / 3 / 3 / 2 건이어야 정상
-- [v1] SELECT
-- [v1]     (SELECT COUNT(*) FROM meal_histories) AS histories,
-- [v1]     (SELECT COUNT(*) FROM posts)          AS posts,
-- [v1]     (SELECT COUNT(*) FROM comments)       AS comments,
-- [v1]     (SELECT COUNT(*) FROM post_likes)     AS likes
-- [v1] FROM dual;
-- SELECT
--     (SELECT COUNT(*) FROM meal_histories  WHERE use_yn='Y') AS histories,
--     (SELECT COUNT(*) FROM recipe_reviews  WHERE use_yn='Y') AS reviews,
--     (SELECT COUNT(*) FROM review_comments WHERE use_yn='Y') AS comments,
--     (SELECT COUNT(*) FROM review_likes    WHERE use_yn='Y') AS likes
-- FROM dual;
--
-- 2) 미공유 기록 확인 — '계란밥' 한 건만 나와야 정상
--    (글쓰기 화면의 선택 목록이 이 쿼리와 같은 결과를 쓴다)
--    LEFT JOIN 의 use_yn 조건은 ON 절에 둔다. WHERE 로 내리면 바깥 조인이
--    안쪽 조인처럼 동작해서 미공유 기록이 한 건도 안 나온다.
-- [v1] SELECT h.id, h.food_name, h.recipe_name
-- [v1] FROM   meal_histories h
-- [v1] LEFT   JOIN posts p ON p.meal_history_id = h.id
-- [v1] WHERE  p.id IS NULL;
-- SELECT h.id, h.food_name, h.recipe_name
-- FROM   meal_histories h
-- LEFT   JOIN recipe_reviews r
--         ON  r.meal_history_id = h.id AND r.use_yn = 'Y'
-- WHERE  h.use_yn = 'Y' AND r.id IS NULL;
--
-- 3) 후기 목록 (좋아요/댓글 수 포함)
--    food_name 은 이제 recipe_reviews 에 없으므로 meal_histories 에서 JOIN 해 온다.
-- [v1] SELECT p.id, p.title, u.nickname,
-- [v1]        (SELECT COUNT(*) FROM post_likes l WHERE l.post_id = p.id) AS likes,
-- [v1]        (SELECT COUNT(*) FROM comments  c WHERE c.post_id = p.id) AS comments
-- [v1] FROM   posts p JOIN users u ON u.id = p.user_id
-- [v1] ORDER  BY p.created_at DESC;
-- SELECT r.id, r.title, h.food_name, u.nickname,
--        (SELECT COUNT(*) FROM review_likes l
--          WHERE l.review_id = r.id AND l.use_yn = 'Y') AS likes,
--        (SELECT COUNT(*) FROM review_comments c
--          WHERE c.review_id = r.id AND c.use_yn = 'Y') AS comments
-- FROM   recipe_reviews r
-- JOIN   meal_histories h ON h.id = r.meal_history_id
-- JOIN   users u          ON u.id = r.user_id
-- WHERE  r.use_yn = 'Y'
-- ORDER  BY r.created_at DESC;
--
-- 4) 시드 계정 주소/좌표 확인 — 날씨가 fallback 좌표로 뜨는지 볼 때
-- SELECT email, nickname, address_sido, address_sigungu, latitude, longitude
-- FROM   users WHERE email = 'kmg8585@naver.com';
