package com.meokji.api.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.meokji.api.service.AuthService;

/**
 * 회원가입 요청. 파이썬 쪽 {@code schemas/auth.py} 의 RegisterRequest 자리다.
 *
 * <p>규칙을 그쪽과 같게 맞춘다. 특히 {@code @NotBlank} 가 중요하다 -
 * <b>오라클은 빈 문자열을 NULL 로 취급</b>해서, 빈 값을 그대로 넘기면 NOT NULL
 * 위반(ORA-01400)으로 500 이 난다. 규칙으로 먼저 걸러야 422 가 나간다.
 *
 * <p>비밀번호 72바이트 상한은 여기서 검사하지 않는다. bcrypt 자체의 제약이고
 * 한글이 섞이면 글자 수로는 잴 수 없어서, {@code AuthService} 가 바이트로 본다.
 */
public record RegisterRequest(

        @NotBlank(message = "이메일을 입력해주세요")
        @Email(message = "올바른 이메일 형식이 아닙니다")
        @Size(max = 255, message = "이메일이 너무 깁니다")
        String email,

        @NotBlank(message = "비밀번호를 입력해주세요")
        @Size(min = 8, max = 72, message = "비밀번호는 8자 이상이어야 합니다")
        String password,

        @NotBlank(message = "닉네임을 입력해주세요")
        @Size(max = 50, message = "닉네임은 50자 이하여야 합니다")
        String nickname,

        // 아래 셋은 선택 항목이다. 안 보내면 NULL 로 들어간다.
        @Pattern(regexp = "MALE|FEMALE|OTHER", message = "성별 값이 올바르지 않습니다")
        String gender,

        @Size(max = 30) String addressSido,
        @Size(max = 30) String addressSigungu) {

    /** 앞뒤 공백을 떼어낸 닉네임. 공백만 넣은 값은 @NotBlank 가 이미 걸렀다. */
    public String trimmedNickname() {
        return nickname == null ? null : nickname.trim();
    }
}
