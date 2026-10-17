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
 *
 * 규칙을 코드가 아니라 파일에 둔 이유
 *
 * 손질 표현("다진" · "채썬")도 별칭("멸치액젓" -&gt; "액젓")도 계속 늘어난다.
 * 레시피를 하나 추가할 때마다 자바 코드를 고치고 다시 빌드할 수는 없다.
 *
 * 못 찾으면 null 이다 (예외가 아니다)
 *
 * KAMIS 가 조사하지 않는 품목이 많다(가공식품 대부분). 못 찾는 것이
 * 흔한 일이라 에러로 보지 않는다. PriceServiceImpl 이 그때 "조사 대상이
 * 아님" 으로 표시해 내보낸다.
 *
 * 쓰는 곳이 둘이다
 *   PriceServiceImpl    : 레시피 재료 하나씩 코드를 찾는다
 *   BargainServiceImpl  : uniqueItemNames() 로 전체를 훑는다
 */
@Component
public class IngredientMatcher {

    private static final String PATH = "data/kamis-items.json";

    // 괄호와 그 안의 글자. "밀가루 (중력분)" 의 "(중력분)" 을 뗄 때 쓴다. ( ) [ ] （ ） 모두
    //
    // 전각 괄호（）까지 넣은 이유 — 레시피를 손으로 적다 보면 한글 자판에서
    // 전각 괄호가 섞여 들어간다. 눈으로는 구별이 잘 안 돼서, 안 넣어두면
    // "왜 이 재료만 시세가 안 붙지" 를 한참 찾게 된다
    private static final Pattern PARENTHESES = Pattern.compile("[（(\\[][^）)\\]]*[）)\\]]");

    /** KAMIS 를 조회할 때 필요한 코드 세 개. kind(품종)는 없을 수 있다. */
    // @Data 가 equals · hashCode 도 만들어 준다. 아래 uniqueItemNames 가
    // 이 객체를 Map 의 열쇠로 쓰기 때문에 그 둘이 꼭 필요하다 -
    // 없으면 값이 같아도 다른 열쇠로 취급되어 중복이 안 걸러진다
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Codes {

        private String group; // 부류 코드  예) 100 = 식량작물

        private String item; // 품목 코드

        private String kind; // 품종 코드 (없으면 null)
    }

    // ----- kamis-items.json 에서 읽어오는 값들 -----

    // LinkedHashMap 인 이유 — 파일에 적은 순서를 지킨다. uniqueItemNames 가
    // "코드마다 처음 나온 이름" 을 남기는데, 순서가 흔들리면 화면 칩에
    // 적히는 이름이 서버를 켤 때마다 달라진다
    private final Map<String, Codes> itemCodes = new LinkedHashMap<>(); // 품목명 → 코드  예) "마늘" → {...}
    private final Map<String, String> aliases = new LinkedHashMap<>(); // 별칭 → 품목명  예) "멸치액젓" → "액젓"
    private List<String> modifiers = new ArrayList<>(); // 손질 표현  예) "다진", "채썬"
    private List<String> purposeSuffixes = new ArrayList<>(); // 쓰임새 끝말  예) "반죽용" 의 "용"
    private Set<String> notPriced = new HashSet<>(); // 가격을 안 물어보는 것  예) 물, 얼음
    private Set<String> processed = new HashSet<>(); // 가공식품  예) 고추장, 배추김치

    // 부분일치(③)에 쓸 품목명. 긴 것부터 본다 - '양파' 를 '파' 로 잡으면 안 되니까
    //
    // 짧은 것부터 보면 "양파" 가 "파" 에 걸려 대파 시세가 붙는다.
    // 긴 것부터 보면 "양파" 가 먼저 걸린다
    private List<String> partialKeys = new ArrayList<>();

