package com.meokji.api.review.dao;

import org.apache.ibatis.annotations.Mapper;

<<<<<<< HEAD
import com.meokji.api.review.vo.RecipeReviewVO;

/**
 * RECIPE_REVIEWS 테이블 (커뮤니티 게시글). SQL 은 resources/mapper/ReviewMapper.xml 에 있다.
 * 댓글(POST_COMMENTS, BOARD_TYPE='R'), 좋아요(REVIEW_LIKES) 도 여기에 같이 만들어도 된다.
 *
 * TODO(게시판 담당): 필요한 메서드를 더 만든다. (목록, 개수, 작성, 수정, 삭제, 댓글, 좋아요 ...)
 * 메서드를 하나 만들면 ReviewMapper.xml 에도 id 가 같은 SQL 을 하나 만들어야 한다.
 * 규칙은 docs/BOARD_SPEC.md 에 있다.
 *
 * <p>이름이 어긋나면 서버가 뜰 때 "찾을 수 없다" 로 막힌다. 실행 중에
 * 터지는 것이 아니라 시작 자체가 안 되므로, 빠뜨린 것은 바로 드러난다.
 *
 * <p>지금은 메서드가 하나뿐이다. 서비스가 아직 501 을 던지고 있어서
 * 쓰이지도 않지만, 모양을 보여주려고 남겨두었다.
 *
 * <p>댓글 · 좋아요를 이 DAO 에 같이 둘지는 정하기에 따른다. 자유게시판은
 * 글(FreePostDAO)과 댓글(FreeCommentDAO)을 나눴다 — 표가 다르면 DAO 도
 * 나누는 쪽이 "이름만 보고 어느 표를 건드리는지" 알기 쉽다.
 */
@Mapper
public interface ReviewDAO {

    // 게시글 한 건 (지운 글도 나온다. use_yn 은 부르는 쪽에서 확인)
    //
    // SQL 에 use_yn 조건을 넣지 않는 이유 — 넣으면 "없는 번호" 와
    // "지운 글" 이 똑같이 null 로 돌아와서 구별할 수 없다
    // (BanDAO.findById 주석과 같은 이유)
    RecipeReviewVO findById(@Param("id") Long id);
=======
/** RECIPE_REVIEWS 테이블 (레시피 후기). */
@Mapper
public interface ReviewDAO {
>>>>>>> 137ab2e5d83abad60912fb1f0b6b8a1315e728eb
}
