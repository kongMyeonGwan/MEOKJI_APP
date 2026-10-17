package com.meokji.api.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
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
 * 재료 이름으로 KAMIS 품목코드를 찾는다.
 *
 * 레시피에는 "다진 마늘 (국산)" 처럼 적혀 있지만 KAMIS 품목명은 "마늘" 이다.
 * 그래서 이름을 먼저 다듬고(normalize) 나서 찾는다(findCodes).
 *
 *   "다진 마늘 (국산)"
 *     → 괄호 떼기        "다진 마늘"
 *     → 손질 표현 떼기   "마늘"
 *     → 품목표에서 찾기  마늘의 코드
 *
 * 찾는 순서 : ① 품목명과 똑같은지  ② 별칭인지  ③ 이름 안에 품목명이 들어 있는지
 *
 * ※ 가공식품은 ③ 을 건너뛴다. 안 그러면 이렇게 엉뚱한 시세가 붙는다.
 *     고추장 → '고추' 시세,  배추김치 → '배추' 시세,  깨소금 → '소금' 시세
 *
 * 규칙(품목표 · 손질 표현 · 별칭 ...)은 전부 data/kamis-items.json 에 있다.
 */
@Component
public class IngredientMatcher {

    private static final String PATH = "data/kamis-items.json";

    // 괄호와 그 안의 글자. "밀가루 (중력분)" 의 "(중력분)" 을 뗄 때 쓴다. ( ) [ ] （ ） 모두
    private static final Pattern PARENTHESES = Pattern.compile("[（(\\[][^）)\\]]*[）)\\]]");

    /** KAMIS 를 조회할 때 필요한 코드 세 개. kind(품종)는 없을 수 있다. */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Codes {

        private String group; // 부류 코드  예) 100 = 식량작물

        private String item; // 품목 코드

        private String kind; // 품종 코드 (없으면 null)
    }

    // ----- kamis-items.json 에서 읽어오는 값들 -----

    private final Map<String, Codes> itemCodes = new LinkedHashMap<>(); // 품목명 → 코드  예) "마늘" → {...}
    private final Map<String, String> aliases = new LinkedHashMap<>(); // 별칭 → 품목명  예) "멸치액젓" → "액젓"
    private List<String> modifiers = new ArrayList<>(); // 손질 표현  예) "다진", "채썬"
    private List<String> purposeSuffixes = new ArrayList<>(); // 쓰임새 끝말  예) "반죽용" 의 "용"
    private Set<String> notPriced = new HashSet<>(); // 가격을 안 물어보는 것  예) 물, 얼음
    private Set<String> processed = new HashSet<>(); // 가공식품  예) 고추장, 배추김치

    // 부분일치(③)에 쓸 품목명. 긴 것부터 본다 - '양파' 를 '파' 로 잡으면 안 되니까
    private List<String> partialKeys = new ArrayList<>();

    // 서버가 켜질 때 한 번 kamis-items.json 을 읽는다
    @PostConstruct
    void load() {
        try (InputStream in = new ClassPathResource(PATH).getInputStream()) {
            JsonNode root = JsonMapper.builder().build().readTree(in);

            // 1. 품목명 → 코드
            for (Map.Entry<String, JsonNode> entry : root.path("item_codes").properties()) {
                JsonNode value = entry.getValue();

                String kind = null;
                if (value.hasNonNull("kind")) {
                    kind = value.get("kind").asString();
                }

                Codes codes = new Codes(value.path("group").asString(), value.path("item").asString(), kind);
                itemCodes.put(entry.getKey(), codes);
            }

            // 2. 별칭 → 품목명
            for (Map.Entry<String, JsonNode> entry : root.path("aliases").properties()) {
                aliases.put(entry.getKey(), entry.getValue().asString());
            }

            // 3. 나머지 목록들
            modifiers = toList(root.path("modifiers"));
            purposeSuffixes = toList(root.path("purpose_suffixes"));
            notPriced = new HashSet<>(toList(root.path("not_priced")));
            processed = new HashSet<>(toList(root.path("processed")));
        } catch (Exception e) {
            throw new IllegalStateException(PATH + " 을 읽지 못했습니다.", e);
        }

        if (itemCodes.isEmpty()) {
            throw new IllegalStateException(PATH + " 의 item_codes 가 비어 있습니다.");
        }

        // 4. 부분일치 후보 : 두 글자 이상인 품목명만, 긴 것부터
        for (String name : itemCodes.keySet()) {
            if (name.length() >= 2) {
                partialKeys.add(name);
            }
        }
        sortLongFirst(partialKeys);

        // 5. 손질 표현도 긴 것부터. "잘게썬" 이 "썬" 보다 먼저 걸려야 한다
        sortLongFirst(modifiers);
    }

    // JSON 배열 → 글자 목록
    private static List<String> toList(JsonNode array) {
        List<String> values = new ArrayList<>();
        for (JsonNode item : array) {
            values.add(item.asString());
        }
        return values;
    }

    // 긴 글자가 앞에 오게 정렬한다
    private static void sortLongFirst(List<String> list) {
        list.sort((a, b) -> b.length() - a.length());
    }

    // ===================== 밖에서 쓰는 메서드 =====================

    /** 품목 수. 문서에 적은 숫자(116)가 맞는지 테스트에서 확인한다. */
    public int itemCount() {
        return itemCodes.size();
    }

