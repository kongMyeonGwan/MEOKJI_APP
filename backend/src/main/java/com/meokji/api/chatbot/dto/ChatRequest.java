package com.meokji.api.chatbot.dto;

import java.util.ArrayList;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 챗봇에 보내는 요청.  POST /api/chat
 *
 * {
 *   "message": "오이 대신 뭐 쓸까?",
 *   "history": [
 *     { "role": "user",      "content": "우유 대신 뭐 쓸까?" },
 *     { "role": "assistant", "content": "두유나 귀리 우유를 추천해요..." }
 *   ]
 * }
 *
 * LLM 은 이전 대화를 기억하지 못한다. 그래서 화면이 지금까지의 대화를
 * 들고 있다가 매번 같이 보낸다. 이번 질문은 history 가 아니라 message 다.
 *
 * 제외 목록(밴 재료)은 여기에 없다. 화면이 보낸 값은 믿을 수 없어서
 * 서버가 로그인한 사람의 목록을 DB 에서 직접 읽는다.
 */
@Data
@NoArgsConstructor
public class ChatRequest {

    @NotBlank(message = "질문을 입력해주세요")
    @Size(max = 200, message = "질문은 200자 이하여야 합니다")
    private String message;

    // 화면이 최근 것만 보내지만 서버도 개수를 막는다. 길어질수록 OpenAI 요금이 늘어난다.
    // 처음 질문이면 빈 목록이거나 null 로 올 수 있다 (ChatService 가 null 을 받아낸다)
    @Valid
    @Size(max = 10, message = "이전 대화는 10개까지만 보낼 수 있습니다")
    private List<Turn> history = new ArrayList<>();

    /** 이전 대화 한 줄. */
    @Data
    @NoArgsConstructor
    public static class Turn {

        // system 은 받지 않는다. 사용자가 제외 규칙을 덮어쓰는 지시를 끼워 넣을 수 있다
        @NotBlank(message = "role 이 비어 있습니다")
        @Pattern(regexp = "user|assistant", message = "role 은 user 또는 assistant 여야 합니다")
        private String role;

        @NotBlank(message = "대화 내용이 비어 있습니다")
        @Size(max = 1000, message = "이전 대화 한 줄은 1000자 이하여야 합니다")
        private String content;
    }
}