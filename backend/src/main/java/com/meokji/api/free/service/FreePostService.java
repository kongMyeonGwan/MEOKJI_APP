package com.meokji.api.free.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.meokji.api.common.Page;
import com.meokji.api.common.Paging;
import com.meokji.api.exception.ForbiddenException;
import com.meokji.api.exception.NotFoundException;
import com.meokji.api.free.dto.FreeCommentRequest;
import com.meokji.api.free.dto.FreeCommentResponse;
import com.meokji.api.free.dto.FreeFileDownload;
import com.meokji.api.free.dto.FreeFileResponse;
import com.meokji.api.free.dto.FreePostDetailResponse;
import com.meokji.api.free.dto.FreePostListResponse;
import com.meokji.api.free.dto.FreePostRequest;
import com.meokji.api.free.entity.FreePostEntity;
import com.meokji.api.free.entity.PostCommentEntity;
import com.meokji.api.free.repository.FreeCommentView;
import com.meokji.api.free.repository.FreePostListView;
import com.meokji.api.free.repository.FreePostRepository;
import com.meokji.api.free.repository.PostCommentRepository;
import com.meokji.api.user.vo.UserVO;
import com.meokji.api.util.FileStore;

/**
 * 자유게시판. 글 · 댓글 · 첨부 전부 JPA 로 읽고 쓴다.
 *
 * 보기는 누구나, 쓰기 · 수정 · 삭제 · 댓글은 로그인해야 한다.
 * 수정 · 삭제는 글쓴이 본인만 된다 — 그 판단을 전부 여기서 한다.
 * 화면이 버튼을 숨기는 것은 눈에 안 보이게 하는 것일 뿐이다.
 *
 * 트랜잭션은 클래스에 readOnly 로 걸어 두고, 쓰는 메서드에만 @Transactional 을
 * 다시 붙인다. 조회 메서드는 따로 적지 않아도 읽기 전용으로 돈다.
 *
 * 쓰기는 엔티티(FreePostEntity · PostCommentEntity)의 메서드를 부르는 것으로
 * 끝난다 — 트랜잭션이 끝날 때 변경 감지로 UPDATE 가 나간다.
 * 닉네임이 필요한 조회(목록 · 댓글)만 Repository 의 네이티브 쿼리를 쓴다.
 *
 * 받는 모양은 Request, 내보내는 모양은 Response DTO 다 (free/dto).
 * 엔티티를 그대로 내보내지 않는다 — 표에 없는 칸(닉네임 · 댓글 · is_mine)을
 * 담아야 하고, 글쓴이 id 나 파일 저장 경로처럼 화면에 나가면 안 되는 칸이 있다.
 */
@Service
@Transactional(readOnly = true) // 기본은 조회. 쓰는 메서드만 아래에서 다시 건다
public class FreePostService {

    @Autowired
    private FreePostRepository freePostRepository;

    @Autowired
    private PostCommentRepository postCommentRepository;

    @Autowired
    private FileStore fileStore;

    // ===================== 글 =====================

    /** 목록 한 페이지. keyword 가 있으면 제목 · 내용에서 찾는다 */
    public Page<FreePostListResponse> getList(String keyword, int page, int size) {
        // 빈 검색어는 "검색 안 함" 이다. SQL 이 null 만 그렇게 본다
        String safeKeyword = (keyword == null || keyword.isBlank()) ? null : keyword;

        // offset 과 total_pages 계산은 Paging 이 한다 (common/Paging.java)
        Paging paging = new Paging(page, size);
        paging.setTotal(freePostRepository.countAlive(safeKeyword));

        List<FreePostListResponse> items = new ArrayList<>();
        for (FreePostListView row : freePostRepository.findPage(safeKeyword, paging.getOffset(), paging.getSize())) {
            items.add(new FreePostListResponse(row));
        }

        Page<FreePostListResponse> result = new Page<>();
        result.Page(paging, items);
        return result;
    }

    /** 상세. loginUser 가 null 이면(비로그인) is_mine 은 false 다 */
    public FreePostDetailResponse getDetail(UserVO loginUser, Long postId) {
        return toDetail(findAlivePost(postId), loginUser);
    }

