package com.meokji.api.user.vo;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// 내 정보 수정 요청 (PATCH). 보내지 않은 칸(null)은 바꾸지 않는다.
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProfileUpdateVO {

    @Size(min = 1, max = 50, message = "닉네임은 50자 이하여야 합니다")
    private String nickname;

//    @Pattern(regexp = "MALE|FEMALE|OTHER", message = "성별 값이 올바르지 않습니다")
    @Pattern(regexp = "(MALE|FEMALE|OTHER)?", message = "성별 값이 올바르지 않습니다")
    private String gender;

    @Size(max = 30)
    private String addressSido;

    @Size(max = 30)
    private String addressSigungu;

    @Size(max = 30)
    private String addressDong;

    /**
     * 위 주소를 '현재 위치로 채우기' 로 채웠는지.
     *
     * true 면 좌표를 저장하지 않는다. 좌표가 비어 있어야 날씨가 그때그때
     * 브라우저로 잡은 위치를 쓰는데, 그게 "현재 위치" 를 고른 뜻이기 때문이다.
     * 드롭다운으로 직접 고른 주소만 좌표까지 박아서 위치보다 앞세운다.
     */
    private Boolean addressFromGps;
}
