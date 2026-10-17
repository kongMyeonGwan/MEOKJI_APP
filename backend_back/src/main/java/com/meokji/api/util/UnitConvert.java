package com.meokji.api.util;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 레시피 필요량 ↔ KAMIS 판매 단위 환산 (STEP 17~19 · 42).
 *
 * 왜 환산이 필요한가
 *
 * 레시피는 "고등어 120g" 이라고 하고 KAMIS 는 "1마리" 로 값을 매긴다. 견줄
 * 방법이 없으면 환산이 통째로 실패해서 고등어구이가 100원으로 나온다(STEP 37).
 *
 * 쪼개 팔지 않는 것은 올려서 산다 (STEP 42)
 *
 * 레시피가 "무 0.2개" 를 요구하는데 무를 0.2개 파는 가게는 없다. 그래서 배율을
 * 살 수 있는 배율로 한 번 더 올린다. 다만 전부 올리지는 않는다 - 판매 단위가
 * 무게·부피면 저울로 달아 파니까 쪼갤 수 있다.
 *
 *   무 0.2개      · 판매 1개    640원 -> 3,200원   올림
 *   계란 2개      · 판매 30구   466원 -> 7,000원   올림
 *   고춧가루 1큰술 · 판매 1kg    176원 ->   176원   그대로
 *
 * 무게 쪽까지 올리면 고춧가루 1큰술이 1kg(25,200원)이 되어, 애초에 환산이
 * 없애려던 그 문제로 되돌아간다.
 *
 * 환산을 못 하는 경우도 있다 (null 을 돌려준다)
 *
 * 판매 단위를 못 읽거나, 세는 단위끼리 종류가 다르고 재료별 무게도
 * 모르는 경우다. 그때는 금액을 안 보여준다 - 틀린 숫자를 보여주는 것보다
 * "모른다" 가 낫다. PriceItemVO 의 lowCost 가 null 로 나간다.
 *
 * static 메서드와 인스턴스 메서드가 섞여 있다
 *
 * neededRatio 만 인스턴스 메서드다. 재료별 무게(IngredientRules)를 봐야
 * 해서 빈을 주입받아야 한다. 나머지는 받은 값만 가지고 계산하므로
 * static 으로 두어, 부르는 쪽이 빈을 들고 있지 않아도 된다.
 */
@Component
public class UnitConvert {

    /** "20kg" · "1마리" 를 (크기, 단위) 로 쪼갠다. */
    // 패턴을 풀어 읽으면
    //   ^\s*      앞의 공백은 버린다
    //   ([\d.]*)  숫자와 점 (없어도 된다 - "마리" 처럼 숫자가 없는 경우)
    //   \s*       숫자와 단위 사이의 공백도 버린다
    //   (.+?)      단위. 한 글자 이상이고, 뒤의 공백은 안 먹는다
    //   \s*$      뒤의 공백도 버린다
    //
    // static final 로 미리 만들어 두는 이유 — Pattern.compile 은 가볍지
    // 않다. 재료마다 두 번씩 부르는 자리라 쓸 때마다 만들면 낭비다
    private static final Pattern UNIT = Pattern.compile("^\\s*([\\d.]*)\\s*(.+?)\\s*$");

    /** 무게·부피를 그램으로. 요리에서 물 1ml 는 1g 으로 봐도 무방하다. */
    // L 과 l, mL 과 ml 을 둘 다 적어둔 이유 — KAMIS 가 어느 쪽으로 줄지
    // 알 수 없다. 대소문자를 맞추는 코드를 넣는 대신 둘 다 적었다
    //
    // 기름은 물보다 가벼워서 1ml 가 1g 이 아니지만, 재료비를 어림하는
    // 자리라 그 차이까지 보지 않는다
    private static final Map<String, Double> GRAM_PER_UNIT = Map.of(
            "kg", 1000.0,
            "g", 1.0,
            "l", 1000.0,
            "L", 1000.0,
            "ml", 1.0,
            "mL", 1.0);

    /** 세는 단위를 같은 뜻끼리 묶는다. (표준 이름, 표준 단위 몇 개인가) */
    // 왜 묶나 — 레시피는 "계란 2개" 라고 하고 KAMIS 는 "30구" 로 값을
    // 매긴다. 둘이 같은 것을 센다는 것을 알아야 나눌 수 있다.
    //
    // "마리" 와 "개" 는 묶지 않는다. 고등어 1마리와 계란 1개를 같다고
    // 보면 안 된다 - 서로 나누면 엉뚱한 금액이 나온다.
    //
    // "손" 만 per 이 2.0 이다. 생선 1손 = 2마리라서, 표준 단위(마리)로
    // 바꿀 때 2를 곱해야 한다
    private static final Map<String, Count> COUNT_ALIASES = Map.of(
            "개", new Count("개", 1.0),
            "구", new Count("개", 1.0),
            "알", new Count("개", 1.0),
            "통", new Count("개", 1.0),
            "송이", new Count("개", 1.0),
            "마리", new Count("마리", 1.0),
            "손", new Count("마리", 2.0),   // 생선 1손 = 2마리
            "포기", new Count("포기", 1.0),
            "장", new Count("장", 1.0));

