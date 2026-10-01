package com.meokji.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * {@code resources/data/*.json} 다섯 개가 성한지 본다.
 *
 * <h2>왜 필요한가</h2>
 *
 * <p><b>이 파일들이 원본이다.</b> 만들어 주는 프로그램이 따로 없어 손으로 고치게
 * 되는데, 116개 · 107개짜리 표를 손으로 고치면 한두 개는 틀리고 <b>틀려도 티가 안 난다</b> — 좌표가 어긋나면 다른 동네
 * 날씨가 나오고, 분량이 어긋나면 가격이 조용히 달라진다. 서버는 멀쩡히 뜬다.
 *
 * <p>실제로 그런 일이 한 번 있었다. 데모 데이터 시드가 확정하는 메뉴 중
 * {@code 크림 파스타} 가 STEP 41 에서 레시피를 다시 정의할 때 빠졌는데,
 * <b>기존 DB 에 옛 행이 남아 있어 "이미 있음" 으로 건너뛰는 바람에</b> 빈 DB 에
 * 돌리기 전까지 아무도 몰랐다. 아래 {@link #catalogAndRecipesMatch()} 가 그
 * 종류를 잡는 자리다.
 *
 * <h2>숫자를 왜 박아두나</h2>
 *
 * <p>"비어 있지 않다" 만 보면 절반이 날아가도 통과한다. 지금 개수를 적어두면
 * 일부러 늘리고 줄일 때만 이 파일을 같이 고치게 된다 — 실수로 줄어든 것과
 * 일부러 줄인 것이 구분된다.
 */
@DisplayName("데이터 파일")
class DataFileTest {

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private static JsonNode read(String name) {
        try (InputStream in = DataFileTest.class.getClassLoader()
                .getResourceAsStream("data/" + name)) {
            assertThat(in).as("data/%s 가 없다", name).isNotNull();
            return MAPPER.readTree(in);
        } catch (Exception exc) {
            throw new IllegalStateException("data/" + name + " 을 읽지 못했습니다.", exc);
        }
    }

    private static Set<String> foodNames(JsonNode catalog) {
        Set<String> names = new LinkedHashSet<>();
        for (JsonNode food : catalog.path("foods")) {
            names.add(food.path("food_name").asString(""));
        }
        return names;
    }

    // ------------------------------------------------------------ catalog.json

    @Test
    @DisplayName("catalog.json — 음식 100종과 화면용 목록이 다 있다")
    void catalogIsComplete() {
        JsonNode catalog = read("catalog.json");

        assertThat(catalog.path("foods")).hasSize(100);

        // 화면이 각각 다른 자리에서 읽는다. 하나가 비면 그 화면만 조용히 빈다.
        assertThat(catalog.path("categories")).as("분류 칩").hasSize(11);
        assertThat(catalog.path("roulette")).as("룰렛 칸").hasSize(8);
        assertThat(catalog.path("games")).as("미니게임 카드").hasSize(3);
        assertThat(catalog.path("balance_questions")).as("밸런스 게임").hasSize(5);
        assertThat(catalog.path("search_suggestions")).as("검색 추천어").hasSize(8);
        assertThat(catalog.path("budget_presets")).as("예산 버튼").hasSize(4);

        // 더운 날 고르는 메뉴. 태그만 봐서는 냉면과 칼국수가 같은 점수를 받는다
        // (docs/PROGRESS.md 의 "추천 점수가 가장 조용히 틀릴 수 있는 부분").
        assertThat(catalog.path("hot_weather_picks")).as("더운 날 메뉴").hasSize(18);
    }

    @Test
    @DisplayName("catalog.json — 음식마다 화면이 읽는 칸이 채워져 있다")
    void everyFoodHasRequiredFields() {
        JsonNode catalog = read("catalog.json");

        List<String> broken = new ArrayList<>();

        for (JsonNode food : catalog.path("foods")) {
            String name = food.path("food_name").asString("");

            if (name.isBlank()) {
                broken.add("이름이 빈 항목이 있다");
                continue;
            }
            if (food.path("category").asString("").isBlank()) {
                broken.add(name + ": category 없음");
            }
            // 예산 필터와 추천 정렬(가격 오름차순)이 이 값을 쓴다.
            if (food.path("estimated_cost").asInt(0) <= 0) {
                broken.add(name + ": estimated_cost 가 0 이하");
            }
            // 사진이 없는 동안 쓰는 SVG 종류. 없으면 카드가 빈 칸으로 뜬다.
            if (food.path("art").asString("").isBlank()) {
                broken.add(name + ": art 없음");
            }
            if (!food.path("tags").isArray() || food.path("tags").isEmpty()) {
                broken.add(name + ": tags 가 비었다 (날씨 점수가 0 이 된다)");
            }
            if (!food.path("ingredients").isArray() || food.path("ingredients").isEmpty()) {
                broken.add(name + ": ingredients 가 비었다 (알레르기 제외가 안 먹는다)");
            }
        }

        assertThat(broken).as("채워지지 않은 칸").isEmpty();
    }

    @Test
    @DisplayName("catalog.json — 이름이 겹치지 않는다")
    void foodNamesAreUnique() {
        JsonNode catalog = read("catalog.json");

        // 이름이 키 노릇을 한다. 겹치면 레시피 조회가 어느 쪽을 집을지 알 수 없다.
        assertThat(foodNames(catalog))
                .as("음식 이름은 유일해야 한다")
                .hasSize(catalog.path("foods").size());
    }

    @Test
    @DisplayName("catalog.json — 더운 날 메뉴가 전부 카탈로그에 있다")
    void hotWeatherPicksExist() {
        JsonNode catalog = read("catalog.json");
        Set<String> names = foodNames(catalog);

        List<String> missing = new ArrayList<>();
        for (JsonNode pick : catalog.path("hot_weather_picks")) {
            String name = pick.asString("");
            if (!names.contains(name)) {
                missing.add(name);
            }
        }

        // 여기 적힌 이름은 점수를 더 받는데, 카탈로그에 없으면 그냥 무시된다.
        // 오타가 나도 에러가 안 나고 "더운 날인데 국물이 나오는" 증상만 남는다.
        assertThat(missing).as("카탈로그에 없는 더운 날 메뉴").isEmpty();
    }

    // ------------------------------------------------------------ recipes.json

    @Test
    @DisplayName("recipes.json — 레시피 100종에 만드는 법이 들어 있다")
    void recipesAreComplete() {
        JsonNode recipes = read("recipes.json").path("recipes");

        assertThat(recipes).hasSize(100);

        List<String> broken = new ArrayList<>();

        recipes.properties().forEach(entry -> {
            String name = entry.getKey();
            JsonNode recipe = entry.getValue();

            if (recipe.path("recipe_name").asString("").isBlank()) {
                broken.add(name + ": recipe_name 없음");
            }
            if (!recipe.path("steps").isArray() || recipe.path("steps").isEmpty()) {
                broken.add(name + ": steps 가 비었다");
            }
            if (!recipe.path("ingredients").isArray() || recipe.path("ingredients").isEmpty()) {
                broken.add(name + ": ingredients 가 비었다");
            }
            // 2인분 기준이다. 분량 환산(portions)이 이 값을 전제로 돈다.
            if (recipe.path("servings").asInt(0) <= 0) {
                broken.add(name + ": servings 가 0 이하");
            }
        });

        assertThat(broken).as("망가진 레시피").isEmpty();
    }

    @Test
    @DisplayName("catalog.json 과 recipes.json 의 메뉴가 정확히 같다")
    void catalogAndRecipesMatch() {
        Set<String> catalog = foodNames(read("catalog.json"));

        Set<String> recipes = new LinkedHashSet<>();
        read("recipes.json").path("recipes").properties()
                .forEach(entry -> recipes.add(entry.getKey()));

        // 추천은 카탈로그에서 고르고, 고른 메뉴의 레시피를 recipes.json 에서 찾는다.
        // 한쪽에만 있으면 "추천은 되는데 눌러보면 404" 가 된다.
        assertThat(catalog).as("레시피가 없는 추천 메뉴").isSubsetOf(recipes);

        // 반대쪽은 화면에 닿을 길이 없는 레시피다. 지금은 1:1 이라 같이 본다.
        assertThat(recipes).as("추천에 안 나오는 레시피").isSubsetOf(catalog);
    }

    // ------------------------------------------------------------ regions.json

    @Test
    @DisplayName("regions.json — 시·도 17개와 좌표가 한국 안에 있다")
    void regionsAreSane() {
        JsonNode regions = read("regions.json");

        assertThat(regions).as("시·도").hasSize(17);

        int sigunguCount = 0;
        List<String> broken = new ArrayList<>();

        for (JsonNode sido : regions) {
            String name = sido.path("name").asString("");

            if (name.isBlank()) {
                broken.add("이름 없는 시·도가 있다");
            }

            for (JsonNode sigungu : sido.path("sigungu")) {
                sigunguCount++;

                double lat = sigungu.path("lat").asDouble(0);
                double lon = sigungu.path("lon").asDouble(0);

                // 위도 · 경도가 뒤바뀌거나 부호가 빠지면 "다른 동네 날씨" 가 나온다.
                // 값이 없는 것도 아니고 에러도 안 나서 찾기 어렵다.
                if (lat < 33 || lat > 39) {
                    broken.add(name + " " + sigungu.path("name").asString() + ": 위도 " + lat);
                }
                if (lon < 124 || lon > 132) {
                    broken.add(name + " " + sigungu.path("name").asString() + ": 경도 " + lon);
                }
            }
        }

        assertThat(broken).as("한국 밖 좌표").isEmpty();
        assertThat(sigunguCount).as("시·군·구").isEqualTo(228);
    }

    // --------------------------------------------------- ingredient-rules.json

    @Test
    @DisplayName("ingredient-rules.json — 분량 환산 표가 다 있다")
    void ingredientRulesAreComplete() {
        JsonNode rules = read("ingredient-rules.json");

        assertThat(rules.path("count_grams")).as("세는 단위 무게").hasSize(52);
        assertThat(rules.path("grams_per_unit")).as("단위별 무게").hasSize(10);
        assertThat(rules.path("count_units")).as("세는 단위 이름").hasSize(7);
        assertThat(rules.path("substitutes")).as("대체재료").hasSize(12);
        assertThat(rules.path("portions")).as("2인분 분량").hasSize(107);

        assertThat(rules.path("default_grams_per_unit").asInt(0))
                .as("단위를 모를 때 쓸 무게").isPositive();

        // 이 파일이 원본이고 누가 읽는지 적어 둔 표시. 이 줄이 있어야 손으로 고칠 때 알아본다.
        assertThat(rules.path("_note").asString(""))
                .as("출처 표시").isNotBlank();

        // 무게가 0 이면 가격이 0 원으로 조용히 계산된다.
        List<String> broken = new ArrayList<>();
        rules.path("count_grams").properties().forEach(entry -> {
            if (entry.getValue().asDouble(0) <= 0) {
                broken.add(entry.getKey() + " = " + entry.getValue());
            }
        });
        assertThat(broken).as("무게가 0 이하인 재료").isEmpty();
    }

    // ------------------------------------------------------- kamis-items.json

    @Test
    @DisplayName("kamis-items.json — 품목코드 116개가 모양을 갖췄다")
    void kamisItemsAreComplete() {
        JsonNode items = read("kamis-items.json");

        assertThat(items.path("item_codes")).as("품목코드").hasSize(116);
        assertThat(items.path("modifiers")).as("손질 표현").hasSize(34);
        assertThat(items.path("aliases")).as("별칭").hasSize(26);
        assertThat(items.path("processed")).as("가공식품").hasSize(27);
        assertThat(items.path("not_priced")).as("조사 대상 아님").hasSize(11);

        assertThat(items.path("_note").asString("")).as("출처 표시").isNotBlank();

        List<String> broken = new ArrayList<>();

        items.path("item_codes").properties().forEach(entry -> {
            JsonNode codes = entry.getValue();

            // group · item 이 비면 KAMIS 요청이 그 품목만 조용히 빈 값으로 온다.
            if (codes.path("group").asString("").isBlank()) {
                broken.add(entry.getKey() + ": group 없음");
            }
            if (codes.path("item").asString("").isBlank()) {
                broken.add(entry.getKey() + ": item 없음");
            }
        });

        assertThat(broken).as("코드가 빈 품목").isEmpty();

        // 별칭이 가리키는 이름은 품목표에 있어야 한다. 없으면 그 별칭은
        // 아무 일도 하지 않는다 — "다진마늘" 이 시세를 못 찾는 식이다.
        List<String> danglingAliases = new ArrayList<>();
        items.path("aliases").properties().forEach(entry -> {
            String target = entry.getValue().asString("");
            if (!items.path("item_codes").has(target)) {
                danglingAliases.add(entry.getKey() + " -> " + target);
            }
        });

        assertThat(danglingAliases).as("없는 품목을 가리키는 별칭").isEmpty();
    }
}
