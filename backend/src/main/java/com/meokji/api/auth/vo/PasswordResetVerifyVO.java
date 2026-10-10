package com.meokji.api.auth.vo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 비밀번호 재설정 2단계 요청. 이메일 + 받은 인증번호 6자리
//
// 이메일을 또 받는 이유 — 1단계와 2단계 사이에 로그인 상태가 없다.
// 코드만 받으면 "누구의 코드인지" 를 알 수 없고, 코드가 6자리뿐이라
// 사용자를 안 가리면 다른 사람의 코드와 우연히 맞을 수 있다.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetVerifyVO {

    // 1단계와 달리 @Size(max = 255) 가 없다. 1단계를 통과한 이메일을
    // 화면이 그대로 다시 보내는 자리라 길이는 이미 걸러졌다.
    @NotBlank(message = "이메일을 입력해주세요")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    // min 과 max 를 둘 다 6 으로 둬서 정확히 6자리만 받는다.
    //
    // 길이를 여기서 막는 이유 — 길이가 틀린 값은 어차피 안 맞는데,
    // 통과시키면 bcrypt 비교를 한 번 하고 시도 횟수가 1 올라간다.
    // 횟수 제한이 있으므로 오타 한 번에 기회를 깎을 이유가 없다.
    //
    // 숫자(int)가 아니라 문자열인 이유 — "012345" 처럼 0 으로 시작하는
    // 코드가 int 로는 12345 가 되어 5자리가 된다.
    @NotBlank(message = "인증번호를 입력해주세요")
    @Size(min = 6, max = 6, message = "인증번호는 6자리입니다")
    private String code;
}
