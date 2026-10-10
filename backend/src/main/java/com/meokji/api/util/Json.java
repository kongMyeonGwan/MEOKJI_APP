package com.meokji.api.util;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * CLOB 에 담긴 JSON 문자열과 JsonNode 사이를 오간다.
 *
 * recipe_json · ingredients_json · substitutes_json 세 칸이
 * CLOB + CHECK(... IS JSON) 이다. DB 에는 문자열로 넣고 응답에서는 JSON 그대로
 * 내보내야 해서, 그 변환을 한 군데로 모았다.
 *
 * 응답 DTO 가 String 을 들고 있으면 화면에 따옴표 붙은 문자열이 나가고,
 * React 가 recipe_json.steps 를 읽지 못한다.
 *
 * <p>왜 CLOB 인가 — 레시피 한 건의 조리 순서가 배열이고, 재료도 배열이다.
 * 표로 풀면 MEAL_HISTORIES 하나에 딸린 표가 둘 더 생기고, 기록을 읽을
 * 때마다 JOIN 을 두 번 해야 한다. 식사 기록은 그때 모습 그대로 남기면
 * 되는 값이라 통째로 담았다.
 *
 * <p>MAPPER 를 static final 로 하나만 두는 이유 — JsonMapper 를 만드는
 * 일이 가볍지 않다. 쓸 때마다 새로 만들면 기록을 읽을 때마다 세 번씩
 * 만들게 된다. 만들고 나면 상태를 바꾸지 않으므로 같이 써도 안전하다.
 */
public final class Json {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private Json() {
    }

    /** JSON 을 DB 에 넣을 문자열로. null 은 그대로 null (컬럼이 NULL 을 받는다). */
    // isNull() 도 같이 보는 이유 — JSON 의 null 은 자바의 null 이 아니라
    // "null 이라는 값" 을 담은 노드다. 그대로 글자로 바꾸면 "null" 이라는
    // 네 글자가 DB 에 들어간다
    public static String text(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }

        return MAPPER.writeValueAsString(node);
    }

    /**
     * DB 에서 읽은 문자열을 JSON 으로.
     *
     * 깨진 값이면 null 을 돌려준다. DDL 의 CHECK 제약이 막아주므로 정상적으로는
     * 일어나지 않지만, 제약이 걸리기 전에 들어간 옛 행이 있을 수 있다. 그 한 건
     * 때문에 목록 전체가 500 이 되는 것보다 그 칸만 비는 편이 낫다.
     */
    public static JsonNode node(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        try {
            return MAPPER.readTree(text);
        } catch (JacksonException exc) {
            return null;
        }
    }
}
