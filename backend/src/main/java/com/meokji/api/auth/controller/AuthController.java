package com.meokji.api.auth.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
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
import com.meokji.api.security.RefreshTokenCookie;
import com.meokji.api.user.vo.UserVO;

import jakarta.servlet.http.HttpServletRequest;
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
    private RefreshTokenCookie refreshTokenCookie;

    // 회원가입  POST /api/auth/register
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED) // 201
    public UserVO register(@Valid @RequestBody RegisterVO request) {
        UserVO user = authService.register(request);
        return user;
    }

    /**
     * 로그인  POST /api/auth/login
     *
     * <pre>
     *   이메일 · 비밀번호 확인
     *          ↓
     *   Access Token (30분) · Refresh Token (14일) 발급
     *          ↓
     *   Refresh Token 해시를 USERS 에 저장
     *          ↓
     *   Refresh Token 은 HttpOnly 쿠키로,  Access Token 은 JSON 으로
     * </pre>
     *
     * <h3>왜 둘을 다른 길로 보내나</h3>
     *
     * Access Token 은 API 를 부를 때마다 헤더에 붙여야 해서 자바스크립트가 알아야 한다.
     * 대신 30분짜리로 짧게 두어 새어도 오래 못 쓰게 한다.
     *
     * Refresh Token 은 14일짜리라 자바스크립트가 알면 안 된다. HttpOnly 쿠키로 보내면
     * 자바스크립트가 읽을 수조차 없고, 브라우저가 /api/auth 로 갈 때만 알아서 붙여준다.
     */
    @PostMapping("/login")
    public ResponseEntity<TokenVO> login(@Valid @RequestBody LoginVO request) {
        UserVO user = authService.login(request);

        // 토큰 두 개를 만들고 Refresh Token 해시를 DB 에 저장한다
        TokenVO response = authService.issueTokens(user, request.isKeep());

        return withRefreshCookie(response);
    }

    /**
     * Access Token 재발급  POST /api/auth/refresh
     *
     * <pre>
     *   브라우저가 Refresh 쿠키를 자동으로 붙여 보냄
     *          ↓
     *   토큰 검사 (서명 · 만료 · 종류) → 회원 확인 → DB 해시 대조 → DB 만료 확인
     *          ↓
     *   새 Access Token + 새 Refresh Token (회전)
     * </pre>
     *
     * <h3>언제 불리나</h3>
     *
     * <pre>
     *   1. F5 · 브라우저 재시작    React 메모리의 Access Token 이 사라졌을 때
     *   2. Access Token 30분 만료  API 가 401 을 낼 때 (axios 가 자동으로 부른다)
     * </pre>
     *
     * 그래서 PC 를 껐다 켜도, 로그아웃하지 않았고 14일이 지나지 않았다면 로그인이 복원된다.
     *
     * <h3>로그인하지 않아도 부를 수 있는 이유</h3>
     *
     * SecurityConfig 에서 permitAll 이다. 이 주소는 "Access Token 이 없거나 만료됐을 때"
     * 부르는 곳이라, 로그인을 요구하면 영영 들어올 수 없다. 대신 쿠키 하나로 모든 검사를
     * 통과해야 해서 아무나 토큰을 받아가지는 못한다.
     *
     * <h3>응답에 user 를 같이 담는 이유</h3>
     *
     * F5 로 들어오는 길에서는 화면이 로그인한 사람의 정보까지 필요하다. 여기서 안 주면
     * 프론트가 곧바로 /api/auth/me 를 한 번 더 불러야 한다. 어차피 검사하면서 DB 에서
     * 읽은 값이라 공짜로 얹을 수 있고, 로그인 응답과 모양이 같아져서 화면 코드도 하나로 준다.
     */
    @PostMapping("/refresh")
    public ResponseEntity<TokenVO> refresh(HttpServletRequest httpRequest) {
        String refreshToken = refreshTokenCookie.read(httpRequest);

        // 쿠키가 아직 남아 있다는 건 "로그인 상태 유지" 를 켠 것이거나, 켜지 않았어도
        // 브라우저를 아직 안 닫은 것이다. 어느 쪽이든 쿠키를 다시 심을 때 전과 같은
        // 수명으로 두는 게 맞다 - keep 을 알 수 없으니 쿠키가 살아 있던 사실로 갈음한다.
        //
        // 세션 쿠키(keep=false)였다면 브라우저를 닫는 순간 사라져서 여기까지 오지 않는다.
        // 그래서 여기 오는 건 사실상 keep=true 이거나, 같은 브라우저 세션 안이다.
        TokenVO response = authService.refresh(refreshToken, true);

        return withRefreshCookie(response);
    }

    /**
     * 로그아웃  POST /api/auth/logout
     *
     * <pre>
     *   USERS 의 Refresh Token 을 NULL 로
     *          ↓
     *   Refresh 쿠키 삭제
     *          ↓
     *   200 (프론트는 메모리의 Access Token 도 버린다)
     * </pre>
     *
     * <h3>DB 와 쿠키를 둘 다 지우는 이유</h3>
     *
     * 쿠키만 지우면 그 값을 미리 복사해둔 사람이 계속 재발급을 받을 수 있다.
     * DB 만 지우면 브라우저에 쓸모없는 쿠키가 남아 매번 헛되이 실려 나간다. 둘 다 지운다.
     *
     * <h3>로그인하지 않아도 부를 수 있는 이유</h3>
     *
     * Access Token 은 30분이면 만료된다. 만료된 뒤에 로그아웃을 누르는 건 아주 흔한 일인데,
     * 여기서 401 을 내면 "로그아웃조차 안 되는" 상태에 갇힌다. 그래서 permitAll 로 두고,
     * 누구인지는 토큰이 아니라 쿠키로 알아낸다.
     *
     * <h3>브라우저 종료와 로그아웃의 차이</h3>
     *
     * <pre>
     *   브라우저 종료  쿠키가 남는다(keep=true) → 다시 열면 로그인 복원
     *   로그아웃       DB 기록이 지워진다       → 쿠키가 남아 있어도 복원 불가
     * </pre>
     */
    @PostMapping("/logout")
    public ResponseEntity<MessageResponse> logout(
            @AuthenticationPrincipal UserVO loginUser,
            HttpServletRequest httpRequest) {

        // 누구를 로그아웃시킬지 찾는다.
        //   1순위: Access Token (아직 안 만료됐으면 필터가 이미 찾아뒀다)
        //   2순위: Refresh 쿠키 (Access Token 이 만료된 흔한 경우)
        Long userId = null;
        if (loginUser != null) {
            userId = loginUser.getId();
        } else {
            userId = authService.findUserIdByRefreshToken(refreshTokenCookie.read(httpRequest));
        }

        if (userId != null) {
            authService.logout(userId);
        }
        // userId 가 null 이어도 에러를 내지 않는다. 이미 로그아웃된 사람이 한 번 더
        // 눌렀을 뿐이고, 결과(로그아웃 상태)는 바라는 것과 같다. 쿠키는 어차피 지운다.

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshTokenCookie.delete().toString())
                .body(new MessageResponse("로그아웃되었습니다."));
    }

    /**
     * 응답에 Refresh Token 쿠키를 얹는다. 로그인과 재발급이 같이 쓴다.
     *
     * Set-Cookie 헤더로 내보낸다. 응답 본문(TokenVO)에는 @JsonIgnore 때문에
     * Refresh Token 이 실리지 않는다 - 쿠키로만 나가는 게 이 방식의 핵심이다.
     */
    private ResponseEntity<TokenVO> withRefreshCookie(TokenVO response) {
        ResponseCookie cookie =
                refreshTokenCookie.create(response.getRefreshToken(), response.isKeep());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(response);
    }

    // 내 정보 (토큰이 아직 쓸 수 있는지 확인할 때도 쓴다)  GET /api/auth/me
    //
    // 여기서 하는 일이 없어 보이지만, 토큰을 풀고 DB 에서 회원을 읽고 탈퇴 · 잠김까지
    // 확인하는 일은 이미 JwtAuthenticationFilter 가 다 했다. @AuthenticationPrincipal
    // 은 그 결과를 꺼내 쓰는 것뿐이다. 토큰이 없거나 썩었으면 이 메서드에 들어오지도
    // 못하고 401 이 나간다 (SecurityConfig 의 anyRequest().authenticated()).
    @GetMapping("/me")
    public UserVO me(@AuthenticationPrincipal UserVO loginUser) {
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
