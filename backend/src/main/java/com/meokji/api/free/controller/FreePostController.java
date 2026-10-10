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
 *
 * <p>이 프로젝트에서 가장 메서드가 많은 컨트롤러다(10개). 한 기능에 글 ·
 * 댓글 · 사진이 다 들어 있어서다. 그래서 주석으로 세 묶음으로 나눠 두었다.
 *
 * <p>주소 짜임새 — 댓글과 사진이 글에 딸려 있다는 것을 주소로 드러낸다
 *   /api/free-posts                       글 목록 · 글쓰기
 *   /api/free-posts/{id}                  글 상세 · 수정 · 삭제
 *   /api/free-posts/{id}/view             조회수
 *   /api/free-posts/{id}/comments         댓글 쓰기
 *   /api/free-posts/{id}/comments/{cid}   댓글 수정 · 삭제
 *   /api/free-posts/{id}/file             사진 붙이기 · 떼기 · 받기
 *
 * <p>댓글 주소에 postId 가 꼭 들어가는 이유 — commentId 만으로도 댓글을
 * 찾을 수 있지만, 그러면 "3번 글의 댓글" 이라고 해놓고 실제로는 다른 글의
 * 댓글을 고칠 수 있다. 서비스가 둘이 맞는지 확인한다.
 *
 * <p>이 기능만 JPA 와 MyBatis 를 같이 쓴다. 글쓰기 · 수정 · 삭제는 JPA,
 * 목록 · 검색 · 조회수 · 댓글 · 사진은 MyBatis 다. 컨트롤러에서는 그 차이가
 * 보이지 않는다 — 서비스가 가려준다.
 */
@RestController
@RequestMapping("/api/free-posts")
public class FreePostController {

    @Autowired
    private AuthService authService;

    @Autowired
    private FreePostService freePostService;

    // 파일 확장자 -> Content-Type 을 알아내는 데만 쓴다 (아래 downloadFile).
    // 서비스를 거치지 않고 컨트롤러가 직접 쓰는 이유는, 그게 HTTP 응답
    // 헤더를 만드는 일이라서다
    @Autowired
    private FileStore fileStore;

    // ===================== 글 =====================

    // 목록  GET /api/free-posts?keyword=두부&page=1&size=20
    //
    // token 을 받지 않는다. 누구나 볼 수 있고, 목록에서는 "내 글인지" 를
    // 표시하지 않기 때문이다 (상세에서만 표시한다)
    //
    // keyword 에 @Size(max = 100) 를 거는 이유 — 검색어는 SQL 의 LIKE 에
    // 들어간다. 길이를 막지 않으면 아주 긴 글자로 검색을 걸어 DB 를
    // 무겁게 만들 수 있다
    @GetMapping
    public Page<FreePostVO> getList(
            @RequestParam(required = false) @Size(max = 100) String keyword,
            @RequestParam(defaultValue = "1") @Min(1) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(Paging.MAX_SIZE) int size) {
        return freePostService.getList(keyword, page, size);
    }

    // 상세  GET /api/free-posts/3
    // 로그인했으면 "내 글인지" 도 알려준다 (수정 · 삭제 버튼을 보일지 판단용)
    //
    // getLoginUser 가 아니라 findLoginUser 다. 글은 로그인 없이도 보여야
    // 하므로 토큰이 없을 때 에러를 내면 안 된다 - null 을 받아 넘긴다
    //
    // 화면이 글쓴이 번호를 받아 스스로 비교하지 않는 이유 — 그러려면
    // 응답에 user_id 를 실어 보내야 하고, 그건 화면에 쓸 일이 없는 값이다
    @GetMapping("/{postId}")
    public FreePostVO getDetail(
            @RequestHeader(value = "Authorization", required = false) String token,
            @PathVariable @Min(1) Long postId) {
        UserVO loginUser = authService.findLoginUser(token); // 로그인 안 했으면 null
        return freePostService.getDetail(loginUser, postId);
    }

