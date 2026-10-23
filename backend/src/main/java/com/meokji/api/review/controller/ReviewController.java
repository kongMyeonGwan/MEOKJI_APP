package com.meokji.api.review.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

import com.meokji.api.auth.service.AuthService;
import com.meokji.api.common.Page;
import com.meokji.api.common.Paging;
import com.meokji.api.review.dto.ReviewCommentRequest;
import com.meokji.api.review.dto.ReviewCommentResponse;
import com.meokji.api.review.dto.ReviewFileDownload;
import com.meokji.api.review.dto.ReviewFileResponse;
import com.meokji.api.review.dto.ReviewDetailResponse;
import com.meokji.api.review.dto.ReviewListResponse;
import com.meokji.api.review.dto.ReviewRequest;
import com.meokji.api.review.service.ReviewService;
import com.meokji.api.user.vo.UserVO;
import com.meokji.api.util.FileStore;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {

    @Autowired
    private AuthService authService;

    @Autowired
    private ReviewService reviewService;

    @Autowired
    private FileStore fileStore;

    @GetMapping
    public Page<ReviewListResponse> getList(
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(Paging.MAX_SIZE) int size) {
        return reviewService.getList(keyword, page, size);
    }

    @GetMapping("/{postId}")
    public ReviewDetailResponse getDetail(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable @Min(1) Long postId) {
        UserVO loginUser = authService.findLoginUser(token);
        return reviewService.getDetail(loginUser, postId);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewDetailResponse add(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody ReviewRequest request) {
        UserVO loginUser = authService.getLoginUser(token);
        return reviewService.add(loginUser, request);
    }

    @PutMapping("/{postId}")
    public ReviewDetailResponse update(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable @Min(1) Long postId,
            @Valid @RequestBody ReviewRequest request) {
        UserVO loginUser = authService.getLoginUser(token);
        return reviewService.update(loginUser, postId, request);
    }

    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable @Min(1) Long postId) {
        UserVO loginUser = authService.getLoginUser(token);
        reviewService.delete(loginUser, postId);
    }

    @PostMapping("/{postId}/view")
    public int increaseView(@PathVariable @Min(1) Long postId) {
        return reviewService.increaseView(postId);
    }

    @PostMapping("/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewCommentResponse addComment(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable @Min(1) Long postId,
            @Valid @RequestBody ReviewCommentRequest request) {
        UserVO loginUser = authService.getLoginUser(token);
        return reviewService.addComment(loginUser, postId, request);
    }

    @PutMapping("/{postId}/comments/{commentId}")
    public ReviewCommentResponse updateComment(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable @Min(1) Long postId,
            @PathVariable @Min(1) Long commentId,
            @Valid @RequestBody ReviewCommentRequest request) {
        UserVO loginUser = authService.getLoginUser(token);
        return reviewService.updateComment(loginUser, postId, commentId, request);
    }

    @DeleteMapping("/{postId}/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteComment(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable @Min(1) Long postId,
            @PathVariable @Min(1) Long commentId) {
        UserVO loginUser = authService.getLoginUser(token);
        reviewService.deleteComment(loginUser, postId, commentId);
    }

    @PostMapping("/{postId}/file")
    public ReviewFileResponse attachFile(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable @Min(1) Long postId,
            @RequestPart("file") MultipartFile file) {
        UserVO loginUser = authService.getLoginUser(token);
        return reviewService.attachFile(loginUser, postId, file);
    }

    @DeleteMapping("/{postId}/file")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void detachFile(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable @Min(1) Long postId) {
        UserVO loginUser = authService.getLoginUser(token);
        reviewService.detachFile(loginUser, postId);
    }

    @GetMapping("/{postId}/file")
    public ResponseEntity<byte[]> downloadFile(@PathVariable @Min(1) Long postId) {
        ReviewFileDownload file = reviewService.download(postId);

        String disposition = ContentDisposition.inline()
                .filename(file.name(), StandardCharsets.UTF_8)
                .build()
                .toString();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileStore.contentType(file.ext())))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .body(file.bytes());
    }
}
