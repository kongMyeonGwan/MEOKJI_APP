package com.meokji.api.user.vo;

import jakarta.validation.constraints.NotBlank;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 회원 탈퇴 요청. 비밀번호를 한 번 더 받는다
//
// 토큰이 있으면 로그인한 사람은 맞지만, 자리를 비운 사이에 누가 그
// 브라우저를 쓸 수 있다. 되돌리기 어려운 일이라 한 번 더 확인한다.
//
// 칸이 하나뿐인데 VO 를 만든 이유 — @RequestBody String 으로 받으면
// JSON 글자가 그대로 들어와 직접 파싱해야 하고, @RequestParam 으로
// 받으면 비밀번호가 주소줄에 남는다(브라우저 기록 · 접근 로그)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WithdrawVO {

    // @Size 를 걸지 않는다. 가입 규칙이 바뀌면 옛 비밀번호가 새 규칙에
    // 안 맞아 탈퇴도 못 하게 된다
    @NotBlank(message = "비밀번호를 입력해주세요")
    private String password;
}
