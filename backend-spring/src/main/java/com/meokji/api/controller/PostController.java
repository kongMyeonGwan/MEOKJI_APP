package com.meokji.api.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.meokji.api.common.ApiException;
import com.meokji.api.common.Page;
import com.meokji.api.dto.post.CommentCreateRequest;
import com.meokji.api.dto.post.CommentResponse;
import com.meokji.api.dto.post.LikeResponse;
import com.meokji.api.dto.post.PopularItem;
import com.meokji.api.dto.post.PostCreateRequest;
import com.meokji.api.dto.post.PostDetailResponse;
import com.meokji.api.dto.post.PostFileInfo;
import com.meokji.api.dto.post.PostListItem;
import com.meokji.api.dto.post.PostUpdateRequest;
import com.meokji.api.security.LoginUser;
import com.meokji.api.service.PostService;
import com.meokji.api.vo.User;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 게시판(레시피 후기) — <b>아직 만들지 않은 기능이다. 담당자가 채운다.</b>
 *
 * <p>주소 · 요청/응답 모양 · 로그인 필요 여부는 그대로 두었으니 메서드 안만
 * 구현하면 된다. 화면({@code frontend/src/data/sampleBoard.js})은 지금 하드코딩한
 * 가데이터로 그려지고 있고, 여기가 완성되면 그쪽도 API 로 바꾸면 된다.
 *
 * <p>404 나 500 이 아니라 <b>501</b> 을 돌려주는 이유가 있다. 404 면 "주소가
 * 틀렸나" 가 되고 500 이면 "내가 뭘 잘못했나" 가 된다. 501 은 "안 만들었다" 는
 * 뜻이라 눌러본 사람에게 그대로 읽힌다.
 *
 * <p>자세한 사정은 {@code docs/PROGRESS.md} 의 「게시판 · 공지를 담당자에게 넘김」
 * 절과 {@link PostService} 의 주석을 참고한다.
 */
@RestController
@RequestMapping("/api/posts")
public class PostController {

    /** 아직 구현하지 않은 엔드포인트가 내려주는 문구. 파이썬 쪽과 같게 맞춘다. */
    private static final String TODO = "아직 만들지 않은 기능입니다. (게시판 담당자 작업 예정)";

    private static ApiException todo() {
        return ApiException.notImplemented(TODO);
    }

    /** 목록 · 검색 · 페이징. 게스트도 볼 수 있다. */
    @GetMapping
    public Page<PostListItem> list(
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(Page.MAX_SIZE) int size) {

        throw todo();
    }

    /**
     * 기간 내 좋아요 순위.
     *
     * <p>{@code start} · {@code end} 는 <b>한국 날짜</b>다. 조회할 때 UTC 로 바꿔야
     * 한다({@link com.meokji.api.common.Kst}). 안 맞추면 새벽에 눌린 좋아요가 전날
     * 것으로 세어진다.
     */
    @GetMapping("/popular")
    public List<PopularItem> popular(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate start,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate end,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {

        throw todo();
    }

    /**
     * 상세 + 댓글.
     *
     * <p>로그인해도 되고 안 해도 된다. 로그인했으면 {@code liked} · {@code isMine} 이
     * 그 사람 기준으로 채워진다.
     */
    @GetMapping("/{postId}")
    public PostDetailResponse detail(
            @PathVariable @Min(1) Long postId,
            @LoginUser(required = false) User me) {

        throw todo();
    }

    /**
     * 글쓰기.
     *
     * <p><b>자기 식사 기록만 공유할 수 있다.</b> {@code mealHistoryId} 의 주인이
     * 나인지 확인해야 한다 - FK 가 막아주지만 그때는 500 이 나간다.
     */
    @PostMapping("")
    public ResponseEntity<PostDetailResponse> create(
            @LoginUser User me,
            @Valid @RequestBody PostCreateRequest request) {

        throw todo();
    }

    /** 글 수정. 남의 글이면 403. */
    @PutMapping("/{postId}")
    public PostDetailResponse update(
            @LoginUser User me,
            @PathVariable @Min(1) Long postId,
            @Valid @RequestBody PostUpdateRequest request) {

        throw todo();
    }

    /** 글 삭제. 행을 지우지 않고 {@code use_yn='N'}. */
    @DeleteMapping("/{postId}")
    public ResponseEntity<Void> delete(
            @LoginUser User me,
            @PathVariable @Min(1) Long postId) {

        throw todo();
    }

    /**
     * 조회수 +1.
     *
     * <p>상세 조회에서 같이 올리지 않고 따로 뺐다. 화면이 새로 그릴 때마다 오르면
     * 숫자가 의미를 잃는다.
     */
    @PostMapping("/{postId}/view")
    public int increaseView(@PathVariable @Min(1) Long postId) {
        throw todo();
    }

    /** 좋아요 켜기. 이미 눌렀으면 그대로 둔다(멱등). */
    @PutMapping("/{postId}/like")
    public LikeResponse like(@LoginUser User me, @PathVariable @Min(1) Long postId) {
        throw todo();
    }

    /** 좋아요 끄기. 행을 지우지 않고 {@code use_yn='N'}. */
    @DeleteMapping("/{postId}/like")
    public LikeResponse unlike(@LoginUser User me, @PathVariable @Min(1) Long postId) {
        throw todo();
    }

    /** 댓글 쓰기. */
    @PostMapping("/{postId}/comments")
    public ResponseEntity<CommentResponse> createComment(
            @LoginUser User me,
            @PathVariable @Min(1) Long postId,
            @Valid @RequestBody CommentCreateRequest request) {

        throw todo();
    }

    /** 댓글 수정. 남의 댓글이면 403. */
    @PutMapping("/{postId}/comments/{commentId}")
    public CommentResponse updateComment(
            @LoginUser User me,
            @PathVariable @Min(1) Long postId,
            @PathVariable @Min(1) Long commentId,
            @Valid @RequestBody CommentCreateRequest request) {

        throw todo();
    }

    /** 댓글 삭제. */
    @DeleteMapping("/{postId}/comments/{commentId}")
    public ResponseEntity<Void> deleteComment(
            @LoginUser User me,
            @PathVariable @Min(1) Long postId,
            @PathVariable @Min(1) Long commentId) {

        throw todo();
    }

    /**
     * 사진 붙이기 · 교체. 글 하나당 사진 하나다.
     *
     * <p>글쓰기(JSON)와 업로드를 따로 둔 이유는, 업로드가 실패해도 남는 것이
     * '첨부 없는 멀쩡한 글' 이기 때문이다. 첨부는 원래 선택 사항이라 그게 정상
     * 상태다.
     */
    @PostMapping("/{postId}/file")
    public PostFileInfo attachFile(
            @LoginUser User me,
            @PathVariable @Min(1) Long postId,
            @RequestPart("file") MultipartFile file) {

        throw todo();
    }

    /** 사진 떼기. 디스크의 파일도 같이 지운다. */
    @DeleteMapping("/{postId}/file")
    public ResponseEntity<Void> detachFile(
            @LoginUser User me,
            @PathVariable @Min(1) Long postId) {

        throw todo();
    }

    /**
     * 사진 내려받기. 원본 파일명으로 준다.
     *
     * <p>업로드 폴더를 정적으로 열지 않는 이유는, 그러면 지워진 글의 사진도 주소만
     * 알면 계속 보이기 때문이다. 여기서 {@code use_yn} 을 보고 내려준다.
     */
    @GetMapping("/{postId}/file")
    public ResponseEntity<byte[]> downloadFile(@PathVariable @Min(1) Long postId) {
        throw todo();
    }
}
