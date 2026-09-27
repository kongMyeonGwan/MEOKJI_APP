package com.meokji.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 비밀번호 재설정 2단계 — 받은 인증번호 확인.
 *
 * <p>{@code code} 는 정확히 6자리다. 길이가 다르면 대조까지 가지 않고 422 로 끊는다.
 */
public record PasswordResetVerifyRequest(

        @NotBlank(message = "이메일을 입력해주세요")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        String email,

        @NotBlank(message = "인증번호를 입력해주세요")
        @Size(min = 6, max = 6, message = "인증번호는 6자리입니다")
        String code) {
}
