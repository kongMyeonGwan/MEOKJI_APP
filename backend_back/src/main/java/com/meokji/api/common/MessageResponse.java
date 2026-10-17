package com.meokji.api.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * "잘 됐다" 는 문장 하나만 돌려주는 응답.
 * 모양: { "message": "비밀번호가 변경되었습니다." }
 *
 * 왜 이런 클래스가 필요한가 — 비밀번호 변경 · 탈퇴 · 삭제처럼 돌려줄 값이 없는
 * API 도 응답 모양은 JSON 이어야 한다. 그렇다고 Map 을 만들어 put 하면
 * 칸 이름("message")을 서비스마다 손으로 적게 되고, 한 곳에서 "msg" 로
 * 적는 실수가 난다. 칸 이름을 이 클래스 하나에만 두었다.
 *
 * 롬복 세 줄이 하는 일
 *   @Data             : getter · setter · toString 을 만들어 준다
 *   @NoArgsConstructor: 빈 생성자. Jackson 이 JSON 을 객체로 만들 때 필요하다
 *   @AllArgsConstructor: new MessageResponse("저장했습니다.") 로 한 줄에 만들 수 있게 한다
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {

    /** 화면에 그대로 띄울 문장. 사용자가 읽을 말이라 한국어로 적는다 */
    private String message;
}
