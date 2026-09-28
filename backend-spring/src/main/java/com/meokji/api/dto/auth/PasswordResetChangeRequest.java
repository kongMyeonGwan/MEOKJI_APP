package com.meokji.api.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 비밀번호 재설정 3단계 — 새 비밀번호로 교체.
 *
 * <p>2단계에서 확인한 코드를 한 번 더 보낸다. 서버가 "확인을 마친 코드인지" 와
 * "그 코드가 맞는지" 를 다시 보기 때문에, 확인만 해두고 남의 계정 비밀번호를
 * 바꿀 수는 없다.
 *
 * <p>비밀번호 규칙은 회원가입과 같게 맞춘다. 72바이트 상한만 여기서 재지 않는다 -
 * bcrypt 자체의 제약이고 한글이 섞이면 글자 수로 잴 수 없어서 서비스가 본다.
 */
public record PasswordResetChangeRequest(

        @NotBlank(message = "이메일을 입력해주세요")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        String email,

        @NotBlank(message = "인증번호를 입력해주세요")
        @Size(min = 6, max = 6, message = "인증번호는 6자리입니다")
        String code,

        @NotBlank(message = "새 비밀번호를 입력해주세요")
        @Size(min = 8, max = 72, message = "비밀번호는 8자 이상이어야 합니다")
        String newPassword) {
}
