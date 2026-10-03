package com.meokji.api.free.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.meokji.api.common.ApiException;
import com.meokji.api.common.Page;
import com.meokji.api.common.Paging;
import com.meokji.api.free.dao.FreeCommentDAO;
import com.meokji.api.free.dao.FreePostDAO;
import com.meokji.api.free.vo.FreeFileVO;
import com.meokji.api.free.vo.FreePostCommentVO;
import com.meokji.api.free.vo.FreePostDetailVO;
import com.meokji.api.free.vo.FreePostListVO;
import com.meokji.api.free.vo.FreePostVO;
import com.meokji.api.user.vo.UserVO;
import com.meokji.api.util.FileStore;
import com.meokji.api.util.Times;

@Service
public class FreePostServiceImpl implements FreePostService {

    @Autowired
    private FreePostDAO freePostDAO;

    @Autowired
    private FreeCommentDAO freeCommentDAO;

    @Autowired
    private FileStore fileStore;

    // ===================== 글 =====================

    @Override
    public Page<FreePostListVO> getList(String keyword, int page, int size) {
        // offset 과 total_pages 계산은 Paging 이 한다 (common/Paging.java)
        Paging paging = new Paging(page, size);
        paging.setTotal(freePostDAO.count(keyword));

        List<FreePostListVO> items = freePostDAO.findPage(keyword, paging.getOffset(), paging.getSize());

        Page<FreePostListVO> result = new Page<>();
        result.fill(paging, items);
        return result;
    }

    @Override
    public FreePostDetailVO getDetail(UserVO loginUser, Long postId) {
        FreePostVO post = findAlivePost(postId);
        return toDetail(post, loginUser);
    }

    @Override
    @Transactional
    public FreePostDetailVO add(UserVO loginUser, FreePostVO request) {
        FreePostVO post = new FreePostVO();
        post.setUserId(loginUser.getId()); // 글쓴이는 토큰에서. 본문으로 받으면 남의 이름으로 쓸 수 있다
        post.setTitle(request.getTitle().trim());
        post.setContent(request.getContent());

        freePostDAO.insert(post);

        // 방금 넣은 행을 다시 읽는다. 닉네임 · 작성 시각은 DB 와 JOIN 이 채우는 값이다
        return toDetail(findAlivePost(post.getId()), loginUser);
    }

    @Override
    @Transactional
    public FreePostDetailVO update(UserVO loginUser, Long postId, FreePostVO request) {
        FreePostVO post = findAlivePost(postId);
        checkOwner(loginUser, post.getUserId(), "글");

        post.setTitle(request.getTitle().trim());
        post.setContent(request.getContent());
        freePostDAO.update(post);

        return toDetail(findAlivePost(postId), loginUser);
    }

    @Override
    @Transactional
    public void delete(UserVO loginUser, Long postId) {
        FreePostVO post = findAlivePost(postId);
        checkOwner(loginUser, post.getUserId(), "글");

        // 붙어 있던 사진도 같이 지운다. 글이 안 보이는데 파일만 남으면
        // 디스크만 계속 찬다 (주소를 아는 사람에게는 계속 열린다)
        fileStore.delete(post.getFilePath(), post.getFileName());

        freePostDAO.deactivate(postId);
    }

    @Override
    @Transactional
    public int increaseView(Long postId) {
        findAlivePost(postId); // 없는 글이면 404
        freePostDAO.increaseView(postId);
        return freePostDAO.findViewCount(postId);
    }

    // ===================== 댓글 =====================

    @Override
    @Transactional
    public FreePostCommentVO addComment(UserVO loginUser, Long postId, String content) {
        findAlivePost(postId); // 지워진 글에는 댓글을 못 단다

        FreePostCommentVO comment = new FreePostCommentVO();
        comment.setPostId(postId);
        comment.setUserId(loginUser.getId());
        comment.setContent(content.trim());

        freeCommentDAO.insert(comment);

        FreePostCommentVO saved = freeCommentDAO.findById(comment.getId());
        saved.setIsMine(true);
        return saved;
    }

    @Override
    @Transactional
    public FreePostCommentVO updateComment(UserVO loginUser, Long postId, Long commentId, String content) {
        FreePostCommentVO comment = findAliveComment(postId, commentId);
        checkOwner(loginUser, comment.getUserId(), "댓글");

        comment.setContent(content.trim());
        freeCommentDAO.update(comment);

        FreePostCommentVO saved = freeCommentDAO.findById(commentId);
        saved.setIsMine(true);
        return saved;
    }

    @Override
    @Transactional
    public void deleteComment(UserVO loginUser, Long postId, Long commentId) {
        FreePostCommentVO comment = findAliveComment(postId, commentId);
        checkOwner(loginUser, comment.getUserId(), "댓글");

        freeCommentDAO.deactivate(commentId);
    }

    // ===================== 사진 =====================

