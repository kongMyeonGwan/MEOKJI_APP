import { useState } from 'react'
import { Link, useParams } from 'react-router-dom'
import { useAuth } from '../contexts/AuthContext'
import { MOCK_COMMENTS, MOCK_POSTS } from '../mocks'
import { formatDateTime } from '../utils/format'
import '../styles/pages/PostDetailPage.css'

/**
 * 게시글 상세 + 좋아요 + 댓글 (STEP 21~23).
 *
 * 조회는 Guest도 가능하다. 좋아요와 댓글 작성은 로그인이 필요하다.
 */
export default function PostDetailPage() {
  const { postId } = useParams()
  // key를 주면 postId가 바뀔 때 컴포넌트가 새로 마운트된다.
  // 이게 없으면 다른 글로 이동해도 좋아요/댓글 state가 이전 글 것으로 남는다.
  // useState의 초기값은 첫 렌더에서 한 번만 계산되기 때문이다.
  return <PostDetail key={postId} postId={postId} />
}

function PostDetail({ postId }) {
  const { isAuthenticated, user } = useAuth()

  const post = MOCK_POSTS.find((p) => String(p.id) === postId)

  const [liked, setLiked] = useState(false)
  const [comments, setComments] = useState(
    MOCK_COMMENTS.filter((c) => String(c.post_id) === postId)
  )
  const [draft, setDraft] = useState('')

  if (!post) {
    return (
      <div className="card">
        <p className="empty">
          게시글을 찾을 수 없습니다.
          <br />
          <Link to="/community" className="post__link">목록으로 →</Link>
        </p>
      </div>
    )
  }

  // 서버 연동 전이라 화면에서만 반영한다.
  // 실제로는 POST/DELETE 후 다시 조회하거나 응답값으로 갱신한다.
  const onToggleLike = () => setLiked((prev) => !prev)

  const onSubmitComment = (e) => {
    e.preventDefault()
    const content = draft.trim()
    if (!content) return
    setComments((prev) => [
      ...prev,
      {
        id: Date.now(),
        post_id: post.id,
        nickname: user?.nickname ?? '나',
        content,
        created_at: new Date().toISOString(),
      },
    ])
    setDraft('')
  }

  const likeCount = post.like_count + (liked ? 1 : 0)

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

        <div className="post__actions">
          <button
            type="button"
            className={`post__like ${liked ? 'is-on' : ''}`}
            onClick={onToggleLike}
            disabled={!isAuthenticated}
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
              <button type="submit" className="btn btn--sm" disabled={!draft.trim()}>
                등록
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
