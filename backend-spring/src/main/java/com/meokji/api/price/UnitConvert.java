package com.meokji.api.price;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.meokji.api.catalog.IngredientRules;
import com.meokji.api.common.Numbers;

/**
 * 레시피 필요량 ↔ KAMIS 판매 단위 환산 (STEP 17~19 · 42).
 *
 * <p>파이썬 쪽 {@code app/services/unit_convert.py} 자리다.
 *
 * <h2>왜 환산이 필요한가</h2>
 *
 * <p>레시피는 "고등어 120g" 이라고 하고 KAMIS 는 "1마리" 로 값을 매긴다. 견줄
 * 방법이 없으면 환산이 통째로 실패해서 고등어구이가 100원으로 나온다(STEP 37).
 *
 * <h2>쪼개 팔지 않는 것은 올려서 산다 (STEP 42)</h2>
 *
 * <p>레시피가 "무 0.2개" 를 요구하는데 무를 0.2개 파는 가게는 없다. 그래서 배율을
 * 살 수 있는 배율로 한 번 더 올린다. 다만 <b>전부 올리지는 않는다</b> - 판매 단위가
 * 무게·부피면 저울로 달아 파니까 쪼갤 수 있다.
 *
 * <pre>
 *   무 0.2개      · 판매 1개    640원 -&gt; 3,200원   올림
 *   계란 2개      · 판매 30구   466원 -&gt; 7,000원   올림
 *   고춧가루 1큰술 · 판매 1kg    176원 -&gt;   176원   그대로
 * </pre>
 *
 * <p>무게 쪽까지 올리면 고춧가루 1큰술이 1kg(25,200원)이 되어, 애초에 환산이
 * 없애려던 그 문제로 되돌아간다.
 */
@Component
public class UnitConvert {

    /** "20kg" · "1마리" 를 (크기, 단위) 로 쪼갠다. */
    private static final Pattern UNIT = Pattern.compile("^\\s*([\\d.]*)\\s*(.+?)\\s*$");

    /** 무게·부피를 그램으로. 요리에서 물 1ml 는 1g 으로 봐도 무방하다. */
    private static final Map<String, Double> GRAM_PER_UNIT = Map.of(
            "kg", 1000.0,
            "g", 1.0,
            "l", 1000.0,
            "L", 1000.0,
            "ml", 1.0,
            "mL", 1.0);

    /** 세는 단위를 같은 뜻끼리 묶는다. (표준 이름, 표준 단위 몇 개인가) */
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
    public record Unit(double size, String token) {
    }

    private record Count(String name, double per) {
    }

    @Autowired
    private IngredientRules rules;


    /** KAMIS 단위 문자열을 쪼갠다. 못 읽으면 null. */
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
            return null;
        }

        return value <= 0 ? null : new Unit(value, token);
    }

    /** 무게·부피 단위면 그램으로. 세는 단위면 null. */
    private static Double toGrams(Unit unit) {
        Double factor = GRAM_PER_UNIT.get(unit.token());

        return factor == null ? null : unit.size() * factor;
    }

    /** 세는 단위면 (표준 이름, 개수) 로. 아니면 null. */
    private static Count toCount(Unit unit) {
        Count alias = COUNT_ALIASES.get(unit.token());

        return alias == null ? null : new Count(alias.name(), unit.size() * alias.per());
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
        if (sale == null) {
            return null;
        }

        // 1) 판매 단위가 무게·부피면 그램으로 나눈다.
        Double saleGrams = toGrams(sale);
        if (saleGrams != null) {
            Double grams = positive(needGrams);

            return grams == null ? null : grams / saleGrams;
        }

        // 2) 판매 단위가 세는 단위면 세는 단위끼리 맞춘다.
        Count saleCount = toCount(sale);
        if (saleCount == null) {
            return null;
        }

        Count needCount = null;
        if (needQuantity != null && needUnit != null) {
            Unit need = parseUnit(formatQuantity(needQuantity) + needUnit);
            needCount = need == null ? null : toCount(need);
        }

        // 같은 것을 세는 단위끼리만 맞춘다. "개" 를 "마리" 로 나누면 안 된다.
        if (needCount != null && needCount.name().equals(saleCount.name())) {
            return needCount.per() / saleCount.per();
        }

        // 3) 단위가 서로 다르면 무게로 돌아간다.
        Integer per = rules.countGramsOf(name);
        if (per == null) {
            return null;
        }

        Double grams = positive(needGrams);
        if (grams == null) {
            return null;
        }

        // 판매 단위가 "10마리" 면 saleCount.per() 가 10 이다.
        return grams / (per * saleCount.per());
    }

    /** 이 판매 단위를 쪼개서 살 수 있는가. */
    public static boolean divisible(String saleUnit) {
        Unit sale = parseUnit(saleUnit);

        return sale != null && toGrams(sale) != null;
    }

    /**
     * 필요량 배율을 '실제로 사야 하는 배율' 로 올린다.
     *
     * <p>먼저 소수 여섯 자리로 반올림하는 이유가 있다. 2개/1개 가 부동소수점으로
     * {@code 2.0000000000000004} 가 되는 경우가 있는데, 그대로 올리면 두 개 살 것을
     * 세 개 사게 된다.
     */
    public static Double buyRatio(Double ratio, String saleUnit) {
        if (ratio == null || ratio <= 0) {
            return null;
        }

        if (divisible(saleUnit)) {
            return ratio;
        }

        return Math.ceil(Numbers.round(ratio, 6));
    }

    /** 판매 단위 가격에 배율을 곱해 필요한 만큼의 값을 낸다. */
    public static int scalePrice(int price, double ratio) {
        return (int) Math.max(1, Numbers.round(price * ratio));
    }

    private static Double positive(Double value) {
        return value == null || value <= 0 ? null : value;
    }

    /** 0.5 는 "0.5", 2.0 은 "2" 로 적는다. */
    public static String formatQuantity(double quantity) {
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
