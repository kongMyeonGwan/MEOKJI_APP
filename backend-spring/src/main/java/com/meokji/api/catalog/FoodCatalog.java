package com.meokji.api.catalog;

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
 * <p>파이썬 쪽 {@code app/data/__init__.py} 자리다. 데이터 파일
 * ({@code resources/data/catalog.json})은 파이썬 쪽에서 <b>그대로 복사</b>했다.
 *
 * <p>표를 만들지 않는 이유는 사용자가 만드는 값이 아니기 때문이다. 카테고리처럼
 * 운영자도 손대지 않는 목록은 파일에 두고 서버가 내려준다.
 *
 * <p>지금은 음식 이름으로 {@code art}(썸네일 그림 이름)를 찾는 데만 쓴다.
 * 추천 점수 계산과 카탈로그 엔드포인트가 다음 단계에서 이 클래스를 늘려 쓴다.
 */
@Component
public class FoodCatalog {

    private static final String PATH = "data/catalog.json";

    /** 음식 이름 -> 카탈로그 한 줄. 순서를 유지해야 추천 결과가 흔들리지 않는다. */
    private final Map<String, JsonNode> foodsByName = new LinkedHashMap<>();

    /**
     * 더운 날 밀어줄 음식 이름.
     *
     * <p>태그로 못 잡는 것들이 있어서 이름을 직접 적어둔 목록이다. 예를 들어
     * 냉면은 '면' 이면서 차게 먹는데, '면' 태그만 보면 칼국수와 같은 점수가 된다.
     */
    private final Set<String> hotWeatherPicks = new LinkedHashSet<>();

    private JsonNode root;

    @PostConstruct
    void load() {
        JsonMapper mapper = JsonMapper.builder().build();

        try (InputStream in = new ClassPathResource(PATH).getInputStream()) {
            root = mapper.readTree(in);

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
     * <p>사용자가 직접 검색한 이름으로도 기록이 남을 수 있어서, 없을 때를 정상으로
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
    public List<JsonNode> foods() {
        return new ArrayList<>(foodsByName.values());
    }

    /** 더운 날 밀어줄 음식인지. 점수 계산이 이것을 먼저 본다. */
    public boolean isHotWeatherPick(String foodName) {
        return foodName != null && hotWeatherPicks.contains(foodName);
    }

    /** catalog.json 의 최상위 칸 하나 (categories · roulette · games 등). */
    public JsonNode section(String name) {
        return root.path(name);
    }
}
