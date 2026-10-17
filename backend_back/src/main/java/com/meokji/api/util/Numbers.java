package com.meokji.api.util;

/**
 * 반올림을 한 가지 방식으로 맞춰 쓰는 곳.
 *
 * 왜 필요한가
 *
 * 가격에 배율을 곱해 반올림하는 자리가 있어서, 이 차이가 1원씩 어긋난 값을
 * 만든다. 두 서버의 응답을 비교하는 동안에는 그 1원이 "어디가 틀렸나" 를 찾게
 * 만든다. Math#rint 가 짝수로 맞춤이라 그것을 쓴다.
 *
 * <p>Math.round 와 무엇이 다른가 — Math.round 는 0.5 를 늘 위로 올린다
 * (2.5 -&gt; 3, 3.5 -&gt; 4). rint 는 가까운 짝수로 간다 (2.5 -&gt; 2, 3.5 -&gt; 4).
 * 어느 쪽이 더 맞다기보다, 한쪽으로 정해두지 않으면 자리마다 다른 값이 나온다.
 *
 * <p>왜 한 곳에 모았나 — 가격을 반올림하는 자리가 여럿이다(단위 환산 ·
 * 재료비 합계 · 등락률). 각자 Math.round 와 rint 를 섞어 쓰면 같은 계산이
 * 자리에 따라 1원씩 달라진다.
 *
 * <p>클래스가 final 이고 생성자가 private 인 이유 — 도구 모음이라
 * new Numbers() 로 만들 일이 없다. 상속할 일도 없다.
 */
public final class Numbers {

    // 바깥에서 만들지 못하게 막는다. 전부 static 메서드라 만들 이유가 없다
    private Numbers() {
    }

    /**
     * 소수점을 없애고 정수로. 예) 3600.5 -&gt; 3600
     *
     * long 으로 돌려주는 이유 — 돈을 다루는 자리에서 쓴다. int 는
     * 약 21억이 한계라 더하다 보면 넘칠 걱정을 해야 한다
     */
    public static long round(double value) {
        return (long) Math.rint(value);
    }

    /**
     * 소수점 자리를 정해 반올림. 예) round(12.456, 1) -&gt; 12.5
     *
     * 10의 거듭제곱을 곱했다가 다시 나누는 방식이다. 12.456 에 10 을
     * 곱하면 124.56, 반올림하면 124, 10 으로 나누면 12.4 가 된다.
     *
     * 왜 필요한가 — 등락률(12.456%)을 그대로 보내면 화면에 길게 나오고,
     * 정수로 반올림하면 5% 기준선 근처에서 목록과 표시가 어긋난다
     */
    public static double round(double value, int digits) {
        double factor = Math.pow(10, digits);

        return Math.rint(value * factor) / factor;
    }
}
