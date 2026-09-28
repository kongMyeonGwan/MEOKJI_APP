import { Link } from 'react-router-dom'
import { formatDate } from '../../utils/format'
import '../../styles/components/community/ReviewCard.css'

// 게시글 목록의 한 줄
export default function ReviewCard({ review }) {
  return (
    <article className="review-card">
      <Link to={'/community/' + review.id} className="review-card__link">
        <h3 className="review-card__title">
          {review.title}
          {/* 사진이 붙은 글이라고 알려준다.
              목록에서는 파일 정보까지 필요 없어서 서버가 has_file 만 준다. */}
          {review.has_file && (
            <span
              className="review-card__clip"
              title="사진 있음"
              aria-label="사진 있음"
            >
              📎
            </span>
          )}
        </h3>

        {/* 본문 미리보기. 2줄까지만 보이게 CSS 로 잘랐다. */}
        <p className="muted review-card__excerpt">{review.content}</p>
      </Link>

      <footer className="review-card__meta">
        {review.food_name && <span className="tag">{review.food_name}</span>}
        <span className="muted">{review.nickname}</span>
        <span className="muted">{formatDate(review.created_at)}</span>
        <span className="muted review-card__counts">
          ♥ {review.like_count} · 💬 {review.comment_count} · 조회 {review.view_count}
        </span>
      </footer>
    </article>
  )
}
