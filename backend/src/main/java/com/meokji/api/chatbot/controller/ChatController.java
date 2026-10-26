package com.meokji.api.chatbot.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.auth.service.AuthService;
import com.meokji.api.chatbot.dto.ChatRequest;
import com.meokji.api.chatbot.dto.ChatResponse;
import com.meokji.api.chatbot.service.ChatService;
import com.meokji.api.user.vo.UserVO;

import jakarta.validation.Valid;

/**
 * 재료 대체 챗봇.  주소: /api/chat
 *
 * 로그인한 사람만 쓸 수 있다. 제외 재료(밴)를 그 사람 것으로 읽어야 하기 때문이다.
 * 로그인 여부 확인은 authService.getLoginUser 가 한다 (아니면 401).
 */
@RestController
@RequestMapping("/api/chat")
public class ChatController {

    @Autowired
    private AuthService authService;

    @Autowired
    private ChatService chatService;

    // 질문하기  POST /api/chat
    @PostMapping
    public ChatResponse ask(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody ChatRequest request) {
        UserVO loginUser = authService.getLoginUser(token);
        return chatService.chat(loginUser, request);
    }
}