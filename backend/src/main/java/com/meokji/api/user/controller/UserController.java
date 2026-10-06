package com.meokji.api.user.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.common.MessageResponse;
import com.meokji.api.user.vo.NotificationItemVO;
import com.meokji.api.user.vo.PasswordChangeVO;
import com.meokji.api.user.vo.ProfileUpdateVO;
import com.meokji.api.user.vo.ProfileVO;
import com.meokji.api.user.vo.WithdrawVO;
import com.meokji.api.user.service.UserService;
import com.meokji.api.user.vo.UserVO;

import jakarta.validation.Valid;

import java.util.List;

/**
 * 내 정보 · 알림 · 회원 탈퇴.  주소: /api/users/me/...
 * 전부 로그인이 필요하다.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserService userService;

    // 내 정보  GET /api/users/me
    @GetMapping("/me")
    public ProfileVO getProfile(@AuthenticationPrincipal UserVO loginUser) {
        return userService.getProfile(loginUser);
    }

    // 내 정보 수정  PATCH /api/users/me
    @PatchMapping("/me")
    public ProfileVO updateProfile(
            @AuthenticationPrincipal UserVO loginUser,
            @Valid @RequestBody ProfileUpdateVO request) {
        return userService.updateProfile(loginUser, request);
    }

    // 비밀번호 변경  POST /api/users/me/password
    @PostMapping("/me/password")
    public MessageResponse changePassword(
            @AuthenticationPrincipal UserVO loginUser,
            @Valid @RequestBody PasswordChangeVO request) {
        userService.changePassword(loginUser, request);
        return new MessageResponse("비밀번호가 변경되었습니다.");
    }

    // 알림  GET /api/users/me/notifications
    @GetMapping("/me/notifications")
    public List<NotificationItemVO> getNotifications(
            @AuthenticationPrincipal UserVO loginUser) {
        return userService.getNotifications(loginUser);
    }

    // 회원 탈퇴  DELETE /api/users/me
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204 (돌려줄 내용 없음)
    public void withdraw(
            @AuthenticationPrincipal UserVO loginUser,
            @Valid @RequestBody WithdrawVO request) {
        userService.withdraw(loginUser, request.getPassword());
    }
}
