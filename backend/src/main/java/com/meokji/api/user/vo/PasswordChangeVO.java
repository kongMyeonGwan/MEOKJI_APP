package com.meokji.api.user.vo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 로그인한 상태에서 비밀번호 바꾸기 요청
//
// 비밀번호를 잊어버린 사람이 쓰는 길(PasswordResetChangeVO)과 다르다.
// 이쪽은 지금 비밀번호를 알고 있고, 토큰도 있다.
//
// 현재 비밀번호를 또 받는 이유 — 토큰이 있으면 로그인한 사람은 맞지만,
// 자리를 비운 사이에 누가 그 브라우저를 쓸 수 있다. 비밀번호를 바꾸면
// 계정을 넘겨주는 셈이라 한 번 더 확인한다.
//
// 요청 전용이다. 돌려줄 값이 없어서 응답은 MessageResponse 가 맡는다
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PasswordChangeVO {

    // @Size 를 걸지 않는다. 가입 규칙이 바뀌면 옛 비밀번호가 새 규칙에
    // 안 맞아 "지금 비밀번호" 를 넣을 수 없게 된다. 맞는지는 bcrypt
    // 비교로 가리므로 길이를 볼 이유가 없다
    @NotBlank(message = "현재 비밀번호를 입력해주세요")
    private String currentPassword;

    // 새 비밀번호에는 @Size 를 건다. 지금부터 쓸 값이라 규칙을 지켜야 한다.
    // 8~72 는 RegisterVO.password 와 같은 값이다 - 다르게 두면
    // "가입은 되는데 변경은 안 되는" 비밀번호가 생긴다
    @NotBlank(message = "새 비밀번호를 입력해주세요")
    @Size(min = 8, max = 72, message = "비밀번호는 8자 이상이어야 합니다")
    private String newPassword;
}
