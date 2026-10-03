package com.meokji.api.review.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.meokji.api.common.ApiException;
import com.meokji.api.common.Page;
import com.meokji.api.review.vo.ReviewCommentVO;
import com.meokji.api.review.vo.LikeStatusVO;
import com.meokji.api.review.vo.PopularVO;
import com.meokji.api.review.vo.RecipeReviewVO;
import com.meokji.api.review.vo.ReviewDetailVO;
import com.meokji.api.review.vo.ReviewFileVO;
import com.meokji.api.review.vo.ReviewListVO;
import com.meokji.api.user.vo.UserVO;

/**
 * 커뮤니티 게시판.
 *
 * TODO(게시판 담당): 아래 메서드들을 채운다. 지금은 전부 501(미구현) 에러를 던진다.
 *   1. dao/ReviewDAO.java 에 메서드를 만든다     (예: List<RecipeReviewVO> findPage(...))
 *   2. resources/mapper/ReviewMapper.xml 에 SQL 을 쓴다
 *   3. 여기서 @Autowired private ReviewDAO reviewDAO; 로 받아서 부른다
 *   4. VO → 응답 DTO 로 옮겨 담아 돌려준다
 * 비슷한 예시: BanServiceImpl (목록 · 추가 · 삭제), HistoryServiceImpl (페이지 목록), LikeServiceImpl (켜기/끄기)
 * 규칙: docs/BOARD_SPEC.md
 */
@Service
public class ReviewServiceImpl implements ReviewService {

    private static final String TODO = "아직 만들지 않은 기능입니다. (게시판 담당자 작업 예정)";

    // ===================== 글 =====================

    @Override
    public Page<ReviewListVO> getList(String keyword, int page, int size) {
        // 예시) offset 과 total_pages 는 직접 계산하지 않는다. common/Paging.java 가 한다.
        // Paging paging = new Paging(page, size);
        // paging.setTotal(reviewDAO.count(keyword));
        //
        // List<ReviewListVO> items = new ArrayList<>();
        // for (RecipeReviewVO review : reviewDAO.findPage(keyword, paging.getOffset(), paging.getSize())) {
        //     items.add(...);
        // }
        //
        // Page<ReviewListVO> result = new Page<>();
        // result.fill(paging, items);
        // return result;
        throw ApiException.notImplemented(TODO);
    }

    @Override
    public List<PopularVO> getPopular(LocalDate start, LocalDate end, int limit) {
        throw ApiException.notImplemented(TODO);
    }

    @Override
    public ReviewDetailVO getDetail(UserVO loginUser, Long reviewId) {
        throw ApiException.notImplemented(TODO);
    }

    @Override
    public ReviewDetailVO add(UserVO loginUser, RecipeReviewVO request) {
        // 예시)
        // 1. 내 식사 기록인지 확인 (historyDAO.findById)
        // 2. 이미 이 기록으로 쓴 글이 있는지 확인 → 있으면 409
        // 3. RecipeReviewVO 를 만들어 reviewDAO.insert(review)
        // 4. getDetail(loginUser, review.getId()) 로 돌려준다
        throw ApiException.notImplemented(TODO);
    }

    @Override
    public ReviewDetailVO update(UserVO loginUser, Long reviewId, RecipeReviewVO request) {
        throw ApiException.notImplemented(TODO);
    }

    @Override
    public void delete(UserVO loginUser, Long reviewId) {
        throw ApiException.notImplemented(TODO);
    }

    @Override
    public int increaseView(Long reviewId) {
        throw ApiException.notImplemented(TODO);
    }

    // ===================== 좋아요 =====================

    @Override
    public LikeStatusVO setLike(UserVO loginUser, Long reviewId, boolean liked) {
        throw ApiException.notImplemented(TODO);
    }

    // ===================== 댓글 =====================

    @Override
    public ReviewCommentVO addComment(UserVO loginUser, Long reviewId, String content) {
        throw ApiException.notImplemented(TODO);
    }

    @Override
    public ReviewCommentVO updateComment(UserVO loginUser, Long reviewId, Long commentId, String content) {
        throw ApiException.notImplemented(TODO);
    }

    @Override
    public void deleteComment(UserVO loginUser, Long reviewId, Long commentId) {
        throw ApiException.notImplemented(TODO);
    }

    // ===================== 사진 =====================

    @Override
    public ReviewFileVO attachFile(UserVO loginUser, Long reviewId, MultipartFile file) {
        throw ApiException.notImplemented(TODO);
    }

    @Override
    public void detachFile(UserVO loginUser, Long reviewId) {
        throw ApiException.notImplemented(TODO);
    }

    @Override
    public byte[] downloadFile(Long reviewId) {
        throw ApiException.notImplemented(TODO);
    }
}
