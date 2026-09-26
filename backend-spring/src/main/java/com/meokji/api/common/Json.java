package com.meokji.api.common;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * CLOB 에 담긴 JSON 문자열과 {@link JsonNode} 사이를 오간다.
 *
 * <p>{@code recipe_json} · {@code ingredients_json} · {@code substitutes_json} 세 칸이
 * CLOB + {@code CHECK(... IS JSON)} 이다. DB 에는 문자열로 넣고 응답에서는 JSON 그대로
 * 내보내야 해서, 그 변환을 한 군데로 모았다.
 *
 * <p>응답 DTO 가 {@code String} 을 들고 있으면 화면에 따옴표 붙은 문자열이 나가고,
 * React 가 {@code recipe_json.steps} 를 읽지 못한다.
 */
public final class Json {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private Json() {
    }

    /** JSON 을 DB 에 넣을 문자열로. null 은 그대로 null (컬럼이 NULL 을 받는다). */
    public static String text(JsonNode node) {
        if (node == null || node.isNull()) {
            return null;
        }

        return MAPPER.writeValueAsString(node);
    }

    /**
     * DB 에서 읽은 문자열을 JSON 으로.
     *
     * <p>깨진 값이면 null 을 돌려준다. DDL 의 CHECK 제약이 막아주므로 정상적으로는
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
