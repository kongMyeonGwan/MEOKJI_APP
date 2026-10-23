import { useState } from 'react'
import { Link } from 'react-router-dom'
import RecipeView from '../../../components/common/RecipeView'
import ReviewAttachment from './components/ReviewAttachment'
import CommentItem from './components/CommentItem'
import { SAMPLE_REVIEW_DETAIL } from '../../../data/sampleBoard'
import { formatDateTime } from '../../../utils/format'

const COMMENT_MAX = 1000 // 댓글 최대 글자 수

// 게시글 상세 (/community/3) + 좋아요 + 댓글.
//
// 퍼블리싱만 된 화면이다. 가데이터를 그리고, 주소의 번호와 상관없이 같은 글이 나온다.
// 수정 폼 열고 닫기만 동작하고 버튼들은 아직 아무 일도 하지 않는다.
//
// TODO(게시판 담당)
//   - useParams 로 :reviewId 를 꺼내 getReview(reviewId) 로 받아온다
//   - 조회수를 countReviewView(reviewId) 로 올린다
//   - 좋아요 · 댓글 등록/수정/삭제 · 글 수정/삭제 · 사진 첨부를 붙인다
//   - 로그인 여부로 버튼을 가린다 (App.jsx 의 <Route> 에서 user={user} 를 넘겨받는다)
//   - 내 글인지는 서버가 주는 isMine 으로 판단한다
export default function ReviewDetailPage() {
  const review = SAMPLE_REVIEW_DETAIL
  const comments = SAMPLE_REVIEW_DETAIL.comments

  // 글 수정 폼을 열었는지 (화면 전환만 한다)
  const [editing, setEditing] = useState(false)

  return (
    <div className="review">
      <p className="muted review__back">
        <Link to="/community">← 커뮤니티</Link>
      </p>

      <article className="card">
        {editing ? (
          <div className="review__edit">
            <label className="review__edit-field">
              <span>제목</span>
              <input type="text" defaultValue={review.title} maxLength={200} />
            </label>
            <label className="review__edit-field">
              <span>내용</span>
              <textarea defaultValue={review.content} rows={12} />
            </label>
            <div className="review__edit-actions">
              <button type="button" className="btn btn--sm">
                저장
              </button>
              <button type="button" className="btn btn--ghost btn--sm" onClick={() => setEditing(false)}>
                취소
              </button>
            </div>
          </div>
        ) : (
          <>
            <h2 className="review__title">{review.title}</h2>

            <div className="review__meta">
              <span>{review.nickname}</span>
              <span className="muted">{formatDateTime(review.createdAt)}</span>
              <span className="muted">조회 {review.viewCount}</span>
              {review.foodName && <span className="tag">{review.foodName}</span>}
            </div>

            <div className="review__content">{review.content}</div>

            <ReviewAttachment file={review.file} />
          </>
        )}

        {/* 글쓴이가 먹은 레시피 */}
        {review.recipeJson && (
          <div className="review__recipe">
            <RecipeView recipe={review.recipeJson} ingredients={review.ingredientsJson} />
          </div>
        )}

        <div className="review__actions">
          <button type="button" className={review.liked ? 'review__like is-on' : 'review__like'}>
            ♥ {review.likeCount}
          </button>

          {/* 내 글이면 수정/삭제 버튼 */}
          {review.isMine && !editing && (
            <span className="review__owner-actions">
              <button type="button" className="btn btn--ghost btn--sm" onClick={() => setEditing(true)}>
                수정
              </button>
              <button type="button" className="btn btn--ghost btn--sm review__danger">
                삭제
              </button>
            </span>
          )}
        </div>
      </article>

      <section className="card">
        <h3 className="section-title">댓글 {comments.length}</h3>

        <ul className="review__comments divided">
          {comments.map((comment) => (
            <CommentItem key={comment.id} comment={comment} />
          ))}
        </ul>

        <div className="review__form">
          <textarea placeholder="댓글을 입력하세요" maxLength={COMMENT_MAX} rows={3} />
          <div className="review__form-foot">
            <span className="muted">0 / {COMMENT_MAX}</span>
            <button type="button" className="btn btn--sm">
              등록
            </button>
          </div>
        </div>
      </section>
    </div>
  )
}
