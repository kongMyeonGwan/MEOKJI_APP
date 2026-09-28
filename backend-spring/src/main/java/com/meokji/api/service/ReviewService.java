package com.meokji.api.service;

import org.springframework.stereotype.Service;

import com.meokji.api.common.Page;
import com.meokji.api.controller.ReviewController;
import com.meokji.api.dto.review.CommentResponse;
import com.meokji.api.dto.review.LikeResponse;
import com.meokji.api.dto.review.PopularItem;
import com.meokji.api.dto.review.ReviewDetailResponse;
import com.meokji.api.dto.review.ReviewFileInfo;
import com.meokji.api.dto.review.ReviewListItem;
import com.meokji.api.mapper.HistoryMapper;
import com.meokji.api.mapper.UserMapper;
import com.meokji.api.vo.RecipeReview;
import com.meokji.api.vo.ReviewComment;
import com.meokji.api.vo.ReviewLike;

/**
 * 게시판(레시피 후기) 로직 — <b>아직 만들지 않았다. 담당자가 채운다.</b>
 *
 * <h2>여기에 만들 메서드</h2>
 *
 * <p>{@link ReviewController} 가 부를 메서드다. 이름과 인자는 바꿔도 되지만,
 * 컨트롤러가 돌려주는 DTO 모양에 맞는 값을 줘야 화면이 그대로 돈다.
 *
 * <pre>
 *   Page&lt;ReviewListItem&gt; list(keyword, page, size)          목록 + 검색 + 페이징
 *   List&lt;PopularItem&gt;   listPopular(start, end, limit)     기간 내 좋아요 순
 *   ReviewDetailResponse  get(reviewId, me)                    상세 + 댓글
 *   ReviewDetailResponse  create(me, request)                글쓰기
 *   ReviewDetailResponse  update(me, reviewId, request)        글 수정
 *   void                delete(me, reviewId)                 글 삭제 (use_yn='N')
 *   int                 increaseView(reviewId)               조회수 +1
 *   LikeResponse        setLike(me, reviewId, liked)          좋아요 켜기/끄기
 *   CommentResponse     createComment(me, reviewId, request)
 *   CommentResponse     updateComment(me, reviewId, commentId, request)
 *   void                deleteComment(me, reviewId, commentId)
 *   ReviewFileInfo        attachFile(me, reviewId, file)
 *   void                detachFile(me, reviewId)
 * </pre>
 *
 * <h2>쓸 수 있는 것은 이미 준비돼 있다</h2>
 *
 * <pre>
 *   표      recipe_reviews · review_comments · review_likes  (db/01_schema.sql)
 *   도메인   {@link RecipeReview} · {@link ReviewComment} · {@link ReviewLike}
 *   DTO     com.meokji.api.post.dto  (요청/응답 모양이 다 정의돼 있다)
 *   페이징   {@link com.meokji.api.common.Page}  (히스토리 · 공지와 같은 모양)
 *   JSON    {@link com.meokji.api.common.Json}  (CLOB 의 recipe_json 을 읽을 때)
 *   시각     {@link com.meokji.api.common.Times}  (응답용 문자열로)
 *   한국날짜 {@link com.meokji.api.common.Kst}  (인기 레시피 기간 검색에 필요)
 *   오류     {@link com.meokji.api.common.ApiException}  (409 · 404 · 403 …)
 * </pre>
 *
 * <p>매퍼는 만들어두지 않았다. {@code PostMapper} 인터페이스와
 * {@code resources/mapper/PostMapper.xml} 을 새로 만들면 된다 -
 * {@code UserMapper} · {@code HistoryMapper} 를 본으로 삼으면 된다.
 *
 * <h2>꼭 지켜야 할 것 넷</h2>
 *
 * <ol>
 *   <li><b>조회에 {@code use_yn='Y'} 를 붙인다.</b> 이 서비스는 물리 삭제를 하지
 *       않는다. 빼먹으면 지운 글이 목록에 다시 나타난다.
 *   <li><b>후기는 자기 식사 기록에서만 쓸 수 있다.</b> 글쓰기에서
 *       {@code meal_histories.user_id} 가 나인지 확인해야 한다. FK 가 막아주지만
 *       그때는 500 이 나가므로, 서비스가 먼저 보고 403 이나 404 를 내야 한다.
 *   <li><b>페이징은 COUNT 와 목록이 같은 조건을 봐야 한다.</b> 한쪽에만 검색어를
 *       붙이면 "3페이지가 있다는데 열면 비어 있는" 일이 생긴다.
 *       {@code HistoryMapper.xml} 의 {@code <sql id="mineWithShared">} 방식을 쓰면 된다.
 *   <li><b>남의 글을 고치거나 지우려 하면 403 이다.</b> 화면이 버튼을 숨기는 것은
 *       잠그는 것이 아니다. 서버가 다시 확인해야 한다.
 * </ol>
 *
 * <h2>오라클에서 걸리는 것</h2>
 *
 * <pre>
 *   대소문자 무시 검색   LOWER(col) LIKE LOWER(:kw)   — ILIKE 가 없다
 *   중복 INSERT         MERGE 또는 DuplicateKeyException 캐치 — ON CONFLICT 가 없다
 *   페이징              OFFSET ? ROWS FETCH NEXT ? ROWS ONLY — LIMIT 이 없다
 *   상위 N건            ORDER BY 를 안쪽에 두고 감싼다 — ROWNUM 은 정렬 전에 매겨진다
 *   빈 문자열           NULL 로 취급된다 — NOT NULL 컬럼에 ''를 넣으면 ORA-01400
 * </pre>
 */
@Service
public class ReviewService {

    // 담당자가 채운다. 지금은 컨트롤러가 501 을 돌려주므로 여기까지 오지 않는다.
}
