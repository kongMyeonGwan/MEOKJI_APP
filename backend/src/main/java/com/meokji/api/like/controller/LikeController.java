package com.meokji.api.like.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.like.vo.LikeListVO;
import com.meokji.api.like.vo.RecipeLikeVO;
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
    private LikeService likeService;

    // 내 찜 목록  GET /api/likes
    @GetMapping
    public LikeListVO getList(@AuthenticationPrincipal UserVO loginUser) {
        return likeService.getList(loginUser);
    }

    // 하트 누르기 (찜돼 있으면 풀고, 아니면 찜한다)  PUT /api/likes
    @PutMapping
    public RecipeLikeVO toggle(
            @AuthenticationPrincipal UserVO loginUser,
            @Valid @RequestBody RecipeLikeVO request) {
        return likeService.toggle(loginUser, request.getFoodName().trim());
    }

    // 찜 전부 지우기  DELETE /api/likes
    @DeleteMapping
    public LikeListVO clear(@AuthenticationPrincipal UserVO loginUser) {
        return likeService.clear(loginUser);
    }
}
