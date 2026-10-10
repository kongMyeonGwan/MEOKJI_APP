package com.meokji.api.user.vo;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 내 정보 수정 요청 (PATCH). 보내지 않은 칸(null)은 바꾸지 않는다.
//
// PUT 이 아니라 PATCH 인 이유 — PUT 은 "이 모양으로 통째로 바꿔라" 는
// 뜻이라, 안 보낸 칸은 비워야 맞다. 화면에서 닉네임만 고치는데
// 주소까지 같이 보내야 하면 번거롭고, 빠뜨리면 주소가 지워진다.
//
// @NotBlank 가 하나도 없는 것도 그래서다. null 은 "안 고친다" 는 뜻이라
// 막으면 안 된다.
//
// 이메일과 비밀번호는 여기 없다. 이메일은 로그인 열쇠라 바꾸려면 다시
// 인증해야 하고, 비밀번호는 PasswordChangeVO 가 따로 받는다
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdateVO {

    // min = 1 을 둔 이유 — 빈 글자("")로 보내면 닉네임이 사라진다.
    // null(안 고침)과 ""(빈 이름으로 바꿈)은 다르다.
    //
    // 겹치는 닉네임인지는 서비스가 DB 를 봐서 확인한다
    @Size(min = 1, max = 50, message = "닉네임은 50자 이하여야 합니다")
    private String nickname;

    // @Pattern 은 null 이면 그냥 통과시킨다. 그래서 안 보내도 422 가 안 난다
    @Pattern(regexp = "MALE|FEMALE|OTHER", message = "성별 값이 올바르지 않습니다")
    private String gender;

    // 주소를 고치면 좌표도 다시 찾아 저장한다 (RegionCatalog.findCoords).
    // 좌표 칸을 여기 두지 않는 이유 — 화면이 좌표를 정할 수 있게 하면
    // 주소와 좌표가 어긋난 값이 들어갈 수 있다
    @Size(max = 30)
    private String addressSido;

    @Size(max = 30)
    private String addressSigungu;
}
