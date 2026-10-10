package com.meokji.api.auth.vo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 비밀번호 재설정 3단계 요청. 이메일 + 확인한 인증번호 + 새 비밀번호
//
// 2단계에서 이미 확인했는데 code 를 또 받는 이유 — 2단계가 성공해도
// 로그인 상태가 생기지 않는다(토큰을 주지 않는다). 그래서 "아까 번호를
// 확인한 그 사람" 임을 증명할 수단이 code 밖에 없다.
//
// code 없이 이 주소만 열어두면, 이메일만 알면 남의 비밀번호를 바꿀 수 있다.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetChangeVO {

    @NotBlank(message = "이메일을 입력해주세요")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    // 2단계에서 "확인됨" 표시가 남은 코드여야 한다. 서비스가 그 표시를 본다
    @NotBlank(message = "인증번호를 입력해주세요")
    @Size(min = 6, max = 6, message = "인증번호는 6자리입니다")
    private String code;

    // RegisterVO.password 와 같은 규칙(8~72)으로 맞췄다.
    // 가입 규칙과 다르게 두면 "가입은 되는데 변경은 안 되는" 비밀번호가 생긴다.
    //
    // 칸 이름이 newPassword 라서 JSON 으로는 new_password 로 받는다
    @NotBlank(message = "새 비밀번호를 입력해주세요")
    @Size(min = 8, max = 72, message = "비밀번호는 8자 이상이어야 합니다")
    private String newPassword;
}
