import { useRef, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import RecipeView from '../../components/recipe/RecipeView'
import ReviewAttachment from '../../components/community/ReviewAttachment'
import { useAuth } from '../../contexts/AuthContext'
import { SAMPLE_REVIEW_DETAIL } from '../../data/sampleBoard'
import { formatDateTime } from '../../utils/format'
import '../../styles/pages/ReviewDetailPage.css'

// 게시글 상세 + 좋아요 + 댓글.
//
// 로그인 안 해도 볼 수 있다.
// 좋아요, 댓글, 수정, 삭제만 로그인이 필요하고
// 수정이랑 삭제는 내가 쓴 것만 된다. 서버가 다시 확인한다.
//
// ! 아직 서버에 붙어 있지 않다. 담당자가 채운다.
//
// 지금은 src/data/sampleBoard.js 의 하드코딩 데이터를 그린다.
// 주소의 :reviewId 가 몇이든 같은 글이 나온다.
//
// 붙일 때 할 일
// -------------
//   1. useEffect 로 getReview(reviewId) 를 부르고 review · comments 에 넣는다
//   2. 같은 자리에서 countReviewView(reviewId) 도 한 번 부른다 (조회수 +1)
//   3. 아래 onToggleLike · onSubmitComment · onEditComment · onRemoveComment ·
//      onSaveEdit · onRemovePost · onPickFile · onRemoveFile 의 TODO 를 채운다
//   4. 좋아요는 화면을 먼저 바꾸고 서버에 보내는 편이 낫다(낙관적 갱신).
//      하트는 바로 반응해야 눌린 느낌이 나고, 실패하면 되돌리면 된다.
export default function ReviewDetailPage() {
  const { reviewId } = useParams() // 주소의 /community/:reviewId 부분
  return <PostDetail key={reviewId} reviewId={reviewId} />
}

function PostDetail({ reviewId }) {
  const { isAuthenticated } = useAuth()

  // TODO: 서버에서 받아오게 바꾼다. 지금은 하드코딩 데이터다.
  //   const [review, setReview] = useState(null)
  //   const [comments, setComments] = useState([])
  //   useEffect(() => { getReview(reviewId).then(...) }, [reviewId])
  const review = SAMPLE_REVIEW_DETAIL
  const comments = SAMPLE_REVIEW_DETAIL.comments

  // 좋아요
  const [liked, setLiked] = useState(SAMPLE_REVIEW_DETAIL.liked)
  const [likeCount, setLikeCount] = useState(SAMPLE_REVIEW_DETAIL.like_count)

  // 댓글 작성
  const [draft, setDraft] = useState('')

  // 글 수정
  const [editing, setEditing] = useState(false)
  const [editTitle, setEditTitle] = useState(SAMPLE_REVIEW_DETAIL.title)
  const [editContent, setEditContent] = useState(SAMPLE_REVIEW_DETAIL.content)

  // 사진
  const fileInput = useRef(null)

  // 하트를 누르면 실행된다.
  //
  // TODO: setReviewLike(review.id, nextLiked) 를 부르고
  //   서버가 돌려준 { liked, like_count } 로 덮어쓴다.
  //   지금은 화면 숫자만 바꾼다 (새로고침하면 돌아온다).
  function onToggleLike() {
    const nextLiked = !liked
    setLiked(nextLiked)

    if (nextLiked) {
      setLikeCount(likeCount + 1)
    } else {
      setLikeCount(likeCount - 1)
    }
  }

  // 댓글 등록 버튼을 누르면 실행된다
  function onSubmitComment(e) {
    e.preventDefault() // 폼이 새로고침되는 걸 막는다

    const content = draft.trim()
    if (!content) {
      return
    }

    // TODO: addComment(review.id, content) 를 부르고
    //   서버가 준 댓글을 목록 맨 뒤에 붙인다. 댓글은 쓴 순서대로 보여준다.
    window.alert('댓글 기능은 아직 준비 중입니다.')
    setDraft('')
  }

  // 댓글을 고친다
  function onEditComment(comment, content) {
    // TODO: updateComment(review.id, comment.id, content)
    window.alert('댓글 기능은 아직 준비 중입니다.')
  }

  // 댓글을 지운다
  function onRemoveComment(comment) {
    const ok = window.confirm('댓글을 지울까요?')
    if (!ok) {
      return
    }

    // TODO: removeComment(review.id, comment.id)
    window.alert('댓글 기능은 아직 준비 중입니다.')
  }

  // 글 수정 저장
  function onSaveEdit(e) {
    e.preventDefault()

    // TODO: updateReview(review.id, { title, content })
    window.alert('게시판 기능은 아직 준비 중입니다.')
    setEditing(false)
  }

  // 글 삭제. 지우고 나면 볼 화면이 없어서 목록으로 보낸다.
  function onRemovePost() {
    const ok = window.confirm(
      '이 글을 지울까요?\n달린 댓글도 함께 보이지 않게 됩니다.',
    )
    if (!ok) {
      return
    }

    // TODO: removeReview(review.id) 하고 navigate('/community')
    window.alert('게시판 기능은 아직 준비 중입니다.')
  }

  // 사진 붙이기 / 갈아 끼우기
  function onPickFile(e) {
    // TODO: uploadReviewFile(review.id, picked) 를 부르고
    //   서버가 돌려준 게시글로 통째로 갈아 끼운다.
    window.alert('사진 첨부는 아직 준비 중입니다.')

    // 같은 파일을 또 골라도 onChange 가 돌게 입력칸을 비운다
    if (fileInput.current) {
      fileInput.current.value = ''
    }
  }

  // 사진 떼기
  function onRemoveFile() {
    const ok = window.confirm('첨부한 사진을 지울까요?')
    if (!ok) {
      return
    }

    // TODO: removeReviewFile(review.id)
    window.alert('사진 첨부는 아직 준비 중입니다.')
  }

  let likeClass = 'review__like'
  if (liked) {
    likeClass = 'review__like is-on'
  }

  let likeTitle = '로그인이 필요합니다'
  if (isAuthenticated) {
    likeTitle = ''
  }

  // 글 수정을 취소했을 때 원래 값으로 되돌린다
  function cancelEdit() {
    setEditTitle(review.title)
    setEditContent(review.content)
    setEditing(false)
  }

  return (
    <div className="review">
      <p className="muted review__back">
        <Link to="/community">← 커뮤니티</Link>
      </p>

      <article className="card">
        {editing ? (
          <form className="review__edit" onSubmit={onSaveEdit}>
            <label className="review__edit-field">
              <span>제목</span>
              <input
                type="text"
                value={editTitle}
                onChange={(e) => setEditTitle(e.target.value)}
                maxLength={200}
                required
              />
            </label>

            <label className="review__edit-field">
              <span>내용</span>
              <textarea
                value={editContent}
                onChange={(e) => setEditContent(e.target.value)}
                rows={12}
                required
              />
            </label>

            <div className="review__edit-actions">
              <button type="submit" className="btn btn--sm">
                저장
              </button>
              <button
                type="button"
                className="btn btn--ghost btn--sm"
                onClick={cancelEdit}
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
              <span className="muted">{formatDateTime(review.created_at)}</span>
              <span className="muted">조회 {review.view_count}</span>
              {review.food_name && <span className="tag">{review.food_name}</span>}
            </div>

            {/* 줄바꿈을 살리기 위해 white-space: pre-wrap 을 쓴다 */}
            <div className="review__content">{review.content}</div>

            {/* 본문 바로 아래, 레시피보다 위에 둔다.
                "이렇게 만들어 먹었다" 는 사진이 후기의 본론이고
                레시피는 그 근거로 뒤따라오는 순서다. */}
            <ReviewAttachment
              file={review.file}
              isMine={review.is_mine}
              busy={false}
              error=""
              inputRef={fileInput}
              onPick={onPickFile}
              onRemove={onRemoveFile}
            />
          </>
        )}

        {/* 글쓴이가 확정했을 때의 레시피. 서버가 원본 기록에서 가져와 준다 */}
        {review.recipe_json && (
          <div className="review__recipe">
            <RecipeView
              recipe={review.recipe_json}
              ingredients={review.ingredients_json}
            />
          </div>
        )}

        <div className="review__actions">
          <button
            type="button"
            className={likeClass}
            onClick={onToggleLike}
            disabled={!isAuthenticated}
            title={likeTitle}
          >
            ♥ {likeCount}
          </button>
          {!isAuthenticated && (
            <span className="muted review__hint">
              <Link to="/login" className="review__link">
                로그인
              </Link>{' '}
              후 좋아요를 누를 수 있습니다
            </span>
          )}

          {/* 내 글에만 나오는 버튼. 서버도 남의 글이면 404로 막는다 */}
          {review.is_mine && !editing && (
            <span className="review__owner-actions">
              <button
                type="button"
                className="btn btn--ghost btn--sm"
                onClick={() => setEditing(true)}
              >
                수정
              </button>
              <button
                type="button"
                className="btn btn--ghost btn--sm review__danger"
                onClick={onRemovePost}
              >
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
            {comments.map(function (comment) {
              return (
                <Comment
                  key={comment.id}
                  comment={comment}
                  onEdit={onEditComment}
                  onRemove={onRemoveComment}
                />
              )
            })}
          </ul>
        )}

        {isAuthenticated ? (
          <form className="review__form" onSubmit={onSubmitComment}>
            <textarea
              value={draft}
              onChange={(e) => setDraft(e.target.value)}
              placeholder="댓글을 입력하세요"
              maxLength={1000}
              rows={3}
            />
            <div className="review__form-foot">
              <span className="muted">{draft.length} / 1000</span>
              <button
                type="submit"
                className="btn btn--sm"
                disabled={!draft.trim()}
              >
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

// 댓글 한 줄.
//
// 따로 컴포넌트로 뺀 이유는 "지금 고치는 중인가" 를 댓글마다 따로
// 들고 있어야 해서다. 부모가 들고 있으면 어느 댓글을 고치는 중인지
// id 로 비교하는 코드가 여기저기 생긴다.
function Comment({ comment, onEdit, onRemove }) {
  const [editing, setEditing] = useState(false)
  const [draft, setDraft] = useState(comment.content)

  function onSubmit(e) {
    e.preventDefault()

    const content = draft.trim()
    if (!content) {
      return
    }

    onEdit(comment, content)
    setEditing(false)
  }

  // 고치던 내용을 버리고 원래대로 되돌린다
  function cancel() {
    setDraft(comment.content)
    setEditing(false)
  }

  return (
    <li>
      <div className="review__comment-head">
        <strong>{comment.nickname}</strong>
        <span className="muted">{formatDateTime(comment.created_at)}</span>

        {/* 내 댓글에만 나오는 버튼. 서버도 남의 댓글이면 404로 막는다 */}
        {comment.is_mine && !editing && (
          <span className="review__comment-actions">
            <button type="button" onClick={() => setEditing(true)}>
              수정
            </button>
            <button
              type="button"
              className="review__danger"
              onClick={() => onRemove(comment)}
            >
              삭제
            </button>
          </span>
        )}
      </div>

      {editing ? (
        <form className="review__form" onSubmit={onSubmit}>
          <textarea
            value={draft}
            onChange={(e) => setDraft(e.target.value)}
            maxLength={1000}
            rows={3}
          />
          <div className="review__form-foot">
            <span className="muted">{draft.length} / 1000</span>
            <span className="review__edit-actions">
              <button type="submit" className="btn btn--sm">
                저장
              </button>
              <button
                type="button"
                className="btn btn--ghost btn--sm"
                onClick={cancel}
              >
                취소
              </button>
            </span>
          </div>
        </form>
      ) : (
        <p className="review__comment-body">{comment.content}</p>
      )}
    </li>
  )
}
