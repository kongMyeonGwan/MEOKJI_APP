package com.meokji.api.ban.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.auth.service.AuthService;
import com.meokji.api.ban.service.BanService;
import com.meokji.api.ban.vo.UserBanVO;
import com.meokji.api.user.vo.UserVO;

import jakarta.validation.Valid;

/**
 * 못 먹는 재료.  주소: /api/bans
 * 여기 등록한 재료는 AI 추천과 레시피에서 빠진다. 로그인이 필요하다.
 */
@RestController
@RequestMapping("/api/bans")
public class BanController {

    @Autowired
    private AuthService authService;

    @Autowired
    private BanService banService;

    // 내 목록  GET /api/bans
    @GetMapping
    public List<UserBanVO> getList(@RequestHeader(value = "Authorization", required = false) String token) {
        UserVO loginUser = authService.getLoginUser(token);
        return banService.getList(loginUser);
    }

    // 추가  POST /api/bans
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // 201
    public UserBanVO add(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody UserBanVO request) {
        UserVO loginUser = authService.getLoginUser(token);
        return banService.add(loginUser, request);
    }

    // 삭제  DELETE /api/bans/3
    @DeleteMapping("/{banId}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204
    public void delete(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable Long banId) {
        UserVO loginUser = authService.getLoginUser(token);
        banService.delete(loginUser, banId);
    }
}
