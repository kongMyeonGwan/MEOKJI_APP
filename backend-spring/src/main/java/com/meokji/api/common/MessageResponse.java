package com.meokji.api.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * "잘 됐다" 는 문장 하나만 돌려주는 응답.
 * 모양: { "message": "비밀번호가 변경되었습니다." }
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {

    private String message;
}
