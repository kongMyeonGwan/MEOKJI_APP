import { useEffect, useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import Loading from '../components/common/Loading'
import RecipeView from '../components/recipe/RecipeView'
import { useAuth } from '../contexts/AuthContext'
import ErrorMessage from '../components/common/ErrorMessage'
import { addComment, getPost, setPostLike } from '../api/posts'
import { parseApiError } from '../utils/apiError'
import { formatDateTime } from '../utils/format'
import '../styles/pages/PostDetailPage.css'

/**
 * 게시글 상세 + 좋아요 + 댓글 (STEP 21~23).
 *
 * 조회는 Guest도 가능하다. 좋아요와 댓글 작성은 로그인이 필요하다.
 *
 * 레시피/재료는 posts 테이블에 복사돼 있는 스냅샷을 그대로 보여준다.
 * meal_histories 를 JOIN하지 않는다 — 남의 개인 기록이기 때문이다.
 */
export default function PostDetailPage() {
  const { postId } = useParams()
  // key를 주면 postId가 바뀔 때 컴포넌트가 새로 마운트된다.
  // 이게 없으면 다른 글로 이동해도 좋아요/댓글 state가 이전 글 것으로 남는다.
  // useState의 초기값은 첫 렌더에서 한 번만 계산되기 때문이다.
  return <PostDetail key={postId} postId={postId} />
}

function PostDetail({ postId }) {
  const { isAuthenticated } = useAuth()

  const [post, setPost] = useState(null)
  const [comments, setComments] = useState([])
  const [loading, setLoading] = useState(true)
  const [loadError, setLoadError] = useState('')

  const [liked, setLiked] = useState(false)
  const [likeCount, setLikeCount] = useState(0)
  const [likeBusy, setLikeBusy] = useState(false)

  const [draft, setDraft] = useState('')
  const [commentBusy, setCommentBusy] = useState(false)
  const [actionError, setActionError] = useState('')

  useEffect(() => {
    let alive = true

    getPost(postId)
      .then((data) => {
        if (!alive) return
        setPost(data)
        setComments(data.comments)
        setLiked(data.liked)
        setLikeCount(data.like_count)
      })
      .catch((err) => {
        if (alive) setLoadError(parseApiError(err).message)
      })
      .finally(() => {
        if (alive) setLoading(false)
      })

    return () => {
      alive = false
    }
  }, [postId])

  if (loading) {
    return (
      <div className="card">
        <Loading message="게시글을 불러오는 중..." />
      </div>
    )
  }

  if (loadError || !post) {
    return (
      <div className="card">
        <p className="empty">
          {loadError || '게시글을 찾을 수 없습니다.'}
          <br />
          <Link to="/community" className="post__link">목록으로 →</Link>
        </p>
      </div>
    )
  }

  const onToggleLike = async () => {
    const next = !liked

    // 낙관적 갱신. 하트는 즉시 반응해야 눌린 느낌이 난다.
    setLiked(next)
    setLikeCount((n) => n + (next ? 1 : -1))
    setActionError('')
    setLikeBusy(true)

    try {
      // 서버가 세어 준 값으로 덮어쓴다. 그 사이 남이 누른 것까지 반영된다.
      const result = await setPostLike(post.id, next)
      setLiked(result.liked)
      setLikeCount(result.like_count)
    } catch (err) {
      // 실패하면 낙관적 갱신을 되돌린다.
      setLiked(!next)
      setLikeCount((n) => n - (next ? 1 : -1))
      setActionError(parseApiError(err).message)
    } finally {
      setLikeBusy(false)
    }
  }

  const onSubmitComment = async (e) => {
    e.preventDefault()
    const content = draft.trim()
    if (!content) return

    setActionError('')
    setCommentBusy(true)
    try {
      // 서버가 돌려준 행을 그대로 붙인다. 목록 정렬이 작성 순이라 맨 뒤가 맞다.
      const created = await addComment(post.id, content)
      setComments((prev) => [...prev, created])
      setDraft('')
    } catch (err) {
      setActionError(parseApiError(err).message)
    } finally {
      setCommentBusy(false)
    }
  }

  return (
    <div className="post">
      <p className="muted post__back">
        <Link to="/community">← 커뮤니티</Link>
      </p>

      <article className="card">
        <h2 className="post__title">{post.title}</h2>

        <div className="post__meta">
          <span>{post.nickname}</span>
          <span className="muted">{formatDateTime(post.created_at)}</span>
          <span className="muted">조회 {post.view_count}</span>
          {post.food_name && <span className="tag">{post.food_name}</span>}
        </div>

        {/* 줄바꿈을 살리기 위해 white-space: pre-wrap 을 쓴다 */}
        <div className="post__content">{post.content}</div>

        {/* 확정 당시 복사해 둔 레시피 스냅샷 */}
        {post.recipe_json && (
          <div className="post__recipe">
            <RecipeView
              recipe={post.recipe_json}
              ingredients={post.ingredients_json}
            />
          </div>
        )}

        <div className="post__actions">
          <button
            type="button"
            className={`post__like ${liked ? 'is-on' : ''}`}
            onClick={onToggleLike}
            disabled={!isAuthenticated || likeBusy}
            title={isAuthenticated ? '' : '로그인이 필요합니다'}
          >
            ♥ {likeCount}
          </button>
          {!isAuthenticated && (
            <span className="muted post__hint">
              <Link to="/login" className="post__link">로그인</Link> 후 좋아요를 누를 수 있습니다
            </span>
          )}
        </div>

        <ErrorMessage message={actionError} />
      </article>

      <section className="card">
        <h3 className="section-title">댓글 {comments.length}</h3>

        {comments.length === 0 ? (
          <p className="empty">첫 댓글을 남겨보세요.</p>
        ) : (
          <ul className="post__comments divided">
            {comments.map((comment) => (
              <li key={comment.id}>
                <div className="post__comment-head">
                  <strong>{comment.nickname}</strong>
                  <span className="muted">{formatDateTime(comment.created_at)}</span>
                </div>
                <p className="post__comment-body">{comment.content}</p>
              </li>
            ))}
          </ul>
        )}

        {isAuthenticated ? (
          <form className="post__form" onSubmit={onSubmitComment}>
            <textarea
              value={draft}
              onChange={(e) => setDraft(e.target.value)}
              placeholder="댓글을 입력하세요"
              maxLength={1000}
              rows={3}
            />
            <div className="post__form-foot">
              <span className="muted">{draft.length} / 1000</span>
              <button
                type="submit"
                className="btn btn--sm"
                disabled={!draft.trim() || commentBusy}
              >
                {commentBusy ? '등록 중...' : '등록'}
              </button>
            </div>
          </form>
        ) : (
          <p className="muted post__login-ask">
            <Link to="/login" className="post__link">로그인</Link> 후 댓글을 작성할 수 있습니다.
          </p>
        )}
      </section>
    </div>
  )
}