    @Override
    @Transactional
    public FreeFileVO attachFile(UserVO loginUser, Long postId, MultipartFile file) {
        FreePostVO post = findAlivePost(postId);
        checkOwner(loginUser, post.getUserId(), "글");

        // 받을 수 있는 파일인지 검사하고 디스크에 저장한다 (안 되면 여기서 끊긴다)
        String[] saved = fileStore.save(file, "free");

        // 바꿔 다는 경우 옛 파일을 지운다. 새 파일이 무사히 저장된 다음에 지워야
        // 저장이 실패했을 때 사진이 통째로 사라지는 일이 없다
        fileStore.delete(post.getFilePath(), post.getFileName());

        FreePostVO target = new FreePostVO();
        target.setId(postId);
        target.setFilePath(saved[0]);
        target.setFileName(saved[1]);
        target.setFileOrgName(file.getOriginalFilename());
        target.setFileExt(fileStore.extensionOf(file.getOriginalFilename()));
        target.setFileSize(file.getSize());
        freePostDAO.setFile(target);

        return toFile(freePostDAO.findById(postId));
    }

    @Override
    @Transactional
    public void detachFile(UserVO loginUser, Long postId) {
        FreePostVO post = findAlivePost(postId);
        checkOwner(loginUser, post.getUserId(), "글");

        if (!post.hasFile()) {
            throw ApiException.notFound("붙어 있는 사진이 없습니다.");
        }

        fileStore.delete(post.getFilePath(), post.getFileName());
        freePostDAO.clearFile(postId);
    }

    @Override
    public FreePostVO getFileOwner(Long postId) {
        FreePostVO post = findAlivePost(postId);

        if (!post.hasFile()) {
            throw ApiException.notFound("붙어 있는 사진이 없습니다.");
        }
        return post;
    }

    @Override
    public byte[] readFile(FreePostVO post) {
        return fileStore.read(post.getFilePath(), post.getFileName());
    }

    // ===================== 안쪽에서만 쓰는 것 =====================

    /** 살아있는 글 하나. 없거나 지워졌으면 404 */
    private FreePostVO findAlivePost(Long postId) {
        FreePostVO post = freePostDAO.findById(postId);

        if (post == null || !post.alive()) {
            throw ApiException.notFound("글을 찾을 수 없습니다.");
        }
        return post;
    }

    /** 살아있는 댓글 하나. 주소의 글 번호와 짝이 맞는지도 본다 */
    private FreePostCommentVO findAliveComment(Long postId, Long commentId) {
        FreePostCommentVO comment = freeCommentDAO.findById(commentId);

        // postId 를 같이 보는 이유: /free-posts/1/comments/7 처럼 남의 글 주소에
        // 내 댓글 번호를 끼워 넣는 요청을 막는다
        if (comment == null || !"Y".equals(comment.getUseYn()) || !comment.getPostId().equals(postId)) {
            throw ApiException.notFound("댓글을 찾을 수 없습니다.");
        }
        return comment;
    }

    /** 내가 쓴 것이 아니면 403 */
    private void checkOwner(UserVO loginUser, Long ownerId, String what) {
        if (!ownerId.equals(loginUser.getId())) {
            throw ApiException.forbidden("내가 쓴 " + what + "만 고치거나 지울 수 있습니다.");
        }
    }

    /** 표 한 행 + 댓글 → 화면으로 나갈 상세 모양 */
    private FreePostDetailVO toDetail(FreePostVO post, UserVO loginUser) {
        List<FreePostCommentVO> comments = freeCommentDAO.findByPost(post.getId());
        for (FreePostCommentVO comment : comments) {
            comment.setIsMine(isMine(loginUser, comment.getUserId()));
        }

        FreePostDetailVO detail = new FreePostDetailVO();
        detail.setId(post.getId());
        detail.setTitle(post.getTitle());
        detail.setContent(post.getContent());
        detail.setNickname(post.getNickname());
        detail.setViewCount(post.getViewCount() == null ? 0 : post.getViewCount());
        detail.setCreatedAt(post.createdAtText());
        detail.setUpdatedAt(Times.toText(post.getUpdatedAt()));
        detail.setFile(toFile(post));
        detail.setIsMine(isMine(loginUser, post.getUserId()));
        detail.setComments(comments == null ? new ArrayList<FreePostCommentVO>() : comments);
        return detail;
    }

    /** 표 한 행의 FILE_* 칸 → 화면으로 나갈 첨부 모양. 첨부가 없으면 null */
    private FreeFileVO toFile(FreePostVO post) {
        if (!post.hasFile()) {
            return null;
        }

        FreeFileVO file = new FreeFileVO();
        file.setName(post.getFileOrgName());
        file.setExt(post.getFileExt());
        file.setSize(post.getFileSize());
        file.setSizeText(fileStore.sizeText(post.getFileSize()));
        // 업로드 폴더를 그대로 열지 않는다. 지워진 글의 사진이 주소만 알면 계속 보인다
        file.setUrl("/api/free-posts/" + post.getId() + "/file");
        return file;
    }

    /** 로그인한 사람이 쓴 것인지. 비로그인이면 false */
    private boolean isMine(UserVO loginUser, Long ownerId) {
        return loginUser != null && ownerId.equals(loginUser.getId());
    }
}
