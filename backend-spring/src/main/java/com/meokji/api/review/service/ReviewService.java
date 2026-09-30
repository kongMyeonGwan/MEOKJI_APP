package com.meokji.api.review.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import com.meokji.api.common.Page;
import com.meokji.api.review.dto.CommentResponse;
import com.meokji.api.review.dto.LikeResponse;
import com.meokji.api.review.dto.PopularItem;
import com.meokji.api.review.dto.ReviewCreateRequest;
import com.meokji.api.review.dto.ReviewDetailResponse;
import com.meokji.api.review.dto.ReviewFileInfo;
import com.meokji.api.review.dto.ReviewListItem;
import com.meokji.api.review.dto.ReviewUpdateRequest;
import com.meokji.api.user.vo.UserVO;

/**
 * 커뮤니티 게시판. 실제 코드는 ReviewServiceImpl 에 있다. (TODO: 게시판 담당)
 * 규칙은 docs/BOARD_SPEC.md 에 있다.
 */
public interface ReviewService {

    // ----- 글 -----
    Page<ReviewListItem> getList(String keyword, int page, int size);

    List<PopularItem> getPopular(LocalDate start, LocalDate end, int limit);

    // loginUser 는 로그인 안 했으면 null
    ReviewDetailResponse getDetail(UserVO loginUser, Long reviewId);

    ReviewDetailResponse add(UserVO loginUser, ReviewCreateRequest request);

    ReviewDetailResponse update(UserVO loginUser, Long reviewId, ReviewUpdateRequest request);

    void delete(UserVO loginUser, Long reviewId);

    int increaseView(Long reviewId);

    // ----- 좋아요 -----
    // liked 가 true 면 켜고, false 면 끈다
    LikeResponse setLike(UserVO loginUser, Long reviewId, boolean liked);

    // ----- 댓글 -----
    CommentResponse addComment(UserVO loginUser, Long reviewId, String content);

    CommentResponse updateComment(UserVO loginUser, Long reviewId, Long commentId, String content);

    void deleteComment(UserVO loginUser, Long reviewId, Long commentId);

    // ----- 사진 -----
    ReviewFileInfo attachFile(UserVO loginUser, Long reviewId, MultipartFile file);

    void detachFile(UserVO loginUser, Long reviewId);

    byte[] downloadFile(Long reviewId);
}
