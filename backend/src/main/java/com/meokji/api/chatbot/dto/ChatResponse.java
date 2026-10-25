package com.meokji.api.chatbot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 챗봇의 답변.
 *
 * { "answer": "오이 대신 쓸 수 있는 재료예요. ..." }
 *
 * 화면은 이 한 칸만 읽어서 대화창에 보여준다.
 * 답변 문장은 OpenAI 가 준 글을 그대로 내보내지 않고, 제외 재료를 걸러낸 뒤
 * 서버가 다시 조립한 것이다 (ChatService).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatResponse {

    private String answer;
}