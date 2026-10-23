package com.meokji.api.review.service;

import com.meokji.api.common.Page;
import com.meokji.api.common.Paging;
import com.meokji.api.review.dto.ReviewListResponse;
import com.meokji.api.review.repository.ReviewListView;
import com.meokji.api.review.repository.ReviewRepository;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ReviewService {

    // DB 조회를 위한 Repository
    private final ReviewRepository reviewRepository;

    // 생성자로 Repository 주입
    public ReviewService(ReviewRepository reviewRepository) {
        this.reviewRepository = reviewRepository;

    }

    // 리뷰 목록 조회
    public Page<ReviewListResponse> getList(String keyword, int page, int size) {
        // 빈 검색어는 "검색 안 함" 이다. SQL 이 null 만 그렇게 본다
        String safeKeyword = (keyword == null || keyword.isBlank()) ? null : keyword;

        // offset 과 total_pages 계산은 Paging 이 한다 (common/Paging.java)
        Paging paging = new Paging(page, size);
        paging.setTotal(reviewRepository.countAlive(safeKeyword));

        List<ReviewListResponse> items = new ArrayList<>();
        for (ReviewListView row : reviewRepository.findPage(safeKeyword, paging.getOffset(), paging.getSize())) {
            items.add(new ReviewListResponse(row));
        }

        Page<ReviewListResponse> result = new Page<>();
        result.Page(paging, items);
        return result;
    }
} //ReviewService