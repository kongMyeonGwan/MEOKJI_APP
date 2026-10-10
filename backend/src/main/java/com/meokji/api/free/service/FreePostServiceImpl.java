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

/**
 * 자유게시판 글 · 댓글 · 사진.
 *
 * <p>이 프로젝트에서 가장 큰 서비스다. 한 기능에 세 가지가 들어 있고,
 * JPA 와 MyBatis 를 같이 쓴다.
 *
 * <p>왜 둘을 같이 쓰나
 *
 * <p>원래 전부 MyBatis 였다. 글쓰기 · 수정 · 삭제만 JPA 로 옮겼다 - 그 셋은
 * "한 행을 만들고 고치고 끄는" 단순한 일이라 SQL 을 손으로 적을 이유가 없다.
 * 목록 · 검색 · 페이징은 JOIN 과 COUNT 와 DBMS_LOB.SUBSTR 이 섞여 있어서
 * 메서드 이름으로 만들 수 있는 모양이 아니라 그대로 두었다.
 *
 * <p>둘을 섞으면서 조심할 것 — JPA 는 트랜잭션이 끝날 때 몰아서 DB 에 보낸다.
 * 그 사이에 MyBatis 로 같은 행을 읽으면 아직 안 보낸 값을 못 본다.
 * 그래서 update 에서 flush() 를 한 번 부른다(아래 주석 참고).
 *
 * <p>되풀이되는 네 가지를 private 메서드로 뺐다
 *   findAlivePost / findAliveEntity : 살아있는 글 찾기 (없으면 404)
 *   findAliveComment               : 살아있는 댓글 찾기 (글과 짝이 맞는지도)
 *   checkOwner                     : 내가 쓴 것인지 (아니면 403)
 *   fillDetail / fillFile          : 표에 없는 칸 채우기
 */
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

    /**
     * 목록 한 페이지.
     *
     * {@code @Transactional} 이 없다. 읽기만 한다.
     *
     * 다른 목록(history)과 달리 for 로 손볼 것이 없어서 DAO 가 준 목록을
     * 그대로 담는다. 닉네임 · 댓글 수는 SQL 이 JOIN 으로 이미 채워 왔다.
     */
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

    /**
     * 상세. 댓글 · 첨부 · "내 글인지" 를 같이 채워 돌려준다.
     *
     * {@code @Transactional} 이 없다. 읽기만 한다 — 조회수는 여기서
     * 올리지 않고 화면이 /view 를 따로 부른다.
     *
     * @param loginUser 로그인 안 했으면 null. isMine 이 false 가 된다
     */
    @Override
    public FreePostVO getDetail(UserVO loginUser, Long postId) {
        FreePostVO post = findAlivePost(postId);
        return fillDetail(post, loginUser);
    }

    /**
     * 글쓰기. JPA 가 한다.
     *
     * {@code @Transactional} 이 꼭 필요하다 — JPA 는 트랜잭션 안에서만
     * 변경을 추적한다. 없으면 save() 가 들어가도 그 뒤의 flush 나
     * 변경 감지가 동작하지 않는다.
     */
    @Override
    @Transactional
    public FreePostVO add(UserVO loginUser, FreePostVO request) {
        // 글쓴이는 토큰에서. 본문으로 받으면 남의 이름으로 쓸 수 있다
        FreePostEntity post = FreePostEntity.create(
                loginUser.getId(),
                request.getTitle().trim(),
                request.getContent());

        // save 는 "새것이면 INSERT, 있던 것이면 상태에 올려두기" 를 한다.
        // 여기는 새 글이라 INSERT 가 나간다
        freePostRepository.save(post);

        // 여기서 flush 를 부르지 않는다. id 가 오라클 IDENTITY 라 JPA 가 번호를
        // 받으려면 INSERT 를 바로 보내야 하고, 그래서 이 줄을 지날 때 이미 DB 에 들어가 있다.

        // 방금 넣은 행을 다시 읽는다. 닉네임 · 작성 시각은 DB 와 JOIN 이 채우는 값이다
        return fillDetail(findAlivePost(post.getId()), loginUser);
    }

    /**
     * 글 수정 (글쓴이만). JPA 가 한다.
     *
     * JPA 로 읽고 JPA 로 고치고, 화면에 줄 모양은 MyBatis 로 다시 읽는다.
     * 그 사이에 flush() 가 한 번 들어가는 이유를 아래에 적어 두었다.
     */
    @Override
    @Transactional
    public FreePostVO update(UserVO loginUser, Long postId, FreePostVO request) {
        // JPA 로 읽어야 한다. MyBatis 로 읽은 VO 는 JPA 가 추적하지 않아서
        // 값을 바꿔도 UPDATE 가 나가지 않는다
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

    /**
     * 글 삭제 (글쓴이만). use_yn 을 N 으로 바꾼다.
     *
     * 이 메서드만 같은 글을 두 번 읽는다 — MyBatis 로 한 번(사진 경로),
     * JPA 로 한 번(끄기). 번거로워 보이지만 각자 필요한 칸이 다르다.
     *
     * DB 와 디스크가 한 트랜잭션에 묶이지 않는 점에 주의 — 파일을 지운 뒤
     * use_yn 바꾸기가 실패하면, 글은 보이는데 사진이 없는 상태가 남는다.
     * 반대 순서면 글은 사라지고 파일만 남는다. 둘 중 전자가 덜 나쁘다고 보고
     * 파일을 먼저 지운다 (화면은 사진이 없으면 자리를 안 그린다).
     */
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

    /**
     * 조회수 +1. 올린 뒤의 값을 돌려준다.
     *
     * {@code @Transactional} 이 붙은 이유 — UPDATE 와 그 뒤의 SELECT 가
     * 한 묶음이어야 한다. 떨어져 있으면 그 사이에 누가 또 열어서
     * 내가 올린 값보다 큰 숫자가 돌아올 수 있다.
     */
    @Override
    @Transactional
    public int increaseView(Long postId) {
        findAlivePost(postId); // 없는 글이면 404
        // 돌려주는 값이 없는 UPDATE 다. SQL 안에서 +1 한다 -
        // 읽어서 더하면 동시에 열었을 때 하나만 오른다
        freePostDAO.increaseView(postId);
        // 올린 뒤의 값을 따로 읽는다. UPDATE 는 바뀐 값을 알려주지 않는다
        return freePostDAO.findViewCount(postId);
    }

    // ===================== 댓글 =====================

    /**
     * 댓글 쓰기.
     *
     * 댓글은 전부 MyBatis 다. JPA 로 옮기지 않은 이유는 글만 옮겼기
     * 때문이다 - 한 번에 다 바꾸지 않고 글부터 옮겨본 상태다.
     */
    @Override
    @Transactional
    public FreePostCommentVO addComment(UserVO loginUser, Long postId, String content) {
        findAlivePost(postId); // 지워진 글에는 댓글을 못 단다

        FreePostCommentVO comment = new FreePostCommentVO();
        comment.setPostId(postId);
        comment.setUserId(loginUser.getId());
        comment.setContent(content.trim());

        freeCommentDAO.insert(comment);

        // 다시 읽는 이유 — 닉네임은 JOIN 으로 오고 작성 시각은 DB 가 찍는다.
        // comment 를 그대로 돌려주면 화면에 이름과 시각이 비어 보인다
        FreePostCommentVO saved = freeCommentDAO.findById(comment.getId());
        // 방금 내가 쓴 댓글이므로 확인할 것도 없이 true 다.
        // 이 값이 있어야 화면이 수정 · 삭제 버튼을 바로 그린다
        saved.setIsMine(true);
        return saved;
    }

    /** 댓글 수정 (쓴 사람만). */
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

    /** 댓글 삭제 (쓴 사람만). 행을 지우지 않고 use_yn 을 N 으로. */
    @Override
    @Transactional
    public void deleteComment(UserVO loginUser, Long postId, Long commentId) {
        FreePostCommentVO comment = findAliveComment(postId, commentId);
        checkOwner(loginUser, comment.getUserId(), "댓글");

        freeCommentDAO.deactivate(commentId);
    }

    // ===================== 사진 =====================

    /**
     * 사진 붙이기 (글쓴이만). 이미 있으면 바꿔 단다.
     *
     * 순서가 중요하다 — 새 파일을 먼저 저장하고, 그 다음에 옛 파일을 지우고,
     * 마지막에 DB 를 고친다. 아래 주석에 이유를 적어 두었다.
     */
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

        // 글 전체를 넘기지 않고 id 와 file 만 담은 빈 VO 를 만든다.
        // setFile 의 SQL 이 쓰는 칸이 그 둘뿐이고, 글을 통째로 넘기면
        // 제목 · 내용까지 실려 보이지만 실제로는 안 쓰인다
        FreePostVO target = new FreePostVO();
        target.setId(postId);
        target.setFile(saving);
        // file_yn 과 FILE_ 칸 여섯 개를 한 SQL 로 바꾼다. 따로 바꾸면
        // 체크 제약(ck_free_posts_file_set)에 걸린다 (DAO 주석 참고)
        freePostDAO.setFile(target);

        // 다시 읽어 매퍼가 담아준 file 을 쓴다. 크기 표시와 주소만 마저 넣으면 된다
        FreePostVO attached = freePostDAO.findById(postId);
        fillFile(attached);
        return attached.getFile();
    }

    /** 사진 떼기 (글쓴이만). 디스크의 파일도 같이 지운다. */
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

    /**
     * 사진이 붙은 글을 돌려준다. 글을 볼 수 있으면 누구나.
     *
     * 로그인을 보지 않는다 — 글이 누구나 볼 수 있으니 거기 붙은 사진도
     * 누구나 볼 수 있어야 한다.
     *
     * 사진 자체가 아니라 글을 돌려주는 이유 — 컨트롤러가 응답 헤더를
     * 만들 때 원래 파일명과 확장자가 필요하다.
     */
    @Override
    public FreePostVO getFileOwner(Long postId) {
        FreePostVO post = findAlivePost(postId);

        if (!post.getHasFile()) {
            throw ApiException.notFound("붙어 있는 사진이 없습니다.");
        }
        return post;
    }

    /**
     * 디스크에서 사진 바이트를 읽는다.
     *
     * 글 번호가 아니라 위 getFileOwner 가 준 글을 받는다. 번호로 받으면
     * 글을 또 읽고 확인도 두 번 하게 된다.
     */
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

    /**
     * 내가 쓴 것이 아니면 403
     *
     * 404 가 아니라 403 인 이유 — 글은 누구나 볼 수 있다. 번호를 넣어
     * 글이 있는지 알아내는 것이 애초에 막혀 있지 않으므로, 감출 것이 없다.
     * (내 것만 보는 ban · history 에서는 반대로 404 로 감춘다)
     *
     * @param what "글" 또는 "댓글". 문장에 끼워 넣어 어느 쪽인지 알려준다
     */
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
        // null 이 올 일은 거의 없지만(MyBatis 는 빈 목록을 준다) 받아둔다.
        // null 이 그대로 나가면 화면의 comments.map 이 터진다
        if (comments == null) {
            comments = new ArrayList<FreePostCommentVO>();
        }

        for (FreePostCommentVO comment : comments) {
            comment.setIsMine(isMine(loginUser, comment.getUserId()));
        }

        post.setComments(comments);
        // 세어서 같이 보낸다. 화면이 comments.length 를 쓸 수도 있지만,
        // 목록에서는 댓글을 안 담고 개수만 보내므로 칸을 맞춰 둔다
        post.setCommentCount(comments.size());
        post.setIsMine(isMine(loginUser, post.getUserId()));
        fillFile(post);

        // 조회수가 null 이면 0 으로 맞춘다. 화면이 숫자로 쓰는 값이라
        // null 이 가면 "조회 null" 로 보인다
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

    /**
     * 로그인한 사람이 쓴 것인지. 비로그인이면 false
     *
     * loginUser 가 null 일 수 있어서 그 확인을 여기 한 번만 둔다.
     * 글과 댓글 둘 다 쓰므로 각자 적으면 한쪽에서 빠뜨리게 된다.
     */
    private boolean isMine(UserVO loginUser, Long ownerId) {
        return loginUser != null && ownerId.equals(loginUser.getId());
    }
}
