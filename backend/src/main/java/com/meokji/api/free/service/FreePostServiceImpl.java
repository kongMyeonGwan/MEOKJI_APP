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
import com.meokji.api.free.entity.FreePostEntity;
import com.meokji.api.free.repository.FreePostRepository;
import com.meokji.api.free.vo.FreeFileVO;
import com.meokji.api.free.vo.FreePostCommentVO;
import com.meokji.api.free.vo.FreePostVO;
import com.meokji.api.user.vo.UserVO;
import com.meokji.api.util.FileStore;

@Service
public class FreePostServiceImpl implements FreePostService {

    // 글쓰기 · 수정 · 삭제는 JPA 가 한다
    @Autowired
    private FreePostRepository freePostRepository;

    // 나머지(목록 · 검색 · 페이징 · 조회수 · 첨부)는 MyBatis 가 한다
    @Autowired
    private FreePostDAO freePostDAO;

    @Autowired
    private FreeCommentDAO freeCommentDAO;

    @Autowired
    private FileStore fileStore;

    // ===================== 글 =====================

    @Override
    public Page<FreePostVO> getList(String keyword, int page, int size) {
        // offset 과 total_pages 계산은 Paging 이 한다 (common/Paging.java)
        Paging paging = new Paging(page, size);
        paging.setTotal(freePostDAO.count(keyword));

        List<FreePostVO> items = freePostDAO.findPage(keyword, paging.getOffset(), paging.getSize());

        Page<FreePostVO> result = new Page<>();
        result.fill(paging, items);
        return result;
    }

    @Override
    public FreePostVO getDetail(UserVO loginUser, Long postId) {
        FreePostVO post = findAlivePost(postId);
        return fillDetail(post, loginUser);
    }

    @Override
    @Transactional
    public FreePostVO add(UserVO loginUser, FreePostVO request) {
        // 글쓴이는 토큰에서. 본문으로 받으면 남의 이름으로 쓸 수 있다
        FreePostEntity post = FreePostEntity.create(
                loginUser.getId(),
                request.getTitle().trim(),
                request.getContent());

        freePostRepository.save(post);

        // 여기서 flush 를 부르지 않는다. id 가 오라클 IDENTITY 라 JPA 가 번호를
        // 받으려면 INSERT 를 바로 보내야 하고, 그래서 이 줄을 지날 때 이미 DB 에 들어가 있다.

        // 방금 넣은 행을 다시 읽는다. 닉네임 · 작성 시각은 DB 와 JOIN 이 채우는 값이다
        return fillDetail(findAlivePost(post.getId()), loginUser);
    }

    @Override
    @Transactional
    public FreePostVO update(UserVO loginUser, Long postId, FreePostVO request) {
        FreePostEntity post = findAliveEntity(postId);
        checkOwner(loginUser, post.getUserId(), "글");

        // setter 를 부르는 것만으로 UPDATE 가 나간다 (변경 감지).
        // save() 를 다시 부르지 않는다
        post.change(request.getTitle().trim(), request.getContent());

        // 바꾼 값을 DB 로 먼저 내보낸다.
        //
        // JPA 는 트랜잭션이 끝날 때 몰아서 보내는데, 바로 아래 findAlivePost 는
        // MyBatis 라 JPA 가 들고 있는 값을 모른다. 이 줄이 없으면 방금 고친 글을
        // 읽는데 옛 제목이 나온다.
        freePostRepository.flush();

        return fillDetail(findAlivePost(postId), loginUser);
    }

    @Override
    @Transactional
    public void delete(UserVO loginUser, Long postId) {
        // 먼저 MyBatis 로 읽는다. 붙어 있는 사진의 저장 경로가 여기 들어 있고,
        // 그 칸들은 JPA 쪽(FreePostEntity)에 일부러 안 두었다
        FreePostVO post = findAlivePost(postId);
        checkOwner(loginUser, post.getUserId(), "글");

        // 붙어 있던 사진도 같이 지운다. 글이 안 보이는데 파일만 남으면
        // 디스크만 계속 찬다 (주소를 아는 사람에게는 계속 열린다)
        deleteStoredFile(post);

        // 행은 지우지 않고 use_yn 만 뒤집는다 (변경 감지로 UPDATE 가 나간다)
        findAliveEntity(postId).deactivate();
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
        deleteStoredFile(post);

        FreeFileVO saving = new FreeFileVO();
        saving.setPath(saved[0]);
        saving.setSavedName(saved[1]);
        saving.setName(file.getOriginalFilename());
        saving.setExt(fileStore.extensionOf(file.getOriginalFilename()));
        saving.setSize(file.getSize());

        FreePostVO target = new FreePostVO();
        target.setId(postId);
        target.setFile(saving);
        freePostDAO.setFile(target);

        // 다시 읽어 매퍼가 담아준 file 을 쓴다. 크기 표시와 주소만 마저 넣으면 된다
        FreePostVO attached = freePostDAO.findById(postId);
        fillFile(attached);
        return attached.getFile();
    }

