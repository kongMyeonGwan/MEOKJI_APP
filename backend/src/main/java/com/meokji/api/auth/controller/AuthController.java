package com.meokji.api.auth.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.common.MessageResponse;
import com.meokji.api.auth.vo.LoginVO;
import com.meokji.api.auth.vo.PasswordResetChangeVO;
import com.meokji.api.auth.vo.PasswordResetSendVO;
import com.meokji.api.auth.vo.PasswordResetVerifyVO;
import com.meokji.api.auth.vo.RegisterVO;
import com.meokji.api.auth.vo.TokenVO;
import com.meokji.api.auth.service.AuthService;
import com.meokji.api.auth.service.PasswordResetService;
import com.meokji.api.util.JwtUtil;
import com.meokji.api.user.vo.UserVO;

import jakarta.validation.Valid;

/** 회원가입 · 로그인 · 비밀번호 재설정.  주소: /api/auth/... */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private PasswordResetService passwordResetService;

    @Autowired
    private JwtUtil jwtUtil;

    // 회원가입  POST /api/auth/register
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED) // 201
    public UserVO register(@Valid @RequestBody RegisterVO request) {
        UserVO user = authService.register(request);
        return user;
    }

    // 로그인  POST /api/auth/login
    // 로그인 토큰과 회원 정보를 같이 돌려준다
    @PostMapping("/login")
    public TokenVO login(@Valid @RequestBody LoginVO request) {
        UserVO user = authService.login(request);

        TokenVO response = new TokenVO();
        response.setAccessToken(jwtUtil.createToken(user.getId()));
        response.setTokenType("bearer");
        response.setExpiresIn(jwtUtil.getExpiresInSeconds());
        response.setUser(user);
        return response;
    }

    // 내 정보 (토큰이 아직 쓸 수 있는지 확인할 때도 쓴다)  GET /api/auth/me
    @GetMapping("/me")
    public UserVO me(@RequestHeader(value = "Authorization", required = false) String token) {
        UserVO loginUser = authService.getLoginUser(token);
        return loginUser;
    }

    // 비밀번호 재설정 1단계: 이메일로 인증번호 보내기
    @PostMapping("/password-reset/send")
    public MessageResponse passwordResetSend(@Valid @RequestBody PasswordResetSendVO request) {
        passwordResetService.send(request.getEmail());
        return new MessageResponse("입력하신 이메일로 인증번호를 보냈습니다. 5분 안에 입력해주세요.");
    }

    // 비밀번호 재설정 2단계: 인증번호 확인
    @PostMapping("/password-reset/verify")
    public MessageResponse passwordResetVerify(@Valid @RequestBody PasswordResetVerifyVO request) {
        passwordResetService.verify(request.getEmail(), request.getCode());
        return new MessageResponse("인증되었습니다. 새 비밀번호를 입력해주세요.");
    }

    // 비밀번호 재설정 3단계: 새 비밀번호로 바꾸기
    @PostMapping("/password-reset/change")
    public MessageResponse passwordResetChange(@Valid @RequestBody PasswordResetChangeVO request) {
        passwordResetService.change(request.getEmail(), request.getCode(), request.getNewPassword());
        return new MessageResponse("비밀번호가 변경되었습니다. 다시 로그인해주세요.");
    }
}
