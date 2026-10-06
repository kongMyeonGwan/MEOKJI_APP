package com.meokji.api.notice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.common.Page;
import com.meokji.api.common.Paging;
import com.meokji.api.notice.vo.NoticeVO;
import com.meokji.api.notice.service.NoticeService;
import com.meokji.api.user.vo.UserVO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 공지사항.  주소: /api/notices
 *
 * 보기는 누구나, 쓰기 · 고치기 · 지우기는 관리자만.
 * 이 컨트롤러는 완성돼 있다. 실제 기능은 NoticeServiceImpl 에서 만든다. (지금은 501 을 돌려준다)
 *
 * 관리자인지는 여기서 안 본다. SecurityConfig 가 주소별로 hasRole("ADMIN") 을 걸어두고
 * 있어서, 일반 회원이 쓰기 주소를 부르면 이 메서드에 들어오지도 못하고 403 이 나간다.
 */
@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    @Autowired
    private NoticeService noticeService;

    // 목록  GET /api/notices?page=1&size=20
    @GetMapping
    public Page<NoticeVO> getList(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(Paging.MAX_SIZE) int size) {
        return noticeService.getList(page, size);
    }

    // 상세  GET /api/notices/3
    // 보기는 누구나 된다. 조회수는 화면이 따로 /view 를 불러 올린다
    @GetMapping("/{noticeId}")
    public NoticeVO getDetail(@PathVariable @Min(1) Long noticeId) {
        return noticeService.getDetail(noticeId);
    }

    // 조회수 +1  POST /api/notices/3/view
    @PostMapping("/{noticeId}/view")
    public int increaseView(@PathVariable @Min(1) Long noticeId) {
        return noticeService.increaseView(noticeId);
    }

    // 작성 (관리자)  POST /api/notices
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // 201
    public NoticeVO add(
            @AuthenticationPrincipal UserVO loginUser,
            @Valid @RequestBody NoticeVO request) {
        return noticeService.add(loginUser, request);
    }

    // 수정 (관리자)  PUT /api/notices/3
    @PutMapping("/{noticeId}")
    public NoticeVO update(
            @PathVariable @Min(1) Long noticeId,
            @Valid @RequestBody NoticeVO request) {
        return noticeService.update(noticeId, request);
    }

    // 삭제 (관리자)  DELETE /api/notices/3
    @DeleteMapping("/{noticeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204
    public void delete(
            @PathVariable @Min(1) Long noticeId) {
        noticeService.delete(noticeId);
    }
}
