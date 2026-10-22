package com.meokji.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.meokji.api.ban.vo.UserBanVO;
import com.meokji.api.history.vo.MealHistoryVO;
import com.meokji.api.user.vo.UserVO;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * VO 가 화면으로 나갈 때의 모양을 본다.
 *
 * <p>DTO 를 VO 로 합치면서 "표 한 행" 과 "화면으로 나갈 모양" 이 한 클래스가 됐다.
 * 이제 칸을 하나 추가하면 <b>그 값이 그대로 응답에 실린다.</b> 비밀번호 해시처럼
 * 나가면 안 되는 칸은 {@code @JsonIgnore} 로 막고 있는데, 이건 컴파일러가
 * 검사해주지 않는다 — 빠뜨려도 서버는 멀쩡히 뜨고 응답만 조용히 새어나간다.
 *
 * <p>그래서 여기서 실제로 JSON 을 만들어보고 칸 이름을 확인한다.
 * {@code application.yml} 과 같은 설정을 쓴다 — 이름 바꾸기를 걸지 않으므로
 * 자바 필드 이름(카멜)이 그대로 JSON 키가 된다.
 */
class ResponseShapeTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private JsonNode toJson(Object value) {
        return MAPPER.readTree(MAPPER.writeValueAsString(value));
    }

    @Test
    @DisplayName("UserVO 를 내보내도 비밀번호 해시와 잠금 상태는 안 나간다")
    void userVoHidesSecrets() {
        UserVO user = new UserVO();
        user.setId(7L);
        user.setEmail("minji@example.com");
        user.setNickname("민지");
        user.setPasswordHash("$2a$10$비밀번호해시");
        user.setUserLevel(1);
        user.setLoginFailCount(3);
        user.setIsLocked(1);
        user.setLockedAt(LocalDateTime.now());
        user.setUseYn("Y");
        user.setCreatedAt(LocalDateTime.of(2026, 9, 11, 21, 29, 42));
        user.setLatitude(new BigDecimal("37.5"));

        JsonNode json = toJson(user);

        // 나가면 안 되는 것
        assertThat(json.has("passwordHash")).as("비밀번호 해시").isFalse();
        assertThat(json.has("userLevel")).as("권한 등급 숫자").isFalse();
        assertThat(json.has("loginFailCount")).as("로그인 실패 횟수").isFalse();
        assertThat(json.has("isLocked")).as("계정 잠김").isFalse();
        assertThat(json.has("lockedAt")).isFalse();
        assertThat(json.has("useYn")).as("탈퇴 여부").isFalse();
        assertThat(json.has("lockedAccount")).as("isLockedAccount() 가 새어나감").isFalse();

        // 마이페이지 전용 칸은 여기 없다 (ProfileVO 가 따로 있다)
        assertThat(json.has("latitude")).as("로그인 응답에 좌표").isFalse();
        assertThat(json.has("longitude")).isFalse();
        assertThat(json.has("streakDays")).as("로그인 응답에 연속 기록일").isFalse();
        assertThat(json.has("cookCount")).as("로그인 응답에 요리 횟수").isFalse();

        // 화면이 읽는 칸 (옛 UserResponse 와 같아야 한다)
        assertThat(json.get("id").asInt()).isEqualTo(7);
        assertThat(json.get("email").asString()).isEqualTo("minji@example.com");
        assertThat(json.get("nickname").asString()).isEqualTo("민지");
        assertThat(json.get("isAdmin").asBoolean()).isTrue();
        assertThat(json.get("createdAt").asString()).isEqualTo("2026-09-11T21:29:42Z");
    }

    @Test
    @DisplayName("UserBanVO 는 createdAt 을 글자로 주고 user_id 는 감춘다")
    void banVoShape() {
        UserBanVO ban = new UserBanVO();
        ban.setId(3L);
        ban.setUserId(7L);
        ban.setIngredientName("오이");
        ban.setBanType("ALLERGY");
        ban.setUseYn("Y");
        ban.setCreatedAt(LocalDateTime.of(2026, 9, 11, 21, 29, 42));

        JsonNode json = toJson(ban);

        assertThat(json.has("userId")).isFalse();
        assertThat(json.has("useYn")).isFalse();
        assertThat(json.get("id").asInt()).isEqualTo(3);
        assertThat(json.get("ingredientName").asString()).isEqualTo("오이");
        assertThat(json.get("banType").asString()).isEqualTo("ALLERGY");
        assertThat(json.get("createdAt").asString()).isEqualTo("2026-09-11T21:29:42Z");
    }

    @Test
    @DisplayName("MealHistoryVO 의 recipeJson 은 글자가 아니라 JSON 으로 나간다")
    void historyVoKeepsJsonShape() {
        MealHistoryVO history = new MealHistoryVO();
        history.setId(12L);
        history.setUserId(7L);
        history.setFoodName("김치찌개");
        history.setRecipeJson("{\"servings\":2,\"steps\":[\"물을 끓인다\"]}");
        history.setIngredientsJson("[{\"name\":\"두부\"}]");
        history.setUseYn("Y");
        history.setSelectedAt(LocalDateTime.of(2026, 9, 11, 21, 29, 42));
        history.setArt("stew");

        JsonNode json = toJson(history);

        assertThat(json.has("userId")).isFalse();
        assertThat(json.has("useYn")).isFalse();

        // React 가 recipeJson.steps 를 읽는다. 따옴표 붙은 글자로 나가면 안 된다.
        assertThat(json.get("recipeJson").isObject()).as("recipeJson 이 객체인가").isTrue();
        assertThat(json.get("recipeJson").get("servings").asInt()).isEqualTo(2);
        assertThat(json.get("ingredientsJson").isArray()).isTrue();
        assertThat(json.get("selectedAt").asString()).isEqualTo("2026-09-11T21:29:42Z");
        assertThat(json.get("art").asString()).isEqualTo("stew");
    }
}
