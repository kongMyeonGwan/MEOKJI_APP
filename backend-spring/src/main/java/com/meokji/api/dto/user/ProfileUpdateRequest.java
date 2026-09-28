package com.meokji.api.dto.user;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * 내 정보 수정 (PATCH).
 *
 * <p>위경도는 받지 않는다. 주소를 바꾸면 서버가 좌표를 다시 계산한다 - 화면이
 * 좌표를 보내게 하면 주소와 좌표가 어긋난 회원이 생긴다.
 *
 * <p>보내지 않은 칸은 null 로 오고 <b>고치지 않는다.</b> PUT 이 아니라 PATCH 인
 * 이유다. 그래서 "값을 지우고 싶다" 를 표현할 방법이 필요한데, 주소는 빈 문자열을
 * 지움으로 본다({@link #blankToNull}).
 */
public record ProfileUpdateRequest(

        @Size(min = 1, max = 50, message = "닉네임은 50자 이하여야 합니다")
        String nickname,

        @Pattern(regexp = "MALE|FEMALE|OTHER", message = "성별 값이 올바르지 않습니다")
        String gender,

        @Size(max = 30) String addressSido,
        @Size(max = 30) String addressSigungu) {

    /** 앞뒤 공백을 떼어낸 닉네임. 공백만 보냈으면 null 로 본다(고치지 않음). */
    public String trimmedNickname() {
        return blankToNull(nickname);
    }

    /** 주소를 건드렸는지. 좌표를 다시 계산해야 하는지 판단하는 데 쓴다. */
    public boolean touchesAddress() {
        return addressSido != null || addressSigungu != null;
    }

    /**
     * 빈 문자열은 null 로.
     *
     * <p>오라클은 빈 문자열을 NULL 로 취급한다. 그대로 넘겨도 결과는 같지만,
     * 여기서 미리 바꿔두면 "빈 값을 보냈다" 와 "안 보냈다" 가 한 가지로 모여서
     * 아래 서비스 코드의 분기가 하나 줄어든다.
     */
    public static String blankToNull(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty() ? null : trimmed;
    }
}