    // 글쓰기  POST /api/free-posts
    //
    // 사진은 같이 안 받는다. 글을 먼저 만들고 그 번호로 /file 에 올린다 -
    // 첨부가 글에 딸린 것이라 글 번호가 있어야 저장할 수 있다
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED) // 201
    public FreePostVO add(
            @RequestHeader(value = "Authorization", required = false) String token,
            @Valid @RequestBody FreePostVO request) {
        UserVO loginUser = authService.getLoginUser(token);
        return freePostService.add(loginUser, request);
    }

    // 글 수정  PUT /api/free-posts/3
    //
    // loginUser 를 서비스에 넘기는 이유 — 글쓴이 본인인지 확인해야 한다.
    // 컨트롤러에서 확인하지 않는 것은, 글을 읽어봐야 알 수 있는 판단이라서다
    @PutMapping("/{postId}")
    public FreePostVO update(
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
    //
    // GET 이 아니라 POST 인 이유 — 값을 바꾸는 요청이다. GET 으로 두면
    // 브라우저나 중간 서버가 미리 불러서 조회수가 멋대로 오른다
    //
    // 로그인을 요구하지 않는다. 로그인 안 한 사람이 읽어도 조회는 조회다
    @PostMapping("/{postId}/view")
    public int increaseView(@PathVariable @Min(1) Long postId) {
        return freePostService.increaseView(postId);
    }

    // ===================== 댓글 =====================

    // 댓글 쓰기  POST /api/free-posts/3/comments
    //
    // request.getContent() 만 꺼내 넘기는 이유 — FreePostCommentVO 에는
    // id · 글번호 · 닉네임 · 작성일까지 들어 있는데, 화면이 보내도 되는 칸은
    // content 하나뿐이다. VO 통째로 넘기면 서비스가 다시 골라내야 한다
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
    //
    // postId 와 commentId 를 둘 다 받는다. 서비스가 "그 댓글이 정말 그 글의
    // 댓글인지" 를 확인한다 - 주소와 실제가 다르면 404 다
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
    //
    // @RequestBody 가 아니라 @RequestPart 다. 사진은 JSON 으로 보낼 수 없어
    // multipart/form-data 로 온다 - 그 안의 "file" 칸을 꺼내는 표시다
    //
    // 글 하나에 사진 하나다. 또 올리면 앞의 것이 바뀐다 (서비스가 그렇게 한다).
    // 여러 장을 받으려면 표를 따로 만들어야 하는데, 지금 화면은 한 장만 쓴다
    //
    // 201 이 아니라 200 인 이유 — 새 자원을 만드는 것이 아니라 이미 있는
    // 글에 사진을 붙이는 것이다. 주소도 안 바뀐다
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
     *
     * <p>업로드 폴더를 그대로 열어두지 않고 이 주소를 거치는 이유 — 폴더를
     * 열면 지워진 글의 사진도 주소만 알면 계속 보인다. 이 주소를 지나면
     * 서비스가 글이 살아있는지 확인한 뒤에 읽는다.
     *
     * <p>로그인을 요구하지 않는다. 글이 누구나 볼 수 있으니 거기 붙은 사진도
     * 누구나 볼 수 있어야 한다.
     */
    @GetMapping("/{postId}/file")
    public ResponseEntity<byte[]> downloadFile(@PathVariable @Min(1) Long postId) {
        // 글이 살아있고 사진이 붙어 있는지 확인한 뒤 그 글을 돌려준다.
        // 둘 중 하나라도 아니면 여기서 404 가 던져진다
        FreePostVO post = freePostService.getFileOwner(postId);
        byte[] bytes = freePostService.readFile(post);

        // inline = 브라우저에서 바로 보여준다 (attachment 면 곧장 내려받기 창이 뜬다)
        //
        // StandardCharsets.UTF_8 을 넘기면 filename*=UTF-8''... 모양으로 적힌다.
        // 그냥 filename= 으로 적으면 한글 이름이 깨져서 "???.jpg" 로 저장된다
        String disposition = ContentDisposition.inline()
                .filename(post.getFile().getName(), StandardCharsets.UTF_8)
                .build()
                .toString();

        return ResponseEntity.ok()
                // 확장자로 종류를 정한다. 안 주면 브라우저가 내려받기로 처리한다
                .contentType(MediaType.parseMediaType(fileStore.contentType(post.getFile().getExt())))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                .body(bytes);
    }
}
