import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import RecipeView from '../../components/recipe/RecipeView'
import ReviewAttachment from '../../components/community/ReviewAttachment'
import CommentItem from '../../components/community/CommentItem'
import { SAMPLE_REVIEW_DETAIL } from '../../data/sampleBoard'
import useForm from '../../hook/useForm'
import { formatDateTime } from '../../utils/format'
import '../../styles/pages/ReviewDetailPage.css'

const COMMENT_MAX = 1000 // 댓글 최대 글자 수

// 게시글 상세 (/community/3) + 좋아요 + 댓글.
// 보는 건 로그인 없이 되고, 좋아요/댓글은 로그인해야 된다. 수정/삭제는 내 글만.
//
// TODO(게시판 담당): 지금은 가데이터(SAMPLE_REVIEW_DETAIL)를 그린다. 주소의 번호와 상관없이 같은 글이 나온다.
//   1. review, comments 를 useState 로 바꾸고
//   2. useEffect 에서 getReview(reviewId) 로 받아오고, countReviewView(reviewId) 로 조회수를 올린다
//   3. 아래 handle... 함수들의 TODO 자리에서 API 를 부른다
export default function ReviewDetailPage({ user }) {
  const { reviewId } = useParams() // 주소의 :reviewId 부분 (서버에 붙일 때 쓴다)

  const review = SAMPLE_REVIEW_DETAIL
  const comments = SAMPLE_REVIEW_DETAIL.comments

  // 좋아요
  const [liked, setLiked] = useState(review.liked)
  const [likeCount, setLikeCount] = useState(review.like_count)

  // 댓글 입력칸
  const [commentText, setCommentText] = useState('')

  // 글 수정. useForm 이 돌려준 이름이 위의 것들과 겹치지 않게 edit~ 로 바꿔 받는다
  const [editing, setEditing] = useState(false)
  const {
    values: editValues,
    handleChange: handleEditChange,
    reset: resetEdit,
  } = useForm({ title: review.title, content: review.content })

  // 좋아요 누르기
  // TODO: setReviewLike(review.id, !liked) 를 부르고 서버가 준 { liked, like_count } 를 넣는다
  const handleLike = () => {
    setLiked(!liked)
    setLikeCount(liked ? likeCount - 1 : likeCount + 1)
  }

  // 댓글 등록
  // TODO: addComment(review.id, commentText) 를 부르고 목록에 붙인다
  const handleCommentSubmit = (e) => {
    e.preventDefault() // 폼이 새로고침되는 걸 막는다
    if (!commentText.trim()) {
      return
    }
    alert('댓글 기능은 아직 준비 중입니다.')
    setCommentText('')
  }

  // 댓글 수정 — TODO: updateComment(review.id, comment.id, content)
  const handleCommentEdit = (comment, content) => {
    alert('댓글 기능은 아직 준비 중입니다.')
  }

  // 댓글 삭제 — TODO: removeComment(review.id, comment.id)
  const handleCommentDelete = (comment) => {
    if (window.confirm('댓글을 지울까요?')) {
      alert('댓글 기능은 아직 준비 중입니다.')
    }
  }

  // 글 수정 저장 — TODO: updateReview(review.id, { title: editValues.title, content: editValues.content })
  const handleEditSave = (e) => {
    e.preventDefault()
    alert('게시판 기능은 아직 준비 중입니다.')
    setEditing(false)
  }

  // 글 수정 취소 (원래 값으로 되돌린다)
  const handleEditCancel = () => {
    resetEdit()
    setEditing(false)
  }

  // 글 삭제 — TODO: removeReview(review.id) 후 navigate('/community', { replace: true })
  //   replace: 지워진 글 주소를 방문 기록에서 빼서, 뒤로 가기를 눌러도 다시 안 오게 한다
  const handleDelete = () => {
    if (window.confirm('이 글을 지울까요?')) {
      alert('게시판 기능은 아직 준비 중입니다.')
    }
  }

  // 사진 붙이기 — TODO: uploadReviewFile(review.id, e.target.files[0])
  const handlePickFile = (e) => {
    alert('사진 첨부는 아직 준비 중입니다.')
    e.target.value = '' // 같은 파일을 다시 골라도 onChange 가 불리게 비운다
  }

  // 사진 지우기 — TODO: removeReviewFile(review.id)
  const handleRemoveFile = () => {
    if (window.confirm('첨부한 사진을 지울까요?')) {
      alert('사진 첨부는 아직 준비 중입니다.')
    }
  }

  return (
    <div className="review">
      <p className="muted review__back">
        <Link to="/community">← 커뮤니티</Link>
      </p>

      <article className="card">
        {editing ? (
          <form className="review__edit" onSubmit={handleEditSave}>
            <label className="review__edit-field">
              <span>제목</span>
              <input type="text" name="title" value={editValues.title} onChange={handleEditChange} maxLength={200} />
            </label>
            <label className="review__edit-field">
              <span>내용</span>
              <textarea name="content" value={editValues.content} onChange={handleEditChange} rows={12} />
            </label>
            <div className="review__edit-actions">
              <button type="submit" className="btn btn--sm">
                저장
              </button>
              <button type="button" className="btn btn--ghost btn--sm" onClick={handleEditCancel}>
                취소
              </button>
            </div>
          </form>
        ) : (
          <>
            <h2 className="review__title">{review.title}</h2>

            <div className="review__meta">
              <span>{review.nickname}</span>
              <span className="muted">{formatDateTime(review.created_at)}</span>
              <span className="muted">조회 {review.view_count}</span>
              {review.food_name && <span className="tag">{review.food_name}</span>}
            </div>

            <div className="review__content">{review.content}</div>

            <ReviewAttachment
              file={review.file}
              isMine={review.is_mine}
              busy={false}
              error=""
              onPick={handlePickFile}
              onRemove={handleRemoveFile}
            />
          </>
        )}

        {/* 글쓴이가 먹은 레시피 */}
        {review.recipe_json && (
          <div className="review__recipe">
            <RecipeView recipe={review.recipe_json} ingredients={review.ingredients_json} />
          </div>
        )}

        <div className="review__actions">
          <button
            type="button"
            className={liked ? 'review__like is-on' : 'review__like'}
            onClick={handleLike}
            disabled={!user}
          >
            ♥ {likeCount}
          </button>

          {!user && (
            <span className="muted review__hint">
              <Link to="/login" className="review__link">
                로그인
              </Link>{' '}
              후 좋아요를 누를 수 있습니다
            </span>
          )}

          {/* 내 글이면 수정/삭제 버튼 */}
          {review.is_mine && !editing && (
            <span className="review__owner-actions">
              <button type="button" className="btn btn--ghost btn--sm" onClick={() => setEditing(true)}>
                수정
              </button>
              <button type="button" className="btn btn--ghost btn--sm review__danger" onClick={handleDelete}>
                삭제
              </button>
            </span>
          )}
        </div>
      </article>

      <section className="card">
        <h3 className="section-title">댓글 {comments.length}</h3>

        {comments.length === 0 && <p className="empty">첫 댓글을 남겨보세요.</p>}

        {comments.length > 0 && (
          <ul className="review__comments divided">
            {comments.map((comment) => (
              <CommentItem key={comment.id} comment={comment} onEdit={handleCommentEdit} onDelete={handleCommentDelete} />
            ))}
          </ul>
        )}

        {user ? (
          <form className="review__form" onSubmit={handleCommentSubmit}>
            <textarea
              value={commentText}
              onChange={(e) => setCommentText(e.target.value)}
              placeholder="댓글을 입력하세요"
              maxLength={COMMENT_MAX}
              rows={3}
            />
            <div className="review__form-foot">
              {/* 거의 다 차면 빨간 글씨로 알려준다 */}
              <span className={commentText.length > COMMENT_MAX - 100 ? 'review__count-warn' : 'muted'}>
                {commentText.length} / {COMMENT_MAX}
              </span>
              <button type="submit" className="btn btn--sm" disabled={!commentText.trim()}>
                등록
              </button>
            </div>
          </form>
        ) : (
          <p className="muted review__login-ask">
            <Link to="/login" className="review__link">
              로그인
            </Link>{' '}
            후 댓글을 작성할 수 있습니다.
          </p>
        )}
      </section>
    </div>
  )
}
