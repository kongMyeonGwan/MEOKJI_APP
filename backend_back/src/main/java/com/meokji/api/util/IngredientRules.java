package com.meokji.api.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

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
 * <p>왜 필요한가
 *
 * <p>레시피는 "고등어 1마리", "간장 2큰술" 처럼 적혀 있고, 시세는
 * "고등어 100g 에 2,000원" 처럼 무게로 온다. 둘을 견주려면 "1마리가 몇
 * 그램인지" 를 어딘가 적어둬야 한다. 계산으로는 알 수 없는 값이다.
 *
 * <p>대체재료도 마찬가지다. "오이를 뺀다" 까지는 코드로 되지만, "오이 대신
 * 애호박" 은 사람이 정해줘야 한다.
 *
 * <p>왜 표가 아니라 파일인가 — 사용자도 운영자도 안 고치는 값이다.
 * resources/data/ingredient-rules.json 에 두고 서버가 뜰 때 한 번 읽는다.
 *
 * <p>값이 세 겹으로 떨어진다
 *   1. 재료별 무게 (countGrams)      "고등어 1마리 = 400g"
 *   2. 단위별 무게 (gramsPerUnit)    "큰술 = 15g"
 *   3. 기본값 (defaultGramsPerUnit)  "모르겠으면 100g"
 * 어느 단계에서도 못 찾아 계산이 통째로 실패하는 일이 없게 하려는 것이다.
 */
@Component
public class IngredientRules {

    private static final String PATH = "data/ingredient-rules.json";

    /** 세는 단위 하나의 무게. "고등어 1마리 = 400g" 같은 값이다. */
    private final Map<String, Integer> countGrams = new LinkedHashMap<>();

    /** 단위 하나의 무게. 큰술 15g · 컵 200g … */
    private final Map<String, Integer> gramsPerUnit = new LinkedHashMap<>();

    /** 세는 단위 목록. 이 단위일 때만 #countGrams 를 본다. */
    // 왜 가리나 — "고등어 1마리" 와 "고등어 1g" 은 다르다. countGrams 에
    // 고등어가 있다고 해서 모든 단위에 400 을 곱하면 안 된다.
    //
    // Set.of() 로 빈 값을 넣어두는 이유 — load 전에 누가 부르면
    // null 이라 터진다. 비어 있으면 그냥 2번 단계로 떨어진다
    private Set<String> countUnits = Set.of();

    /** 표에 없는 단위에 쓸 기본값. */
    // 100g 이 맞아서가 아니라, 값이 없어 계산이 멈추는 것보다 낫기 때문이다.
    // 대략이라도 금액이 나오면 사용자가 "비슷하네" 를 판단할 수 있다
    private int defaultGramsPerUnit = 100;

    /** 싫어하는 재료를 뺄 때 제안할 대체재료. */
    private final Map<String, Substitute> substitutes = new LinkedHashMap<>();

    // 표에 없는 재료를 뺄 때 쓸 말. 무엇으로 바꿀지 모르면 "생략" 이다.
    //
    // 코드에 적어두는 이유 — 파일에 default_substitute 가 없어도
    // 동작해야 한다. 파일에 있으면 아래 load 가 덮어쓴다
    private Substitute defaultSubstitute =
            new Substitute("생략", "빼고 만들어도 맛이 크게 달라지지 않습니다");

    /** 무엇으로 바꿀지와 왜 그런지. */
    // reason 을 같이 들고 있는 이유 — 화면이 "오이를 애호박으로 바꿨어요"
    // 만 띄우면 사용자는 왜 바꿨는지, 맛이 달라지는지 알 수 없다.
    //
    // 안쪽 static class 로 둔 이유 — 이 클래스 밖에서는 만들 일이 없는
    // 모양이다. vo 폴더로 빼면 "화면에 나가는 값" 으로 보인다
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Substitute {

        private String substitute;

        private String reason;
    }

