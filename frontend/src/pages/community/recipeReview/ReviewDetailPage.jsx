import { useEffect, useState } from 'react'
import { Link, useNavigate, useParams } from 'react-router-dom'
import Modal from '../../../components/common/Modal'
import ReviewCommentItem from './components/CommentItem'
import ReviewAttachment from './components/ReviewAttachment'
import Loading from '../../../components/common/Loading'
import ErrorMessage from '../../../components/common/ErrorMessage'
import {
  addReviewComment,
  countReviewView,
  removeReviewComment,
  updateReviewComment,
  getReview,
  removeReview,
  updateReview,
  uploadReviewFile,
  removeReviewFile
} from '../../../api/reviews'
import { formatDateTime } from '../../../utils/format'

const COMMENT_MAX = 1000
export default function ReviewDetailPage({ user }) {
  const { reviewId } = useParams()

<<<<<<< HEAD
  const [review, setReview] = useState(null)
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [actionError, setActionError] = useState('')
=======
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
>>>>>>> develop

  const [editing, setEditing] = useState(false)
  const [editTitle, setEditTitle] = useState('')
  const [editContent, setEditContent] = useState('')
  const [editMealId, setEditMealId] = useState('')

  const [commentText, setCommentText] = useState('')

  const [confirmOpen, setConfirmOpen] = useState(false)

  const [fileBusy, setFileBusy] = useState(false)
  const [fileError, setFileError] = useState('')

  const navigate = useNavigate()


  const messageOf = (err) => {
    if (err.response) {
      // 입력값 검사에 걸리면 detail 이 배열로 온다. 예) [{ msg: '댓글을 입력해주세요' }]
      const detail = err.response.data.detail
      if (Array.isArray(detail)) {
        return detail[0].msg
      }
      return detail || '요청을 처리하지 못했습니다.'
    }
    if (err.request) {
      // 백엔드를 안 켰거나 30초가 지난 경우
      return '서버에 연결할 수 없습니다. 백엔드가 실행 중인지 확인하세요.'
    }
    return err.message
  }

  // 화면에 들어올 때 한 번: 글을 받아오고 조회수를 올린다
  useEffect(() => {
    let active = true
    const load = async () => {
      setLoading(true)
      try {
        const data = await getReview(reviewId)
        if (!active) return
        setReview(data)
        setError('')

        // 조회수는 여기서 따로 올린다. 목록을 열 때마다 오르면 숫자가 의미를 잃는다.
        // 실패해도 글은 이미 보이니 화면을 막지 않는다.
        try {
          const count = await countReviewView(reviewId)
          if (active) setReview({ ...data, view_count: count })
        } catch (ignore) {
          // 조회수만 못 올린 것이라 넘어간다
        }
      } catch (err) {
        if (active) setError(messageOf(err))
      } finally {
        if (active) setLoading(false)
      }
    }
    load()
    return () => { active = false }
  }, [reviewId])

  const handleEditOpen = () => {
    setEditTitle(review.title)
    setEditContent(review.content)
    setEditMealId(review.meal_history_id)
    setActionError('')
    setEditing(true)
  }

  const handleEditSave = async (e) => {
    e.preventDefault()

    if (!editTitle.trim()) {
      setActionError('제목을 입력해 주세요')
      return
    }
    if (!editContent.trim()) {
      setActionError('내용을 입력해 주세요')
      return
    }

    setActionError('')
    try {
      const data = await updateReview(reviewId, {
        title: editTitle.trim(),
        content: editContent,
        meal_history_id: editMealId,
      })
      setReview(data)
      setEditing(false)
    } catch (err) {
      setActionError(messageOf(err))
    }
  }

  // 글 삭제. replace를 쓰면 뒤로 가기를 눌러도 지워진 글로 안 돌아온다.
  const handleDelete = async () => {
    setConfirmOpen(false)
    try {
      await removeReview(reviewId)
      navigate('/reviews', { replace: true })
    } catch (err) {
      setActionError(messageOf(err))
    }
  }

  // 댓글 등록
  const handleCommentSubmit = async (e) => {
    e.preventDefault()

    if (!commentText.trim()) {
      return
    }

    setActionError('')
    try {
      const added = await addReviewComment(reviewId, commentText.trim())
      setReview({ ...review, comments: [...review.comments, added] })
      setCommentText('')
    } catch (err) {
      setActionError(messageOf(err))
    }
  }

  // 댓글 수정
  const handleCommentEdit = async (comment, content) => {
    setActionError('')
    try {
      const saved = await updateReviewComment(reviewId, comment.id, content)
      const next = review.comments.map((item) => (item.id === comment.id ? saved : item))
      setReview({ ...review, comments: next })
    } catch (err) {
      setActionError(messageOf(err))
    }
  }

  // 댓글 삭제
  const handleCommentDelete = async (comment) => {
    setActionError('')
    try {
      await removeReviewComment(reviewId, comment.id)
      setReview({ ...review, comments: review.comments.filter((item) => item.id !== comment.id) })
    } catch (err) {
      setActionError(messageOf(err))
    }
  }

  // 사진 붙이기 · 바꾸기
  const handlePickFile = async (e) => {
    const picked = e.target.files[0]
    e.target.value = '' // 같은 파일을 다시 골라도 onChange 가 불리게 비운다

    if (!picked) {
      return
    }
    if (picked.size > 5 * 1024 * 1024) {
      setFileError('5MB 이하 사진만 올릴 수 있어요')
      return
    }

    setFileError('')
    setFileBusy(true)
    try {
      const file = await uploadReviewFile(reviewId, picked)
      setReview({ ...review, file: file })
    } catch (err) {
      setFileError(messageOf(err))
    } finally {
      setFileBusy(false)
    }
  }

  // 사진 지우기
  const handleRemoveFile = async () => {
    setFileError('')
    setFileBusy(true)
    try {
      await removeReviewFile(reviewId)
      setReview({ ...review, file: null })
    } catch (err) {
      setFileError(messageOf(err))
    } finally {
      setFileBusy(false)
    }
  }

  if (error || (!loading && !review)) {
    return <div className="card"><ErrorMessage message={error || '글을 찾을 수 없습니다.'} /><Link to="/reviews">목록으로</Link></div>
  }
  if (loading) {
    return <Loading message="글을 불러오는 중..." />
  }


  return (
    <div className="review">
      <p className="muted review__back">
        <Link to="/reviews">← 레시피 추천</Link>
      </p>

      <article className="card">
        {editing ? (
          <form className="review__edit" onSubmit={handleEditSave}>
            <label className="review__edit-field">
              <span>제목</span>
              <input
                type="text"
                value={editTitle}
                onChange={(e) => setEditTitle(e.target.value)}
                maxLength={200}
              />
            </label>
            <label className="review__edit-field">
              <span>내용</span>
              <textarea
                value={editContent}
                onChange={(e) => setEditContent(e.target.value)}
                rows={12}
                maxLength={2000}
              />
            </label>
            <div className="review__edit-actions">
              <button type="submit" className="btn btn--sm">
                저장
              </button>
              <button
                type="button"
                className="btn btn--ghost btn--sm"
                onClick={() => setEditing(false)}
              >
                취소
              </button>
            </div>
          </form>
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
            {/* 사진은 글에 붙는 것이라 글의 is_mine 을 넘긴다.
                  댓글의 is_mine 과는 아무 상관이 없다 */}
            <ReviewAttachment
              file={review.file}
              isMine={review.is_mine}
              busy={fileBusy}
              error={fileError}
              onPick={handlePickFile}
              onRemove={handleRemoveFile}
            />
          </>
=======
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
>>>>>>> develop
        )}

        <ErrorMessage message={actionError} />

        <div className="review__actions">
          <Link to="/reviews" className="btn btn--ghost btn--sm">
            목록
          </Link>

          {/* 내가 쓴 글일 때만 수정 · 삭제. 여기는 글의 is_mine 이다 —
                이 글에 달린 댓글들의 is_mine 과는 따로 논다.
                수정 중(editing)에는 폼 안에 저장 · 취소가 따로 있으니 숨긴다 */}
          {review.is_mine && !editing && (
            <span className="review__owner-actions">
              <button type="button" className="btn btn--ghost btn--sm" onClick={handleEditOpen}>
                수정
              </button>
              <button
                type="button"
                className="btn btn--ghost btn--sm review__danger"
                onClick={() => setConfirmOpen(true)}
              >
                삭제
              </button>
            </span>
          )}
        </div>
      </article>

      <section className="card">
        <h3 className="section-title">댓글 {review.comments.length}</h3>
        {review.comments.length === 0 && <p className="empty">첫 댓글을 남겨보세요.</p>}
        {/* 댓글마다 자기 is_mine 을 달고 온다. 그래서 로그인한 사람을 따로
            넘기지 않아도 ReviewCommentItem 이 그 댓글의 버튼을 정할 수 있다.
            key 는 화면을 다시 그릴 때 리액트가 어느 줄이 그대로인지 알아보는
            표식이다 — 댓글 하나를 지웠을 때 엉뚱한 줄이 지워지지 않게 한다 */}
        {review.comments.length > 0 && (
          <ul className="review__comments divided">
            {review.comments.map((comment) => (
              <ReviewCommentItem
                key={comment.id}
                comment={comment}
                onEdit={handleCommentEdit}
                onDelete={handleCommentDelete}
              />
            ))}
          </ul>
        )}
        {/* 댓글 입력칸은 로그인한 사람에게만. 여기는 is_mine 이 아니라 user 를
            본다 — 댓글은 남의 글에도 달 수 있어서 "내 글인지" 와 무관하다 */}
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
      {confirmOpen && (
        <Modal onClose={() => setConfirmOpen(false)}>
          <h3 className="modal__title">이 글을 지울까요?</h3>
          <p className="muted">달린 댓글과 사진도 같이 보이지 않게 됩니다.</p>
          <div className="modal__actions">
            <button type="button" className="btn btn--ghost btn--sm" onClick={() => setConfirmOpen(false)}>
              취소
            </button>
            <button type="button" className="btn btn--sm" onClick={handleDelete}>
              삭제하기
            </button>
          </div>
        </Modal>
      )}
    </div>
  )
}
