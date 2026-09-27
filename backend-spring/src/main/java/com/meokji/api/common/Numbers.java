package com.meokji.api.common;

/**
 * 파이썬 {@code round()} 와 같게 반올림한다.
 *
 * <h2>왜 필요한가</h2>
 *
 * <p>파이썬의 {@code round()} 는 <b>짝수로 맞춤(banker's rounding)</b> 이다.
 * {@code round(2.5)} 가 3 이 아니라 2 다. 자바의 {@code Math.round()} 는 올림이라
 * 3 이 된다.
 *
 * <p>가격에 배율을 곱해 반올림하는 자리가 있어서, 이 차이가 1원씩 어긋난 값을
 * 만든다. 두 서버의 응답을 비교하는 동안에는 그 1원이 "어디가 틀렸나" 를 찾게
 * 만든다. {@link Math#rint} 가 짝수로 맞춤이라 그것을 쓴다.
 */
public final class Numbers {

    private Numbers() {
    }

    /** 정수로. 파이썬 {@code round(value)} 와 같다. */
    public static long round(double value) {
        return (long) Math.rint(value);
    }

    /** 소수 {@code digits} 자리로. 파이썬 {@code round(value, digits)} 와 같다. */
    public static double round(double value, int digits) {
        double factor = Math.pow(10, digits);

        return Math.rint(value * factor) / factor;
    }
}