    @PostConstruct
    void load() {
        JsonMapper mapper = JsonMapper.builder().build();

        try (InputStream in = new ClassPathResource(PATH).getInputStream()) {
            JsonNode root = mapper.readTree(in);

            // properties() 는 JSON 객체의 칸들을 (이름, 값) 짝으로 준다.
            // {"고등어": 400, "배추": 1200} 이 그대로 Map 이 된다
            root.path("count_grams").properties()
                    .forEach(entry -> countGrams.put(entry.getKey(), entry.getValue().asInt()));

            root.path("grams_per_unit").properties()
                    .forEach(entry -> gramsPerUnit.put(entry.getKey(), entry.getValue().asInt()));

            countUnits = new java.util.LinkedHashSet<>();
            for (JsonNode unit : root.path("count_units")) {
                countUnits.add(unit.asString());
            }

            // asInt(100) 의 100 은 "칸이 없으면 쓸 값" 이다.
            // 위 필드의 기본값과 같은 숫자를 적어두었다
            defaultGramsPerUnit = root.path("default_grams_per_unit").asInt(100);

            root.path("substitutes").properties().forEach(entry -> substitutes.put(
                    entry.getKey(),
                    new Substitute(
                            entry.getValue().path("substitute").asString(),
                            entry.getValue().path("reason").asString())));

            // 파일에 있으면 덮어쓰고, 없으면 코드에 적어둔 "생략" 을 쓴다
            JsonNode fallback = root.path("default_substitute");
            if (!fallback.isMissingNode()) {
                defaultSubstitute = new Substitute(
                        fallback.path("substitute").asString(),
                        fallback.path("reason").asString());
            }
        } catch (Exception exc) {
            // 이 파일이 없으면 재료비가 전부 엉뚱하게 나온다. 에러가 아니라
            // "값이 이상한" 상태라 알아채기 어려우므로 시작 자체를 막는다
            throw new IllegalStateException(PATH + " 을 읽지 못했습니다.", exc);
        }

        // 파일은 읽었는데 표가 비어 있는 경우. 읽기가 성공했으니 위 catch 에
        // 안 걸리지만, 모든 단위가 기본값 100g 으로 계산되어 금액이 틀어진다
        if (countGrams.isEmpty() || gramsPerUnit.isEmpty()) {
            throw new IllegalStateException(PATH + " 의 표가 비어 있습니다.");
        }
    }

    /**
     * 그 분량이 몇 그램인지.
     *
     * 세는 단위(개 · 마리 · 포기 …)면 재료별 무게를 본다. 이게 있어야
     * "고등어 1마리" 를 "고등어 100g" 시세와 견줄 수 있다 - 없으면 환산이 통째로
     * 실패해서 고등어구이가 100원으로 나왔다(STEP 37).
     */
    public double gramsFor(String name, double quantity, String unit) {
        // 1단계 — 재료별 무게가 있고, 세는 단위이면 그것을 쓴다
        Integer per = countGrams.get(name);

        if (per != null && countUnits.contains(unit)) {
            return round1(quantity * per);
        }

        // 2단계 — 단위별 무게. 3단계 — 그것도 없으면 기본값 100g.
        // getOrDefault 한 줄이 두 단계를 다 맡는다
        return round1(quantity * gramsPerUnit.getOrDefault(unit, defaultGramsPerUnit));
    }

    // 소수점 한 자리로 자른다. 0.30000000000000004 같은 값이 그대로
    // 금액 계산에 들어가면 1원씩 어긋난다
    private static double round1(double value) {
        return Math.round(value * 10) / 10.0;
    }

    /**
     * 세는 단위 하나의 무게. 없으면 null.
     *
     * 단위 환산이 이 값을 쓴다 - "고등어 120g" 을 "1마리" 시세와 견주려면
     * 한 마리가 몇 그램인지 알아야 한다.
     */
    public Integer countGramsOf(String name) {
        return name == null ? null : countGrams.get(name);
    }

    /** 이 재료를 무엇으로 바꿀지. 표에 없으면 "생략". */
    public Substitute substituteFor(String name) {
        return substitutes.getOrDefault(name, defaultSubstitute);
    }

    /**
     * 재료 이름 하나가 목록의 어느 값에든 걸치는지.
     *
     * 양방향으로 본다. "고기" 로 등록한 사람에게 "돼지고기" 도 걸리고,
     * "돼지고기" 로 등록한 사람에게 "고기" 도 걸려야 한다.
     */
    // static 인 이유 — 들고 있는 표를 쓰지 않는다. 받은 두 값만 견준다.
    // 빈으로 받지 않고 바로 부를 수 있어서 쓰는 쪽이 간단해진다
    public static boolean matchesAny(String name, List<String> names) {
        if (name == null || names == null) {
            return false;
        }

        for (String other : names) {
            // 등록한 재료 이름에 공백이 섞여 있을 수 있다(" 고수").
            // 그대로 비교하면 "고수" 가 안 걸린다
            String trimmed = other == null ? "" : other.trim();

            // 빈 글자를 빼는 것이 중요하다. ""는 모든 글자에 들어 있어서,
            // 안 빼면 재료가 전부 걸려 레시피에 재료가 하나도 안 남는다
            //
            // contains 를 양쪽으로 본다 - 어느 쪽이 더 긴 이름인지
            // 미리 알 수 없다
            if (!trimmed.isEmpty() && (name.contains(trimmed) || trimmed.contains(name))) {
                return true;
            }
        }

        return false;
        // 글자가 겹치기만 하면 걸린다. "고추" 로 등록하면 "고추장" 도
        // 걸리는데, 매운 것을 못 먹는 사람에게는 그 편이 맞다고 봤다
    }
}
