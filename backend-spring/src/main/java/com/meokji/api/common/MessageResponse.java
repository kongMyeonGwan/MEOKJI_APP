package com.meokji.api.common;

/**
 * 할 일을 끝냈다고만 알려주는 응답.
 *
 * <p>파이썬 쪽 {@code MessageResponse} 자리다. 화면은 이 문구를 그대로 띄운다.
 */
public record MessageResponse(String message) {
}
