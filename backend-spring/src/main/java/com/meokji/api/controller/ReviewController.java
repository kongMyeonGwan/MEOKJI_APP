package com.meokji.api.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.meokji.api.common.Page;
import com.meokji.api.dto.review.CommentCreateRequest;
import com.meokji.api.dto.review.CommentResponse;
import com.meokji.api.dto.review.LikeResponse;
import com.meokji.api.dto.review.PopularItem;
import com.meokji.api.dto.review.ReviewCreateRequest;
import com.meokji.api.dto.review.ReviewDetailResponse;
import com.meokji.api.dto.review.ReviewFileInfo;
import com.meokji.api.dto.review.ReviewListItem;
import com.meokji.api.dto.review.ReviewUpdateRequest;
import com.meokji.api.service.AuthService;
import com.meokji.api.service.ReviewService;
import com.meokji.api.vo.UserVO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 커뮤니티 게시판 (레시피 후기).  주소: /api/reviews
 *
 * 보기는 누구나, 쓰기 · 좋아요 · 댓글은 로그인해야 한다.
 * 이 컨트롤러는 완성돼 있다. 실제 기능은 ReviewServiceImpl 에서 만든다. (지금은 501 을 돌려준다)
 */
@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private AuthService authService;

    @Autowired
    private ReviewService reviewService;

    // ===================== 글 =====================

    // 목록  GET /api/reviews?keyword=김치&page=1&size=20
    @GetMapping
    public Page<ReviewListItem> getList(
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(Page.MAX_SIZE) int size) {
        return reviewService.getList(keyword, page, size);
    }

    // 인기 레시피  GET /api/reviews/popular?start=2026-09-01&end=2026-09-30&limit=20
    @GetMapping("/popular")
    public List<PopularItem> getPopular(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return reviewService.getPopular(start, end, limit);
    }

    // 상세  GET /api/reviews/3
    // 로그인했으면 "내가 좋아요를 눌렀는지", "내 글인지" 도 알려준다
    @GetMapping("/{reviewId}")
    public ReviewDetailResponse getDetail(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable @Min(1) Long reviewId) {
        UserVO loginUser = authService.findLoginUser(token); // 로그인 안 했으면 null
        return reviewService.getDetail(loginUser, reviewId);
    }

    // 글쓰기  POST /api/reviews
    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED) // 201
    public ReviewDetailResponse add(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody ReviewCreateRequest request) {
        UserVO loginUser = authService.getLoginUser(token);
        return reviewService.add(loginUser, request);
    }

    // 글 수정  PUT /api/reviews/3
    @PutMapping("/{reviewId}")
    public ReviewDetailResponse update(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable @Min(1) Long reviewId,
            @Valid @RequestBody ReviewUpdateRequest request) {
        UserVO loginUser = authService.getLoginUser(token);
        return reviewService.update(loginUser, reviewId, request);
    }

    // 글 삭제  DELETE /api/reviews/3
    @DeleteMapping("/{reviewId}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204
    public void delete(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable @Min(1) Long reviewId) {
        UserVO loginUser = authService.getLoginUser(token);
        reviewService.delete(loginUser, reviewId);
    }

    // 조회수 +1  POST /api/reviews/3/view
    @PostMapping("/{reviewId}/view")
    public int increaseView(@PathVariable @Min(1) Long reviewId) {
        return reviewService.increaseView(reviewId);
    }

    // ===================== 좋아요 =====================

    // 좋아요 켜기  PUT /api/reviews/3/like
    @PutMapping("/{reviewId}/like")
    public LikeResponse like(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable @Min(1) Long reviewId) {
        UserVO loginUser = authService.getLoginUser(token);
        return reviewService.setLike(loginUser, reviewId, true);
    }

    // 좋아요 끄기  DELETE /api/reviews/3/like
    @DeleteMapping("/{reviewId}/like")
    public LikeResponse unlike(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable @Min(1) Long reviewId) {
        UserVO loginUser = authService.getLoginUser(token);
        return reviewService.setLike(loginUser, reviewId, false);
    }

    // ===================== 댓글 =====================

    // 댓글 쓰기  POST /api/reviews/3/comments
    @PostMapping("/{reviewId}/comments")
    @ResponseStatus(HttpStatus.CREATED) // 201
    public CommentResponse addComment(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable @Min(1) Long reviewId,
            @Valid @RequestBody CommentCreateRequest request) {
        UserVO loginUser = authService.getLoginUser(token);
        return reviewService.addComment(loginUser, reviewId, request.getContent());
    }

    // 댓글 수정  PUT /api/reviews/3/comments/7
    @PutMapping("/{reviewId}/comments/{commentId}")
    public CommentResponse updateComment(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable @Min(1) Long reviewId,
            @PathVariable @Min(1) Long commentId,
            @Valid @RequestBody CommentCreateRequest request) {
        UserVO loginUser = authService.getLoginUser(token);
        return reviewService.updateComment(loginUser, reviewId, commentId, request.getContent());
    }

    // 댓글 삭제  DELETE /api/reviews/3/comments/7
    @DeleteMapping("/{reviewId}/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204
    public void deleteComment(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable @Min(1) Long reviewId,
            @PathVariable @Min(1) Long commentId) {
        UserVO loginUser = authService.getLoginUser(token);
        reviewService.deleteComment(loginUser, reviewId, commentId);
    }

    // ===================== 사진 =====================

    // 사진 붙이기  POST /api/reviews/3/file  (form-data 의 file 칸)
    @PostMapping("/{reviewId}/file")
    public ReviewFileInfo attachFile(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable @Min(1) Long reviewId,
            @RequestPart("file") MultipartFile file) {
        UserVO loginUser = authService.getLoginUser(token);
        return reviewService.attachFile(loginUser, reviewId, file);
    }

    // 사진 떼기  DELETE /api/reviews/3/file
    @DeleteMapping("/{reviewId}/file")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204
    public void detachFile(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable @Min(1) Long reviewId) {
        UserVO loginUser = authService.getLoginUser(token);
        reviewService.detachFile(loginUser, reviewId);
    }

    // 사진 받기  GET /api/reviews/3/file
    @GetMapping("/{reviewId}/file")
    public byte[] downloadFile(@PathVariable @Min(1) Long reviewId) {
        return reviewService.downloadFile(reviewId);
    }
}
