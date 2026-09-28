package com.meokji.api.user;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.common.MessageResponse;
import com.meokji.api.security.LoginUser;
import com.meokji.api.user.dto.NotificationItem;
import com.meokji.api.user.dto.PasswordChangeRequest;
import com.meokji.api.user.dto.ProfileResponse;
import com.meokji.api.user.dto.ProfileUpdateRequest;
import com.meokji.api.user.dto.WithdrawRequest;

import jakarta.validation.Valid;

/**
 * 내 정보 · 알림 · 회원 탈퇴.
 *
 * <p>파이썬 쪽 {@code app/routers/users.py} 자리다. 전부 로그인이 필요해서
 * 모든 메서드가 {@code @LoginUser} 를 받는다.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;


    /** 내 정보. 연속 방문일 · 요리 횟수는 DB 컬럼이 아니라 조회할 때 센다. */
    @GetMapping("/me")
    public ProfileResponse readProfile(@LoginUser User me) {
        return userService.getProfile(me);
    }

    /**
     * 내 정보 수정.
     *
     * <p>PUT 이 아니라 PATCH 다. 화면이 한 칸만 고치는 일이 많은데 PUT 이면
     * 나머지 칸을 전부 다시 보내야 하고, 빠뜨린 칸이 지워진다.
     */
    @PatchMapping("/me")
    public ProfileResponse editProfile(
            @LoginUser User me,
            @Valid @RequestBody ProfileUpdateRequest request) {

        return userService.updateProfile(me, request);
    }

    /** 비밀번호 변경. 잊어버린 경우는 /api/auth/password-reset 쪽을 쓴다. */
    @PostMapping("/me/password")
    public MessageResponse changePassword(
            @LoginUser User me,
            @Valid @RequestBody PasswordChangeRequest request) {

        userService.changePassword(me, request);

        return new MessageResponse("비밀번호가 변경되었습니다.");
    }

    /** 내 글의 댓글 · 좋아요. 알림 표는 없고 조회할 때 모아서 만든다. */
    @GetMapping("/me/notifications")
    public List<NotificationItem> readNotifications(@LoginUser User me) {
        return userService.listNotifications(me);
    }

    /**
     * 회원 탈퇴. 행을 지우지 않고 {@code use_yn='N'} 으로 바꾼다.
     *
     * <p>DELETE 인데 본문(비밀번호)을 받는다. 토큰만으로 지울 수 있게 두면 남의
     * 기기에서 열린 화면으로 탈퇴가 된다.
     */
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void withdraw(
            @LoginUser User me,
            @Valid @RequestBody WithdrawRequest request) {

        userService.withdraw(me, request.password());
    }
}
