package com.meokji.api.catalog;

import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 재료에 붙는 규칙 — 세는 단위의 무게 · 단위별 무게 · 대체재료.
 *
 * <p>파이썬 쪽 {@code app/integrations/portion.py} 의 표와 {@code ai_api.py} 의
 * {@code SUBSTITUTES} 를 옮긴 것이다. 표를 손으로 옮기지 않고
 * {@code resources/data/ingredient-rules.json} 으로 <b>내보내서 읽는다</b> - 52개 ·
 * 12개를 손으로 베끼면 한두 개는 틀리고, 틀려도 티가 안 난다.
 *
 * <p>고치려면 파이썬 쪽을 고친 뒤 다시 내보낸다. 파일 맨 위에 그렇게 적어뒀다.
 */
@Component
public class IngredientRules {

    private static final String PATH = "data/ingredient-rules.json";

    /** 세는 단위 하나의 무게. "고등어 1마리 = 400g" 같은 값이다. */
    private final Map<String, Integer> countGrams = new LinkedHashMap<>();

    /** 단위 하나의 무게. 큰술 15g · 컵 200g … */
    private final Map<String, Integer> gramsPerUnit = new LinkedHashMap<>();

    /** 세는 단위 목록. 이 단위일 때만 {@link #countGrams} 를 본다. */
    private Set<String> countUnits = Set.of();

    /** 표에 없는 단위에 쓸 기본값. */
    private int defaultGramsPerUnit = 100;

    /** 싫어하는 재료를 뺄 때 제안할 대체재료. */
    private final Map<String, Substitute> substitutes = new LinkedHashMap<>();

    private Substitute defaultSubstitute =
            new Substitute("생략", "빼고 만들어도 맛이 크게 달라지지 않습니다");

    /** 무엇으로 바꿀지와 왜 그런지. */
    public record Substitute(String substitute, String reason) {
    }

    @PostConstruct
    void load() {
        JsonMapper mapper = JsonMapper.builder().build();

        try (InputStream in = new ClassPathResource(PATH).getInputStream()) {
            JsonNode root = mapper.readTree(in);

            root.path("count_grams").properties()
                    .forEach(entry -> countGrams.put(entry.getKey(), entry.getValue().asInt()));

            root.path("grams_per_unit").properties()
                    .forEach(entry -> gramsPerUnit.put(entry.getKey(), entry.getValue().asInt()));

            countUnits = new java.util.LinkedHashSet<>();
            for (JsonNode unit : root.path("count_units")) {
                countUnits.add(unit.asString());
            }

            defaultGramsPerUnit = root.path("default_grams_per_unit").asInt(100);

            root.path("substitutes").properties().forEach(entry -> substitutes.put(
                    entry.getKey(),
                    new Substitute(
                            entry.getValue().path("substitute").asString(),
                            entry.getValue().path("reason").asString())));

            JsonNode fallback = root.path("default_substitute");
            if (!fallback.isMissingNode()) {
                defaultSubstitute = new Substitute(
                        fallback.path("substitute").asString(),
                        fallback.path("reason").asString());
            }
        } catch (Exception exc) {
            throw new IllegalStateException(PATH + " 을 읽지 못했습니다.", exc);
        }

        if (countGrams.isEmpty() || gramsPerUnit.isEmpty()) {
            throw new IllegalStateException(PATH + " 의 표가 비어 있습니다.");
        }
    }

    /**
     * 그 분량이 몇 그램인지.
     *
     * <p>세는 단위(개 · 마리 · 포기 …)면 재료별 무게를 본다. 이게 있어야
     * "고등어 1마리" 를 "고등어 100g" 시세와 견줄 수 있다 - 없으면 환산이 통째로
     * 실패해서 고등어구이가 100원으로 나왔다(STEP 37).
     *
     * @return 그램. 파이썬과 같게 소수 한 자리로 자른다
     */
    public double gramsFor(String name, double quantity, String unit) {
        Integer per = countGrams.get(name);

        if (per != null && countUnits.contains(unit)) {
            return round1(quantity * per);
        }

        return round1(quantity * gramsPerUnit.getOrDefault(unit, defaultGramsPerUnit));
    }

    private static double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }

    /** 이 재료를 무엇으로 바꿀지. 표에 없으면 "생략". */
    public Substitute substituteFor(String name) {
        return substitutes.getOrDefault(name, defaultSubstitute);
    }

    /**
     * 재료 이름 하나가 목록의 어느 값에든 걸치는지.
     *
     * <p>양방향으로 본다. "고기" 로 등록한 사람에게 "돼지고기" 도 걸리고,
     * "돼지고기" 로 등록한 사람에게 "고기" 도 걸려야 한다.
     */
    public static boolean matchesAny(String name, List<String> names) {
        if (name == null || names == null) {
            return false;
        }

        for (String other : names) {
            String trimmed = other == null ? "" : other.trim();

            if (!trimmed.isEmpty() && (name.contains(trimmed) || trimmed.contains(name))) {
                return true;
            }
        }

        return false;
    }
}
