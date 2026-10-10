package com.meokji.api.review.controller;

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
import com.meokji.api.common.Paging;
import com.meokji.api.review.vo.ReviewCommentVO;
import com.meokji.api.review.vo.RecipeReviewVO;
import com.meokji.api.review.vo.ReviewFileVO;
import com.meokji.api.review.vo.ReviewLikeVO;
import com.meokji.api.auth.service.AuthService;
import com.meokji.api.review.service.ReviewService;
import com.meokji.api.user.vo.UserVO;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 커뮤니티 게시판 (레시피 후기).  주소: /api/reviews
 *
 * 보기는 누구나, 쓰기 · 좋아요 · 댓글은 로그인해야 한다.
 * 이 컨트롤러는 완성돼 있다. 실제 기능은 ReviewServiceImpl 에서 만든다. (지금은 501 을 돌려준다)
 *
 * <p>컨트롤러를 먼저 만들어 둔 이유 — 주소와 요청 · 응답 모양이 정해져 있으면
 * 화면 쪽 작업을 먼저 시작할 수 있다. 지금 부르면 501 이 오지만 주소는
 * 확정이고, 서비스가 채워지면 화면은 한 글자도 안 고친다.
 *
 * <p>자유게시판(FreePostController)과 거의 같은 모양이다. 다른 점이 셋이다
 *   /popular     인기 후기. 기간 안에 좋아요를 많이 받은 순
 *   /{id}/like   좋아요 켜기(PUT) · 끄기(DELETE)
 *   글을 쓰는 조건 — 내 식사 기록을 하나 골라야 쓸 수 있다
 *
 * <p>메서드가 열다섯 개다. 주석으로 네 묶음으로 나눠 두었다.
 *
 * <p>주소 짜임새
 *   /api/reviews                       글 목록 · 글쓰기
 *   /api/reviews/popular               인기 후기
 *   /api/reviews/{id}                  글 상세 · 수정 · 삭제
 *   /api/reviews/{id}/view             조회수
 *   /api/reviews/{id}/like             좋아요 켜기 · 끄기
 *   /api/reviews/{id}/comments         댓글 쓰기
 *   /api/reviews/{id}/comments/{cid}   댓글 수정 · 삭제
 *   /api/reviews/{id}/file             사진 붙이기 · 떼기 · 받기
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
    //
    // token 을 받지 않는다. 누구나 볼 수 있고, 목록에서는 "내 글인지" 를
    // 표시하지 않는다 (상세에서만 표시한다)
    //
    // keyword 에 @Size(max = 100) 를 거는 이유 — 검색어가 SQL 의 LIKE 에
    // 들어간다. 길이를 막지 않으면 아주 긴 글자로 DB 를 무겁게 만들 수 있다
    @GetMapping
    public Page<RecipeReviewVO> getList(
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(Paging.MAX_SIZE) int size) {
        return reviewService.getList(keyword, page, size);
    }

    // 인기 레시피  GET /api/reviews/popular?start=2026-09-01&end=2026-09-30&limit=20
    //
    // 목록과 주소를 나눈 이유 — 정렬 기준과 조건이 다르다. 목록은 최신순에
    // 검색어이고, 이쪽은 좋아요 순에 기간이다. ?sort=popular 로 한 주소에
    // 섞으면 어느 파라미터가 어느 때 쓰이는지가 흐려진다.
    //
    // Page 가 아니라 List 를 주는 이유 — 인기 목록은 "상위 몇 개" 라서
    // 2페이지가 없다. 전체 건수나 전체 페이지 수도 쓸 데가 없다.
    //
    // start · end 가 required = false 인 이유 — 안 보내면 서비스가 기본
    // 기간(이번 달 등)을 정한다. 화면이 날짜를 계산해 보내게 하면
    // 화면마다 기준이 달라진다.
    //
    // @DateTimeFormat 이 있어야 "2026-09-01" 글자를 LocalDate 로 바꿔준다.
    // 없으면 글자를 날짜로 못 읽어 400 이 난다
    @GetMapping("/popular")
    public List<RecipeReviewVO> getPopular(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate end,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        return reviewService.getPopular(start, end, limit);
    }

    // 상세  GET /api/reviews/3
    // 로그인했으면 "내가 좋아요를 눌렀는지", "내 글인지" 도 알려준다
    //
    // getLoginUser 가 아니라 findLoginUser 다. 후기는 로그인 없이도 봐야
    // 하므로 토큰이 없을 때 에러를 내면 안 된다
    @GetMapping("/{reviewId}")
    public RecipeReviewVO getDetail(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable @Min(1) Long reviewId) {
        UserVO loginUser = authService.findLoginUser(token); // 로그인 안 했으면 null
        return reviewService.getDetail(loginUser, reviewId);
    }

    // 글쓰기  POST /api/reviews
    //
    // 자유게시판과 달리 조건이 있다 — 요청 본문에 내 식사 기록 번호를
    // 담아야 한다. "실제로 먹어본 사람의 후기" 로 두려는 것이다.
    // 그 확인은 서비스가 한다 (남의 기록이면 막아야 하므로)
    //
    // 사진은 같이 안 받는다. 글을 먼저 만들고 그 번호로 /file 에 올린다
    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED) // 201
    public RecipeReviewVO add(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody RecipeReviewVO request) {
        UserVO loginUser = authService.getLoginUser(token);
        return reviewService.add(loginUser, request);
    }

    // 글 수정  PUT /api/reviews/3
    //
    // loginUser 를 넘기는 이유 — 글쓴이 본인인지 확인해야 한다.
    // 컨트롤러에서 확인하지 않는 것은 글을 읽어봐야 알 수 있는 판단이라서다
    @PutMapping("/{reviewId}")
    public RecipeReviewVO update(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable @Min(1) Long reviewId,
            @Valid @RequestBody RecipeReviewVO request) {
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
    //
    // GET 이 아니라 POST 인 이유 — 값을 바꾸는 요청이다. GET 으로 두면
    // 브라우저나 중간 서버가 미리 불러서 조회수가 멋대로 오른다
    //
    // 상세를 읽을 때 같이 올리지 않는 이유 — 글쓴이가 고치려고 열어도
    // 조회수가 오르고, 화면이 다시 그려질 때마다 또 오른다
    @PostMapping("/{reviewId}/view")
    public int increaseView(@PathVariable @Min(1) Long reviewId) {
        return reviewService.increaseView(reviewId);
    }

    // ===================== 좋아요 =====================

    // 좋아요 켜기  PUT /api/reviews/3/like
    //
    // 찜(PUT /api/likes)과 달리 켜기와 끄기를 주소로 나눴다.
    //
    // 왜 다른가 — 찜은 하트 하나를 누르는 것이라 "뒤집기" 가 자연스럽다.
    // 좋아요도 화면에서는 같아 보이지만, 켜기와 끄기를 나누면 여러 번
    // 보내도 결과가 같다. 뒤집기는 같은 요청이 두 번 가면 원래대로 돌아간다 -
    // 느린 연결에서 사용자가 두 번 누르면 꺼진 채로 끝난다.
    //
    // 요청 본문이 없다. 누가(토큰) 어느 글에(주소) 무엇을(메서드) 하는지가
    // 이미 다 드러나 있다
    @PutMapping("/{reviewId}/like")
    public ReviewLikeVO like(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable @Min(1) Long reviewId) {
        UserVO loginUser = authService.getLoginUser(token);
        // 바뀐 뒤의 상태(liked)와 이 글의 좋아요 수를 돌려준다.
        // 화면은 그 값으로 하트와 숫자를 한 번에 맞춘다
        return reviewService.setLike(loginUser, reviewId, true);
    }

    // 좋아요 끄기  DELETE /api/reviews/3/like
    //
    // 204 가 아니라 200 + 상태를 주는 이유 — 끈 뒤의 좋아요 수를 화면에
    // 바로 반영해야 한다. 화면이 1 을 빼서 맞추면 다른 사람이 같은
    // 시간에 누른 것과 어긋난다
    @DeleteMapping("/{reviewId}/like")
    public ReviewLikeVO unlike(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable @Min(1) Long reviewId) {
        UserVO loginUser = authService.getLoginUser(token);
        // 위 like 와 같은 메서드를 부르고 마지막 값만 다르다.
        // 서비스에 setLike 하나만 두면 켜기 · 끄기의 확인 절차가 한 곳에 모인다
        return reviewService.setLike(loginUser, reviewId, false);
    }

    // ===================== 댓글 =====================

    // 댓글 쓰기  POST /api/reviews/3/comments
    //
    // request.getContent() 만 꺼내 넘기는 이유 — ReviewCommentVO 에는
    // id · 글번호 · 닉네임 · 작성일까지 들어 있는데, 화면이 보내도 되는
    // 칸은 content 하나뿐이다
    @PostMapping("/{reviewId}/comments")
    @ResponseStatus(HttpStatus.CREATED) // 201
    public ReviewCommentVO addComment(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable @Min(1) Long reviewId,
            @Valid @RequestBody ReviewCommentVO request) {
        UserVO loginUser = authService.getLoginUser(token);
        return reviewService.addComment(loginUser, reviewId, request.getContent());
    }

    // 댓글 수정  PUT /api/reviews/3/comments/7
    //
    // reviewId 와 commentId 를 둘 다 받는다. 서비스가 "그 댓글이 정말
    // 그 글의 댓글인지" 를 확인한다 - 주소와 실제가 다르면 404 다
    @PutMapping("/{reviewId}/comments/{commentId}")
    public ReviewCommentVO updateComment(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable @Min(1) Long reviewId,
            @PathVariable @Min(1) Long commentId,
            @Valid @RequestBody ReviewCommentVO request) {
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
    //
    // @RequestBody 가 아니라 @RequestPart 다. 사진은 JSON 으로 보낼 수 없어
    // multipart/form-data 로 온다 - 그 안의 "file" 칸을 꺼내는 표시다
    //
    // 글 하나에 사진 하나다. 또 올리면 앞의 것이 바뀐다.
    //
    // 201 이 아니라 200 인 이유 — 새 자원을 만드는 것이 아니라 이미 있는
    // 글에 사진을 붙이는 것이다. 주소도 안 바뀐다
    @PostMapping("/{reviewId}/file")
    public ReviewFileVO attachFile(
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
    //
    // 업로드 폴더를 그대로 열어두지 않고 이 주소를 거치는 이유 — 폴더를
    // 열면 지워진 글의 사진도 주소만 알면 계속 보인다. 이 주소를 지나면
    // 서비스가 글이 살아있는지 확인한 뒤에 읽는다.
    //
    // 로그인을 요구하지 않는다. 글이 누구나 볼 수 있으니 거기 붙은 사진도
    // 누구나 볼 수 있어야 한다.
    //
    // 자유게시판과 달리 ResponseEntity 가 아니라 byte[] 를 그대로 돌려준다.
    // 그래서 Content-Type 과 원래 파일명(Content-Disposition)이 안 붙는다 -
    // 브라우저가 무슨 그림인지 몰라 <img> 에 못 그릴 수 있다.
    // 구현할 때 자유게시판의 downloadFile 처럼 헤더를 붙이는 쪽으로
    // 바꿔야 할 자리다
    @GetMapping("/{reviewId}/file")
    public byte[] downloadFile(@PathVariable @Min(1) Long reviewId) {
        return reviewService.downloadFile(reviewId);
    }
}
