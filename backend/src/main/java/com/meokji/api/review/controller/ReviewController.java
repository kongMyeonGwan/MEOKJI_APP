package com.meokji.api.review.controller;

import com.meokji.api.common.Page;
import com.meokji.api.common.Paging;
import com.meokji.api.review.dto.ReviewListResponse;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.review.service.ReviewService;

/** 커뮤니티 게시판 (레시피 후기).  주소: /api/reviews */
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    public ReviewController(ReviewService reviewService){
        this.reviewService = reviewService;
    }
    // 목록  GET /api/reviews
    @GetMapping
    public Page<ReviewListResponse> getList(
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(Paging.MAX_SIZE) int size) {
        return reviewService.getList(keyword, page, size);
    }
}
