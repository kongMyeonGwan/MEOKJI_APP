package com.meokji.api.free.controller;

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
import com.meokji.api.free.service.FreePostService;
import com.meokji.api.free.vo.FreeFileVO;
import com.meokji.api.free.vo.FreePostCommentVO;
import com.meokji.api.free.vo.FreePostDetailVO;
import com.meokji.api.free.vo.FreePostListVO;
import com.meokji.api.free.vo.FreePostVO;
import com.meokji.api.user.vo.UserVO;
import com.meokji.api.util.FileStore;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * 자유게시판.  주소: /api/free-posts
 *
 * 보기는 누구나, 쓰기 · 수정 · 삭제 · 댓글은 로그인해야 한다.
 * 수정 · 삭제는 글쓴이 본인만 된다 (판단은 FreePostServiceImpl 이 한다).
 *
 * 레시피 후기(/api/reviews)와 다른 점은 글을 쓰는 조건이 없다는 것뿐이다.
 * 후기는 내 식사 기록을 하나 골라야 하지만 여기는 로그인만 하면 된다.
 */
@RestController
@RequestMapping("/api/free-posts")
public class FreePostController {

    @Autowired
    private AuthService authService;

    @Autowired
    private FreePostService freePostService;

    @Autowired
    private FileStore fileStore;

    // ===================== 글 =====================

    // 목록  GET /api/free-posts?keyword=두부&page=1&size=20
    @GetMapping
    public Page<FreePostListVO> getList(
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(Paging.MAX_SIZE) int size) {
        return freePostService.getList(keyword, page, size);
    }

    // 상세  GET /api/free-posts/3
    // 로그인했으면 "내 글인지" 도 알려준다 (수정 · 삭제 버튼을 보일지 판단용)
    @GetMapping("/{postId}")
    public FreePostDetailVO getDetail(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable @Min(1) Long postId) {
        UserVO loginUser = authService.findLoginUser(token); // 로그인 안 했으면 null
        return freePostService.getDetail(loginUser, postId);
    }

    // 글쓰기  POST /api/free-posts
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // 201
    public FreePostDetailVO add(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody FreePostVO request) {
        UserVO loginUser = authService.getLoginUser(token);
        return freePostService.add(loginUser, request);
    }

    // 글 수정  PUT /api/free-posts/3
    @PutMapping("/{postId}")
    public FreePostDetailVO update(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable @Min(1) Long postId,
            @Valid @RequestBody FreePostVO request) {
        UserVO loginUser = authService.getLoginUser(token);
        return freePostService.update(loginUser, postId, request);
    }

    // 글 삭제  DELETE /api/free-posts/3
    @DeleteMapping("/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204
    public void delete(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable @Min(1) Long postId) {
        UserVO loginUser = authService.getLoginUser(token);
        freePostService.delete(loginUser, postId);
    }

    // 조회수 +1  POST /api/free-posts/3/view
    // 목록에서 같이 올리지 않고 따로 뺐다. 목록을 열 때마다 모든 글의 조회수가
    // 오르면 숫자가 의미를 잃는다 - 화면은 상세를 열 때 한 번 부른다
    @PostMapping("/{postId}/view")
    public int increaseView(@PathVariable @Min(1) Long postId) {
        return freePostService.increaseView(postId);
    }

    // ===================== 댓글 =====================

    // 댓글 쓰기  POST /api/free-posts/3/comments
    @PostMapping("/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED) // 201
    public FreePostCommentVO addComment(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable @Min(1) Long postId,
            @Valid @RequestBody FreePostCommentVO request) {
        UserVO loginUser = authService.getLoginUser(token);
        return freePostService.addComment(loginUser, postId, request.getContent());
    }

    // 댓글 수정  PUT /api/free-posts/3/comments/7
    @PutMapping("/{postId}/comments/{commentId}")
    public FreePostCommentVO updateComment(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable @Min(1) Long postId,
            @PathVariable @Min(1) Long commentId,
            @Valid @RequestBody FreePostCommentVO request) {
        UserVO loginUser = authService.getLoginUser(token);
        return freePostService.updateComment(loginUser, postId, commentId, request.getContent());
    }

    // 댓글 삭제  DELETE /api/free-posts/3/comments/7
    @DeleteMapping("/{postId}/comments/{commentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204
    public void deleteComment(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable @Min(1) Long postId,
            @PathVariable @Min(1) Long commentId) {
        UserVO loginUser = authService.getLoginUser(token);
        freePostService.deleteComment(loginUser, postId, commentId);
    }

    // ===================== 사진 =====================

    // 사진 붙이기  POST /api/free-posts/3/file  (form-data 의 file 칸)
    @PostMapping("/{postId}/file")
    public FreeFileVO attachFile(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable @Min(1) Long postId,
            @RequestPart("file") MultipartFile file) {
        UserVO loginUser = authService.getLoginUser(token);
        return freePostService.attachFile(loginUser, postId, file);
    }

    // 사진 떼기  DELETE /api/free-posts/3/file
    @DeleteMapping("/{postId}/file")
    @ResponseStatus(HttpStatus.NO_CONTENT) // 204
    public void detachFile(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable @Min(1) Long postId) {
        UserVO loginUser = authService.getLoginUser(token);
        freePostService.detachFile(loginUser, postId);
    }

    /**
     * 사진 받기  GET /api/free-posts/3/file
     *
     * <p>ResponseEntity 로 돌려주는 이유: 바이트만 보내면 브라우저가 무슨 그림인지
     * 모른다. Content-Type 을 같이 줘야 &lt;img&gt; 에 그대로 그려진다.
     *
     * <p>Content-Disposition 에는 올린 원래 이름을 담는다. 내려받을 때 UUID 이름
     * 대신 그 이름으로 저장된다. 한글 파일명이 깨지지 않게 filename* 로 적는다.
     */
    @GetMapping("/{postId}/file")
    public ResponseEntity<byte[]> downloadFile(@PathVariable @Min(1) Long postId) {
        FreePostVO post = freePostService.getFileOwner(postId);
        byte[] bytes = freePostService.readFile(post);

        // inline = 브라우저에서 바로 보여준다 (attachment 면 곧장 내려받기 창이 뜬다)
        String disposition = ContentDisposition.inline()
                .filename(post.getFileOrgName(), StandardCharsets.UTF_8)
                .build()
                .toString();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(fileStore.contentType(post.getFileExt())))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .body(bytes);
    }
}