    // 서버가 켜질 때 한 번 kamis-items.json 을 읽는다
    @PostConstruct
    void load() {
        try (InputStream in = new ClassPathResource(PATH).getInputStream()) {
            JsonNode root = JsonMapper.builder().build().readTree(in);

            // 1. 품목명 → 코드
            for (Map.Entry<String, JsonNode> entry : root.path("item_codes").properties()) {
                JsonNode value = entry.getValue();

                // 품종 코드는 없을 수 있다. hasNonNull 로 가리는 이유 —
                // 칸이 없는 경우와 null 로 적힌 경우를 한 번에 거른다
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
            // 이 파일이 없으면 재료 가격이 하나도 안 붙는다. 조용히 빈
            // 목록으로 뜨면 "가격이 왜 다 없지" 를 한참 찾게 되므로
            // 시작 자체를 막는다
            throw new IllegalStateException(PATH + " 을 읽지 못했습니다.", e);
        }

        // 파일은 읽었는데 품목표가 비어 있는 경우. 결과는 같다
        if (itemCodes.isEmpty()) {
            throw new IllegalStateException(PATH + " 의 item_codes 가 비어 있습니다.");
        }

        // 4. 부분일치 후보 : 두 글자 이상인 품목명만, 긴 것부터
        //
        // 한 글자를 빼는 이유 — "파" · "무" 같은 품목명이 들어 있으면
        // "파프리카" 가 "파" 에 걸리고 "무청" 이 "무" 에 걸린다.
        // 한 글자 품목은 ①(정확히 같음)과 ②(별칭)로만 찾는다
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
    //
    // b.length() - a.length() 로 적으면 내림차순이다. 음수면 a 가 앞,
    // 양수면 b 가 앞으로 가는데, b 가 더 길 때 양수가 나온다
    private static void sortLongFirst(List<String> list) {
        list.sort((a, b) -> b.length() - a.length());
    }

    // ===================== 밖에서 쓰는 메서드 =====================

    /** 품목 수. 문서에 적은 숫자(116)가 맞는지 테스트에서 확인한다. */
    // 파일을 고치다 한 줄을 빠뜨려도 에러가 안 난다. 그 품목의 가격만
    // 조용히 안 나올 뿐이라, 숫자로 확인할 길을 열어둔다
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
        // 열쇠가 Codes(객체)다. @Data 가 만들어준 equals · hashCode 덕분에
        // 값이 같은 코드끼리 같은 열쇠로 묶인다
        Map<Codes, String> firstNameOfCodes = new LinkedHashMap<>(); // 코드 → 처음 나온 이름

        for (Map.Entry<String, Codes> entry : itemCodes.entrySet()) {
            // 이미 있으면 건너뛴다. 그래서 "처음 나온 이름" 이 남는다 -
            // put 을 그냥 하면 나중 이름이 덮어써서 "대파" 대신 "파" 가 남는다
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
        // 찾기 전에 이름을 다듬는다. 이 한 줄이 "다진 마늘 (국산)" 을
        // "마늘" 로 만든다
        String name = normalize(rawName);

        // 다듬고 나니 아무것도 안 남은 경우. 레시피에 "약간" 처럼
        // 재료 이름이 아닌 것이 들어 있을 수 있다
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
        //
        // partialKeys 가 긴 것부터 정렬돼 있어서, 처음 걸린 것이
        // 가장 긴(= 가장 구체적인) 품목명이다
        for (String key : partialKeys) {
            if (name.contains(key)) {
                return itemCodes.get(key);
            }
        }

        // 셋 다 안 걸리면 조사 품목이 아니다. 흔한 일이라 에러가 아니다
        return null;
    }

    /**
     * 가격을 물어볼 대상이 아닌지. 물 · 얼음 같은 것.
     *
     * findCodes 로도 못 찾을 값이지만 따로 가리는 이유 — 못 찾은 것과
     * 애초에 살 필요가 없는 것은 다르다. 물에 "조사 대상이 아닙니다" 가
     * 붙으면 사용자는 "물 값을 못 가져왔나" 로 읽는다.
     *
     * (지금은 둘 다 surveyed = false 로 나가지만, 나눠 두면 나중에
     *  화면에서 다르게 보여줄 수 있다)
     */
    public boolean isNotPriced(String rawName) {
        // 1. 다듬은 이름이 목록에 있으면  예) "찬물"
        if (notPriced.contains(normalize(rawName))) {
            return true;
        }

        // 2. 마지막 낱말이 목록에 있으면  예) "멸치 다시마 육수용 물" 의 "물"
        //
        // 한국어는 꾸미는 말이 앞에 오고 실제 재료가 뒤에 온다.
        // 마지막 낱말을 보면 "무엇인지" 가 대개 나온다.
        //
        // normalize 를 안 거치는 이유 — 그쪽은 낱말을 붙여버려서
        // "육수용물" 이 되고, 마지막 낱말을 떼어낼 수 없다
        String[] words = removeParentheses(rawName).trim().split("\\s+");
        // split 은 빈 글자에도 길이 1 짜리 배열을 주므로 터지지 않는다
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
    // package-private 이다 (public 도 private 도 아니다).
    // 같은 패키지의 테스트가 이 메서드를 바로 불러 확인할 수 있게 열어둔 것이고,
    // 바깥 기능이 쓸 일은 없다
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
        //
        // 다 빠지는 경우 — "다진 것" 처럼 꾸미는 말만 있는 이름이다.
        // 빈 글자로 두면 아무것도 못 찾으므로, 원래 이름으로 한 번 더 해본다
        //
        // 붙이는 이유 — 품목표에는 "느타리버섯" 처럼 붙여 적혀 있다.
        // "느타리 버섯" 으로 온 이름을 띄운 채로 두면 못 찾는다
        if (words.isEmpty()) {
            text = text.replace(" ", "");
        } else {
            text = String.join("", words);
        }

        // 3. 앞에 붙은 손질 표현을 하나씩 뗀다 ("다진마늘" → "마늘")
        //    단, 이미 품목명이면 멈춘다. '생강' 에서 '생' 을 떼면 '강' 이 되어 못 찾는다
        // while 로 반복하는 이유 — "잘게다진마늘" 처럼 손질 표현이
        // 두 겹으로 붙는 경우가 있다. 한 번만 떼면 "다진마늘" 이 남는다
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
    //
    // 끝말로 가린다. "반죽용" · "튀김용" · "무침용" 을 하나씩 적는 대신
    // "용" 으로 끝나는지만 본다
    private boolean isPurposeWord(String word) {
        if (word.length() <= 1) {
            return false; // "용" 한 글자는 쓰임새로 보지 않는다
            // 재료 이름이 한 글자인 경우("물" · "무")가 있어서, 끝말과
            // 통째로 같으면 재료로 본다
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
    //
    // 길이를 견주는 조건이 그 "뒤에 아무것도 없으면" 을 막는다.
    // 안 막으면 "다진" 이라는 이름이 빈 글자가 되어 사라진다
    //
    // modifiers 가 긴 것부터 정렬돼 있어서(load 의 5번), "잘게썬" 이
    // "썬" 보다 먼저 걸린다
    private String findLeadingModifier(String text) {
        for (String modifier : modifiers) {
            if (text.startsWith(modifier) && text.length() > modifier.length()) {
                return modifier;
            }
        }
        return null;
    }

    // 괄호와 그 안의 글자를 빈칸으로 바꾼다. null 이면 빈 글자
    //
    // 빈 글자가 아니라 빈칸(공백) 하나로 바꾸는 이유 — "밀가루(중력분)소금"
    // 같은 이름에서 괄호만 지우면 "밀가루소금" 이 되어 붙어버린다.
    // 공백을 넣으면 낱말로 나뉜다
    private static String removeParentheses(String name) {
        if (name == null) {
            return "";
        }
        return PARENTHESES.matcher(name).replaceAll(" ");
    }
}
