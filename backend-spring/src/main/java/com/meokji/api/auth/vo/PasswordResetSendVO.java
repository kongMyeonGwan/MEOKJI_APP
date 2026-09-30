package com.meokji.api.auth.vo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 비밀번호 재설정 1단계 요청. 인증번호를 받을 이메일
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetSendVO {

    @NotBlank(message = "이메일을 입력해주세요")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    @Size(max = 255, message = "이메일이 너무 깁니다")
    private String email;
}
