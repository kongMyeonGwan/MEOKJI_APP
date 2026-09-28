package com.meokji.api.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 비밀번호 재설정 1단계 — 인증번호를 보낼 이메일. */
public record PasswordResetSendRequest(

        @NotBlank(message = "이메일을 입력해주세요")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        @Size(max = 255, message = "이메일이 너무 깁니다")
        String email) {
}