    /** 쪼갠 단위. "20kg" -> size 20, token "kg" */
    // public 인 이유 — PriceServiceImpl 의 buyText 가 이것을 받아
    // "1개 구입" 문장을 만든다
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Unit {

        private double size;

        private String token;
    }

    // 세는 단위를 표준 이름과 개수로 나타낸 것. 위 Unit 과 달리
    // private 이다 - 이 클래스 안에서만 쓴다
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    private static class Count {

        private String name;

        private double per;
    }

    @Autowired
    private IngredientRules rules;

    /**
     * KAMIS 단위 문자열을 쪼갠다. 못 읽으면 null.
     *
     * 예) "20kg" -&gt; (20, "kg"),  "마리" -&gt; (1, "마리")
     *
     * 예외를 던지지 않고 null 을 주는 이유 — 바깥 API 가 주는 값이라
     * 우리가 모르는 모양이 올 수 있다. 그때는 금액만 안 보여주면 된다
     */
    public static Unit parseUnit(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }

        Matcher matcher = UNIT.matcher(text);
        if (!matcher.matches()) {
            return null;
        }

        String size = matcher.group(1);
        String token = matcher.group(2);

        if (token == null || token.isBlank()) {
            return null;
        }

        double value;
        try {
            // 크기가 없으면 1 이다. "마리" 는 "1마리" 로 본다.
            value = size == null || size.isEmpty() ? 1.0 : Double.parseDouble(size);
        } catch (NumberFormatException exc) {
            // 패턴이 숫자와 점만 받지만, "1.2.3" 처럼 점이 여러 개면
            // 통과한 뒤 parseDouble 에서 터진다
            return null;
        }

