package com.meokji.api;

import static org.assertj.core.api.Assertions.assertThat;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ObjectNode;

/**
 * 회원 정보 중 DB 가 정하는 두 칸(id · created_at)을 정답지 대조에서 뺀다.
 *
 * <p>두 칸은 계정을 만드는 순간 DB 가 채운다. gradlew seedDemo 로 데모 계정을
 * 다시 만들거나 팀원마다 DB 가 다르면 값이 달라질 수밖에 없어서, 정답지에
 * 적힌 값(id 197 등)과 글자까지 같기를 바랄 수 없다. 그래서 값 대신
 * <b>모양</b>만 본다 — id 는 양의 정수, created_at 은 UTC 문자열("...Z").
 *
 * <p>나머지 칸(이메일 · 닉네임 · 주소 ...)은 그대로 글자까지 비교한다.
 *
 * <pre>
 * MemberDbValues.assertShape(actual);
 * assertThat(MemberDbValues.without(actual)).isEqualTo(MemberDbValues.without(want));
 * </pre>
 */
final class MemberDbValues {

    private MemberDbValues() {
    }

    /** id · created_at 을 뺀 사본. 받은 노드는 건드리지 않는다 */
    static JsonNode without(JsonNode member) {
        ObjectNode copy = (ObjectNode) member.deepCopy();
        copy.remove("id");
        copy.remove("created_at");
        return copy;
    }

    /** id · created_at 이 값은 몰라도 제 모양으로 들어 있는지 */
    static void assertShape(JsonNode member) {
        assertThat(member.get("id").isIntegralNumber()).as("id 는 정수").isTrue();
        assertThat(member.get("id").asLong()).as("id 는 양수").isPositive();

        assertThat(member.get("created_at").isString()).as("created_at 은 문자열").isTrue();
        assertThat(member.get("created_at").asString()).as("created_at 은 UTC").endsWith("Z");
    }
}
