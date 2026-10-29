package com.meokji.api.auth.vo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 회원가입 요청.
// @NotBlank 같은 검사에 걸리면 컨트롤러까지 오지 않고 422 에러가 나간다 (ApiExceptionHandler).
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterVO {

    @NotBlank(message = "이메일을 입력해주세요")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    @Size(max = 255, message = "이메일이 너무 깁니다")
    private String email;

    @NotBlank(message = "비밀번호를 입력해주세요")
    @Size(min = 8, max = 72, message = "비밀번호는 8자 이상이어야 합니다")
    private String password;

    @NotBlank(message = "닉네임을 입력해주세요")
    @Size(max = 50, message = "닉네임은 50자 이하여야 합니다")
    private String nickname;

    // 아래 셋은 선택 항목. 안 보내면 null 로 저장된다.
//    @Pattern(regexp = "MALE|FEMALE|OTHER", message = "성별 값이 올바르지 않습니다")
    @Pattern(regexp = "(MALE|FEMALE|OTHER)?", message = "성별 값이 올바르지 않습니다")
    private String gender;

    @Size(max = 30)
    private String addressSido;

    @Size(max = 30)
    private String addressSigungu;

    @Size(max = 30)
    private String addressDong;
}
