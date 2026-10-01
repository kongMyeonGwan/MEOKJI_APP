package com.meokji.api.util;

/**
 * 왜 필요한가
 *
 * 가격에 배율을 곱해 반올림하는 자리가 있어서, 이 차이가 1원씩 어긋난 값을
 * 만든다. 두 서버의 응답을 비교하는 동안에는 그 1원이 "어디가 틀렸나" 를 찾게
 * 만든다. Math#rint 가 짝수로 맞춤이라 그것을 쓴다.
 */
public final class Numbers {

    private Numbers() {
    }

    public static long round(double value) {
        return (long) Math.rint(value);
    }

    public static double round(double value, int digits) {
        double factor = Math.pow(10, digits);

        return Math.rint(value * factor) / factor;
    }
}
