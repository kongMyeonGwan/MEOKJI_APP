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
 *
 * <p>왜 "자르는 쪽이면 위험한가" — 72바이트를 넘는 비밀번호를 잘라서
 * 쓰면, 앞 72바이트가 같은 서로 다른 비밀번호가 전부 같은 것이 된다.
 * 긴 비밀번호를 쓴 사람이 오히려 뒷부분을 틀려도 로그인된다.
 *
 * <p>쓰는 곳이 넷이다 — 가입 · 비밀번호 변경 · 비밀번호 재설정(저장 전),
 * 로그인(대조 전). 앞의 셋은 checkBytes 로 막고, 로그인은 tooLong 으로
 * 가려 대조 자체를 건너뛴다.
 *
 * <p>로그인에서 예외를 안 던지는 이유 — 넘는 값은 어차피 안 맞는다.
 * 거기서 422 를 내면 "그 길이는 틀렸다" 를 알려주는 셈이고, 응답도
 * 비밀번호가 틀렸을 때와 달라진다.
 */
public final class Passwords {

    /** bcrypt 알고리즘 자체의 제약. */
    // 우리가 정한 값이 아니다. VO 의 @Size(max = 72) 도 이 숫자를 보고 맞췄다
    public static final int BCRYPT_MAX_BYTES = 72;

    // 바깥에서 만들지 못하게 막는다. 전부 static 메서드다
    private Passwords() {
    }

    /**
     * 너무 길면 422 를 던진다. 저장하기 전에 부른다.
     *
     * 문장에 "한글은 1자당 3바이트" 를 적어주는 이유 — 글자 수로만
     * 생각하면 24자짜리 한글 비밀번호가 왜 막히는지 알 수 없다
     */
    public static void checkBytes(String rawPassword) {
        if (tooLong(rawPassword)) {
            throw ApiException.unprocessable(
                    "비밀번호는 UTF-8 기준 " + BCRYPT_MAX_BYTES
                    + "바이트 이하여야 합니다 (한글은 1자당 3바이트)");
        }
    }

    /**
     * 대조 전에 부른다. 넘는 입력은 대조하지 않고 실패로 본다.
     *
     * null 이면 false 다 - "길지 않다" 는 뜻이다. null 검사는 부르는 쪽이
     * @NotBlank 로 이미 하고 있다
     */
    public static boolean tooLong(String rawPassword) {
        return rawPassword != null
                && rawPassword.getBytes(StandardCharsets.UTF_8).length > BCRYPT_MAX_BYTES;
    }
}
