package com.meokji.api.util;

import java.nio.charset.StandardCharsets;

import com.meokji.api.common.ApiException;

/**
 * 비밀번호 길이 규칙 한 곳.
 *
 * bcrypt 는 72바이트가 상한이다. 넘는 입력을 구현마다 자르거나 거부하는데,
 * 자르는 쪽이면 서로 다른 비밀번호가 같은 것으로 통한다. 그래서 저장 전에
 * 반드시 막는다.
 *
 * 글자 수가 아니라 바이트로 잰다. UTF-8 에서 한글 1자가 3바이트라 한글
 * 비밀번호는 24자가 상한이다 - DTO 의 @Size(max = 72) 만으로는 못 잡는다.
 */
public final class Passwords {

    /** bcrypt 알고리즘 자체의 제약. */
    public static final int BCRYPT_MAX_BYTES = 72;

    private Passwords() {
    }

    public static void checkBytes(String rawPassword) {
        if (tooLong(rawPassword)) {
            throw ApiException.unprocessable(
                    "비밀번호는 UTF-8 기준 " + BCRYPT_MAX_BYTES
                    + "바이트 이하여야 합니다 (한글은 1자당 3바이트)");
        }
    }

    /** 대조 전에 부른다. 넘는 입력은 대조하지 않고 실패로 본다. */
    public static boolean tooLong(String rawPassword) {
        return rawPassword != null
                && rawPassword.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES;
    }
}
