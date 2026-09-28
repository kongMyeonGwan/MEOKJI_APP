package com.meokji.api.auth;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.auth.dto.LoginRequest;
import com.meokji.api.auth.dto.PasswordResetChangeRequest;
import com.meokji.api.auth.dto.PasswordResetSendRequest;
import com.meokji.api.auth.dto.PasswordResetVerifyRequest;
import com.meokji.api.auth.dto.RegisterRequest;
import com.meokji.api.auth.dto.TokenResponse;
import com.meokji.api.auth.dto.UserResponse;
import com.meokji.api.common.ApiException;
import com.meokji.api.common.MessageResponse;
import com.meokji.api.security.JwtProvider;
import com.meokji.api.security.LoginUser;
import com.meokji.api.user.User;

import jakarta.validation.Valid;

/**
 * 회원가입 · 로그인 · 토큰 확인 · 비밀번호 재설정.
 *
 * <p>파이썬 쪽 {@code app/routers/auth.py} 자리다.
 *
 * <p>여기 있는 주소는 모두 <b>로그인 없이</b> 부른다. 비밀번호를 잊은 사람은
 * 토큰이 없기 때문이다. 로그인한 채로 비밀번호를 바꾸는 길은
 * {@code POST /api/users/me/password} 쪽이다.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;
    @Autowired
    private PasswordResetService passwordReset;
    @Autowired
    private JwtProvider jwt;


    /**
     * 회원가입.
     *
     * <p>201 로 응답한다. 가입만 시키고 토큰은 주지 않는다 - 파이썬 쪽과 같다.
     * 화면은 가입 성공 뒤 로그인 화면으로 보낸다.
     */
    @PostMapping("/register")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        User created = authService.register(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(created));
    }

    /** 로그인. Access Token 과 회원 정보를 같이 준다. */
    @PostMapping("/login")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        User user = authService.login(request);

        return TokenResponse.of(
                jwt.createAccessToken(user.getId()),
                jwt.getExpiresInSeconds(),
                UserResponse.from(user));
    }

    /**
     * 토큰이 아직 살아있는지 확인한다.
     *
     * <p>프론트가 새로 열릴 때마다 부르는 자리라 가볍게 둔다. 요리 횟수 · 연속
     * 방문일처럼 계산이 드는 값은 {@code GET /api/users/me} 가 준다.
     */
    @GetMapping("/me")
    public UserResponse me(@LoginUser User me) {
        if (me == null) {
            // @LoginUser 가 required=true 라 여기까지 null 로 오지는 않는다.
            // 리졸버 등록(WebConfig)을 빼먹었을 때 조용히 열리는 것을 막는 방어선이다.
            throw ApiException.unauthorized("인증이 필요합니다.");
        }

        return UserResponse.from(me);
    }

    // ------------------------------------------------- 비밀번호 재설정 (STEP 8)

    /**
     * 1단계 — 이메일로 6자리 인증번호를 보낸다.
     *
     * <p>가입되지 않은 이메일이면 404 로 알려준다(STEP 32). 계정 열거를 막는
     * 관례와는 어긋나지만, 오타를 친 사람이 오지 않는 메일을 5분 기다리게 두는
     * 쪽이 더 나쁘다고 판단했다. 방금 보냈으면 429 와 {@code Retry-After} 가 나간다.
     */
    @PostMapping("/password-reset/send")
    public MessageResponse passwordResetSend(
            @Valid @RequestBody PasswordResetSendRequest request) {

        passwordReset.send(request.email());

        return new MessageResponse("입력하신 이메일로 인증번호를 보냈습니다. 5분 안에 입력해주세요.");
    }

    /** 2단계 — 인증번호 확인. 5회까지 틀릴 수 있고 넘으면 429 다. */
    @PostMapping("/password-reset/verify")
    public MessageResponse passwordResetVerify(
            @Valid @RequestBody PasswordResetVerifyRequest request) {

        passwordReset.verify(request.email(), request.code());

        return new MessageResponse("인증되었습니다. 새 비밀번호를 입력해주세요.");
    }

    /** 3단계 — 비밀번호를 바꾸고 계정 잠금을 함께 푼다. */
    @PostMapping("/password-reset/change")
    public MessageResponse passwordResetChange(
            @Valid @RequestBody PasswordResetChangeRequest request) {

        passwordReset.change(request.email(), request.code(), request.newPassword());

        return new MessageResponse("비밀번호가 변경되었습니다. 다시 로그인해주세요.");
    }
}
