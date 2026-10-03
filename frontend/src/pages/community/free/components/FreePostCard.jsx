import { Link } from 'react-router-dom'
import { formatDate } from '../../../../utils/format'

// 자유게시판 목록의 한 줄.
// 레시피 후기와 달리 음식 이름 · 좋아요가 없어서 조회수와 댓글 수만 보여준다.
export default function FreePostCard({ post }) {
  return (
    <article className="free-card">
      <Link to={`/free/${post.id}`} className="free-card__link">
        <h3 className="free-card__title">
          {post.title}
          {post.has_file && (
            <span className="free-card__clip" title="사진 있음">
              📎
            </span>
          )}
          {post.comment_count > 0 && <span className="free-card__comments">[{post.comment_count}]</span>}
        </h3>
        <p className="muted free-card__excerpt">{post.content}</p>
      </Link>

      <footer className="free-card__meta">
        <span className="muted">{post.nickname}</span>
        <span className="muted">{formatDate(post.created_at)}</span>
        <span className="muted free-card__counts">
          💬 {post.comment_count} · 조회 {post.view_count}
        </span>
      </footer>
    </article>
  )
}
