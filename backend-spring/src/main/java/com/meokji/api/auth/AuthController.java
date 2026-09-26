package com.meokji.api.auth;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.auth.dto.LoginRequest;
import com.meokji.api.auth.dto.RegisterRequest;
import com.meokji.api.auth.dto.TokenResponse;
import com.meokji.api.auth.dto.UserResponse;
import com.meokji.api.common.ApiException;
import com.meokji.api.security.JwtProvider;
import com.meokji.api.security.LoginUser;
import com.meokji.api.user.User;

import jakarta.validation.Valid;

/**
 * 회원가입 · 로그인 · 토큰 확인.
 *
 * <p>파이썬 쪽 {@code app/routers/auth.py} 자리다. 비밀번호 재설정
 * (password-reset/{send,verify,change}) 은 아직 옮기지 않았다 - 메일 발송과
 * verification_codes 표가 필요해서 따로 간다.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtProvider jwt;

    public AuthController(AuthService authService, JwtProvider jwt) {
        this.authService = authService;
        this.jwt = jwt;
    }

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
}
