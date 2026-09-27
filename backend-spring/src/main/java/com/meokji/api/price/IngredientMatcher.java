package com.meokji.api.price;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 재료 이름을 KAMIS 품목코드로 잇는다.
 *
 * <p>파이썬 쪽 {@code app/integrations/ingredient_match.py} 자리다. 표
 * (품목 116개 · 손질 표현 34개 · 별칭 26개 …)는
 * {@code resources/data/kamis-items.json} 으로 내보내서 읽는다.
 *
 * <h2>왜 이름을 다듬어야 하나</h2>
 *
 * <p>레시피에는 "다진 마늘 (국산)" 처럼 적혀 있고 KAMIS 품목명은 "마늘" 이다.
 * 괄호를 떼고, 손질 표현을 떼고, 별칭을 펴고, 그래도 안 맞으면 부분일치로 찾는다.
 *
 * <h2>가공식품은 원물 시세를 빌려 쓰지 않는다</h2>
 *
 * <p>부분일치가 이름 안에서 품목명을 찾기 때문에 가공식품이 원물에 걸린다.
 *
 * <pre>
 *   고추장   -&gt; '고추' 시세
 *   배추김치 -&gt; '배추' 시세
 *   깨소금   -&gt; '소금' 시세
 * </pre>
 *
 * <p>값이 틀리는 것도 문제지만, 화면이 "이건 조사 대상이다" 라고 말하게 되는 게
 * 더 나쁘다(STEP 39). {@code processed} 에 적힌 이름은 부분일치를 건너뛴다.
 */
@Component
public class IngredientMatcher {

    private static final String PATH = "data/kamis-items.json";

    /** 괄호와 그 안의 설명. "밀가루 (중력분)" 의 "(중력분)" 을 뗀다. */
    private static final Pattern PARENTHESES =
            Pattern.compile("[（(\\[][^）)\\]]*[）)\\]]");

    /** KAMIS 조회에 필요한 코드 세 개. {@code kind} 는 없을 수 있다. */
    public record Codes(String group, String item, String kind) {
    }

    private final Map<String, Codes> itemCodes = new LinkedHashMap<>();

    /** 손질 표현. 긴 것부터 떼야 "잘게썬" 이 "썬" 에 먼저 걸리지 않는다. */
    private List<String> modifiers = List.of();

    private List<String> purposeSuffixes = List.of();

    private final Map<String, String> aliases = new LinkedHashMap<>();

    private Set<String> notPriced = Set.of();
    private Set<String> processed = Set.of();

    /** 부분일치 후보. 짧은 이름이 긴 이름을 가로채지 않게 긴 것부터 본다. */
    private List<String> partialKeys = List.of();

    @PostConstruct
    void load() {
        JsonMapper mapper = JsonMapper.builder().build();

        try (InputStream in = new ClassPathResource(PATH).getInputStream()) {
            JsonNode root = mapper.readTree(in);

            root.path("item_codes").properties().forEach(entry -> {
                JsonNode value = entry.getValue();
                JsonNode kind = value.get("kind");

                itemCodes.put(entry.getKey(), new Codes(
                        value.path("group").asString(),
                        value.path("item").asString(),
                        kind == null || kind.isNull() ? null : kind.asString()));
            });

            modifiers = texts(root.path("modifiers"));
            purposeSuffixes = texts(root.path("purpose_suffixes"));

            root.path("aliases").properties()
                    .forEach(entry -> aliases.put(entry.getKey(), entry.getValue().asString()));

            notPriced = new LinkedHashSet<>(texts(root.path("not_priced")));
            processed = new LinkedHashSet<>(texts(root.path("processed")));
        } catch (Exception exc) {
            throw new IllegalStateException(PATH + " 을 읽지 못했습니다.", exc);
        }

        if (itemCodes.isEmpty()) {
            throw new IllegalStateException(PATH + " 의 item_codes 가 비어 있습니다.");
        }

        // 긴 품목명부터 본다. '양파' 를 '파' 로 잡으면 안 된다.
        List<String> keys = new ArrayList<>();
        for (String key : itemCodes.keySet()) {
            if (key.length() >= 2) {
                keys.add(key);
            }
        }
        keys.sort(Comparator.comparingInt(String::length).reversed());
        partialKeys = List.copyOf(keys);

        // 손질 표현도 긴 것부터.
        List<String> sortedModifiers = new ArrayList<>(modifiers);
        sortedModifiers.sort(Comparator.comparingInt(String::length).reversed());
        modifiers = List.copyOf(sortedModifiers);
    }