    /** 글쓰기 */
    @Transactional
    public FreePostDetailResponse add(UserVO loginUser, FreePostRequest request) {
        // 글쓴이는 토큰에서. 본문으로 받으면 남의 이름으로 쓸 수 있다
        FreePostEntity post = FreePostEntity.create(
                loginUser.getId(),
                request.getTitle().trim(),
                request.getContent());

        // 바로 INSERT 를 보낸다. 그래야 DB 가 채운 작성 시각 · 조회수를
        // @Generated 가 다시 읽어 와서 응답에 담을 수 있다
        freePostRepository.saveAndFlush(post);

        return toDetail(post, loginUser);
    }

    /** 글 수정 (글쓴이만) */
    @Transactional
    public FreePostDetailResponse update(UserVO loginUser, Long postId, FreePostRequest request) {
        FreePostEntity post = findAlivePost(postId);
        checkOwner(loginUser, post.getUserId(), "글");

        // 이 메서드만 부르면 UPDATE 가 나간다 (변경 감지). save() 를 다시 부르지 않는다
        post.change(request.getTitle().trim(), request.getContent());

        return toDetail(post, loginUser);
    }

    /** 글 삭제 (글쓴이만). use_yn='N' */
    @Transactional
    public void delete(UserVO loginUser, Long postId) {
        FreePostEntity post = findAlivePost(postId);
        checkOwner(loginUser, post.getUserId(), "글");

        // 붙어 있던 사진도 같이 지운다. 글이 안 보이는데 파일만 남으면
        // 디스크만 계속 찬다 (주소를 아는 사람에게는 계속 열린다)
        deleteStoredFile(post);

        // 행은 지우지 않고 use_yn 만 뒤집는다 (변경 감지로 UPDATE 가 나간다)
        post.deactivate();
    }

    /** 조회수 +1. 올린 뒤의 값을 돌려준다 */
    @Transactional
    public int increaseView(Long postId) {
        // 지운 글이거나 없는 글이면 바뀐 행이 0 이다
        if (freePostRepository.increaseView(postId) == 0) {
            throw new NotFoundException("글을 찾을 수 없습니다.");
        }
        return freePostRepository.findViewCount(postId);
    }

    // ===================== 댓글 =====================

    /** 댓글 쓰기 */
    @Transactional
    public FreeCommentResponse addComment(UserVO loginUser, Long postId, FreeCommentRequest request) {
        findAlivePost(postId); // 지워진 글에는 댓글을 못 단다

        PostCommentEntity comment = PostCommentEntity.create(postId, loginUser.getId(), request.getContent().trim());
        postCommentRepository.save(comment); // id 가 IDENTITY 라 여기서 INSERT 가 나간다

        return toComment(postCommentRepository.findView(comment.getId()), loginUser);
    }

    /** 댓글 수정 (쓴 사람만) */
    @Transactional
    public FreeCommentResponse updateComment(UserVO loginUser, Long postId, Long commentId, FreeCommentRequest request) {
        PostCommentEntity comment = findAliveComment(postId, commentId);
        checkOwner(loginUser, comment.getUserId(), "댓글");

        comment.change(request.getContent().trim());

        // 아래는 네이티브 쿼리라 JPA 가 실행 전에 바뀐 내용을 먼저 DB 로 보낸다
        return toComment(postCommentRepository.findView(commentId), loginUser);
    }

    /** 댓글 삭제 (쓴 사람만) */
    @Transactional
    public void deleteComment(UserVO loginUser, Long postId, Long commentId) {
        PostCommentEntity comment = findAliveComment(postId, commentId);
        checkOwner(loginUser, comment.getUserId(), "댓글");

        comment.deactivate();
    }

    // ===================== 사진 =====================

