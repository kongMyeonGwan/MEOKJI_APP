package com.meokji.api.review.controller;

import com.meokji.api.auth.service.AuthService;
import com.meokji.api.util.FileStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.review.service.ReviewService;

/** 커뮤니티 게시판 (레시피 후기).  주소: /api/reviews */
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private FileStore fileStore;

    @Autowired
    private AuthService authService;

    @Autowired
    private ReviewService reviewService;

    // 목록  GET /api/reviews
    @GetMapping
    public void getList() {
    }

    // 목록  GET /api/reviews
    @PostMapping
    public void WriteReviews() {
    }
}
