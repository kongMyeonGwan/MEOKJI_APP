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

/**
 * 회원가입 · 로그인 · 비밀번호 재설정.  주소: /api/auth/...
 *
 * <p>이 기능의 주소만 로그인 없이 부를 수 있다. 로그인을 하려면 토큰이 있어야
 * 하는데 토큰은 로그인을 해야 생기므로, 시작점은 열려 있어야 한다.
 * (예외가 하나 있다 — /me 는 토큰이 있어야 한다)
 *
 * <p>컨트롤러가 하는 일은 셋뿐이다
 *   1. 주소와 메서드를 받는다 (@PostMapping 등)
 *   2. @Valid 로 요청 값의 모양을 검사한다
 *   3. 서비스를 부르고 결과를 돌려준다
 *
 * <p>겹치는 이메일 확인 · 비밀번호 비교 같은 판단은 전부 서비스가 한다. 그래야
 * 같은 판단이 필요한 다른 자리(배치 · 테스트)에서 컨트롤러를 거치지 않고도
 * 쓸 수 있다.
 *
 * <p>try-catch 가 하나도 없는 이유 — 서비스가 ApiException 을 던지면
 * ApiExceptionHandler 가 받아 {"detail": "..."} 로 바꿔 보낸다. 여기서 잡으면
 * 메서드마다 같은 catch 가 반복된다.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private PasswordResetService passwordResetService;

    // 토큰을 만드는 도구. 아래 login 에서만 쓴다.
    //
    // 서비스가 아니라 컨트롤러가 토큰을 만드는 이유 — 서비스는 "이메일 ·
    // 비밀번호가 맞는가" 까지만 책임진다. 그 결과를 토큰으로 포장하는 것은
    // HTTP 사정이다. 나중에 쿠키로 바꾸더라도 서비스는 안 고친다.
    @Autowired
    private JwtUtil jwtUtil;

    // 회원가입  POST /api/auth/register
    //
    // 201 을 주는 이유 — 200(성공)이 아니라 "새로 만들었다" 는 뜻이다.
    // 같은 이메일로 또 보내면 409 가 나가므로 둘이 구별된다.
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED) // 201
    public UserVO register(@Valid @RequestBody RegisterVO request) {
        // 응답에 비밀번호는 나가지 않는다. UserVO 의 그 칸에 @JsonIgnore 가 붙어 있다.
        UserVO user = authService.register(request);
        return user;
    }

    // 로그인  POST /api/auth/login
    // 로그인 토큰과 회원 정보를 같이 돌려준다
    //
    // 회원 정보를 같이 보내는 이유 — 토큰만 주면 화면은 닉네임을 띄우려고
    // 곧바로 /me 를 한 번 더 불러야 한다. 어차피 방금 DB 에서 읽은 값이라
    // 같이 실어 보낸다.
    @PostMapping("/login")
    public TokenVO login(@Valid @RequestBody LoginVO request) {
        // 비밀번호가 틀리면 여기서 ApiException 이 던져지고 아래로 내려오지 않는다
        UserVO user = authService.login(request);

        TokenVO response = new TokenVO();
        // 토큰 안에는 회원 번호만 넣는다. 닉네임 · 이메일을 넣으면 정보를
        // 고친 뒤에도 토큰에는 옛 값이 남는다
        response.setAccessToken(jwtUtil.createToken(user.getId()));
        // 항상 "bearer". 화면은 이 값을 Authorization 헤더 앞에 붙인다
        response.setTokenType("bearer");
        // 남은 초. 화면이 이 값으로 만료 전에 미리 로그아웃 처리를 할 수 있다
        response.setExpiresIn(jwtUtil.getExpiresInSeconds());
        response.setUser(user);
        return response;
    }

    // 내 정보 (토큰이 아직 쓸 수 있는지 확인할 때도 쓴다)  GET /api/auth/me
    //
    // 화면을 새로 고치면 메모리에 있던 회원 정보가 사라진다. 토큰만
    // localStorage 에 남아 있으므로, 그 토큰으로 이걸 불러 다시 채운다.
    // 토큰이 만료됐으면 401 이 와서 화면이 로그인 페이지로 보낸다.
    //
    // required = false 로 둔 이유 — 헤더가 없을 때 스프링이 400 을 내면
    // 응답 모양이 다른 에러와 달라진다. 여기서 받아 서비스가 401 로 맞춘다.
    @GetMapping("/me")
    public UserVO me(@RequestHeader(value = "Authorization", required = false) String token) {
        UserVO loginUser = authService.getLoginUser(token);
        return loginUser;
    }

    // 비밀번호 재설정 1단계: 이메일로 인증번호 보내기
    //
    // 세 단계로 나눈 이유 — 한 번에 받으면 "메일을 열어 번호를 확인하는" 시간
    // 동안 입력칸을 다 띄워놓게 된다. 번호가 맞는지 먼저 확인해 주면
    // 사용자가 새 비밀번호를 다 적고 나서 "번호가 틀렸다" 를 보는 일이 없다.
    //
    // 가입되지 않은 이메일이면 서비스가 404 를 던져 이 줄까지 오지 않는다.
    // 오타를 낸 사용자가 오지 않는 메일을 기다리는 쪽이 더 나쁘다고 봤다.
    // (2 · 3단계는 반대로 가입 여부를 구별해 주지 않는다 — 그쪽은 번호까지
    //  찍어볼 수 있는 자리라서 단서를 주면 안 된다)
    @PostMapping("/password-reset/send")
    public MessageResponse passwordResetSend(@Valid @RequestBody PasswordResetSendVO request) {
        passwordResetService.send(request.getEmail());
        return new MessageResponse("입력하신 이메일로 인증번호를 보냈습니다. 5분 안에 입력해주세요.");
    }

    // 비밀번호 재설정 2단계: 인증번호 확인
    //
    // 확인에 성공하면 그 코드에 "확인됨" 표시를 남긴다. 3단계가 그 표시를 본다.
    @PostMapping("/password-reset/verify")
    public MessageResponse passwordResetVerify(@Valid @RequestBody PasswordResetVerifyVO request) {
        passwordResetService.verify(request.getEmail(), request.getCode());
        return new MessageResponse("인증되었습니다. 새 비밀번호를 입력해주세요.");
    }

    // 비밀번호 재설정 3단계: 새 비밀번호로 바꾸기
    //
    // 2단계를 거쳤는데 code 를 또 받는 이유 — 2단계와 3단계 사이에 로그인
    // 상태가 없다. 토큰도 없으므로 "아까 확인한 그 사람" 임을 code 로 증명한다.
    @PostMapping("/password-reset/change")
    public MessageResponse passwordResetChange(@Valid @RequestBody PasswordResetChangeVO request) {
        passwordResetService.change(request.getEmail(), request.getCode(), request.getNewPassword());
        return new MessageResponse("비밀번호가 변경되었습니다. 다시 로그인해주세요.");
    }
}
