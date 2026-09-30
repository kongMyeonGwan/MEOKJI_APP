package com.meokji.api.dao;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.meokji.api.vo.RecipeReviewVO;

/**
 * RECIPE_REVIEWS 테이블 (커뮤니티 게시글). SQL 은 resources/mapper/ReviewMapper.xml 에 있다.
 * 댓글(REVIEW_COMMENTS), 좋아요(REVIEW_LIKES) 도 여기에 같이 만들어도 된다.
 *
 * TODO(게시판 담당): 필요한 메서드를 더 만든다. (목록, 개수, 작성, 수정, 삭제, 댓글, 좋아요 ...)
 * 메서드를 하나 만들면 ReviewMapper.xml 에도 id 가 같은 SQL 을 하나 만들어야 한다.
 * 규칙은 docs/BOARD_SPEC.md 에 있다.
 */
@Mapper
public interface ReviewDAO {

    // 게시글 한 건 (지운 글도 나온다. use_yn 은 부르는 쪽에서 확인)
    RecipeReviewVO findById(@Param("id") Long id);
}
