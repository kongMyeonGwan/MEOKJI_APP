package com.meokji.api.free.service;

import org.springframework.web.multipart.MultipartFile;

import com.meokji.api.common.Page;
import com.meokji.api.free.vo.FreeFileVO;
import com.meokji.api.free.vo.FreePostCommentVO;
import com.meokji.api.free.vo.FreePostVO;
import com.meokji.api.user.vo.UserVO;

/**
 * 자유게시판. 실제 코드는 FreePostServiceImpl 에 있다.
 *
 * 보기는 누구나, 쓰기 · 수정 · 삭제 · 댓글은 로그인해야 한다.
 * 수정 · 삭제는 글쓴이 본인만 된다 — 그 판단을 전부 여기서 한다.
 * 화면이 버튼을 숨기는 것은 눈에 안 보이게 하는 것일 뿐이다.
 *
 * <p>메서드가 열두 개다. 글 · 댓글 · 사진이 한 기능에 들어 있어서
 * 주석으로 세 묶음으로 나눠 두었다.
 *
 * <p>UserVO 를 받는 메서드와 안 받는 메서드가 나뉜다
 *   안 받는 것 : getList · increaseView · getFileOwner · readFile
 *                누구나 할 수 있는 일이라 누구인지가 판단에 안 들어간다
 *   null 을 받는 것 : getDetail — 로그인 없이도 보이지만, 로그인했으면
 *                "내 글인지" 를 같이 알려줘야 한다
 *   꼭 받는 것 : 나머지. 쓰거나 고치거나 지우는 일이다
 *
 * <p>JPA 와 MyBatis 를 같이 쓰는 유일한 기능이다. 글쓰기 · 수정 · 삭제는
 * JPA, 나머지는 MyBatis 다. 이 인터페이스에서는 그 차이가 드러나지 않는다 -
 * 부르는 쪽(컨트롤러)이 알 필요가 없는 사정이다.
 */
public interface FreePostService {

    // ---------- 글 ----------

    /** 목록 한 페이지. keyword 가 있으면 제목 · 내용에서 찾는다 */
    Page<FreePostVO> getList(String keyword, int page, int size);

    /** 상세. loginUser 가 null 이면(비로그인) isMine 은 false 다 */
    FreePostVO getDetail(UserVO loginUser, Long postId);

    /** 글쓰기 */
    FreePostVO add(UserVO loginUser, FreePostVO request);

    /** 글 수정 (글쓴이만) */
    FreePostVO update(UserVO loginUser, Long postId, FreePostVO request);

    /** 글 삭제 (글쓴이만). use_yn='N' */
    void delete(UserVO loginUser, Long postId);

    /** 조회수 +1. 올린 뒤의 값을 돌려준다 */
    int increaseView(Long postId);

    // ---------- 댓글 ----------

    /** 댓글 쓰기 */
    FreePostCommentVO addComment(UserVO loginUser, Long postId, String content);

    /** 댓글 수정 (쓴 사람만) */
    FreePostCommentVO updateComment(UserVO loginUser, Long postId, Long commentId, String content);

    /** 댓글 삭제 (쓴 사람만) */
    void deleteComment(UserVO loginUser, Long postId, Long commentId);

    // ---------- 사진 ----------

    /** 사진 붙이기 (글쓴이만). 이미 있으면 바꿔 단다 */
    FreeFileVO attachFile(UserVO loginUser, Long postId, MultipartFile file);

    /** 사진 떼기 (글쓴이만) */
    void detachFile(UserVO loginUser, Long postId);

    /**
     * 사진 내려받기. 글을 볼 수 있으면 누구나
     *
     * 사진을 주는 게 아니라 "그 사진이 붙은 글" 을 돌려준다. 글이
     * 살아있는지 · 사진이 붙어 있는지 확인하고, 아니면 404 를 던진다.
     *
     * 아래 readFile 과 둘로 나눈 이유 — 컨트롤러가 응답 헤더를 만들 때
     * 원래 파일명과 확장자가 필요하다. 바이트만 받으면 그 정보가 없다.
     */
    FreePostVO getFileOwner(Long postId);

    /**
     * 저장해둔 사진의 바이트
     *
     * 글 번호가 아니라 위 getFileOwner 가 준 글을 받는다. 번호로 받으면
     * 글을 또 읽어야 하고, 확인도 두 번 하게 된다.
     */
    byte[] readFile(FreePostVO post);
}
