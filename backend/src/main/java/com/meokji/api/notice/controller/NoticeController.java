package com.meokji.api.notice.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.meokji.api.notice.service.NoticeService;

<<<<<<< HEAD
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 공지사항.  주소: /api/notices
 *
 * 보기는 누구나, 쓰기 · 고치기 · 지우기는 관리자만.
 * 이 컨트롤러는 완성돼 있다. 실제 기능은 NoticeServiceImpl 에서 만든다. (지금은 501 을 돌려준다)
 *
 * <p>컨트롤러를 먼저 만들어 둔 이유
 *
 * <p>주소와 요청 · 응답 모양이 정해져 있으면 화면 쪽 작업을 먼저 시작할 수 있다.
 * 지금 부르면 501 이 오지만, 어느 주소로 무엇을 보내야 하는지는 확정이다.
 * 서비스 쪽이 채워지면 화면은 한 글자도 안 고친다.
 *
 * <p>목록 · 상세는 로그인이 필요 없고, 쓰기 쪽 셋은 getAdminUser 를 거친다.
 * 공지는 모두가 봐야 하는 글이라 로그인 전에도 보여야 하고, 쓰는 것은
 * 운영자뿐이다.
 */
=======
/** 공지사항.  주소: /api/notices */
>>>>>>> 137ab2e5d83abad60912fb1f0b6b8a1315e728eb
@RestController
@RequestMapping("/api/notices")
public class NoticeController {

    @Autowired
    private NoticeService noticeService;

<<<<<<< HEAD
    // 목록  GET /api/notices?page=1&size=20
    //
    // token 을 받지 않는다. 누구나 볼 수 있어서 토큰을 볼 이유가 없다
    //
    // Paging.MAX_SIZE 를 쓰는 이유 — 50 을 손으로 적으면 Paging 의 한계와
    // 어긋날 수 있다. 어긋나면 size=60 이 여기서 통과하고 Paging 이 조용히
    // 50 으로 줄여서, 화면은 60개를 기대하는데 50개만 온다
    @GetMapping
    public Page<NoticeVO> getList(
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(Paging.MAX_SIZE) int size) {
        return noticeService.getList(page, size);
    }

    // 상세  GET /api/notices/3
    // 보기는 누구나 된다. 조회수는 화면이 따로 /view 를 불러 올린다
    //
    // 상세를 읽을 때 같이 올리지 않는 이유 — 관리자가 글을 고치려고 열어도
    // 조회수가 오르고, 화면이 다시 그려질 때마다 또 오른다. 올리는 시점을
    // 화면이 정할 수 있게 주소를 나눴다
    @GetMapping("/{noticeId}")
    public NoticeVO getDetail(@PathVariable @Min(1) Long noticeId) {
        return noticeService.getDetail(noticeId);
    }

    // 조회수 +1  POST /api/notices/3/view
    //
    // GET 이 아니라 POST 다. 값을 바꾸는 요청이라서다 - GET 으로 두면
    // 브라우저나 중간 서버가 캐시하거나 미리 불러서 조회수가 멋대로 오른다
    //
    // 올린 뒤의 숫자를 돌려준다. 화면이 1 을 더해 맞추면 다른 사람이
    // 동시에 본 것과 어긋난다
    @PostMapping("/{noticeId}/view")
    public int increaseView(@PathVariable @Min(1) Long noticeId) {
        return noticeService.increaseView(noticeId);
    }

    // 작성 (관리자)  POST /api/notices
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // 201
    public NoticeVO add(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody NoticeVO request) {
        UserVO admin = authService.getAdminUser(token); // 관리자가 아니면 여기서 403
        // 작성자를 남기려고 admin 을 넘긴다. 아래 수정 · 삭제는 작성자를
        // 바꾸지 않으므로 넘기지 않는다
        return noticeService.add(admin, request);
    }

    // 수정 (관리자)  PUT /api/notices/3
    //
    // getAdminUser 의 결과를 변수에 받지 않는다. 확인만 하면 되고,
    // 수정은 작성자를 바꾸지 않기 때문이다. 부르는 것 자체가 검사다 -
    // 관리자가 아니면 여기서 403 이 던져지고 아래로 내려오지 않는다
    @PutMapping("/{noticeId}")
    public NoticeVO update(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable @Min(1) Long noticeId,
            @Valid @RequestBody NoticeVO request) {
        authService.getAdminUser(token);
        return noticeService.update(noticeId, request);
    }

    // 삭제 (관리자)  DELETE /api/notices/3
    //
    // 204(내용 없음). 지운 뒤에 돌려줄 값이 없다
    @DeleteMapping("/{noticeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204
    public void delete(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable @Min(1) Long noticeId) {
        authService.getAdminUser(token);
        noticeService.delete(noticeId);
=======
    // 목록  GET /api/notices
    @GetMapping
    public void getList() {
>>>>>>> 137ab2e5d83abad60912fb1f0b6b8a1315e728eb
    }
}
