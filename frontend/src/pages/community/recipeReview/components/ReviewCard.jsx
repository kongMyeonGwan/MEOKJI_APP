import { Link } from 'react-router-dom'
import { formatDate } from '../../../../utils/format'

// 게시글 목록의 한 줄
export default function ReviewCard({ review }) {
  return (
    <article className="review-card">
      <Link to={`/community/${review.id}`} className="review-card__link">
        <h3 className="review-card__title">
          {review.title}
          {review.hasFile && <span className="review-card__clip" title="사진 있음">📎</span>}
        </h3>
        <p className="muted review-card__excerpt">{review.content}</p>
      </Link>

      <footer className="review-card__meta">
        {review.foodName && <span className="tag">{review.foodName}</span>}
        <span className="muted">{review.nickname}</span>
        <span className="muted">{formatDate(review.createdAt)}</span>
        <span className="muted review-card__counts">
          ♥ {review.likeCount} · 💬 {review.commentCount} · 조회 {review.viewCount}
        </span>
      </footer>
    </article>
  )
}
