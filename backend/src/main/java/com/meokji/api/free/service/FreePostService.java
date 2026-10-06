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

    /** 사진 내려받기. 글을 볼 수 있으면 누구나 */
    FreePostVO getFileOwner(Long postId);

    /** 저장해둔 사진의 바이트 */
    byte[] readFile(FreePostVO post);
}