    private static List<String> texts(JsonNode array) {
        List<String> values = new ArrayList<>();

        for (JsonNode item : array) {
            values.add(item.asString());
        }

        return values;
    }

    /** 품목 수. 문서에 적은 숫자가 맞는지 확인할 때 쓴다. */
    public int itemCount() {
        return itemCodes.size();
    }

    /** 비교하기 좋은 형태로 다듬는다. "다진 마늘 (국산)" -> "마늘" */
    String normalize(String rawName) {
        String text = stripParentheses(rawName == null ? "" : rawName).trim();

        // 낱말 단위로 손질 표현과 용도 표현을 뗀다.
        List<String> words = new ArrayList<>();

        for (String word : text.split("\\s+")) {
            if (word.isEmpty() || modifiers.contains(word)) {
                continue;
            }

            boolean purpose = false;
            for (String suffix : purposeSuffixes) {
                // "반죽용 물" 의 "반죽용" 처럼 재료가 아니라 쓰임새다.
                if (word.endsWith(suffix) && word.length() > 1) {
                    purpose = true;
                    break;
                }
            }
            if (purpose) {
                continue;
            }

            words.add(word);
        }

        text = words.isEmpty() ? text.replace(" ", "") : String.join("", words);

        // 붙여 쓴 손질 표현도 뗀다 ("다진마늘" -> "마늘").
        boolean changed = true;
        while (changed) {
            changed = false;

            for (String modifier : modifiers) {
                if (!text.startsWith(modifier) || text.length() <= modifier.length()) {
                    continue;
                }

                // 떼기 전 이름이 이미 품목이면 건드리지 않는다. '생강' 의 '생' 을
                // 떼면 '강' 이 되어 아무것도 못 찾는다.
                if (itemCodes.containsKey(text)) {
                    return text;
                }

                text = text.substring(modifier.length());
                changed = true;
                break;
            }
        }

        return text;
    }

    private static String stripParentheses(String name) {
        return PARENTHESES.matcher(name).replaceAll(" ");
    }

    /** 가격을 물어볼 대상이 아닌지. 물 · 얼음 같은 것. */
    public boolean isNotPriced(String rawName) {
        if (notPriced.contains(normalize(rawName))) {
            return true;
        }

        // "멸치 다시마 육수용 물" 처럼 마지막 낱말이 물인 경우.
        String[] words = stripParentheses(rawName == null ? "" : rawName).trim().split("\\s+");

        return words.length > 0 && notPriced.contains(words[words.length - 1]);
    }

    /** 재료명으로 KAMIS 코드를 찾는다. 조사 품목이 아니면 null. */
    public Codes findCodes(String rawName) {
        String text = normalize(rawName);

        if (text.isEmpty()) {
            return null;
        }

        Codes exact = itemCodes.get(text);
        if (exact != null) {
            return exact;
        }

        String alias = aliases.get(text);
        if (alias != null) {
            return itemCodes.get(alias);
        }

        // 가공식품은 원물 시세를 빌려 쓰면 안 된다. 부분일치에 넘기지 않는다.
        if (processed.contains(text)) {
            return null;
        }

        for (String key : partialKeys) {
            if (text.contains(key)) {
                return itemCodes.get(key);
            }
        }

        return null;
    }
}
