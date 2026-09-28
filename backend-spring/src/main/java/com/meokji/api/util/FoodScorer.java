package com.meokji.api.util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.meokji.api.dto.recommend.FoodItem;
import com.meokji.api.dto.weather.WeatherResponse;

import tools.jackson.databind.JsonNode;

/**
 * 카탈로그 100종에 점수를 매겨 고른다.
 *
 * <p>파이썬 쪽 {@code app/integrations/ai_api.py} 의 {@code catalog_recommend_foods}
 * 와 점수 함수들을 옮긴 것이다. 점수 값(8 · 6 · 4 · -3 …)은 그쪽에서 실제 결과를
 * 보며 맞춰둔 것이라 <b>바꾸지 않았다.</b> 바꾸면 같은 조건에 다른 메뉴가 나온다.
 */
@Component
public class FoodScorer {

    /** 한 번에 만들 후보 개수의 상한. 많이 받아봐야 화면에서 다 안 본다. */
    private static final int MAX_FOODS = 12;

    /** 싫어하는 재료가 들어갔을 때 깎는 점수. 빼지는 않는다 — 대체재료를 낼 수 있다. */
    private static final int DISLIKE_PENALTY = 4;

    @Autowired
    private FoodCatalog catalog;


    /** 점수를 매긴 한 줄. 정렬에 쓴다. */
    private record Scored(int score, int cost, String name, JsonNode food) {
    }

    /**
     * 조건에 맞는 후보를 골라준다.
     *
     * @param allergies 알레르기 재료. 들어간 메뉴는 <b>통째로 뺀다</b>
     * @param dislikes  싫어하는 재료. 순위만 뒤로 미룬다
     * @param exclude   이미 보여준 메뉴
     */
    public List<FoodItem> recommend(
            int limit,
            String category,
            Integer budgetMin,
            Integer budgetMax,
            String query,
            WeatherResponse weather,
            List<String> allergies,
            List<String> dislikes,
            List<String> exclude) {

        List<Scored> scored = new ArrayList<>();

        for (JsonNode food : catalog.foods()) {
            String name = food.path("food_name").asString();

            if (exclude.contains(name)) {
                continue;
            }

            // 알레르기 재료가 들어간 메뉴는 후보에서 통째로 뺀다.
            if (usesAny(food, allergies)) {
                continue;
            }

            if (category != null && !category.isBlank()
                    && !category.equals(food.path("category").asString())) {
                continue;
            }

            int cost = food.path("estimated_cost").asInt();

            if (budgetMin != null && cost < budgetMin) {
                continue;
            }
            if (budgetMax != null && cost > budgetMax) {
                continue;
            }

            int score = scoreFood(food, query, weather);

            if (usesAny(food, dislikes)) {
                score -= DISLIKE_PENALTY;
            }

            scored.add(new Scored(score, cost, name, food));
        }

        // 점수가 높은 순 -> 싼 순 -> 이름순.
        // 마지막 이름순이 없으면 점수가 같을 때 순서가 들쭉날쭉해 보인다.
        scored.sort(Comparator
                .comparingInt(Scored::score).reversed()
                .thenComparingInt(Scored::cost)
                .thenComparing(Scored::name));

        List<FoodItem> results = new ArrayList<>();
        int take = Math.min(Math.max(limit, 1), MAX_FOODS);

        for (Scored row : scored.subList(0, Math.min(take, scored.size()))) {
            results.add(toFoodItem(row.food()));
        }

        return results;
    }

    /** 이 음식이 주어진 재료 중 하나라도 쓰는지. 음식 이름도 함께 본다. */
    public static boolean usesAny(JsonNode food, List<String> names) {
        if (names == null || names.isEmpty()) {
            return false;
        }

        List<String> haystack = new ArrayList<>();
        for (JsonNode ingredient : food.path("ingredients")) {
            haystack.add(ingredient.asString());
        }
        // 재료 목록에 없어도 이름으로 걸리는 것이 있다 ("돼지고기 김치찌개").
        haystack.add(food.path("food_name").asString());

        for (String banned : names) {
            String needle = banned == null ? "" : banned.trim();
            if (needle.isEmpty()) {
                continue;
            }

            for (String item : haystack) {
                // 양방향으로 본다. "고기" 로 등록한 사람에게 "돼지고기" 도 걸리고,
                // "돼지고기" 로 등록한 사람에게 "고기" 도 걸려야 한다.
                if (item.contains(needle) || needle.contains(item)) {
                    return true;
                }
            }
        }

        return false;
    }

    /** 조건에 얼마나 맞는지. 높을수록 앞에 온다. */
    int scoreFood(JsonNode food, String query, WeatherResponse weather) {
        int score = 0;

        boolean hasQuery = query != null && !query.isBlank();

        if (hasQuery) {
            score += scoreQuery(food, query);
        }

        // 검색어가 있으면 날씨는 보지 않는다. 사용자가 직접 적은 조건이
        // 날씨보다 강하다 — 더운 날 "얼큰한 국물" 을 찾는 사람도 있다.
        if (weather != null && !hasQuery) {
            score += scoreWeather(food, weather);
        }

        return score;
    }