    /** 사진 붙이기 (글쓴이만). 이미 있으면 바꿔 단다 */
    @Transactional
    public FreeFileResponse attachFile(UserVO loginUser, Long postId, MultipartFile file) {
        FreePostEntity post = findAlivePost(postId);
        checkOwner(loginUser, post.getUserId(), "글");

        // 받을 수 있는 파일인지 검사하고 디스크에 저장한다 (안 되면 여기서 끊긴다)
        String[] saved = fileStore.save(file, "free");

        // 바꿔 다는 경우 옛 파일을 지운다. 새 파일이 무사히 저장된 다음에 지워야
        // 저장이 실패했을 때 사진이 통째로 사라지는 일이 없다
        deleteStoredFile(post);

        post.attachFile(
                saved[0],
                saved[1],
                file.getOriginalFilename(),
                fileStore.extensionOf(file.getOriginalFilename()),
                file.getSize());

        return toFile(post);
    }

    /** 사진 떼기 (글쓴이만) */
    @Transactional
    public void detachFile(UserVO loginUser, Long postId) {
        FreePostEntity post = findAlivePost(postId);
        checkOwner(loginUser, post.getUserId(), "글");

        if (!post.hasFile()) {
            throw new NotFoundException("붙어 있는 사진이 없습니다.");
        }

        deleteStoredFile(post);
        post.detachFile();
    }

    /** 사진 내려받기. 글을 볼 수 있으면 누구나 */
    public FreeFileDownload download(Long postId) {
        FreePostEntity post = findAlivePost(postId);

        if (!post.hasFile()) {
            throw new NotFoundException("붙어 있는 사진이 없습니다.");
        }

        byte[] bytes = fileStore.read(post.getFilePath(), post.getFileName());
        return new FreeFileDownload(post.getFileOrgName(), post.getFileExt(), bytes);
    }

    // ===================== 안쪽에서만 쓰는 것 =====================

    /** 살아있는 글 하나. 없거나 지워졌으면 404 */
    private FreePostEntity findAlivePost(Long postId) {
        return freePostRepository
                .findByIdAndUseYn(postId, "Y")
                .orElseThrow(() -> new NotFoundException("글을 찾을 수 없습니다."));
    }

    /** 살아있는 댓글 하나. 주소의 글 번호와 짝이 맞아야 한다 */
    private PostCommentEntity findAliveComment(Long postId, Long commentId) {
        return postCommentRepository
                .findByIdAndPostIdAndUseYn(commentId, postId, "Y")
                .orElseThrow(() -> new NotFoundException("댓글을 찾을 수 없습니다."));
    }

    /** 내가 쓴 것이 아니면 403 */
    private void checkOwner(UserVO loginUser, Long ownerId, String what) {
        if (!ownerId.equals(loginUser.getId())) {
            throw new ForbiddenException("내가 쓴 " + what + "만 고치거나 지울 수 있습니다.");
        }
    }

    /** 로그인한 사람이 쓴 것인지. 비로그인이면 false */
    private boolean isMine(UserVO loginUser, Long ownerId) {
        return loginUser != null && ownerId.equals(loginUser.getId());
    }

    /** 상세 응답. 닉네임 · 댓글 · 첨부 · 내 글 여부까지 채운다 */
    private FreePostDetailResponse toDetail(FreePostEntity post, UserVO loginUser) {
        List<FreeCommentResponse> comments = new ArrayList<>();
        for (FreeCommentView row : postCommentRepository.findViewsByPost(post.getId())) {
            comments.add(toComment(row, loginUser));
        }

        return new FreePostDetailResponse(
                post,
                freePostRepository.findNickname(post.getUserId()),
                toFile(post),
                comments,
                isMine(loginUser, post.getUserId()));
    }

    /** 첨부 사진 응답. 없으면 null — 화면은 file 이 없으면 사진 자리를 통째로 안 그린다 */
    private FreeFileResponse toFile(FreePostEntity post) {
        if (!post.hasFile()) {
            return null;
        }
        return new FreeFileResponse(post, fileStore.sizeText(post.getFileSize()));
    }

    /** 댓글 응답 */
    private FreeCommentResponse toComment(FreeCommentView row, UserVO loginUser) {
        return new FreeCommentResponse(row, isMine(loginUser, row.getUserId()));
    }

    /** 디스크에 저장해둔 사진을 지운다. 첨부가 없으면 할 일이 없다 */
    private void deleteStoredFile(FreePostEntity post) {
        if (!post.hasFile()) {
            return;
        }

        fileStore.delete(post.getFilePath(), post.getFileName());
    }
}