        // 0 이나 음수면 못 쓴다. 나누는 데 쓰는 값이라 0 이면 터지고,
        // 음수면 금액이 마이너스가 된다
        return value <= 0 ? null : new Unit(value, token);
    }

    /**
     * 무게·부피 단위면 그램으로. 세는 단위면 null.
     *
     * null 이 "모른다" 가 아니라 "세는 단위다" 라는 뜻으로 쓰인다.
     * 부르는 쪽이 그것을 보고 다음 갈래로 넘어간다
     */
    private static Double toGrams(Unit unit) {
        Double factor = GRAM_PER_UNIT.get(unit.getToken());

        return factor == null ? null : unit.getSize() * factor;
    }

    /**
     * 세는 단위면 (표준 이름, 개수) 로. 아니면 null.
     *
     * 예) "30구" -&gt; ("개", 30),  "1손" -&gt; ("마리", 2)
     */
    private static Count toCount(Unit unit) {
        Count alias = COUNT_ALIASES.get(unit.getToken());

        return alias == null ? null : new Count(alias.getName(), unit.getSize() * alias.getPer());
    }

    /**
     * 판매 단위 하나를 1 로 볼 때, 필요한 양이 몇 배인지.
     *
     * @param name 재료 이름. 세는 단위("1마리")는 품목마다 무게가 달라서 이름이 있어야 푼다
     * @return 배율. 견줄 수 없으면 null
     */
    public Double neededRatio(
            Double needGrams, Double needQuantity, String needUnit, String saleUnit, String name) {

        Unit sale = parseUnit(saleUnit);
        // 판매 단위를 못 읽으면 더 해볼 것이 없다
        if (sale == null) {
            return null;
        }

        // 1) 판매 단위가 무게·부피면 그램으로 나눈다.
        //
        // 가장 흔하고 가장 정확한 길이다. 레시피에 grams 가 적혀 있고
        // 판매도 무게 단위면 그냥 나누면 된다
        Double saleGrams = toGrams(sale);
        if (saleGrams != null) {
            Double grams = positive(needGrams);

            // grams 가 없거나 0 이면 못 한다. 2 · 3번으로 넘어가지도 않는다 -
            // 판매가 무게 단위인데 필요량의 무게를 모르면 방법이 없다
            return grams == null ? null : grams / saleGrams;
        }

        // 2) 판매 단위가 세는 단위면 세는 단위끼리 맞춘다.
        Count saleCount = toCount(sale);
        if (saleCount == null) {
            return null;
        }

        // 필요량도 세는 단위인지 본다.
        //
        // 숫자와 단위를 글자로 이어 붙여 다시 쪼개는 모양인 이유 —
        // parseUnit 이 "2개" 같은 한 덩어리를 받게 만들어져 있다.
        // formatQuantity 를 거치는 것은 2.0 을 "2" 로 적어야 패턴에 맞기 때문이다
        Count needCount = null;
        if (needQuantity != null && needUnit != null) {
            Unit need = parseUnit(formatQuantity(needQuantity) + needUnit);
            needCount = need == null ? null : toCount(need);
        }

        // 같은 것을 세는 단위끼리만 맞춘다. "개" 를 "마리" 로 나누면 안 된다.
        if (needCount != null && needCount.getName().equals(saleCount.getName())) {
            return needCount.getPer() / saleCount.getPer();
        }

        // 3) 단위가 서로 다르면 무게로 돌아간다.
        //
        // "고등어 120g" 과 "1마리" 처럼, 레시피는 무게로 적고 판매는
        // 마리로 매긴 경우다. 한 마리가 몇 그램인지를 알아야 푼다 -
        // 그 값은 계산으로 알 수 없어서 파일에 적어두었다
        Integer per = rules.countGramsOf(name);
        // 그 재료의 무게가 파일에 없으면 포기한다. 억지로 1마리 = 100g
        // 같은 값을 쓰면 금액이 크게 틀어진다
        if (per == null) {
            return null;
        }

        Double grams = positive(needGrams);
        if (grams == null) {
            return null;
        }

        // 판매 단위가 "10마리" 면 saleCount.per() 가 10 이다.
        return grams / (per * saleCount.getPer());
    }

    /**
     * 이 판매 단위를 쪼개서 살 수 있는가.
     *
     * 무게 · 부피면 저울로 달아 파니까 쪼갤 수 있다. 세는 단위면
     * 1개 미만으로는 못 산다.
     *
     * toGrams 가 null 이 아닌지로 가린다 - 무게 단위로 읽히면 쪼갤 수
     * 있다는 뜻이다
     */
    public static boolean divisible(String saleUnit) {
        Unit sale = parseUnit(saleUnit);

        return sale != null && toGrams(sale) != null;
    }

    /**
     * 필요량 배율을 '실제로 사야 하는 배율' 로 올린다.
     *
     * 먼저 소수 여섯 자리로 반올림하는 이유가 있다. 2개/1개 가 부동소수점으로
     * 2.0000000000000004 가 되는 경우가 있는데, 그대로 올리면 두 개 살 것을
     * 세 개 사게 된다.
     */
    public static Double buyRatio(Double ratio, String saleUnit) {
        if (ratio == null || ratio <= 0) {
            return null;
        }

        // 쪼갤 수 있으면 그대로 쓴다. 고춧가루 1큰술을 1kg 으로 올리면
        // 176원이 25,200원이 된다
        if (divisible(saleUnit)) {
            return ratio;
        }

        // 쪼갤 수 없으면 올린다. 0.2개 -> 1개, 1.1개 -> 2개.
        // 먼저 소수 여섯 자리로 반올림하는 이유는 위 주석에 적어 두었다
        return Math.ceil(Numbers.round(ratio, 6));
    }

    /** 판매 단위 가격에 배율을 곱해 필요한 만큼의 값을 낸다. */
    //
    // Math.max(1, ...) 로 최소 1원을 보장한다. 아주 조금만 쓰는 재료
    // (소금 한 꼬집)는 반올림하면 0원이 되는데, 0원은 "공짜" 로 읽혀서
    // "값을 모른다(null)" 와 헷갈린다
    //
    // Numbers.round 를 쓰는 이유 — 반올림 방식을 프로젝트 전체에서
    // 하나로 맞췄다. Math.round 와 섞으면 자리마다 1원씩 달라진다
    public static int scalePrice(int price, double ratio) {
        return (int) Math.max(1, Numbers.round(price * ratio));
    }

    // null · 0 · 음수를 모두 null 로 모은다. 나누는 데 쓰는 값이라
    // 0 이면 터지고 음수면 금액이 마이너스가 된다.
    //
    // 세 경우를 한 메서드로 묶은 이유 — 세 군데에서 같은 확인을 한다
    private static Double positive(Double value) {
        return value == null || value <= 0 ? null : value;
    }

    /**
     * 0.5 는 "0.5", 2.0 은 "2" 로 적는다.
     *
     * 왜 필요한가 — 화면에 "2.0개 필요" 로 나가면 어색하다. 반대로
     * 0.5 를 정수로 만들면 분량이 틀어진다.
     *
     * 쓰는 곳이 둘이다 — 화면에 보여줄 글자를 만들 때(needText · buyText),
     * 그리고 위 neededRatio 가 parseUnit 에 넘길 글자를 만들 때다
     */
    public static String formatQuantity(double quantity) {
        // rint 와 같으면 소수점 아래가 없다는 뜻이다. 2.0 == rint(2.0) 이다.
        //
        // isInfinite 를 같이 보는 이유 — 무한대도 rint 와 같게 나온다.
        // long 으로 바꾸면 엉뚱한 큰 숫자가 된다
        if (quantity == Math.rint(quantity) && !Double.isInfinite(quantity)) {
            return String.valueOf((long) quantity);
        }

        // 소수점 뒤의 0 을 떼어낸다. 0.250 이 아니라 0.25 로 적는다.
        String text = String.valueOf(quantity);

        return text.contains(".")
                ? text.replaceAll("0+$", "").replaceAll("\\.$", "")
                : text;
    }
}