    /** 검색 문장이 이 음식과 얼마나 맞는지. */
    static int scoreQuery(JsonNode food, String query) {
        int score = 0;

        for (String word : splitWords(query)) {
            if (food.path("food_name").asString().contains(word)) {
                score += 10;
            } else if (food.path("category").asString().contains(word)) {
                score += 6;
            } else if (wordInArray(word, food.path("tags"))) {
                score += 6;
            } else if (wordInArray(word, food.path("ingredients"))) {
                score += 4;
            } else if (food.path("reason").asString().contains(word)) {
                score += 2;
            }
        }

        return score;
    }

    /** 검색 문장을 낱말로 쪼갠다. 한 글자짜리는 아무 데나 걸려서 뺀다. */
    static List<String> splitWords(String query) {
        List<String> words = new ArrayList<>();

        for (String raw : query.replace(",", " ").split("\\s+")) {
            String word = trimMarks(raw);

            if (word.length() > 1) {
                words.add(word);
            }
        }

        return words;
    }

    /** 앞뒤의 물음표 · 느낌표 · 마침표 · 가운뎃점을 뗀다. */
    private static String trimMarks(String raw) {
        String marks = "?!.·";
        int start = 0;
        int end = raw.length();

        while (start < end && marks.indexOf(raw.charAt(start)) >= 0) {
            start++;
        }
        while (end > start && marks.indexOf(raw.charAt(end - 1)) >= 0) {
            end--;
        }

        return raw.substring(start, end);
    }

    private static boolean wordInArray(String word, JsonNode array) {
        for (JsonNode item : array) {
            String value = item.asString();

            if (value.contains(word) || word.contains(value)) {
                return true;
            }
        }

        return false;
    }

    /** 차게 먹는 음식인지. */
    boolean isColdDish(JsonNode food) {
        // 이름으로 적어둔 목록을 먼저 본다. 냉면처럼 태그로는 안 잡히는 것이 있다.
        return catalog.isHotWeatherPick(food.path("food_name").asString())
                || hasTag(food, "시원한음식")
                || hasTag(food, "여름메뉴");
    }

    private static boolean hasTag(JsonNode food, String tag) {
        for (JsonNode item : food.path("tags")) {
            if (tag.equals(item.asString())) {
                return true;
            }
        }

        return false;
    }

    /** 날씨에 어울리면 가산점. 기온과 하늘을 따로 재서 더한다. */
    int scoreWeather(JsonNode food, WeatherResponse weather) {
        return scoreTemperature(food, weather.temp()) + scoreSky(food, weather.condition());
    }

    int scoreTemperature(JsonNode food, Integer temp) {
        if (temp == null) {
            return 0;
        }

        if (temp >= 25) {
            // 이름으로 적어둔 것이 가장 강하다. 더운 날엔 냉면 · 콩국수가 먼저다.
            if (catalog.isHotWeatherPick(food.path("food_name").asString())) {
                return 8;
            }
            if (hasTag(food, "시원한음식") || hasTag(food, "면")) {
                return 4;
            }
            if (hasTag(food, "국물")) {
                return -3;  // 더운 날 뜨끈한 국물은 뒤로
            }
            return 0;
        }

        if (temp < 15) {
            if (hasTag(food, "국물") || hasTag(food, "얼큰")) {
                return 6;
            }
            if (isColdDish(food)) {
                return -6;
            }
            return 0;
        }

        // 15 ~ 24도. 덥지도 춥지도 않은 구간이다.
        //
        // 점수를 작게 주는 이유가 있다. 이 구간에는 "반드시 이것" 이 없다. 더운 날의
        // 냉면이나 추운 날의 국물 같은 강한 선호가 생기지 않는다. 크게 주면 Ban 같은
        // 다른 조건을 덮어쓴다 (STEP 35).
        if (hasTag(food, "선선한")) {
            return 3;
        }

        // 찬 음식은 조금 뒤로. 20도에 냉면이 1등으로 오르는 것은 이르다.
        if (isColdDish(food)) {
            return -2;
        }

        return 0;
    }

    int scoreSky(JsonNode food, String condition) {
        if ("RAIN".equals(condition)) {
            if (hasTag(food, "국물")) {
                return 6;
            }
            if (hasTag(food, "튀김")) {
                return 4;
            }
            if (isColdDish(food)) {
                return -5;
            }
            return 0;
        }

        if ("SNOW".equals(condition)) {
            if (hasTag(food, "국물")) {
                return 7;
            }
            if (isColdDish(food)) {
                return -8;
            }
            return 0;
        }

        return 0;
    }

    /** 카탈로그의 음식 하나를 화면용으로. */
    static FoodItem toFoodItem(JsonNode food) {
        List<String> tags = new ArrayList<>();
        for (JsonNode tag : food.path("tags")) {
            tags.add(tag.asString());
        }

        JsonNode imageUrl = food.get("image_url");

        return new FoodItem(
                food.path("food_name").asString(),
                food.path("category").asString(),
                food.path("reason").asString(),
                food.path("cook_time_minutes").asInt(),
                food.path("difficulty").asString(),
                food.path("estimated_cost").asInt(),
                tags,
                food.path("art").asString(null),
                imageUrl == null || imageUrl.isNull() ? null : imageUrl.asString());
    }
}
