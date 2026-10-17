package com.meokji.api.auth.vo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 비밀번호 재설정 1단계 요청. 인증번호를 받을 이메일
//
// 칸이 하나뿐인데 VO 를 따로 만든 이유 — @RequestBody String 으로 받으면
// JSON 문자열이 그대로 들어와 직접 파싱해야 하고, @RequestParam 으로 받으면
// 이메일이 주소줄에 남는다(브라우저 기록 · 서버 접근 로그). 본문으로 받고
// 검사까지 걸려면 VO 가 있어야 한다.
//
// 단계마다 VO 를 따로 둔 이유 — 하나로 합치면 code 와 newPassword 가
// 1단계에서도 보내도 되는 칸처럼 보이고, @NotBlank 를 걸 수가 없다.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordResetSendVO {

    // RegisterVO 의 email 과 같은 규칙으로 맞췄다. 가입할 때 통과한 값이
    // 여기서 막히면 비밀번호를 영원히 못 바꾸게 된다.
    @NotBlank(message = "이메일을 입력해주세요")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    @Size(max = 255, message = "이메일이 너무 깁니다")
    private String email;
}
