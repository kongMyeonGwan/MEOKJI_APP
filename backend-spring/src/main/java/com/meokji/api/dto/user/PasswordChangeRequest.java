package com.meokji.api.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 로그인한 상태에서 비밀번호 바꾸기.
 *
 * <p>비밀번호를 잊어버린 경우는 {@code /api/auth/password-reset} 쪽을 쓴다.
 */
public record PasswordChangeRequest(

        @NotBlank(message = "현재 비밀번호를 입력해주세요")
        String currentPassword,

        @NotBlank(message = "새 비밀번호를 입력해주세요")
        @Size(min = 8, max = 72, message = "비밀번호는 8자 이상이어야 합니다")
        String newPassword) {
}
