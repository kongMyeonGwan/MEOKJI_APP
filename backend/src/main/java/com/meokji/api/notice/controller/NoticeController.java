package com.meokji.api.notice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.auth.service.AuthService;
import com.meokji.api.common.Page;
import com.meokji.api.common.Paging;
import com.meokji.api.notice.dto.NoticeDetailResponse;
import com.meokji.api.notice.dto.NoticeListResponse;
import com.meokji.api.notice.dto.NoticeRequest;
import com.meokji.api.notice.service.NoticeService;
import com.meokji.api.user.vo.UserVO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 공지사항.  주소: /api/notices
 *
 * 보기는 누구나, 쓰기 · 고치기 · 지우기는 관리자만.
 * 관리자 확인(로그인 + 관리자 여부)은 authService.getAdminUser 가 한다.
 */
@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    @Autowired
    private AuthService authService;

    @Autowired
    private NoticeService noticeService;

    // 목록 - 페이징
    // GET /api/notices?page=1&size=20
    @GetMapping
    public Page<NoticeListResponse> list(
            @RequestParam(name = "page", defaultValue = "1") @Min(1) int page,
            @RequestParam(name = "size", defaultValue = "20") @Min(1) @Max(Paging.MAX_SIZE) int size) {

        return noticeService.getList(page, size);
    }

    // 상세 보기 (조회수는 화면이 따로 /view 를 불러 올린다)
    // GET /api/notices/3
    @GetMapping("/{noticeId}")
    public NoticeDetailResponse detail(@PathVariable("noticeId") @Min(1) Long noticeId) {
        return noticeService.getDetail(noticeId);
    }

    // 조회수 +1 → 올린 뒤의 값을 돌려준다. 로그인하지 않아도 된다
    // POST /api/notices/3/view
    @PostMapping("/{noticeId}/view")
    public int increaseView(@PathVariable("noticeId") @Min(1) Long noticeId) {
        return noticeService.increaseView(noticeId);
    }

    // 작성 (관리자) → 201
    // POST /api/notices
    @PostMapping
    public ResponseEntity<NoticeDetailResponse> create(
            @RequestHeader(name = "Authorization", required = false) String token,
            @Valid @RequestBody NoticeRequest request) {

        UserVO admin = authService.getAdminUser(token); // 관리자가 아니면 여기서 401/403
        NoticeDetailResponse saved = noticeService.add(admin, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    // 수정 (관리자)
    // PUT /api/notices/3
    @PutMapping("/{noticeId}")
    public NoticeDetailResponse update(
            @PathVariable("noticeId") @Min(1) Long noticeId,
            @RequestHeader(name = "Authorization", required = false) String token,
            @Valid @RequestBody NoticeRequest request) {

        authService.getAdminUser(token);
        return noticeService.update(noticeId, request);
    }

    // 삭제 (관리자) → 204 No Content
    // DELETE /api/notices/3
    @DeleteMapping("/{noticeId}")
    public ResponseEntity<Void> delete(
            @PathVariable("noticeId") @Min(1) Long noticeId,
            @RequestHeader(name = "Authorization", required = false) String token) {

        authService.getAdminUser(token);
        noticeService.delete(noticeId);
        return ResponseEntity.noContent().build();
    }
}