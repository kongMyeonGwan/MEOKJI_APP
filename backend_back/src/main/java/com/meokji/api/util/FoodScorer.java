package com.meokji.api.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.meokji.api.recommend.vo.FoodVO;
import com.meokji.api.recommend.vo.WeatherVO;

import tools.jackson.databind.JsonNode;

/**
 * 카탈로그 100종에 점수를 매겨 고른다.
 *
 * <p>왜 점수인가
 *
 * <p>조건이 여럿이고 서로 어긋난다. "한식" 이면서 "1만원 이하" 면서 "더운 날에
 * 어울리는" 것을 전부 만족하는 메뉴가 없을 수도 있다. if 로 걸러내기만 하면
 * 그때 결과가 0건이 된다. 점수로 매기면 "가장 가까운 것" 을 고를 수 있다.
 *
 * <p>걸러내기와 점수 깎기를 나눈다
 *
 * <p>알레르기 재료가 들어간 메뉴는 아예 뺀다 - 먹으면 안 되는 것이라
 * 순위를 미루는 정도로는 안 된다. 분류 · 예산도 걸러낸다 - 사용자가
 * 분명히 적은 조건이다.
 *
 * <p>싫어하는 재료는 점수만 깎는다. 대체재료를 낼 수 있어서, 아예 안
 * 보여주기보다 "오이 대신 애호박" 을 제안하는 쪽이 낫다.
 *
 * <p>점수를 몇 점 주는지는 전부 눈대중으로 정한 값이다. 다만 서로의 크기는
 * 뜻이 있다 — 이름이 맞으면(10) 분류가 맞는 것(6)보다 강하고, 더운 날의
 * 냉면(8)이 국물 태그 하나(4)보다 강하다.
 *
 * <p>쓰는 곳이 둘이다 — 추천(RecommendServiceImpl)과 찜 목록(LikeServiceImpl).
 * 찜 목록은 점수를 안 쓰고 toFoodVO 만 가져다 쓴다.
 */
@Component
public class FoodScorer {

    /** 한 번에 만들 후보 개수의 상한. 많이 받아봐야 화면에서 다 안 본다. */
    // 요청의 @Max(12) 와 같은 값이다. 양쪽에 둔 이유 — 요청 쪽은 422 로
    // 막아 알려주고, 이쪽은 서비스를 직접 부르는 자리(테스트 등)에서도
    // 100개를 뽑으려 들지 않게 한다
    private static final int MAX_FOODS = 12;

    /** 싫어하는 재료가 들어갔을 때 깎는 점수. 빼지는 않는다 — 대체재료를 낼 수 있다. */
    // 4점은 "분류가 맞는 것(6점)보다 약하고, 태그 하나(6점)와 비슷한" 정도다.
    // 크게 깎으면 싫어하는 재료가 든 메뉴가 아예 안 나오고, 작게 깎으면
    // 순위가 안 밀린다
    private static final int DISLIKE_PENALTY = 4;

    // 음식 100종과 "더운 날 밀어줄 이름" 목록을 들고 있다
    @Autowired
    private FoodCatalog catalog;

    /** 점수를 매긴 한 줄. 정렬에 쓴다. */
    // 임시로 쓰는 모양이라 private static class 로 안쪽에 두었다.
    // food(원본 JsonNode)를 같이 들고 있어서, 정렬이 끝난 뒤에
    // 그것으로 FoodVO 를 만든다 - 미리 만들면 걸러질 후보까지 다 만들게 된다
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Scored {

        private int score;

        private int cost;

        private String name;

        private JsonNode food;
    }

