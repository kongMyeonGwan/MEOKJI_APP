package com.meokji.api.user.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.common.MessageResponse;
import com.meokji.api.user.vo.NotificationItemVO;
import com.meokji.api.user.vo.PasswordChangeVO;
import com.meokji.api.user.vo.ProfileUpdateVO;
import com.meokji.api.user.vo.ProfileVO;
import com.meokji.api.user.vo.WithdrawVO;

import com.meokji.api.auth.service.AuthService;
import com.meokji.api.user.service.UserService;
import com.meokji.api.user.vo.UserVO;

import jakarta.validation.Valid;

/**
 * 내 정보 · 알림 · 회원 탈퇴.  주소: /api/users/me/...
 * 전부 로그인이 필요하다.
 *
 * <p>주소에 회원 번호가 들어가지 않는다. /api/users/3 으로 두면 번호만
 * 바꿔 남의 정보를 볼 수 있는 길이 생긴다. "me" 는 토큰이 가리키는
 * 사람이라 바꿔치기할 수 없다.
 *
 * <p>auth 기능과 주소를 나눈 기준은 "로그인 전인가 후인가" 다.
 * /api/auth/... 는 아직 로그인하지 않은 사람이 쓰고, 여기는 이미
 * 로그인한 사람이 쓴다. 표는 USERS 하나로 같다.
 *
 * <p>다섯 메서드가 모두 첫 줄에서 getLoginUser 를 부른다. findLoginUser
 * (없으면 null)를 쓰는 자리가 하나도 없다 - 로그인 없이 볼 것이 없는
 * 기능이라서다.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    // 내 정보  GET /api/users/me
    //
    // GET /api/auth/me 와 다르다. 저쪽은 "토큰이 아직 쓸 수 있는지" 를
    // 확인하는 자리라 UserVO 를 그대로 주고, 이쪽은 마이페이지에 띄울
    // 연속 기록일 · 요리 횟수까지 세어 ProfileVO 로 준다
    @GetMapping("/me")
    public ProfileVO getProfile(@RequestHeader(value = "Authorization", required = false) String token) {
        UserVO loginUser = authService.getLoginUser(token);
        return userService.getProfile(loginUser);
    }

    // 내 정보 수정  PATCH /api/users/me
    //
    // PUT 이 아니라 PATCH 다. 안 보낸 칸은 바꾸지 않는다 - 닉네임만
    // 고치는데 주소까지 같이 보내야 하면 번거롭고, 빠뜨리면 지워진다
    //
    // 고친 뒤의 ProfileVO 를 돌려준다. 화면이 받은 것으로 바로 다시
    // 그릴 수 있어서, 수정 후에 /me 를 또 부를 필요가 없다
    @PatchMapping("/me")
    public ProfileVO updateProfile(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody ProfileUpdateVO request) {
        UserVO loginUser = authService.getLoginUser(token);
        return userService.updateProfile(loginUser, request);
    }

    // 비밀번호 변경  POST /api/users/me/password
    //
    // PATCH /me 에 넣지 않고 주소를 따로 둔 이유 — 지금 비밀번호를
    // 한 번 더 확인해야 하고, 바꾸고 나면 다시 로그인해야 한다.
    // 닉네임을 고치는 일과 성격이 다르다.
    //
    // PUT 이 아니라 POST 인 이유 — "이 값으로 맞춰라" 가 아니라
    // "바꿔라" 는 동작이다. 같은 요청을 두 번 보내면 두 번째는
    // 지금 비밀번호가 틀려서 실패한다
    @PostMapping("/me/password")
    public MessageResponse changePassword(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody PasswordChangeVO request) {
        UserVO loginUser = authService.getLoginUser(token);
        userService.changePassword(loginUser, request);
        // 돌려줄 값이 없어서 문장만 보낸다. 204(빈 응답)로 두지 않은 이유는
        // 화면이 띄울 말을 서버가 정해 주려는 것이다
        return new MessageResponse("비밀번호가 변경되었습니다.");
    }

    // 알림  GET /api/users/me/notifications
    //
    // 페이징이 없다. 최근 몇 건만 보여주는 자리라 서비스가 개수를 정해
    // 잘라 온다. 전체를 넘겨볼 화면이 아직 없다
    //
    // 알림 표가 따로 없다. 댓글과 좋아요를 그때그때 읽어 만든다 -
    // 그래서 "읽음 표시" 도 없고, POST 로 읽음 처리할 주소도 없다
    @GetMapping("/me/notifications")
    public List<NotificationItemVO> getNotifications(
            @RequestHeader(value = "Authorization", required = false) String token) {
        UserVO loginUser = authService.getLoginUser(token);
        return userService.getNotifications(loginUser);
    }

    // 회원 탈퇴  DELETE /api/users/me
    //
    // DELETE 인데 본문(@RequestBody)을 받는다. 보통 DELETE 는 본문을
    // 안 쓰지만, 비밀번호를 한 번 더 확인해야 해서 받는다.
    // 주소줄(@RequestParam)로 받으면 비밀번호가 브라우저 기록과
    // 서버 접근 로그에 남는다
    //
    // 실제로는 행을 지우지 않는다. use_yn 을 N 으로 바꾼다 -
    // 지우면 그 사람이 쓴 글 · 댓글 · 좋아요가 가리킬 곳을 잃는다
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204 (돌려줄 내용 없음)
    public void withdraw(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody WithdrawVO request) {
        UserVO loginUser = authService.getLoginUser(token);
        userService.withdraw(loginUser, request.getPassword());
    }
}
