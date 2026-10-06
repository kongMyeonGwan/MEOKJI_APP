package com.meokji.api.review.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.meokji.api.common.Page;
import com.meokji.api.review.vo.ReviewCommentVO;
import com.meokji.api.review.vo.LikeStatusVO;
import com.meokji.api.review.vo.RecipeReviewVO;
import com.meokji.api.review.vo.ReviewFileVO;
import com.meokji.api.user.vo.UserVO;

/**
 * 커뮤니티 게시판. 실제 코드는 ReviewServiceImpl 에 있다. (TODO: 게시판 담당)
 * 규칙은 docs/BOARD_SPEC.md 에 있다.
 */
public interface ReviewService {

    // ----- 글 -----
    Page<RecipeReviewVO> getList(String keyword, int page, int size);

    // 인기 목록. 기간 안에 좋아요를 많이 받은 후기 순 — 같은 표라 VO 도 같다
    List<RecipeReviewVO> getPopular(LocalDate start, LocalDate end, int limit);

    // loginUser 는 로그인 안 했으면 null
    RecipeReviewVO getDetail(UserVO loginUser, Long reviewId);

    RecipeReviewVO add(UserVO loginUser, RecipeReviewVO request);

    RecipeReviewVO update(UserVO loginUser, Long reviewId, RecipeReviewVO request);

    void delete(UserVO loginUser, Long reviewId);

    int increaseView(Long reviewId);

    // ----- 좋아요 -----
    // liked 가 true 면 켜고, false 면 끈다
    LikeStatusVO setLike(UserVO loginUser, Long reviewId, boolean liked);

    // ----- 댓글 -----
    ReviewCommentVO addComment(UserVO loginUser, Long reviewId, String content);

    ReviewCommentVO updateComment(UserVO loginUser, Long reviewId, Long commentId, String content);

    void deleteComment(UserVO loginUser, Long reviewId, Long commentId);

    // ----- 사진 -----
    ReviewFileVO attachFile(UserVO loginUser, Long reviewId, MultipartFile file);

    void detachFile(UserVO loginUser, Long reviewId);

    byte[] downloadFile(Long reviewId);
}
