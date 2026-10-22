package com.meokji.api.review.service;

import com.meokji.api.common.Page;
import com.meokji.api.common.Paging;
import com.meokji.api.exception.ForbiddenException;
import com.meokji.api.exception.NotFoundException;
import com.meokji.api.review.dto.ReviewCommentRequest;
import com.meokji.api.review.dto.ReviewCommentResponse;
import com.meokji.api.review.dto.ReviewFileDownload;
import com.meokji.api.review.dto.ReviewFileResponse;
import com.meokji.api.review.dto.ReviewRequest;
import com.meokji.api.review.entity.ReviewEntity;
import com.meokji.api.review.entity.ReviewCommentEntity;
import com.meokji.api.review.repository.ReviewCommentView;
import com.meokji.api.review.dto.ReviewDetailResponse;
import com.meokji.api.review.dto.ReviewListResponse;
import com.meokji.api.review.repository.ReviewCommentRepository;
import com.meokji.api.review.repository.ReviewListView;
import com.meokji.api.review.repository.ReviewRepository;
import com.meokji.api.user.vo.UserVO;
import com.meokji.api.util.FileStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class ReviewService {

    @Autowired
    private ReviewRepository reviewRepository;

    @Autowired
    private ReviewCommentRepository reviewCommentRepository;

    @Autowired
    private FileStore fileStore;

    public Page<ReviewListResponse> getList(String keyword, int page, int size){
        String safeKeyword = (keyword == null || keyword.isBlank()) ? null : keyword;

        Paging paging = new Paging(page, size);
        paging.setTotal(reviewRepository.countAlive(safeKeyword));

        List<ReviewListResponse> items = new ArrayList<>();
        for (ReviewListView row : reviewRepository.findPage(safeKeyword, paging.getOffset(), paging.getSize())) {
            items.add(new ReviewListResponse(row));
        }

        Page<ReviewListResponse> result = new Page<>();
        result.Page(paging, items);
        return result;
    }

    public ReviewDetailResponse getDetail(UserVO loginUser, Long postId) {
        return toDetail(findAlivePost(postId), loginUser);
    }

    @Transactional
    public ReviewDetailResponse add(UserVO loginUser, ReviewRequest request) {
        // 글쓴이는 토큰에서. 본문으로 받으면 남의 이름으로 쓸 수 있다
        ReviewEntity post = ReviewEntity.create(
                loginUser.getId(),
                request.getMeal_history_id(),
                request.getTitle().trim(),
                request.getContent());

        // 바로 INSERT 를 보낸다. 그래야 DB 가 채운 작성 시각 · 조회수를
        // @Generated 가 다시 읽어 와서 응답에 담을 수 있다
        reviewRepository.saveAndFlush(post);

        return toDetail(post, loginUser);
    }

    /** 글 수정 (글쓴이만) */
    @Transactional
    public ReviewDetailResponse update(UserVO loginUser, Long postId, ReviewRequest request) {
        ReviewEntity post = findAlivePost(postId);
        checkOwner(loginUser, post.getUserId(), "글");

        // 이 메서드만 부르면 UPDATE 가 나간다 (변경 감지). save() 를 다시 부르지 않는다
        post.change(request.getTitle().trim(), request.getContent());

        return toDetail(post, loginUser);
    }

    /** 글 삭제 (글쓴이만). use_yn='N' */
    @Transactional
    public void delete(UserVO loginUser, Long postId) {
        ReviewEntity post = findAlivePost(postId);
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
        if (reviewRepository.increaseView(postId) == 0) {
            throw new NotFoundException("글을 찾을 수 없습니다.");
        }
        return reviewRepository.findViewCount(postId);
    }

    // ===================== 댓글 =====================

    /** 댓글 쓰기 */
    @Transactional
    public ReviewCommentResponse addComment(UserVO loginUser, Long postId, ReviewCommentRequest request) {
        findAlivePost(postId); // 지워진 글에는 댓글을 못 단다

        ReviewCommentEntity comment = ReviewCommentEntity.create(postId, loginUser.getId(), request.getContent().trim());
        reviewCommentRepository.save(comment); // id 가 IDENTITY 라 여기서 INSERT 가 나간다

        return toComment(reviewCommentRepository.findView(comment.getId())
                .orElseThrow(() -> new NotFoundException("댓글을 찾을 수 없습니다.")), loginUser);
    }

    /** 댓글 수정 (쓴 사람만) */
    @Transactional
    public ReviewCommentResponse updateComment(UserVO loginUser, Long postId, Long commentId, ReviewCommentRequest request) {
        ReviewCommentEntity comment = findAliveComment(postId, commentId);
        checkOwner(loginUser, comment.getUserId(), "댓글");

        comment.change(request.getContent().trim());

        // 아래는 네이티브 쿼리라 JPA 가 실행 전에 바뀐 내용을 먼저 DB 로 보낸다
        return toComment(reviewCommentRepository.findView(commentId)
                .orElseThrow(() -> new NotFoundException("댓글을 찾을 수 없습니다.")), loginUser);
    }

    /** 댓글 삭제 (쓴 사람만) */
    @Transactional
    public void deleteComment(UserVO loginUser, Long postId, Long commentId) {
        ReviewCommentEntity comment = findAliveComment(postId, commentId);
        checkOwner(loginUser, comment.getUserId(), "댓글");

        comment.deactivate();
    }

    // ===================== 사진 =====================

    /** 사진 붙이기 (글쓴이만). 이미 있으면 바꿔 단다 */
    @Transactional
    public ReviewFileResponse attachFile(UserVO loginUser, Long postId, MultipartFile file) {
        ReviewEntity post = findAlivePost(postId);
        checkOwner(loginUser, post.getUserId(), "글");

        // 받을 수 있는 파일인지 검사하고 디스크에 저장한다 (안 되면 여기서 끊긴다)
        String[] saved = fileStore.save(file, "review");

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
        ReviewEntity post = findAlivePost(postId);
        checkOwner(loginUser, post.getUserId(), "글");

        if (!post.hasFile()) {
            throw new NotFoundException("붙어 있는 사진이 없습니다.");
        }

        deleteStoredFile(post);
        post.detachFile();
    }

    /** 사진 내려받기. 글을 볼 수 있으면 누구나 */
    public ReviewFileDownload download(Long postId) {
        ReviewEntity post = findAlivePost(postId);

        if (!post.hasFile()) {
            throw new NotFoundException("붙어 있는 사진이 없습니다.");
        }

        byte[] bytes = fileStore.read(post.getFilePath(), post.getFileName());
        return new ReviewFileDownload(post.getFileOrgName(), post.getFileExt(), bytes);
    }

    // ===================== 안쪽에서만 쓰는 것 =====================

    /** 살아있는 글 하나. 없거나 지워졌으면 404 */
    private ReviewEntity findAlivePost(Long postId) {
        return reviewRepository
                .findByIdAndUseYn(postId, "Y")
                .orElseThrow(() -> new NotFoundException("글을 찾을 수 없습니다."));
    }

    /** 살아있는 댓글 하나. 주소의 글 번호와 짝이 맞아야 한다 */
    private ReviewCommentEntity findAliveComment(Long postId, Long commentId) {
        return reviewCommentRepository
                .findByIdAndPostIdAndUseYn(commentId, postId, "Y")
                .orElseThrow(() -> new NotFoundException("댓글을 찾을 수 없습니다."));
    }

    /**
     * 내가 쓴 것이 아니면 403. 고치거나 지우기 전에 부른다.
     *
     * 아래 isMine 과 달리 loginUser 가 null 일 수 없다 — 이 메서드를 부르는
     * 것은 전부 쓰기 작업이고, 컨트롤러가 getLoginUser 로 토큰을 먼저
     * 확인해서(없으면 401) 들어온다.
     *
     * what 은 "글" 또는 "댓글" 이다. 어느 쪽이 막혔는지 메시지에 넣는다.
     */
    private void checkOwner(UserVO loginUser, Long ownerId, String what) {
        if (!ownerId.equals(loginUser.getId())) {
            throw new ForbiddenException("내가 쓴 " + what + "만 고치거나 지울 수 있습니다.");
        }
    }

    /**
     * ownerId 가 로그인한 사람인지. 비로그인이면 false.
     *
     * ownerId 에 글쓴이를 넣으면 "내 글인지", 댓글 쓴 사람을 넣으면
     * "내 댓글인지" 가 된다. 식이 같아서 메서드는 하나만 두고, 무엇을
     * 넣는지로 글 것과 댓글 것을 가른다 (toDetail · toComment 참고).
     *
     * loginUser 가 null 일 수 있다 — 상세 보기는 로그인 없이도 되기 때문이다.
     * null 검사를 먼저 하지 않으면 비로그인으로 글을 열 때 터진다.
     */
    private boolean isMine(UserVO loginUser, Long ownerId) {
        return loginUser != null && ownerId.equals(loginUser.getId());
    }

    /**
     * 상세 응답. 표에 없는 칸(닉네임 · 댓글 · 첨부 · is_mine)을 여기서 채운다.
     *
     * is_mine 을 두 번 계산한다. 아래 isMine 은 글쓴이 기준이고, 댓글은
     * toComment 가 댓글마다 그 댓글 쓴 사람 기준으로 다시 계산한다.
     * 글 것을 댓글에 돌려쓰면 남의 글에 달린 내 댓글에 수정 버튼이 안 나오고,
     * 내 글에 달린 남의 댓글에 수정 버튼이 나온다.
     */
    private ReviewDetailResponse toDetail(ReviewEntity post, UserVO loginUser) {
        // 댓글마다 그 댓글의 is_mine 을 따로 채운다 (toComment 가 한다)
        List<ReviewCommentResponse> comments = new ArrayList<>();
        for (ReviewCommentView row : reviewCommentRepository.findViewsByPost(post.getId())) {
            comments.add(toComment(row, loginUser));
        }

        return new ReviewDetailResponse(
                post,
                reviewRepository.findNickname(post.getUserId()),
                toFile(post),
                comments,
                isMine(loginUser, post.getUserId())); // 이쪽은 글쓴이 기준
    }

    /** 첨부 사진 응답. 없으면 null — 화면은 file 이 없으면 사진 자리를 통째로 안 그린다 */
    private ReviewFileResponse toFile(ReviewEntity post) {
        if (!post.hasFile()) {
            return null;
        }
        return new ReviewFileResponse(post, fileStore.sizeText(post.getFileSize()));
    }

    /**
     * 댓글 응답.
     *
     * 견주는 상대가 row.getUserId() — 이 댓글을 쓴 사람이다. 글쓴이가 아니다.
     * 그래서 남의 글(글 is_mine=false)에 달린 내 댓글은 is_mine=true 로 나가고,
     * 화면이 그 댓글에만 수정 · 삭제 버튼을 보인다.
     */
    private ReviewCommentResponse toComment(ReviewCommentView row, UserVO loginUser) {
        return new ReviewCommentResponse(row, isMine(loginUser, row.getUserId()));
    }

    /** 디스크에 저장해둔 사진을 지운다. 첨부가 없으면 할 일이 없다 */
    private void deleteStoredFile(ReviewEntity post) {
        if (!post.hasFile()) {
            return;
        }

        fileStore.delete(post.getFilePath(), post.getFileName());
    }
}