    /**
     * 조건에 맞는 후보를 골라준다.
     *
     * @param allergies 알레르기 재료. 들어간 메뉴는 통째로 뺀다
     * @param dislikes  싫어하는 재료. 순위만 뒤로 미룬다
     * @param exclude   이미 보여준 메뉴
     */
    // 인자가 아홉 개라 길다. VO 하나로 묶으면 recommend 기능의 요청 VO 를
    // 알아야 하는데, 찜 목록도 이 클래스를 쓰므로 그쪽에 묶이지 않게 값으로 받는다
    public List<FoodVO> recommend(
            int limit,
            String category,
            Integer budgetMin,
            Integer budgetMax,
            String query,
            WeatherVO weather,
            List<String> allergies,
            List<String> dislikes,
            List<String> exclude) {

        List<Scored> scored = new ArrayList<>();

        // 100종을 하나씩 본다. 100개라 전부 훑어도 빠르다 -
        // 미리 걸러낼 색인 같은 것을 만들 이유가 없다
        for (JsonNode food : catalog.foods()) {
            String name = food.path("food_name").asString();

            // "다른 메뉴 추천" 을 눌렀을 때 이미 본 것을 뺀다
            if (exclude.contains(name)) {
                continue;
            }

            // 알레르기 재료가 들어간 메뉴는 후보에서 통째로 뺀다.
            //
            // 점수를 깎는 것으로는 안 된다. 후보가 적은 날에는 깎여도
            // 1등으로 올라올 수 있다
            if (usesAny(food, allergies)) {
                continue;
            }

            // 분류가 안 맞으면 뺀다. 사용자가 "한식" 을 골랐는데
            // 점수가 높다고 중식이 나오면 조건을 무시한 셈이다
            if (category != null && !category.isBlank()
                    && !category.equals(food.path("category").asString())) {
                continue;
            }

            int cost = food.path("estimated_cost").asInt();

            // 예산도 걸러낸다. 넘는 메뉴를 보여줄 수는 없다 -
            // 대신 결과가 0건이면 서비스가 예산을 3천원 넓혀 한 번 더 부른다
            if (budgetMin != null && cost < budgetMin) {
                continue;
            }
            if (budgetMax != null && cost > budgetMax) {
                continue;
            }

            int score = scoreFood(food, query, weather);

            // 싫어하는 재료는 빼지 않고 점수만 깎는다 (클래스 주석 참고)
            if (usesAny(food, dislikes)) {
                score -= DISLIKE_PENALTY;
            }

            scored.add(new Scored(score, cost, name, food));
        }

        // 점수가 높은 순 -> 싼 순 -> 이름순.
        // 마지막 이름순이 없으면 점수가 같을 때 순서가 들쭉날쭉해 보인다.
        //
        // 조건을 아무것도 안 걸면 점수가 전부 0 이다. 그때는 싼 순이
        // 사실상의 기준이 된다 - 아무 메뉴나 섞어 보여주는 것보다 낫다
        scored.sort(Comparator
                .comparingInt(Scored::getScore).reversed()
                .thenComparingInt(Scored::getCost)
                .thenComparing(Scored::getName));

        List<FoodVO> results = new ArrayList<>();
        // limit 를 1 이상 12 이하로 가둔다. 0 이나 음수가 와도 subList 가
        // 터지지 않고, 100 을 달라고 해도 12개까지만 준다
        int take = Math.min(Math.max(limit, 1), MAX_FOODS);

        // Math.min(take, scored.size()) 로 한 번 더 가린다. 조건이
        // 까다로우면 후보가 take 보다 적을 수 있고, 그때 subList(0, take) 는
        // 터진다
        for (Scored row : scored.subList(0, Math.min(take, scored.size()))) {
            // 여기서야 FoodVO 를 만든다. 걸러진 후보까지 미리 만들면
            // 쓰지도 않을 객체를 90개 만드는 셈이다
            results.add(toFoodVO(row.getFood()));
        }

        return results;
    }

