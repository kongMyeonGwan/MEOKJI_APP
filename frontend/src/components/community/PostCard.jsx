import { Link } from 'react-router-dom'
import { formatDate } from '../../utils/format'
import '../../styles/components/community/PostCard.css'

/** 게시글 목록의 한 줄. */
export default function PostCard({ post }) {
  return (
    <article className="post-card">
      <Link to={`/community/${post.id}`} className="post-card__link">
        <h3 className="post-card__title">{post.title}</h3>

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