    /**
     * 코드가 겹치지 않게 줄인 품목명 목록. 시세를 통째로 훑을 때 쓴다 (BargainService).
     *
     * 품목표에는 같은 코드를 가리키는 이름이 여럿 있다 ("파" · "대파", "계란" · "달걀").
     * 이름마다 물어보면 같은 응답을 두 번 받으니까 코드마다 처음 나온 이름 하나만 남긴다.
     * 그래서 116개가 101개로 줄어든다. 남긴 이름이 화면 칩에 그대로 적힌다.
     */
    public List<String> uniqueItemNames() {
        Map<Codes, String> firstNameOfCodes = new LinkedHashMap<>(); // 코드 → 처음 나온 이름

        for (Map.Entry<String, Codes> entry : itemCodes.entrySet()) {
            if (!firstNameOfCodes.containsKey(entry.getValue())) {
                firstNameOfCodes.put(entry.getValue(), entry.getKey());
            }
        }

        return new ArrayList<>(firstNameOfCodes.values());
    }

    /**
     * 재료명으로 KAMIS 코드를 찾는다. 조사 품목이 아니면 null.
     *
     *   "다진 마늘 (국산)" → 마늘의 코드
     *   "고추장"           → null (가공식품)
     */
    public Codes findCodes(String rawName) {
        String name = normalize(rawName);

        if (name.isEmpty()) {
            return null;
        }

        // ① 품목명과 똑같으면 바로
        if (itemCodes.containsKey(name)) {
            return itemCodes.get(name);
        }

        // ② 별칭이면 원래 품목명으로 바꿔서
        if (aliases.containsKey(name)) {
            return itemCodes.get(aliases.get(name));
        }

        // 가공식품은 여기서 멈춘다. ③ 으로 가면 고추장이 '고추' 시세를 빌려 쓴다
        if (processed.contains(name)) {
            return null;
        }

        // ③ 이름 안에 품목명이 들어 있으면  예) "애느타리버섯" → "느타리버섯"
        for (String key : partialKeys) {
            if (name.contains(key)) {
                return itemCodes.get(key);
            }
        }

        return null;
    }

    /** 가격을 물어볼 대상이 아닌지. 물 · 얼음 같은 것. */
    public boolean isNotPriced(String rawName) {
        // 1. 다듬은 이름이 목록에 있으면  예) "찬물"
        if (notPriced.contains(normalize(rawName))) {
            return true;
        }

        // 2. 마지막 낱말이 목록에 있으면  예) "멸치 다시마 육수용 물" 의 "물"
        String[] words = removeParentheses(rawName).trim().split("\\s+");
        String lastWord = words[words.length - 1];

        return notPriced.contains(lastWord);
    }

    // ===================== 이름 다듬기 =====================

    /**
     * 찾기 좋게 이름을 다듬는다.  "다진 마늘 (국산)" → "마늘"
     *
     *   1. 괄호를 뗀다                  "다진 마늘 (국산)" → "다진 마늘"
     *   2. 손질 · 쓰임새 낱말을 뺀다    "다진 마늘"        → "마늘"
     *   3. 붙여 쓴 손질 표현도 뗀다     "다진마늘"         → "마늘"
     */
    String normalize(String rawName) {
        // 1. 괄호 떼기
        String text = removeParentheses(rawName).trim();

        // 2. 띄어쓰기로 나눠서, 손질 표현("다진")과 쓰임새("반죽용")가 아닌 낱말만 남긴다
        List<String> words = new ArrayList<>();
        for (String word : text.split("\\s+")) {
            if (word.isEmpty() || modifiers.contains(word) || isPurposeWord(word)) {
                continue;
            }
            words.add(word);
        }

        // 남은 낱말을 붙인다. 다 빠졌으면 원래 이름에서 띄어쓰기만 없앤다
        if (words.isEmpty()) {
            text = text.replace(" ", "");
        } else {
            text = String.join("", words);
        }

        // 3. 앞에 붙은 손질 표현을 하나씩 뗀다 ("다진마늘" → "마늘")
        //    단, 이미 품목명이면 멈춘다. '생강' 에서 '생' 을 떼면 '강' 이 되어 못 찾는다
        while (!itemCodes.containsKey(text)) {
            String modifier = findLeadingModifier(text);
            if (modifier == null) {
                break; // 더 뗄 게 없다
            }
            text = text.substring(modifier.length());
        }

        return text;
    }

    // "반죽용" 처럼 재료가 아니라 쓰임새를 말하는 낱말인지
    private boolean isPurposeWord(String word) {
        if (word.length() <= 1) {
            return false; // "용" 한 글자는 쓰임새로 보지 않는다
        }
        for (String suffix : purposeSuffixes) {
            if (word.endsWith(suffix)) {
                return true;
            }
        }
        return false;
    }

    // text 가 손질 표현으로 시작하면 그 표현을 돌려준다. 없으면 null
    // (손질 표현만 있고 뒤에 아무것도 없으면 떼지 않는다)
    private String findLeadingModifier(String text) {
        for (String modifier : modifiers) {
            if (text.startsWith(modifier) && text.length() > modifier.length()) {
                return modifier;
            }
        }
        return null;
    }

    // 괄호와 그 안의 글자를 빈칸으로 바꾼다. null 이면 빈 글자
    private static String removeParentheses(String name) {
        if (name == null) {
            return "";
        }
        return PARENTHESES.matcher(name).replaceAll(" ");
    }
}
