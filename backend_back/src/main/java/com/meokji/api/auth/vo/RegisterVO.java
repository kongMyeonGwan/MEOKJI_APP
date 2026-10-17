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
//
// 검사를 VO 에 붙이는 이유 — 컨트롤러에서 if 로 하나씩 보면 칸이 일곱 개면
// if 가 열 줄 넘고, 새 칸을 넣을 때 검사를 빠뜨려도 돌아간다. 칸 바로 위에
// 적어 두면 칸과 규칙이 같이 보인다.
//
// 여기서 거르는 것은 "모양" 까지다. 이메일이 겹치는지 · 닉네임이 겹치는지는
// DB 를 봐야 알 수 있어서 AuthServiceImpl 이 확인한다.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RegisterVO {

    // max = 255 는 USERS.EMAIL 컬럼 길이와 맞춘 값이다.
    // 안 막으면 DB 에서 "값이 너무 길다" 에러가 나는데, 그 에러는 500 으로
    // 나가서 사용자는 뭘 고쳐야 할지 알 수 없다.
    @NotBlank(message = "이메일을 입력해주세요")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    @Size(max = 255, message = "이메일이 너무 깁니다")
    private String email;

    // min = 8 은 우리가 정한 규칙, max = 72 는 bcrypt 사정이다.
    // bcrypt 는 72바이트까지만 보고 뒤는 버리므로, 73자 이상을 허용하면
    // 뒷부분이 달라도 같은 비밀번호로 로그인된다.
    @NotBlank(message = "비밀번호를 입력해주세요")
    @Size(min = 8, max = 72, message = "비밀번호는 8자 이상이어야 합니다")
    private String password;

    // 글쓴이 이름으로 화면에 보이는 값. 겹치면 안 되므로 서비스가 한 번 더 확인한다
    @NotBlank(message = "닉네임을 입력해주세요")
    @Size(max = 50, message = "닉네임은 50자 이하여야 합니다")
    private String nickname;

    // 아래 셋은 선택 항목. 안 보내면 null 로 저장된다.
    // @NotBlank 가 없으면 null 일 때 나머지 검사는 그냥 통과한다.

    // 추천 점수 계산에 쓰려고 받아두는 값. 셋 중 하나만 들어간다.
    // regexp 로 막는 이유 — DB 의 체크 제약과 같은 값을 쓰는데, DB 까지
    // 가서 걸리면 500 이 된다. 여기서 막으면 422 로 칸 이름까지 알려준다.
    @Pattern(regexp = "MALE|FEMALE|OTHER", message = "성별 값이 올바르지 않습니다")
    private String gender;

    // 주소. 이 둘로 RegionCatalog 가 좌표를 찾아 USERS 에 같이 저장한다.
    // 좌표가 있어야 날씨 기반 추천을 할 수 있다.
    // message 를 안 적은 이유 — 화면 드롭다운에서 고르는 값이라 길이가
    // 넘칠 일이 거의 없다. 기본 문장으로 충분하다.
    @Size(max = 30)
    private String addressSido;

    @Size(max = 30)
    private String addressSigungu;
}
