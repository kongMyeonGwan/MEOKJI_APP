package com.meokji.api.util;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 표가 필요 없는 고정 목록 — 음식 100종 · 카테고리 · 룰렛 · 게임 · 질문.
 *
 * 표를 만들지 않는 이유는 사용자가 만드는 값이 아니기 때문이다. 카테고리처럼
 * 운영자도 손대지 않는 목록은 파일에 두고 서버가 내려준다.
 *
 * 지금 쓰는 곳이 넷이다
 *   CatalogServiceImpl : section() 으로 화면에 내려보낼 칸을 꺼낸다
 *   FoodScorer         : foods() 로 후보 100종을 받아 점수를 매긴다
 *   HistoryServiceImpl · LikeServiceImpl : 이름으로 art(썸네일)를 찾는다
 *
 * <p>서버가 뜰 때 한 번 읽어 메모리에 들고 있는다. 48KB 짜리 파일을
 * 요청마다 다시 읽고 파싱할 이유가 없고, 내용이 바뀌지도 않는다.
 *
 * <p>파일을 못 읽으면 서버가 아예 안 뜬다. 조용히 빈 목록으로 뜨면
 * 추천 · 목록이 전부 비는데 에러는 안 나서 원인을 찾기 어렵다.
 */
@Component
public class FoodCatalog {

    private static final String PATH = "data/catalog.json";

    /** 음식 이름 -> 카탈로그 한 줄. 순서를 유지해야 추천 결과가 흔들리지 않는다. */
    private final Map<String, JsonNode> foodsByName = new LinkedHashMap<>();

    /**
     * 더운 날 밀어줄 음식 이름.
     *
     * 태그로 못 잡는 것들이 있어서 이름을 직접 적어둔 목록이다. 예를 들어
     * 냉면은 '면' 이면서 차게 먹는데, '면' 태그만 보면 칼국수와 같은 점수가 된다.
     */
    private final Set<String> hotWeatherPicks = new LinkedHashSet<>();

    /** 파일 전체. section() 이 여기서 최상위 칸을 꺼낸다 */
    private JsonNode root;

    /**
     * 서버가 뜰 때 한 번 파일을 읽어 메모리에 올린다.
     *
     * {@code @PostConstruct} 는 "이 빈이 만들어진 직후에 불러라" 는 표시다.
     */
    @PostConstruct
    void load() {
        JsonMapper mapper = JsonMapper.builder().build();

        try (InputStream in = new ClassPathResource(PATH).getInputStream()) {
            root = mapper.readTree(in);

            // 음식 100종을 이름 -> 한 줄 로 바꿔 담는다.
            // 배열 그대로 두면 이름으로 찾을 때마다 전체를 훑어야 한다
            for (JsonNode food : root.path("foods")) {
                foodsByName.put(food.path("food_name").asString(), food);
            }

            for (JsonNode name : root.path("hot_weather_picks")) {
                hotWeatherPicks.add(name.asString());
            }
        } catch (Exception exc) {
            // 이 파일이 없으면 추천이 아무것도 못 고른다. 조용히 빈 목록으로 뜨면
            // "추천이 늘 0건인데 이유를 모르겠다" 가 되므로 시작 자체를 막는다.
            throw new IllegalStateException(PATH + " 을 읽지 못했습니다.", exc);
        }

        // 파일은 읽었는데 안이 비어 있는 경우. 읽기가 성공했으니 위 catch 에
        // 안 걸리지만, 추천이 아무것도 못 고르는 것은 똑같다
        if (foodsByName.isEmpty()) {
            throw new IllegalStateException(PATH + " 의 foods 가 비어 있습니다.");
        }
    }

    /** 이름으로 음식 하나. 카탈로그에 없으면 null. */
    public JsonNode findFood(String foodName) {
        if (foodName == null) {
            return null;
        }

        return foodsByName.get(foodName);
    }

    /**
     * 썸네일 그림 이름. 카탈로그에 없는 음식이면 null.
     *
     * 사용자가 직접 검색한 이름으로도 기록이 남을 수 있어서, 없을 때를 정상으로
     * 본다. 화면은 null 이면 기본 그림을 쓴다.
     */
    public String findArt(String foodName) {
        JsonNode food = findFood(foodName);

        if (food == null) {
            return null;
        }

        JsonNode art = food.get("art");

        return art == null || art.isNull() ? null : art.asString();
    }

    /** 음식 100종 전체. 추천이 여기서 고른다. */
    // values() 를 그대로 주지 않고 새 ArrayList 로 복사하는 이유 —
    // values() 는 Map 과 이어진 뷰다. 받는 쪽이 지우면 원본까지 사라진다
    public List<JsonNode> foods() {
        return new ArrayList<>(foodsByName.values());
    }

    /** 더운 날 밀어줄 음식인지. 점수 계산이 이것을 먼저 본다. */
    public boolean isHotWeatherPick(String foodName) {
        return foodName != null && hotWeatherPicks.contains(foodName);
    }

    /** catalog.json 의 최상위 칸 하나 (categories · roulette · games 등). */
    // get 이 아니라 path 다. 없는 이름을 넣어도 null 이 아니라 "빈 노드" 가
    // 와서 부르는 쪽이 터지지 않는다. 대신 이름을 잘못 적으면 조용히
    // 빈 값이 나가므로, 이름이 맞는지는 DataFileTest 가 확인한다
    public JsonNode section(String name) {
        return root.path(name);
    }
}