    @Override
    @Transactional
    public void detachFile(UserVO loginUser, Long postId) {
        FreePostVO post = findAlivePost(postId);
        checkOwner(loginUser, post.getUserId(), "글");

        if (!post.getHasFile()) {
            throw ApiException.notFound("붙어 있는 사진이 없습니다.");
        }

        deleteStoredFile(post);
        freePostDAO.clearFile(postId);
    }

    @Override
    public FreePostVO getFileOwner(Long postId) {
        FreePostVO post = findAlivePost(postId);

        if (!post.getHasFile()) {
            throw ApiException.notFound("붙어 있는 사진이 없습니다.");
        }
        return post;
    }

    @Override
    public byte[] readFile(FreePostVO post) {
        return fileStore.read(post.getFile().getPath(), post.getFile().getSavedName());
    }

    // ===================== 안쪽에서만 쓰는 것 =====================

    /**
     * 살아있는 글 하나를 JPA 로. 없거나 지워졌으면 404.
     *
     * 아래 findAlivePost 와 읽는 행은 같지만 쓰임이 다르다. 이쪽은 JPA 가
     * 들고 있다가 고친 것을 알아채는 대상이고, 저쪽은 화면으로 나갈 모양이다.
     * 닉네임 · 댓글 수는 이쪽에 없다.
     */
    private FreePostEntity findAliveEntity(Long postId) {
        return freePostRepository
                .findByIdAndUseYn(postId, "Y")
                .orElseThrow(() -> ApiException.notFound("글을 찾을 수 없습니다."));
    }

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

    /**
     * 상세에만 필요한 칸을 글에 채워 넣는다.
     *
     * 목록 SQL 은 이 칸들을 안 가져오므로 목록에서는 null 로 남는다 — 댓글 ·
     * 첨부 · 내 글 여부는 상세를 열 때만 있으면 되는 값이다.
     *
     * 새 VO 로 옮겨 담지 않고 받은 글에 그대로 채운다. 칸이 같은 클래스라
     * 옮겨 담을 이유가 없고, 옮기다 한 칸을 빠뜨리는 실수도 안 난다.
     */
    private FreePostVO fillDetail(FreePostVO post, UserVO loginUser) {
        List<FreePostCommentVO> comments = freeCommentDAO.findByPost(post.getId());
        if (comments == null) {
            comments = new ArrayList<FreePostCommentVO>();
        }

        for (FreePostCommentVO comment : comments) {
            comment.setIsMine(isMine(loginUser, comment.getUserId()));
        }

        post.setComments(comments);
        post.setCommentCount(comments.size());
        post.setIsMine(isMine(loginUser, post.getUserId()));
        fillFile(post);

        if (post.getViewCount() == null) {
            post.setViewCount(0);
        }
        return post;
    }

    /**
     * 첨부의 나머지 두 칸을 채운다. 다섯 칸은 매퍼가 이미 담아 왔다.
     *
     * 첨부가 없으면 file 이 null 이라 할 일이 없다.
     */
    private void fillFile(FreePostVO post) {
        FreeFileVO file = post.getFile();

        if (file == null) {
            return;
        }

        file.setSizeText(fileStore.sizeText(file.getSize()));
        // 업로드 폴더를 그대로 열지 않는다. 지워진 글의 사진이 주소만 알면 계속 보인다
        file.setUrl("/api/free-posts/" + post.getId() + "/file");
    }

    /** 디스크에 저장해둔 사진을 지운다. 첨부가 없으면 할 일이 없다 */
    private void deleteStoredFile(FreePostVO post) {
        if (post.getFile() == null) {
            return;
        }

        fileStore.delete(post.getFile().getPath(), post.getFile().getSavedName());
    }

    /** 로그인한 사람이 쓴 것인지. 비로그인이면 false */
    private boolean isMine(UserVO loginUser, Long ownerId) {
        return loginUser != null && ownerId.equals(loginUser.getId());
    }
}
