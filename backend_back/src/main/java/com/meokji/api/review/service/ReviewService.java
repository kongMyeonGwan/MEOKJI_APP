package com.meokji.api.review.service;

<<<<<<< HEAD
import java.time.LocalDate;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.meokji.api.common.Page;
import com.meokji.api.review.vo.ReviewCommentVO;
import com.meokji.api.review.vo.RecipeReviewVO;
import com.meokji.api.review.vo.ReviewFileVO;
import com.meokji.api.review.vo.ReviewLikeVO;
import com.meokji.api.user.vo.UserVO;

/**
 * 커뮤니티 게시판. 실제 코드는 ReviewServiceImpl 에 있다. (TODO: 게시판 담당)
 * 규칙은 docs/BOARD_SPEC.md 에 있다.
 *
 * <p>인터페이스는 완성돼 있고 구현만 비어 있다. 메서드 이름 · 인자 ·
 * 돌려주는 모양이 여기서 정해지므로, 컨트롤러는 이미 이대로 부르고 있다.
 * 구현을 채울 때 이 파일은 안 고쳐도 된다.
 *
 * <p>자유게시판(FreePostService)과 거의 같은 모양이다. 다른 점이 둘이다
 *   getPopular : 기간 안에 좋아요를 많이 받은 순. 자유게시판에는 없다
 *   setLike    : 좋아요. 자유게시판에는 없다
 *
 * <p>그리고 글을 쓰는 조건이 다르다 — 후기는 내 식사 기록을 하나 골라야
 * 쓸 수 있다. "실제로 먹어본 사람의 후기" 로 두려는 것이다.
 *
 * <p>구현할 때 자유게시판 쪽을 본보기로 쓸 수 있다. 되풀이되는 네 가지를
 * private 메서드로 빼둔 모양이 그대로 들어맞는다 — 살아있는 글 찾기 ·
 * 살아있는 댓글 찾기 · 내가 쓴 것인지 · 표에 없는 칸 채우기.
 */
public interface ReviewService {

    // ----- 글 -----

    /** 목록 한 페이지. keyword 가 있으면 제목 · 내용에서 찾는다 */
    Page<RecipeReviewVO> getList(String keyword, int page, int size);

    // 인기 목록. 기간 안에 좋아요를 많이 받은 후기 순 — 같은 표라 VO 도 같다
    List<RecipeReviewVO> getPopular(LocalDate start, LocalDate end, int limit);

    /**
     * 상세. 댓글 · 첨부 · 레시피 · "내 글인지" · "내가 좋아요 눌렀는지" 를 채워 돌려준다
     *
     * loginUser 는 로그인 안 했으면 null
     */
    RecipeReviewVO getDetail(UserVO loginUser, Long reviewId);

    /**
     * 글쓰기. 내 식사 기록을 하나 골라야 쓸 수 있다
     *
     * 한 기록으로는 한 번만 쓸 수 있다. 두 번째는 409 다 - 같은 식사를
     * 여러 번 후기로 올리면 인기 목록이 한 사람 글로 찬다
     */
    RecipeReviewVO add(UserVO loginUser, RecipeReviewVO request);

    /** 글 수정 (글쓴이만) */
    RecipeReviewVO update(UserVO loginUser, Long reviewId, RecipeReviewVO request);

    /** 글 삭제 (글쓴이만). use_yn 을 N 으로 */
    void delete(UserVO loginUser, Long reviewId);

    /**
     * 조회수 +1. 올린 뒤의 값을 돌려준다
     *
     * 로그인을 보지 않는다. 로그인 안 한 사람이 읽어도 조회는 조회다
     */
    int increaseView(Long reviewId);

    // ----- 좋아요 -----

    /**
     * liked 가 true 면 켜고, false 면 끈다. 바꾼 뒤의 상태를 돌려준다
     *
     * 찜(LikeService.toggle)과 다르다 — 찜은 서버가 지금 상태를 보고
     * 뒤집고, 이쪽은 화면이 켤지 끌지를 정한다. 주소가 PUT 과 DELETE 로
     * 나뉘어 있어서 무엇을 하려는지가 이미 드러나 있기 때문이다.
     *
     * 뒤집기가 아니라서 여러 번 보내도 결과가 같다 - 이미 켜진 것에
     * PUT 을 또 보내도 켜진 상태다
     */
    ReviewLikeVO setLike(UserVO loginUser, Long reviewId, boolean liked);

    // ----- 댓글 -----

    /** 댓글 쓰기 */
    ReviewCommentVO addComment(UserVO loginUser, Long reviewId, String content);

    /**
     * 댓글 수정 (쓴 사람만)
     *
     * reviewId 를 같이 받는 이유 — 그 댓글이 정말 그 글의 댓글인지
     * 확인해야 한다. 안 보면 남의 글 주소에 내 댓글 번호를 끼워 넣는
     * 요청이 통과한다
     */
    ReviewCommentVO updateComment(UserVO loginUser, Long reviewId, Long commentId, String content);

    /** 댓글 삭제 (쓴 사람만) */
    void deleteComment(UserVO loginUser, Long reviewId, Long commentId);

    // ----- 사진 -----

    /** 사진 붙이기 (글쓴이만). 글 하나당 사진 하나다 */
    ReviewFileVO attachFile(UserVO loginUser, Long reviewId, MultipartFile file);

    /** 사진 떼기 (글쓴이만) */
    void detachFile(UserVO loginUser, Long reviewId);

    /**
     * 사진 바이트. 글을 볼 수 있으면 누구나
     *
     * 자유게시판은 이 자리가 둘로 나뉘어 있다(getFileOwner + readFile) -
     * 컨트롤러가 응답 헤더에 원래 파일명을 넣으려고 글이 필요해서다.
     * 이쪽은 바이트만 돌려주는 모양이라, 그 헤더를 붙이려면 구현할 때
     * 자유게시판처럼 나눠야 할 수 있다
     */
    byte[] downloadFile(Long reviewId);
=======
/** 커뮤니티 게시판 (레시피 후기). 실제 코드는 ReviewServiceImpl 에 있다. */
public interface ReviewService {
>>>>>>> 137ab2e5d83abad60912fb1f0b6b8a1315e728eb
}