    /** 이 음식이 주어진 재료 중 하나라도 쓰는지. 음식 이름도 함께 본다. */
    // static 인 이유 — 들고 있는 catalog 를 쓰지 않는다. 받은 두 값만 본다
    public static boolean usesAny(JsonNode food, List<String> names) {
        // 등록한 재료가 없으면(비로그인 등) 볼 것이 없다.
        // 아래 for 를 돌아도 결과는 같지만, 100종마다 빈 목록을 훑을 이유가 없다
        if (names == null || names.isEmpty()) {
            return false;
        }

        // 견줄 대상을 한 목록으로 모은다
        List<String> haystack = new ArrayList<>();
        for (JsonNode ingredient : food.path("ingredients")) {
            haystack.add(ingredient.asString());
        }
        // 재료 목록에 없어도 이름으로 걸리는 것이 있다 ("돼지고기 김치찌개").
        haystack.add(food.path("food_name").asString());

        for (String banned : names) {
            String needle = banned == null ? "" : banned.trim();
            // 빈 글자를 빼는 것이 중요하다. ""는 모든 글자에 들어 있어서,
            // 안 빼면 100종이 전부 걸려 추천이 0건이 된다
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
        // IngredientRules.matchesAny 와 하는 일이 거의 같다. 다른 점은
        // 이쪽이 음식 하나(재료 목록 + 이름)를 받고, 저쪽은 이름 하나를
        // 받는다는 것이다
    }

    /** 조건에 얼마나 맞는지. 높을수록 앞에 온다. */
    // package-private 이다. 같은 패키지의 테스트가 점수를 직접 확인할 수
    // 있게 열어둔 것이고, 바깥 기능이 쓸 일은 없다
    int scoreFood(JsonNode food, String query, WeatherVO weather) {
        int score = 0;

        boolean hasQuery = query != null && !query.isBlank();

        if (hasQuery) {
            score += scoreQuery(food, query);
        }

        // 검색어가 있으면 날씨는 보지 않는다. 사용자가 직접 적은 조건이
        // 날씨보다 강하다 — 더운 날 "얼큰한 국물" 을 찾는 사람도 있다.
        //
        // 더하지 않고 아예 안 보는 이유 — 날씨 점수가 음수로 깎는 쪽도
        // 있어서(더운 날 국물 -3), 둘을 섞으면 사용자가 찾은 것이 뒤로 밀린다
        if (weather != null && !hasQuery) {
            score += scoreWeather(food, weather);
        }

        return score;
    }

    /** 검색 문장이 이 음식과 얼마나 맞는지. */
    static int scoreQuery(JsonNode food, String query) {
        int score = 0;

        // 낱말마다 점수를 더한다. "매콤한 한식" 이면 둘 다 맞는 메뉴가
        // 하나만 맞는 메뉴보다 앞에 온다
        for (String word : splitWords(query)) {
            // else if 로 이어 쓴 이유 — 한 낱말에 한 번만 점수를 준다.
            // 이름에도 걸리고 태그에도 걸리면 10 + 6 = 16 이 되어,
            // 두 낱말이 각각 맞은 것(10 + 6)과 구별되지 않는다
            //
            // 점수가 10 · 6 · 6 · 4 · 2 인 순서는 "얼마나 그 음식을
            // 가리키는지" 다. 이름이 맞으면 거의 확실하고, 이유 글귀에
            // 우연히 들어간 낱말은 약하다
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
    // 쉼표를 공백으로 바꾸는 이유 — "한식, 매콤" 처럼 적는 사람이 있다.
    // 안 바꾸면 "한식," 이 한 낱말이 되어 아무것도 못 찾는다
    static List<String> splitWords(String query) {
        List<String> words = new ArrayList<>();

        for (String raw : query.replace(",", " ").split("\\s+")) {
            String word = trimMarks(raw);

            // 한 글자를 빼는 이유 — "국" 은 미역국 · 떡국 · 중국 에
            // 전부 걸린다. 점수가 의미를 잃는다
            if (word.length() > 1) {
                words.add(word);
            }
        }

        return words;
    }

    /** 앞뒤의 물음표 · 느낌표 · 마침표 · 가운뎃점을 뗀다. */
    //
    // 왜 필요한가 — 검색창에 "뭐 먹을까?" 처럼 적는 사람이 있다.
    // "먹을까?" 를 그대로 두면 어디에도 안 걸린다.
    //
    // 가운뎃점(·)을 넣은 이유 — 화면의 칩 글자에 쓰이는 기호라
    // 그대로 복사해 넣는 경우가 있다
    private static String trimMarks(String raw) {
        String marks = "?!.·";
        int start = 0;
        int end = raw.length();

        // 앞에서부터 기호인 동안 건너뛴다
        while (start < end && marks.indexOf(raw.charAt(start)) >= 0) {
            start++;
        }
        // 뒤에서부터 기호인 동안 당긴다. end > start 로 가려야
        // 기호만 있는 낱말("???")에서 넘어가지 않는다
        while (end > start && marks.indexOf(raw.charAt(end - 1)) >= 0) {
            end--;
        }

        return raw.substring(start, end);
    }

    // JSON 배열 안에 그 낱말과 겹치는 값이 있는지.
    //
    // 여기도 양방향으로 본다. "고기" 로 찾으면 "돼지고기" 태그가 걸리고,
    // "돼지고기" 로 찾으면 "고기" 태그가 걸려야 한다
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
        //
        // 냉면은 '면' 태그가 붙어 있지만 칼국수도 '면' 이다. 태그만 보면
        // 둘이 같은 점수를 받는다 - 그래서 이름을 직접 적어둔 목록이 있다
        return catalog.isHotWeatherPick(food.path("food_name").asString())
                || hasTag(food, "시원한음식")
                || hasTag(food, "여름메뉴");
    }

    // 태그 하나가 붙어 있는지. equals 로 정확히 견준다 -
    // 위 wordInArray 와 달리 태그 이름은 우리가 정한 값이라 겹칠 일이 없다
    private static boolean hasTag(JsonNode food, String tag) {
        for (JsonNode item : food.path("tags")) {
            if (tag.equals(item.asString())) {
                return true;
            }
        }

        return false;
    }

    /** 날씨에 어울리면 가산점. 기온과 하늘을 따로 재서 더한다. */
    //
    // 따로 재는 이유 — 더운 날에 비가 오는 경우가 있다. 한 덩어리로 보면
    // 그 조합마다 규칙을 적어야 하지만(날씨 4 x 기온대 3 = 12가지),
    // 따로 재서 더하면 각자 세 줄씩이면 된다
    int scoreWeather(JsonNode food, WeatherVO weather) {
        return scoreTemperature(food, weather.getTemp()) + scoreSky(food, weather.getCondition());
    }

    // 기온으로 재는 점수. 25도 이상 · 15도 미만 · 그 사이 세 구간이다.
    //
    // 구간 기준(15 · 25)은 WeatherServiceImpl.tempFeel 과 맞춰 두었다.
    // 어긋나면 화면에는 "선선한 날씨" 라고 쓰여 있는데 추천은 국물 쪽으로
    // 쏠리는 일이 생긴다
    int scoreTemperature(JsonNode food, Integer temp) {
        // 날씨를 못 가져온 경우. 0 을 주면 날씨를 안 본 것과 같아진다
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
                // 빼지 않고 깎기만 하는 이유 — 더운 날에도 국밥을 먹는
                // 사람이 있다. 1등으로 오르지만 않으면 된다
            }
            return 0;
        }

        if (temp < 15) {
            if (hasTag(food, "국물") || hasTag(food, "얼큰")) {
                return 6;
            }
            if (isColdDish(food)) {
                return -6;
                // 추운 날의 냉면은 더운 날의 국물(-3)보다 더 어긋난다.
                // 그래서 더 크게 깎는다
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

    // 하늘로 재는 점수. 비 · 눈만 본다.
    //
    // 맑음 · 흐림에 점수를 안 주는 이유 — 맑은 날에 어울리는 음식이
    // 따로 있지 않다. 억지로 주면 기온 점수를 흐린다
    int scoreSky(JsonNode food, String condition) {
        if ("RAIN".equals(condition)) {
            if (hasTag(food, "국물")) {
                return 6;
            }
            if (hasTag(food, "튀김")) {
                return 4;
                // 비 오는 날의 부침개 · 튀김. 국물보다는 약하게 둔다
            }
            if (isColdDish(food)) {
                return -5;
            }
            return 0;
        }

        if ("SNOW".equals(condition)) {
            // 눈이 오면 비보다 더 강하게 민다(7 vs 6). 눈 오는 날은
            // 춥기까지 해서, 기온 점수와 같은 방향으로 더해진다
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

    /** 카탈로그의 음식 하나를 화면용으로. 찜 목록(LikeServiceImpl)도 이걸 쓴다. */
    // static 이라 빈 없이도 부를 수 있다. 바꾸는 규칙을 한 곳에 둔 이유 —
    // 추천과 찜 목록이 같은 카드를 그리는데, 각자 옮겨 담으면 한쪽에서
    // 칸을 빠뜨려도 그 화면에서만 비어 보인다
    public static FoodVO toFoodVO(JsonNode food) {
        // 태그는 배열이라 하나씩 옮겨 담는다
        List<String> tags = new ArrayList<>();
        for (JsonNode tag : food.path("tags")) {
            tags.add(tag.asString());
        }

        // 사진은 get 으로 꺼내 직접 확인한다. 100개 중 사진이 없는 음식도
        // 있어서, 빈 글자가 아니라 아예 null 로 둬야 화면이 art 로 떨어진다
        JsonNode imageUrl = food.get("image_url");

        // 생성자로 한 번에 만든다(@AllArgsConstructor). setter 를 아홉 번
        // 늘어놓으면 칸을 하나 빠뜨려도 컴파일이 된다 - 대신 인자 순서를
        // 틀리면 안 되므로 FoodVO 의 칸 순서를 그대로 따른다
        return new FoodVO(
                food.path("food_name").asString(),
                food.path("category").asString(),
                food.path("reason").asString(),
                food.path("cook_time_minutes").asInt(),
                food.path("difficulty").asString(),
                food.path("estimated_cost").asInt(),
                tags,
                // asString(null) 은 "값이 없으면 null" 이라는 뜻이다
                food.path("art").asString(null),
                imageUrl == null || imageUrl.isNull() ? null : imageUrl.asString());
    }
}
