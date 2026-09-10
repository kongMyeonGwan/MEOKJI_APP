import { Link } from 'react-router-dom'
import { formatDate } from '../../utils/format'
import '../../styles/components/community/PostCard.css'

/** 게시글 목록의 한 줄. */
export default function PostCard({ post }) {
  return (
    <article className="post-card">
      <Link to={`/community/${post.id}`} className="post-card__link">
        <h3 className="post-card__title">
          {post.title}
          {/* 사진이 붙은 글임을 알린다. 서버는 목록에 파일 정보를 싣지 않고
              붙었는지 여부(has_file)만 내려준다 — 목록에서 쓸 값이 그것뿐이다. */}
          {post.has_file && (
            <span className="post-card__clip" title="사진 있음" aria-label="사진 있음">
              📎
            </span>
          )}
        </h3>

        {/* 본문 미리보기. 줄바꿈은 CSS로 2줄까지만 보여준다. */}
        <p className="muted post-card__excerpt">{post.content}</p>
      </Link>

      <footer className="post-card__meta">
        {post.food_name && <span className="tag">{post.food_name}</span>}
        <span className="muted">{post.nickname}</span>
        <span className="muted">{formatDate(post.created_at)}</span>
        <span className="muted post-card__counts">
          ♥ {post.like_count} · 💬 {post.comment_count} · 조회 {post.view_count}
        </span>
      </footer>
    </article>
  )
}
