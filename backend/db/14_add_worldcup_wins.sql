-- =========================================================
-- WORLDCUP_WINS : 음식 이상형 월드컵 우승 기록 (명예의 전당)
--
-- 언제 실행하나요?
--   기존 DB 에 딱 한 번 실행합니다.
--
-- 실행 계정 : MEOKJI_APP
-- 실행 방법 : 파일 전체 선택 후 Alt+X (DBeaver) / F5 (SQL Developer)
--             ※ Ctrl+Enter 는 한 문장만 실행합니다
--
-- 무엇이 바뀌나요?
--   WORLDCUP_WINS 테이블이 하나 늘어납니다.
--   로그인한 회원이 월드컵을 끝까지 하면 한 판에 한 줄이 쌓입니다.
--   월드컵 라운드 고르는 화면의 '명예의 전당 TOP 5' 가 이 표를 셉니다.
--
--   GAME_HISTORIES 는 "게임을 시작했다" 만 남기고 결과 칸이 없다.
--   시작할 때 넣은 행을 끝날 때 고치려 해도 그 표에는 행을 집을 PK 가 없어서,
--   우승 결과는 따로 쌓는다. 이 표도 쌓기만 하는 로그라 PK · USE_YN 이 없다.
--
-- 되돌리려면
--   DROP TABLE worldcup_wins;
-- =========================================================


CREATE TABLE worldcup_wins (
    user_id     NUMBER(19)          NOT NULL,                                     -- 우승을 고른 회원. 회원 삭제 시 함께 삭제된다
    food_name   VARCHAR2(150 CHAR)  NOT NULL,                                     -- 우승 음식 (MEAL_HISTORIES.food_name 과 같은 길이)
    runner_up   VARCHAR2(150 CHAR),                                               -- 결승에서 진 음식. 결승 진출 횟수를 셀 때 쓴다
    round_size  NUMBER(3)           NOT NULL,                                     -- 몇 강으로 했는지 (16 · 32 · 64)
    created_at  TIMESTAMP(6)        DEFAULT SYS_EXTRACT_UTC(SYSTIMESTAMP) NOT NULL, -- 우승 시각 (UTC 고정)
    CONSTRAINT fk_worldcup_wins_user  FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT ck_worldcup_wins_round CHECK (round_size BETWEEN 2 AND 128)
);

-- 순위는 음식 이름으로 묶어 센다
CREATE INDEX idx_worldcup_wins_food ON worldcup_wins (food_name);

COMMENT ON TABLE  worldcup_wins            IS '음식 이상형 월드컵 우승 기록. 명예의 전당 순위를 센다';
COMMENT ON COLUMN worldcup_wins.runner_up  IS '결승에서 진 음식';
COMMENT ON COLUMN worldcup_wins.round_size IS '몇 강으로 했는지';

COMMIT;


-- ---------------------------------------------------------
-- 실행 결과 확인 — 5줄이 나와야 합니다
-- ---------------------------------------------------------
-- SELECT column_name, data_type, nullable
-- FROM   user_tab_columns
-- WHERE  table_name = 'WORLDCUP_WINS';
