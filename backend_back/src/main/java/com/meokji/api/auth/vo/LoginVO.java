package com.meokji.api.auth.vo;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 로그인 요청. 모양: { "email": "...", "password": "..." }
//
// 요청만 받는 VO 라서 DB 나 응답에는 쓰이지 않는다. UserVO 를 그대로 쓰지 않는
// 이유는 로그인에 필요한 칸이 둘뿐인데 UserVO 에는 닉네임 · 주소 · 권한까지
// 들어 있어서다. 화면이 보내지도 않는 칸을 받으면 "보내면 반영되나" 를
// 확인해야 한다.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {

    // @NotBlank 는 null · 빈 문자열 · 공백만 있는 값을 모두 막는다.
    // @NotNull 로는 "" 가 통과해서 서비스가 빈 이메일로 DB 를 찾아보게 된다.
    @NotBlank(message = "이메일을 입력해주세요")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    // 비밀번호는 @Size 를 걸지 않는다. 가입 규칙이 바뀌면 옛 비밀번호가
    // 새 규칙에 안 맞아 로그인 자체가 막힐 수 있다. 맞는지는 bcrypt 비교로
    // 가리므로 길이를 여기서 볼 이유가 없다.
    //
    // 응답에는 나가지 않는다 — 이 VO 는 받기만 하고 돌려주지 않는다.
    @NotBlank(message = "비밀번호를 입력해주세요")
    private String password;
}
