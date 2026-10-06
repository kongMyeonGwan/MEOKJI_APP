package com.meokji.api.like.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.like.vo.LikeListVO;
import com.meokji.api.like.vo.RecipeLikeVO;
import com.meokji.api.auth.service.AuthService;
import com.meokji.api.like.service.LikeService;
import com.meokji.api.user.vo.UserVO;

import jakarta.validation.Valid;

/**
 * 찜한 레시피.  주소: /api/likes
 * 로그인이 필요하다.
 */
@RestController
@RequestMapping("/api/likes")
public class LikeController {

    @Autowired
    private AuthService authService;

    @Autowired
    private LikeService likeService;

    // 내 찜 목록  GET /api/likes
    @GetMapping
    public LikeListVO getList(@RequestHeader(value = "Authorization", required = false) String token) {
        UserVO loginUser = authService.getLoginUser(token);
        return likeService.getList(loginUser);
    }

    // 하트 누르기 (찜돼 있으면 풀고, 아니면 찜한다)  PUT /api/likes
    @PutMapping
    public RecipeLikeVO toggle(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody RecipeLikeVO request) {
        UserVO loginUser = authService.getLoginUser(token);
        return likeService.toggle(loginUser, request.getFoodName().trim());
    }

    // 찜 전부 지우기  DELETE /api/likes
    @DeleteMapping
    public LikeListVO clear(@RequestHeader(value = "Authorization", required = false) String token) {
        UserVO loginUser = authService.getLoginUser(token);
        return likeService.clear(loginUser);
    }
}
