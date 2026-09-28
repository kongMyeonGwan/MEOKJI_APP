package com.meokji.api.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.common.ApiException;
import com.meokji.api.common.Page;
import com.meokji.api.dto.notice.NoticeCreateRequest;
import com.meokji.api.dto.notice.NoticeResponse;
import com.meokji.api.dto.notice.NoticeUpdateRequest;
import com.meokji.api.dto.notice.NoticeViewResponse;
import com.meokji.api.security.LoginUser;
import com.meokji.api.vo.User;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 공지사항 — <b>아직 만들지 않은 기능이다. 담당자가 채운다.</b>
 *
 * <p>주소 · 요청/응답 모양 · 권한은 그대로 두었으니 메서드 안만 구현하면 된다.
 * 작성 · 수정 · 삭제는 관리자 전용이고, 그 판단은 {@code @LoginUser(admin = true)}
 * 가 한다 - 서비스에서 다시 볼 필요가 없다.
 */
@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    private static final String TODO = "아직 만들지 않은 기능입니다. (공지사항 담당자 작업 예정)";

    private static ApiException todo() {
        return ApiException.notImplemented(TODO);
    }

    /** 목록. 고정 공지가 먼저, 그 안에서는 최신순. 게스트도 볼 수 있다. */
    @GetMapping
    public Page<NoticeResponse> list(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(Page.MAX_SIZE) int size) {

        throw todo();
    }

    /** 조회수 +1. 화면이 공지를 펼칠 때 부른다. */
    @PostMapping("/{noticeId}/view")
    public NoticeViewResponse increaseView(@PathVariable @Min(1) Long noticeId) {
        throw todo();
    }

    /** 작성. 관리자만. */
    @PostMapping
    public ResponseEntity<NoticeResponse> create(
            @LoginUser(admin = true) User admin,
            @Valid @RequestBody NoticeCreateRequest request) {

        throw todo();
    }

    /** 수정. 관리자만. '내 공지' 라는 개념이 없어서 누가 썼는지는 보지 않는다. */
    @PutMapping("/{noticeId}")
    public NoticeResponse update(
            @LoginUser(admin = true) User admin,
            @PathVariable @Min(1) Long noticeId,
            @Valid @RequestBody NoticeUpdateRequest request) {

        throw todo();
    }

    /** 삭제. 관리자만. 행을 지우지 않고 use_yn='N'. */
    @DeleteMapping("/{noticeId}")
    public ResponseEntity<Void> delete(
            @LoginUser(admin = true) User admin,
            @PathVariable @Min(1) Long noticeId) {

        throw todo();
    }
}
