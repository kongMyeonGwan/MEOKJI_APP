package com.meokji.api.controller;

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
import com.meokji.api.dto.user.NotificationItem;
import com.meokji.api.dto.user.PasswordChangeRequest;
import com.meokji.api.dto.user.ProfileResponse;
import com.meokji.api.dto.user.ProfileUpdateRequest;
import com.meokji.api.dto.user.WithdrawRequest;
import com.meokji.api.service.AuthService;
import com.meokji.api.service.UserService;
import com.meokji.api.vo.UserVO;

import jakarta.validation.Valid;

/**
 * 내 정보 · 알림 · 회원 탈퇴.  주소: /api/users/me/...
 * 전부 로그인이 필요하다.
 */
@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserService userService;

    // 내 정보  GET /api/users/me
    @GetMapping("/me")
    public ProfileResponse getProfile(@RequestHeader(value = "Authorization", required = false) String token) {
        UserVO loginUser = authService.getLoginUser(token);
        return userService.getProfile(loginUser);
    }

    // 내 정보 수정  PATCH /api/users/me
    @PatchMapping("/me")
    public ProfileResponse updateProfile(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody ProfileUpdateRequest request) {
        UserVO loginUser = authService.getLoginUser(token);
        return userService.updateProfile(loginUser, request);
    }

    // 비밀번호 변경  POST /api/users/me/password
    @PostMapping("/me/password")
    public MessageResponse changePassword(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody PasswordChangeRequest request) {
        UserVO loginUser = authService.getLoginUser(token);
        userService.changePassword(loginUser, request);
        return new MessageResponse("비밀번호가 변경되었습니다.");
    }

    // 알림  GET /api/users/me/notifications
    @GetMapping("/me/notifications")
    public List<NotificationItem> getNotifications(
            @RequestHeader(value = "Authorization", required = false) String token) {
        UserVO loginUser = authService.getLoginUser(token);
        return userService.getNotifications(loginUser);
    }

    // 회원 탈퇴  DELETE /api/users/me
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204 (돌려줄 내용 없음)
    public void withdraw(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody WithdrawRequest request) {
        UserVO loginUser = authService.getLoginUser(token);
        userService.withdraw(loginUser, request.getPassword());
    }
}
