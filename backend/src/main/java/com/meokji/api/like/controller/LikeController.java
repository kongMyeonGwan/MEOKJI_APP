package com.meokji.api.like.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.like.service.LikeService;

/** 찜한 레시피.  주소: /api/likes */
@RestController
@RequestMapping("/api/likes")
public class LikeController {

    @Autowired
    private LikeService likeService;

    // 내 찜 목록  GET /api/likes
    @GetMapping
    public void getList() {
    }
}
