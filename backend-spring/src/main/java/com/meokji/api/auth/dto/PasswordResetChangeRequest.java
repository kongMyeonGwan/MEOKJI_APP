package com.meokji.api.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 비밀번호 재설정 3단계 요청. 이메일 + 확인한 인증번호 + 새 비밀번호
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetChangeRequest {

    @NotBlank(message = "이메일을 입력해주세요")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    @NotBlank(message = "인증번호를 입력해주세요")
    @Size(min = 6, max = 6, message = "인증번호는 6자리입니다")
    private String code;

    @NotBlank(message = "새 비밀번호를 입력해주세요")
    @Size(min = 8, max = 72, message = "비밀번호는 8자 이상이어야 합니다")
    private String newPassword;
}
